/*
 * see license.txt
 */
package lita.vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lita.vm.AssemblerParser.AssemblerInstruction;

/**
 * The Assembler reads in assembly language and compiles it down to machine {@link Bytecode}.
 * 
 * @author Tony
 *
 */
public class Assembler {
    
    private List<Integer> instrs;
    private List<Number> numPool;
    private List<String> strPool;
    
    private Map<String, Integer> labels;
    private Map<Integer, AssemblerInstruction> pendingInstructions;
    private Map<String, Integer> constants;
    
    private Map<String, Integer> registers;
    
    private AssemblerParser parser;
    private LitaVM vm;
    
    /**
     * @param vm
     */
    public Assembler(LitaVM vm) {
        this.vm = vm;
        
        this.instrs = new ArrayList<>();
        
        this.numPool = new ArrayList<>();
        this.strPool = new ArrayList<>();
        
        this.constants = new HashMap<>();
        
        this.labels = new HashMap<>();
        this.pendingInstructions = new HashMap<>();
        
        this.registers = new HashMap<>();
        
        Register[] regs = vm.getCpu().getRegisters();
        for(int i = 0; i < regs.length; i++) {
            Register r = regs[i];
            registers.put(r.getName(), i);
        }
        
        this.parser = new AssemblerParser(registers, constants);
    }
    
    
    /**
     * Converts the supplied assembly line into the equivalent machine code line
     * 
     * @param args
     */
    private void addInstruction(AssemblerInstruction instr) {
        
        // if any of the arguments starts with a label marker,
        // mark this as a pending instruction, so that we can
        // reconcile all of the labels
        if(instr.args.stream().anyMatch(a -> a.startsWith(":"))) {
            addInstruction(0);
            
            this.pendingInstructions.put(this.instrs.size() - 1, instr);
        }
        else {            
            int instruction = this.parser.parseInstruction(instr);
            addInstruction(instruction);
        }
    }
    
    private void addInstruction(int instruction) {
        this.instrs.add(instruction);
    }
       
    
    /**
     * Reconciles any labels.  This substitutes any instructions
     * that contain a label with the actual instruction index.
     */
    private void reconcileLabels() {
        this.pendingInstructions.entrySet().forEach( entry -> {
            AssemblerInstruction instr = entry.getValue();
            List<String> args = instr.args;
            String arg1 = args.get(1);
            String arg2 = null;
            if(args.size() > 2) {
                arg2 = args.get(2);
            }
            
            if(arg1.startsWith(":")) {
                arg1 = "#" + this.labels.get(arg1);                
            }
            
            if(arg2 != null && arg2.startsWith(":")) {
                arg2 = "#" + this.labels.get(arg2);                
            }            
            
            int instruction = this.parser.parseInstruction(instr, arg1, arg2);                        
            this.instrs.set(entry.getKey(), instruction );
        });
    }
        
    private int[] buildConstants(List<AssemblerInstruction> parsedLines) {
        class ConstantEntry {
            String constantName;
            int index;
            boolean isNumber;
            
            ConstantEntry(String constantName, int index, boolean isNumber) {
                this.constantName = constantName;
                this.index = index;
                this.isNumber = isNumber;
            }
        }
        
        List<ConstantEntry> constantEntries = new ArrayList<>();
        
        // Build up the constant pools
        for(AssemblerInstruction instr: parsedLines) {
            List<String> args = instr.args;
            if(!args.isEmpty()) {
                String opcode = args.get(0);
                if(opcode != null && opcode.startsWith(".")) {
                    if(args.size() < 2) {
                        throw this.parser.parseError(instr, "Illegal constant expression: '" + args + "'");                        
                    }
                    
                    int index = 0;
                    
                    String arg = args.get(1);
                    try {
                        
                        Number value = args.contains(".") ? Float.parseFloat(arg) : Integer.parseInt(arg);
                        index = numPool.indexOf(value);
                        if(index < 0) {
                            numPool.add(value);
                            index = numPool.size();
                        }
                        
                        constantEntries.add(new ConstantEntry(opcode, index, true));
                    }
                    catch(NumberFormatException e) {
                        index = strPool.indexOf(arg);
                        if(index < 0) {
                            strPool.add(arg);
                            index = strPool.size();
                        }
                        
                        constantEntries.add(new ConstantEntry(opcode, index, false));
                    }
                    
                }
            }
        }
        
        // Build out the constant mappings (constant name => constant pool index)
        constantEntries.stream().filter(c ->  c.isNumber)
                 .forEach(c -> this.constants.put(c.constantName, c.index - 1));
        
        constantEntries.stream().filter(c -> !c.isNumber)
                 .forEach(c -> this.constants.put(c.constantName, numPool.size() + c.index - 1));
        
        
        // Now Build out the mappings to the constant pool to RAM 
        {
            CPU32 cpu = vm.getCpu();
            RAM ram = vm.getRam();
            
            int ramAddress = 0;
            final int addressInc = cpu.getWordSize() / 8;
            
            int[] constants = new int[numPool.size() + strPool.size()];
            int index = 0;
            
            for(Number n : numPool) {
                if(n instanceof Float) {
                    ram.storeFloat(ramAddress, n.floatValue());
                }
                else {               
                    ram.storeInt(ramAddress, n.intValue());
                }
                
                constants[index++] = ramAddress;
                ramAddress += addressInc;
            }
            
            for(String str : strPool) {
                ram.storeStr(ramAddress, str);
                
                constants[index++] = ramAddress;
                ramAddress += (str.length() + 1); // strings are null terminated
            }
            
            // Mark the start of the Heap space
            cpu.getH().address(ramAddress);
            
            return constants;        
        }
    }
    
    
    private Bytecode compileBytecode(int[] constants) {
        int code[] = new int[this.instrs.size()];
        for(int i = 0; i < code.length; i++) {
            code[i] = this.instrs.get(i);
        }
        
        return new Bytecode(constants, code, 0 , code.length);
    }
    
    
    /**
     * Compiles the assembly into {@link Bytecode} to be run by the supplied
     * {@link VM}
     * 
     * @param asm
     * @return the {@link Bytecode}
     */
    public Bytecode compile(String assembly) {        
        List<AssemblerInstruction> parsedLines = this.parser.parse(assembly);
        
        final int[] constants = buildConstants(parsedLines);
        
        for(AssemblerInstruction instr : parsedLines) {
            List<String> args = instr.args;
            String opcode = args.get(0);
            if(opcode != null && !opcode.equals("")) {
                /* The opcode can be either a Label or Data constant
                 */
                
                // Label
                if(opcode.startsWith(":")) {
                    this.labels.put(opcode, this.instrs.size());
                }
                
                // Data Constant
                else if(opcode.startsWith(".")) {
                    continue;
                }
                
                // Actual opcode instruction
                else {                    
                    addInstruction(instr);
                }
            }
        }

        reconcileLabels();
        
        return compileBytecode(constants);
    }
}

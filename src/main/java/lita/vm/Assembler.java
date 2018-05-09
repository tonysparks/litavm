/*
 * see license.txt
 */
package lita.vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Assembler reads in assembly language and compiles it down to machine {@link Bytecode}.
 * 
 * @author Tony
 *
 */
public class Assembler {
    
    private static class ConstantEntry {
        String constantName;
        int index;
        boolean isNumber;
        
        ConstantEntry(String constantName, int index, boolean isNumber) {
            this.constantName = constantName;
            this.index = index;
            this.isNumber = isNumber;
        }
    }
    

    private List<Integer> instrs;
    private Set<Number> numPool;
    private Set<String> strPool;
    
    private Map<String, Integer> labels;
    private Map<Integer, List<String>> pendingInstructions;
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
        
        this.numPool = new HashSet<>();
        this.strPool = new HashSet<>();
        
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
    private void addInstruction(List<String> args) {
        
        // if any of the arguments starts with a label marker,
        // mark this as a pending instruction, so that we can
        // reconcile all of the labels
        if(args.stream().anyMatch(a -> a.startsWith(":"))) {
            addInstruction(0);
            
            this.pendingInstructions.put(this.instrs.size() - 1, args);
        }
        else {
            int opcode = this.parser.parseOpcode(args.get(0));
            int indexedOpcode = Instruction.opcode(opcode);
            if(Opcodes.JMP == indexedOpcode) {
                addInstruction(opcode, 0, this.parser.parseJumpArg(args.get(1)));
            }
            else {
                int numberOfArgs = Opcodes.numberOfArgs(indexedOpcode);
                int arg1 = 0, 
                    arg2 = 0;
                
                switch(numberOfArgs) {
                    case 0: 
                        break;
                    case 1: 
                        arg2 = this.parser.parseArg2(args.get(1)); 
                        break;
                    case 2:
                        if(args.size() < 3) {
                            throw new EvalException("Expected 2 opcode arguments: '" + args + "'");
                        }
                        
                        arg1 = this.parser.parseArg1(args.get(1));            
                        arg2 = this.parser.parseArg2(args.get(2));                    
                        break;
                }
                
                addInstruction(opcode, arg1, arg2);
            }
        }
    }
    
    private void addInstruction(int instruction) {
        this.instrs.add(instruction);
    }
    
    private void addInstruction(int opcode, int arg1, int arg2) {        
        addInstruction(opcode | arg1 | arg2);
    }
    
    
    /**
     * Reconciles any labels.  This substitutes any instructions
     * that contain a label with the actual instruction index.
     */
    private void reconcileLabels() {
        this.pendingInstructions.entrySet().forEach( entry -> {
            List<String> args = entry.getValue();
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
            
            int opcode = this.parser.parseOpcode(args.get(0));
            int indexedOpcode = Instruction.opcode(opcode);
            int parg1 = 0, 
                parg2 = 0;
            
            if(Opcodes.JMP == indexedOpcode) {
                parg2 = this.parser.parseJumpArg(arg1);                
            }
            else {
            
                if(Opcodes.numberOfArgs(Instruction.opcode(opcode)) == 2) {
                    parg1 = this.parser.parseArg1(arg1);
                    parg2 = 0;
                    if(args.size() > 2) {
                        parg2 = this.parser.parseArg2(arg2);
                    }    
                }
                else {
                    parg2 = this.parser.parseArg2(arg1);
                }
            }
                        
            this.instrs.set(entry.getKey(), opcode | parg1 | parg2 );
        });
    }
    
    private int[] buildConstants(List<List<String>> parsedLines) {
        List<ConstantEntry> constantEntries = new ArrayList<>();
        
        // Build up the constant pools
        for(List<String> args: parsedLines) {
            if(!args.isEmpty()) {
                String opcode = args.get(0);
                if(opcode != null && opcode.startsWith(".")) {
                    if(args.size() < 2) {
                        throw new EvalException("Illegal constant expression: '" + args + "'");
                    }
                    
                    String arg = args.get(1);
                    try {
                        
                        Number value = args.contains(".") ? Float.parseFloat(arg) : Integer.parseInt(arg);
                        numPool.add(value);
                        
                        constantEntries.add(new ConstantEntry(opcode, numPool.size(), true));
                    }
                    catch(NumberFormatException e) {
                        strPool.add(arg);
                        constantEntries.add(new ConstantEntry(opcode, strPool.size(), false));
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
            
            int ramAddress = cpu.getStackSize();
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
        List<List<String>> parsedLines = this.parser.parse(assembly);
        
        final int[] constants = buildConstants(parsedLines);
        
        for(List<String> args : parsedLines){
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
                    addInstruction(args);
                }
            }
        }

        reconcileLabels();
        
        return compileBytecode(constants);
    }
}

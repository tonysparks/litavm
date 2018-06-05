/*
 * see license.txt
 */
package lita.vm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Handles the parsing assembly files
 * 
 * @author Tony
 *
 */
public class AssemblerParser {

    public static class AssemblerInstruction {
        final public List<String> args;
        final int lineNumber;
        
        
        AssemblerInstruction(List<String> args, int lineNumber) {
            this.args = args;
            this.lineNumber = lineNumber;
        }
    }
    
    private Map<String, Integer> registers;
    private Map<String, Integer> constants;
    
    
    /**
     * @param registers
     * @param constants
     */
    public AssemblerParser(Map<String, Integer> registers, Map<String, Integer> constants) {
        this.registers = registers;
        this.constants = constants;
    }
    
    public ParserException parseError(AssemblerInstruction instruction, String message) {
        return new ParserException(message + " at line: " + instruction.lineNumber);
    }
    

    /**
     * Parses the OPCODE which should be the first argument in the assembly
     * 
     * @param arg0
     * @return the 32bit positioned opcode
     */
    private int parseOpcode(AssemblerInstruction instr, String arg0) {
        int opcode = Opcodes.strOpcode(arg0);
        if(opcode < 0) {
            throw parseError(instr, "Invalid opcode: '" + arg0 + "'");
        }
        
        return opcode << Instruction.ARG1_SIZE + Instruction.ARG2_SIZE; 
    }
    
    
    /**
     * Parses the argument 1 which can be in the format of:
     * 
     * Always a register, optionally be address value of register
     * 
     * @param arg1
     * @return the 32 bit positioned argument one
     */
    private int parseArg1(AssemblerInstruction instr, String arg1) {
        int instruction = 0;
        
        boolean isAddress = arg1.startsWith("&");
        if(isAddress) {
            instruction |= Instruction.ARG1_ADDR_MASK;
            if(arg1.length() < 3) {
                throw parseError(instr, "Invalid argument structure: '" + arg1 + "'");
            }
            arg1 = arg1.substring(1);
        }
        
        Integer registerNumber = registers.get(arg1.toLowerCase());
        if(registerNumber == null) {
            throw parseError(instr, "Invalid register name: '" + arg1 + "'");
        }
        instruction |= registerNumber;
        
        return instruction << Instruction.ARG2_SIZE;        
    }

    
    /**
     * Jump instructions are special in that they only accept a 24 bit immediate mode
     * number.
     * 
     * @param arg
     * @return the instruction
     */
    private int parseJumpArg(AssemblerInstruction instr, String arg) {
        if(!arg.startsWith("#")) {
            throw parseError(instr, "Invalid jump instruction argument, must be an immediate number: '" + arg + "'");
        }
        
        arg = arg.substring(1);
        int value = Integer.parseInt(arg);
        
        return value;  
    }
    
    /**
     * Parses the argument 2 which can be in the format of:
     * 
     * Can be a register, if register - optionally be address value of register
     * Can be an Immediate Value, in which case the actual integer value is used
     * Can be a Constant Value, in which case the constant index is used
     * 
     * @param arg2
     * @return the 32 bit positioned argument two
     */
    private int parseArg2(AssemblerInstruction instr, String arg2) {
        int instruction = 0;
        
        boolean isRegister = false;
        boolean isAddress = arg2.startsWith("&");
        if(isAddress) {
            isRegister = true;
            instruction |= Instruction.ARG2_ADDR_MASK;
            if(arg2.length() < 3) {
                throw parseError(instr, "Invalid argument structure: '" + arg2 + "'");
            }
            arg2 = arg2.substring(1);
        }
        
        Integer registerNumber = registers.get(arg2.toLowerCase());
        if(registerNumber == null) {
            if(isAddress) {
                throw parseError(instr, "Invalid register argument structure: '" + arg2 + "'");
            }

            if(arg2.startsWith("#")) {
                int base = 10;
                int offset = 1;
                
                if(arg2.startsWith("#0x")) {
                    base = 16;  // Hexidecimal format
                    offset = 3;
                }
                else if(arg2.startsWith("#0b")) {
                    base = 2;  // Binary format
                    offset = 3;
                }
                
                if(arg2.length() < (offset+1)) {
                    throw parseError(instr, "Invalid immediate value argument structure: '" + arg2 + "'");    
                }
                
                arg2 = arg2.substring(offset);
                int value = Integer.parseInt(arg2, base);
                if(value > Instruction.MAX_IMMEDIATE_VALUE) {
                    throw parseError(instr, "Invalid immediate value, above max value (" + Instruction.MAX_IMMEDIATE_VALUE + "): '" + value + "'");
                }
                
                instruction |= Instruction.ARG2_IMM_MASK;
                instruction |= value;                
            }
            else if(arg2.startsWith(".")) {
                Integer index = this.constants.get(arg2);
                if(index == null) {
                    throw parseError(instr, "No constant defined for '" + arg2 + "'");
                }
                
                instruction |= index;
            }
        }
        else {
            isRegister = true;
            instruction |= registerNumber;
        }
        
        if(isRegister) {
            instruction |= Instruction.ARG2_REG_MASK;
        }
        
        return instruction;
    }
    
    /**
     * Creates an argument list, stripping out any comments and combining any constant
     * strings into one argument
     * 
     * @param line
     * @return the argument list
     */
    private AssemblerInstruction parseLine(int lineNumber, String line) {
        StringBuilder sb = new StringBuilder(line.length());
        
        boolean inComment = false;
        boolean inString  = false;
        
        List<String> args = new ArrayList<>();
        
        for(int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if(c == ';') {
                if(!inString) {
                    inComment = true; 
                }
            }            
            else if(c == '"') {
                if(inString) {
                    inString = false;
                    continue;
                }
                else if(!inComment) {
                    inString = true;
                    continue;
                }
            }
            else if(c == ' ') {
                if(!inString && !inComment) {
                    args.add(sb.toString());
                    sb.setLength(0);
                    
                    continue;
                }
            }
            
            if(!inComment) {
                sb.append(c);
            }
        }
        
        args.add(sb.toString());
        
        return new AssemblerInstruction(args, lineNumber);
        
    }
    
    /**
     * Parses out the supplied arguments and forms a CPU machine code instruction
     * 
     * @param instr - the instruction to parse
     * @return the machine code instruction
     */
    public int parseInstruction(AssemblerInstruction instr) {
        String arg1 = null;
        String arg2 = null;
        
        switch(instr.args.size()) {
            case 0:
            case 1:
                break;
            case 2: 
                arg1 = instr.args.get(1);
                break;
            default:
                arg1 = instr.args.get(1);
                arg2 = instr.args.get(2);
                break;
        }
        
        return parseInstruction(instr, arg1, arg2);
    }
    
    /**
     * Parses out the supplied arguments and forms a CPU machine code instruction
     * 
     * @param instr - the instruction to parse
     * @param arg1 - optional first argument to override
     * @param arg2 - optional second argument to override
     * @return the machine code instruction
     */
    public int parseInstruction(AssemblerInstruction instr, String arg1, String arg2) {
        int opcode = parseOpcode(instr, instr.args.get(0));
        int indexedOpcode = Instruction.opcode(opcode);
        int parg1 = 0, 
            parg2 = 0;
        
        if(Opcodes.JMP == indexedOpcode || Opcodes.CALL == indexedOpcode) {
            parg2 = parseJumpArg(instr, arg1);                
        }
        else {
        
            if(Opcodes.numberOfArgs(Instruction.opcode(opcode)) == 2) {
                parg1 = parseArg1(instr, arg1);
                parg2 = 0;
                if(arg2 != null) {
                    parg2 = parseArg2(instr, arg2);
                }    
            }
            else if(arg1 != null) {
                parg2 = parseArg2(instr, arg1);
            }
        }
        
        return opcode | parg1 | parg2;                  
    }
    
    
    /**
     * Parses the assembly text
     * 
     * @param assembly
     * @return the parsed assembly text into instructions with arguments
     */
    public List<AssemblerInstruction> parse(String assembly) {
        // TODO: Account for strings with \n
        List<String> lines = Arrays.asList(assembly.split("\n"));
        
        List<AssemblerInstruction> parsedLines = new ArrayList<>();
        int lineNumber = 1;
        for(String line : lines) {
            AssemblerInstruction instr = parseLine(lineNumber++, line.trim());
            if(!instr.args.isEmpty()) {
                parsedLines.add(instr);
            }
        }
        
        return parsedLines;
    }

}

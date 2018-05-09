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
    
    private ParserException error(String message) {
        return new ParserException(message);
    }
    
    
    /**
     * Parses the OPCODE which should be the first argument in the assembly
     * 
     * @param arg0
     * @return the 32bit positioned opcode
     */
    public int parseOpcode(String arg0) {
        int opcode = Opcodes.strOpcode(arg0);
        if(opcode < 0) {
            throw error("Invalid opcode: '" + arg0 + "'");
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
    public int parseArg1(String arg1) {
        int instruction = 0;
        
        boolean isAddress = arg1.startsWith("&");
        if(isAddress) {
            instruction |= Instruction.ARG1_ADDR_MASK;
            if(arg1.length() < 3) {
                throw error("Invalid argument structure: '" + arg1 + "'");
            }
            arg1 = arg1.substring(1);
        }
        
        int registerNumber = registers.get(arg1.toLowerCase());
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
    public int parseJumpArg(String arg) {
        if(!arg.startsWith("#")) {
            throw error("Invalid jump instruction argument, must be an immediate number: '" + arg + "'");
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
    public int parseArg2(String arg2) {
        int instruction = 0;
        
        boolean isRegister = false;
        boolean isAddress = arg2.startsWith("&");
        if(isAddress) {
            isRegister = true;
            instruction |= Instruction.ARG2_ADDR_MASK;
            if(arg2.length() < 3) {
                throw error("Invalid argument structure: '" + arg2 + "'");
            }
            arg2 = arg2.substring(1);
        }
        
        Integer registerNumber = registers.get(arg2.toLowerCase());
        if(registerNumber == null) {
            if(isAddress) {
                throw error("Invalid register argument structure: '" + arg2 + "'");
            }

            if(arg2.startsWith("#")) {
                if(arg2.length() < 2) {
                    throw error("Invalid immediate value argument structure: '" + arg2 + "'");    
                }
                
                arg2 = arg2.substring(1);
                int value = Integer.parseInt(arg2);
                if(value > Instruction.MAX_IMMEDIATE_VALUE) {
                    throw error("Invalid immediate value, above max value (" + Instruction.MAX_IMMEDIATE_VALUE + "): '" + value + "'");
                }
                
                instruction |= Instruction.ARG2_IMM_MASK;
                instruction |= value;                
            }
            else if(arg2.startsWith(".")) {
                Integer index = this.constants.get(arg2);
                if(index == null) {
                    throw error("No constant defined for '" + arg2 + "'");
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
    private List<String> parseLine(String line) {
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
        
        return args;
        
    }
    
    
    /**
     * Parses the assembly text
     * 
     * @param assembly
     * @return the parsed assembly text into instructions with arguments
     */
    public List<List<String>> parse(String assembly) {
        // TODO: Account for strings with \n
        List<String> lines = Arrays.asList(assembly.split("\n"));
        
        List<List<String>> parsedLines = new ArrayList<>();
        for(String line : lines) {
            List<String> args = parseLine(line.trim());
            if(!args.isEmpty()) {
                parsedLines.add(args);
            }
        }
        
        return parsedLines;
    }

}

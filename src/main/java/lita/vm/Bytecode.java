/*
 * see license.txt
 */
package lita.vm;

/**
 * The VM machine code instructions
 * 
 * @author Tony
 *
 */
public class Bytecode {

    // array index is the Constant ID, and the
    // int value is the address of where the constant
    // is stored in RAM
    public final int[] constants;
    
    public final int[] instr;
    public int length;
    public int pc;
    
    /**
     * @param constants
     * @param instr
     * @param pc
     * @param length
     */
    public Bytecode(int[] constants, int[] instr, int pc, int length) {
        this.constants = constants;
        this.instr = instr;
        this.pc = pc;
        this.length = length;
    }
}

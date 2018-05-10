/*
 * see license.txt
 */
package lita.vm;

import static lita.vm.Instruction.*;
import static lita.vm.Opcodes.*;


/**
 * A 32-bit central processing unit
 * 
 * @author Tony
 *
 */
public class CPU32 {

    private static final int WORD_SIZE = 32; /* 32 bits per word */
    
    class CpuInstruction {
        int[] constants;
        
        boolean isReg = false; 
        
        boolean isArg1Address  = false;
        boolean isArg2Address  = false;
        boolean isImmediate    = false;
                    
        Register x = null;
        Register y = null;

        int arg2Value = 0;
        
        
        /**
         * Process the instruction, parses out the arguments and fills in the appropriate
         * data values based on the instruction
         * 
         * @param pc
         * @param instr
         * @return the opcode
         */
        int process(int pc, int instr) {
            final int opcode = opcode(instr);
            
            isReg = false; 
            
            isArg1Address  = false;
            isArg2Address  = false;
            isImmediate    = false;
                        
            x = null;
            y = null;

            arg2Value = 0;
            
            ////
            // JMP instruction is special in that its argument is
            // a 24 bit immediate mode number
            ////            
            if(opcode==JMP||opcode==CALL) {
                arg2Value = argJmpValue(instr); 
            }
            else {
                ////
                // All other instructions follow arg1/arg2 formats
                ////
                
                if(numberOfArgs(opcode) == 2) {
                    x = registers[arg1Value(instr)];
                    isArg1Address = isArg1Addr(instr);
                    
                    isReg = isArg2Reg(instr);
                    
                    // Determine if Arg2 is a Register or Constant value
                    if(isReg) {
                        // The register is either an address to a value in memory OR a value
                        y = registers[arg2Value(instr)];
                        isArg2Address = isArg2Addr(instr);
                    }
                    else {
                        // The constant is either an immediate value OR index to RAM
                        isImmediate = isArg2Immediate(instr); 
                        arg2Value = arg2Value(instr);
                    }
                }
                else {                                
                    isReg = isArg2Reg(instr);
                    
                    // Determine if Arg2 is a Register or Constant value
                    if(isReg) {
                        // The register is either an address to a value in memory OR a value
                        y = registers[arg2Value(instr)];
                        x = y;
                        isArg2Address = isArg2Addr(instr);
                    }
                    else {
                        // The constant is either an immediate value OR index to RAM
                        isImmediate = isArg2Immediate(instr); 
                        arg2Value = arg2Value(instr);
                    }
                }
            }
            
            return opcode;
        }
        
        int getArg1IntValue() {
            return isArg1Address ? getIntValueAt(x) : x.intValue();
        }
        
        float getArg1FloatValue() {
            return isArg1Address ? getFloatValueAt(x) : x.floatValue();
        }
        
        byte getArg1ByteValue() {
            return isArg1Address ? getByteValueAt(x) : x.byteValue();
        }
        
        int getArg2IntValue() {
            return isReg ? (isArg2Address ? getIntValueAt(y) : y.intValue()) 
                         : isImmediate ? arg2Value 
                                       : ram.readInt(constants[arg2Value]);
        }
        
        byte getArg2ByteValue() {
            return isReg ? (isArg2Address ? getByteValueAt(y) : y.byteValue()) 
                         : isImmediate ? (byte)arg2Value 
                                       : ram.readByte(constants[arg2Value]);
        }
        
        float getArg2FloatValue() {
            return isReg ? (isArg2Address ? getFloatValueAt(y) : y.floatValue()) 
                         : ram.readFloat(constants[arg2Value]);
        }
        
        int getConstantIntValue() {
            return isImmediate ? arg2Value 
                               : ram.readInt(constants[arg2Value]);
        }
        
        float getConstantFloatValue() {
            return ram.readFloat(constants[arg2Value]);
        }
        
        byte getConstantByteValue() {
            return isImmediate ? (byte)arg2Value 
                               : ram.readByte(constants[arg2Value]);
        }
        
        int getConstantAddressValue() {
            return constants[arg2Value];
        }
    }
    
    private final Register[] registers;
    private final Register sp, pc, r;
    
    private final RAM ram;
    
    private final int stackSize;
    
    private final CpuInstruction currentInstruction;
    
    /**
     * @param ram
     * @param stackSize
     */
    public CPU32(RAM ram, int stackSize) {        
        if(stackSize > ram.sizeInBytes()) {
            throw new IllegalArgumentException("Stack size is bigger than RAM amount");
        }
        
        this.ram = ram;
        this.stackSize = stackSize;
        
        this.registers = new Register[9];
        this.registers[0] = new Register("$sp", this);
        this.registers[1] = new Register("$pc", this);
        this.registers[2] = new Register("$r", this);
        
        this.registers[3] = new Register("$a", this);
        this.registers[4] = new Register("$b", this);
        this.registers[5] = new Register("$c", this);
        
        this.registers[6] = new Register("$i", this);
        this.registers[7] = new Register("$j", this);
        this.registers[8] = new Register("$k", this);
        
        this.sp = this.registers[0];
        this.pc = this.registers[1];
        this.r  = this.registers[2];
        
        this.currentInstruction = new CpuInstruction();
    }

    /**
     * The word size of this VM; size is in bits
     * 
     * @return the number of bits per word
     */
    public int getWordSize() {
        return WORD_SIZE;
    }
    
    /**
     * @return the ram
     */
    public RAM getRam() {
        return ram;
    }
    
    /**
     * @return the stackSize
     */
    public int getStackSize() {
        return stackSize;
    }
    
    /**
     * @return the registers for this CPU
     */
    public Register[] getRegisters() {
        return this.registers;
    }
    
    /**
     * An error occurred
     * 
     * @param fmt
     * @param args
     * @return the {@link EvalException} to throw
     */
    private EvalException error(String fmt, Object ... args) {
        final String str = String.format(fmt, args);
        return new EvalException(str);
    }
    
    /**
     * Execute the supplied {@link Bytecode}
     * 
     * @param bytecode
     */
    public void execute(Bytecode bytecode) {
        int pc = bytecode.pc;
        final int len = bytecode.length;
        
        final int[] constants = bytecode.constants;        
        final int[] instrs = bytecode.instr;
        
        this.currentInstruction.constants = constants;
        
        while(pc < len) {
            int instr = instrs[pc++];
            this.pc.address(pc);
            
            final int opcode = this.currentInstruction.process(pc, instr);
                        
            switch(opcode) {
                case NOOP: {
                    break;
                }
                case MOVI: {
                    this.currentInstruction.x.value(this.currentInstruction.getArg2IntValue());
                    break;
                }
                case MOVF: {                    
                    this.currentInstruction.x.value(this.currentInstruction.getArg2FloatValue());
                    break;
                }
                case MOVB: {                    
                    this.currentInstruction.x.value(this.currentInstruction.getArg2ByteValue());
                    break;
                }
                case LDCI: {
                    this.currentInstruction.x.value(this.currentInstruction.getConstantIntValue());                    
                    break;
                }
                case LDCF: {
                    this.currentInstruction.x.value(this.currentInstruction.getConstantFloatValue());                    
                    break;
                }
                case LDCB: {
                    this.currentInstruction.x.value(this.currentInstruction.getConstantByteValue());
                    break;
                }
                case LDCA: {
                    this.currentInstruction.x.value(this.currentInstruction.getConstantAddressValue());
                    break;
                }
                case PUSHI: {
                    int value = this.currentInstruction.getArg2IntValue();
                    
                    setIntValue(this.sp, value);
                    this.sp.incAddress();
                    break;
                }
                case PUSHF: {
                    float value = this.currentInstruction.getArg2FloatValue();
                    
                    setFloatValue(this.sp, value);
                    this.sp.incAddress();
                    break;
                }
                case PUSHB: {
                    byte value = this.currentInstruction.getArg2ByteValue();
                    
                    setByteValue(this.sp, value);
                    this.sp.addressOffset(+1);
                    break;
                }
                case POPI: {
                    this.sp.decAddress();
                    int value = getIntValueAt(this.sp);
                    
                    this.currentInstruction.x.value(value);                    
                    break;
                }
                case POPF: {
                    this.sp.decAddress();
                    float value = getFloatValueAt(this.sp);
                    
                    this.currentInstruction.x.value(value);                    
                    break;
                }
                case POPB: {
                    this.sp.address(-1);
                    byte value = getByteValueAt(this.sp);
                    
                    this.currentInstruction.x.value(value);                    
                    break;
                }
                case IFI: {
                    int yValue = this.currentInstruction.getArg2IntValue();          
                    int xValue = this.currentInstruction.getArg1IntValue();
                    
                    if(xValue > yValue) {
                        pc++;
                    }
                    break;
                }
                case IFF: {
                    float yValue = this.currentInstruction.getArg2FloatValue();          
                    float xValue = this.currentInstruction.getArg1FloatValue();
                    
                    if(xValue > yValue) {
                        pc++;
                    }
                    break;
                }
                case IFB: {
                    byte yValue = this.currentInstruction.getArg2ByteValue();          
                    byte xValue = this.currentInstruction.getArg1ByteValue();
                    
                    if(xValue > yValue) {
                        pc++;
                    }
                    break;
                }
                case JMP: {
                    pc = this.currentInstruction.arg2Value;
                    break;
                }
                    
                case PRINTI: {
                    System.out.println(this.currentInstruction.getArg2IntValue());
                    break;
                }
                case PRINTF: {
                    System.out.println(this.currentInstruction.getArg2FloatValue());
                    break;
                }
                case PRINTB: {
                    System.out.println(this.currentInstruction.getArg2ByteValue());
                    break;
                }
                case CALL: {
                    this.r.address(pc);                    
                    int value = this.currentInstruction.arg2Value;
                    
                    pc = value; 
                    break;
                }
                case RET: {
                    pc = this.r.address();                    
                    break;
                }
                    
                /* ===================================================
                 * ALU operations 
                 * ===================================================
                 */
                    
                case ADDI: {
                    int value = this.currentInstruction.getArg2IntValue();                    
                    this.currentInstruction.x.value(this.currentInstruction.x.intValue() + value);                    
                    break;
                }
                case ADDF: {
                    float value = this.currentInstruction.getArg2FloatValue();                    
                    this.currentInstruction.x.value(this.currentInstruction.x.floatValue() + value);
                    break;
                }
                case ADDB: {
                    byte value = this.currentInstruction.getArg2ByteValue();                    
                    this.currentInstruction.x.value(this.currentInstruction.x.byteValue() + value);
                    break;
                }
                case SUBI: {
                    int value = this.currentInstruction.getArg2IntValue();                    
                    this.currentInstruction.x.value(this.currentInstruction.x.intValue() - value);                    
                    break;
                }
                case SUBF: {
                    float value = this.currentInstruction.getArg2FloatValue();                    
                    this.currentInstruction.x.value(this.currentInstruction.x.floatValue() - value);
                    break;
                }
                case SUBB: {
                    byte value = this.currentInstruction.getArg2ByteValue();                    
                    this.currentInstruction.x.value(this.currentInstruction.x.byteValue() - value);                    
                    break;
                }
                case MULI: {
                    int value = this.currentInstruction.getArg2IntValue();                    
                    this.currentInstruction.x.value(this.currentInstruction.x.intValue() * value);                     
                    break;
                }
                case MULF: {
                    float value = this.currentInstruction.getArg2FloatValue();                    
                    this.currentInstruction.x.value(this.currentInstruction.x.floatValue() * value);
                    break;
                }
                case MULB: {
                    byte value = this.currentInstruction.getArg2ByteValue();                    
                    this.currentInstruction.x.value(this.currentInstruction.x.byteValue() * value);                     
                    break;
                }
                case DIVI: {
                    int value = this.currentInstruction.getArg2IntValue();                    
                    if(value == 0) {
                        error("Divide be zero error.");
                    }
                    
                    this.currentInstruction.x.value(this.currentInstruction.x.intValue() / value); 
                    break;
                }
                case DIVF: {
                    float value = this.currentInstruction.getArg2FloatValue();                    
                    if(value == 0) {
                        error("Divide be zero error.");
                    }
                    this.currentInstruction.x.value(this.currentInstruction.x.floatValue() / value);
                    break;
                }
                case DIVB: {
                    byte value = this.currentInstruction.getArg2ByteValue();                    
                    if(value == 0) {
                        error("Divide be zero error.");
                    }
                    
                    this.currentInstruction.x.value(this.currentInstruction.x.byteValue() / value); 
                    break;
                }
                case MODI: {
                    int value = this.currentInstruction.getArg2IntValue();                    
                    if(value == 0) {
                        error("Divide be zero error.");
                    }
                    
                    this.currentInstruction.x.value(this.currentInstruction.x.intValue() % value);                  
                    break;
                }
                case MODF: {
                    float value = this.currentInstruction.getArg2FloatValue();                    
                    if(value == 0) {
                        error("Divide be zero error.");
                    }
                    this.currentInstruction.x.value(this.currentInstruction.x.floatValue() % value);
                    break;
                }
                case MODB: {
                    byte value = this.currentInstruction.getArg2ByteValue();                    
                    if(value == 0) {
                        error("Divide be zero error.");
                    }
                    
                    this.currentInstruction.x.value(this.currentInstruction.x.byteValue() % value); 
                    break;
                }
                case ORI: {
                    int value = this.currentInstruction.getArg2IntValue();                    
                    this.currentInstruction.x.value(this.currentInstruction.x.intValue() | value);                     
                    break;
                }
                case ORB: {
                    byte value = this.currentInstruction.getArg2ByteValue();                    
                    this.currentInstruction.x.value(this.currentInstruction.x.byteValue() | value);                     
                    break;
                }
                case ANDI: {
                    int value = this.currentInstruction.getArg2IntValue();                    
                    this.currentInstruction.x.value(this.currentInstruction.x.intValue() & value);                     
                    break;
                }
                case ANDB: {
                    byte value = this.currentInstruction.getArg2ByteValue();                    
                    this.currentInstruction.x.value(this.currentInstruction.x.byteValue() & value);                     
                    break;
                }
                case NOTI: {
                    int value = this.currentInstruction.getArg2IntValue();                    
                    this.currentInstruction.x.value(~value);                     
                    break;
                }
                case NOTB: {
                    byte value = this.currentInstruction.getArg2ByteValue();                    
                    this.currentInstruction.x.value(~value);                     
                    break;
                }
                case XORI: {
                    int value = this.currentInstruction.getArg2IntValue();                    
                    this.currentInstruction.x.value(this.currentInstruction.x.intValue() ^ value);                     
                    break;
                }
                case XORB: {
                    byte value = this.currentInstruction.getArg2ByteValue();                    
                    this.currentInstruction.x.value(this.currentInstruction.x.byteValue() ^ value);                     
                    break;
                }
                case SZRLI: {
                    int value = this.currentInstruction.getArg2IntValue();                    
                    this.currentInstruction.x.value(this.currentInstruction.x.intValue() >>> value);                     
                    break;
                }
                case SZRLB: {
                    byte value = this.currentInstruction.getArg2ByteValue();                    
                    this.currentInstruction.x.value(this.currentInstruction.x.byteValue() >>> value);                     
                    break;
                }
                case SRLI: {
                    int value = this.currentInstruction.getArg2IntValue();                    
                    this.currentInstruction.x.value(this.currentInstruction.x.intValue() >> value);                     
                    break;
                }
                case SRLB: {
                    byte value = this.currentInstruction.getArg2ByteValue();                    
                    this.currentInstruction.x.value(this.currentInstruction.x.byteValue() >> value);                     
                    break;
                }
                case SLLI: {
                    int value = this.currentInstruction.getArg2IntValue();                    
                    this.currentInstruction.x.value(this.currentInstruction.x.intValue() << value);                     
                    break;
                }
                case SLLB: {
                    byte value = this.currentInstruction.getArg2ByteValue();                    
                    this.currentInstruction.x.value(this.currentInstruction.x.byteValue() << value);                     
                    break;
                }
                default:
                    throw error("Unknown opcode: %d", opcode);
            }
        }
    }
    
    private int getIntValueAt(Register r) {
        return this.ram.readInt(r.address());
    }
        
    private float getFloatValueAt(Register r) {
        return this.ram.readFloat(r.address());
    }
        
    private byte getByteValueAt(Register r) {
        return this.ram.readByte(r.address());
    }
        
    private void setIntValue(Register r, int value) {
        this.ram.storeInt(r.address(), value);
    }   
    
    private void setFloatValue(Register r, float value) {
        this.ram.storeFloat(r.address(), value);
    }
        
    private void setByteValue(Register r, byte value) {
        this.ram.storeByte(r.address(), value);
    }

}

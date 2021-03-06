/*
 * see license.txt
 */
package lita.vm;

/**
 * Register points to a value in memory or contains an immediate value.
 * 
 * The register is represented by 32-bits.  
 * 
 * @author Tony
 *
 */
public class Register {

    private String name;
    private int address;
    
    private final int addressInc;
    
    /**
     * @param name
     * @param vm
     */
    public Register(String name, CPU32 cpu) {
        this.name = name;
        
        this.address = 0;
        
        // Word Size in bits, convert to bytes as this
        // is what RAM is addressed by
        this.addressInc = cpu.getWordSize() / 8;
    }

    @Override
    public String toString() {
        return this.name + "@0x" + Integer.toHexString(this.address) + "[" + intValue() + "][" + floatValue() + "]";
    }
    
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    
    public int intValue() {
        return this.address;
    }
    public float floatValue() {
        return Float.intBitsToFloat(this.address);
    }
    
    public byte byteValue() {
        return (byte) intValue();
    }
    
    public void value(int newValue) {
        this.address = newValue;        
    }
    
    public void value(float newValue) {
        this.address = Float.floatToIntBits(newValue);        
    }
    
    public int address() {
        return this.address;
    }
    
    public int address(int newAddress) {
        this.address = newAddress;
        return this.address;
    }
    
    public int addressOffset(int adjustBy) {
        this.address += adjustBy;
        return this.address;
    }
    
    public int incAddress() {
        this.address += this.addressInc;
        return this.address;
    }
    
    public int decAddress() {
        this.address -= this.addressInc;
        return this.address;
    }
}

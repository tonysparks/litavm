/*
 * see license.txt
 */
package lita.vm;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Tony
 *
 */
public class InstructionTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testInstructionZero() {
        int instruction = 0b000000_0_0_00000000000_0_0_00000000000;
        assertEquals(Instruction.opcode(instruction), 0);
       // assertEquals(Instruction.isArg1Reg(instruction), false);
        assertEquals(Instruction.isArg1Addr(instruction), false);
        assertEquals(Instruction.arg1Value(instruction), 0);
        
        assertEquals(Instruction.isArg2Reg(instruction), false);
        assertEquals(Instruction.isArg2Addr(instruction), false);
        assertEquals(Instruction.arg2Value(instruction), 0);
    }
    
    @Test
    public void testInstructionValues() {
        final int instruction = 0b000001_1_1_00000000001_0_0_00000000000;
        assertEquals(Instruction.opcode(instruction), 0b000001);
       // assertEquals(Instruction.isArg1Reg(instruction), true);
        assertEquals(Instruction.isArg1Addr(instruction), true);
        assertEquals(Instruction.arg1Value(instruction), 0b00000000001);
        
        assertEquals(Instruction.isArg2Reg(instruction), false);
        assertEquals(Instruction.isArg2Addr(instruction), false);
        assertEquals(Instruction.arg2Value(instruction), 0);
    }

    @Test
    public void testInstructionValuesBoth() {
        final int instruction = 0b000001_1_1_00000000001_0_1_00000000011;
        assertEquals(Instruction.opcode(instruction), 0b000001);
       // assertEquals(Instruction.isArg1Reg(instruction), true);
        assertEquals(Instruction.isArg1Addr(instruction), true);
        assertEquals(Instruction.arg1Value(instruction), 0b00000000001);
        
        assertEquals(Instruction.isArg2Reg(instruction), false);
        assertEquals(Instruction.isArg2Addr(instruction), true);
        assertEquals(Instruction.arg2Value(instruction), 3);
    }
    
    @Test
    public void testInstructionValuesMax() {
        final int instruction = 0b000001_1_1_11111111111_0_1_00000000011;
        assertEquals(Instruction.opcode(instruction), 0b000001);
       // assertEquals(Instruction.isArg1Reg(instruction), true);
        assertEquals(Instruction.isArg1Addr(instruction), true);
        assertEquals(Instruction.arg1Value(instruction), 0b11111111111);
        
        assertEquals(Instruction.isArg2Reg(instruction), false);
        assertEquals(Instruction.isArg2Addr(instruction), true);
        assertEquals(Instruction.arg2Value(instruction), 3);
    }
}


Bytecode Instruction Format 
===
Each instruction consists of a 32 bit integer.  There are two primary formats, one for `JMP` opcode and another for the rest of the opcodes.  

The first 6 bits are to always used to identify the `opcode` to execute.  

JMP Instruction Format
==
The `JMP` instruction has its own special format as it needs the ability to have a large number to support the ability to jump anywhere in the code.  As such, no
program can have more than `2^24` (`16,777,216`) instructions and be able to fully support `JMP` operations.

The remaining 24 bits for the `JMP` instruction is an immediate mode unsigned number.  This number represents where in the program to jump to, it is a zero based 
absolute index.   

Remaining Instructions Format
==
For all other instructions, they use the remaining 24 bits for two arguments.  The first argument takes 5 bits, were the first bit designates if the value in 
the register should be treated as an `address` or `value`.  The remaining 4 bits identify which register.  Although, the format supports up to 16 registers, the CPU 
only has 9 registers.
  
The second argument takes 21 bits.  The first bit designates if the argument is a register.  If its set to `1`, then the second bit designates if the value in
the register should be treated as an `address` or `value`.  The remaining 19 bits identify which register.  Although, the format supports up to `2^19` registers, the CPU
only has 9 registers.  

If the first bit is `0`,  then the second bit designates if the value should be treated either as
an immediate value (second bit = 1) or a constant index lookup (second bit = 0).  In the immediate value case, the immediate value is an unsigned value with a max value of
(`2^19`) (`524,288`).  In the constant index lookup case, the remaining 19 bits are used as a constant index to look up in the constant pool.

Instruction Format Table
==

| 0   | 1   | 2   | 3   | 4   | 5   | 6   | 7   | 8   | 9   | 10  | 11  | 12  | 13  | 14  | 15  | 16  | 17  | 18  | 19  | 20  | 21  | 22  | 23  | 24  | 25  | 26  | 27  | 28  | 29  | 30  | 32  |
| --- |:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| op  | op  | op  | op  | op  | op  | Adr | v1  | v1  | v1  | v1  | Reg | Adr | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | 
| op  | op  | op  | op  | op  | op  | Adr | v1  | v1  | v1  | v1  | 0   | Imm | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  |
| jmp | jmp | jmp | jmp | jmp | jmp | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   |
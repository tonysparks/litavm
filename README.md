LitaVM
==
LitaVM is a 32 bit CPU Virtual Machine, created because who doesn't like understanding how low level hardware works?  The name `lita` comes from my grandma who I love dearly.

The main goals of this project is to create a CPU with a set of instructions to manipulate memory and create an assembly language to program the fancy CPU.  If I feel adventurous enough, I may create a C like language for it as well.

I'm not sure where this project will end up, but I'll continue to churn on it until it becomes a chore.  

Enough introduction, let's get to the meat.

Bytecode Instruction Format 
===
Each instruction consists of a 32 bit integer.  There are two primary formats, one for `JMP`/`CALL` opcodes and another for the rest of the opcodes.  

The first 6 bits are to always used to identify the `opcode` to execute.  

JMP/CALL Instruction Format
==
The `JMP` and `CALL` instructions have their own special format as they need the ability to have a large number to support the ability to jump anywhere in the code.  As such, no
program can have more than `2^24` (`16,777,216`) instructions and be able to fully support `JMP` and `CALL` operations.

The remaining 24 bits for the `JMP` and `CALL` instructions is an immediate mode unsigned number.  This number represents where in the program to jump to, it is a zero based 
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

Registers
==
There are 9 total registers, three reserved and six general purpose.  Registers can contain a 32 bit value (either int or float); use the appropriate `opcode` to interpret the value of the register correctly (`opcodes` come in three flavors `I`, `F`, `B` to parse 32 bit int, 32 bit float and 8 bit bytes respectively.  Additionally, a register can contain a memory address, as all memory addresses are 32 bit.

* `$sp` is the stack pointer and is available for read/write
* `$pc` is the program counter and is available for read
* `$r` stores the return address when invoking a `CALL` instruction; this is available for read/write
* `$a` is a general purpose register for read/write
* `$b` is a general purpose register for read/write
* `$c` is a general purpose register for read/write
* `$i` is a general purpose register for read/write
* `$j` is a general purpose register for read/write
* `$k` is a general purpose register for read/write

Operation Codes
==
There are a number of `opcodes` the CPU can handle.

| Opcode Name  | Value | Arguments | Notes                     |
|--------------|:-----:|:---------:|:-------------------------:|
| NOOP         | 0     | 0         | No operation, does nothing|
| MOVI         | 1     | $a $b     | Moves 32 bit int $a = $b  |
| MOVF         | 2     | $a $b     | Moves 32 bit float $a = $b |
| MOVB         | 3     | $a $b     | Moves 8 bit byte $a = $b  |
| LDCI         | 4     | $a $b     | Loads 32 bit int constant $a = $b |
| LDCF         | 5     | $a $b     | Loads 32 bit float constant $a = $b |
| LDCB         | 6     | $a $b     | Loads 8 bit byte constant $a = $b |
| LDCA         | 7     | $a $b     | Loads 32 bit int constant address $a = $b |
| PUSHI        | 8     | $b        | Push 32 bit int on top of stack (increments $sp by 4) |
| PUSHF        | 9     | $b        | Push 32 bit float on top of stack (increments $sp by 4) |
| PUSHB        | 10    | $b        | Push 8 bit byte on top of stack (increments $sp by 1) |
| POPI         | 11    | $a        | Pops 32 bit int from top of stack (decrements $sp by 4) |
| POPF         | 12    | $a        | Pops 32 bit float from top of stack (decrements $sp by 4) |
| POPB         | 13    | $a        | Pops 8 bit byte from top of stack (decrements $sp by 1) |
| IFI          | 14    | $a $b     | Skips the next instruction if $a > $b |
| IFF          | 15    | $a $b     | Skips the next instruction if $a > $b |
| IFB          | 16    | $a $b     | Skips the next instruction if $a > $b |
| JMP          | 17    | $v        | Moves the program counter to position $v |
| PRINTI       | 18    | $a        | Prints the value of $a to system out |
| PRINTF       | 19    | $a        | Prints the value of $a to system out |
| PRINTB       | 20    | $a        | Prints the value of $a to system out |
| CALL         | 21    | $v        | Stores the current program counter in register `$r` and sets the program counter to $v |
| RET          | 22    | 0         | Sets the program counter to the value stored in register `$r` |


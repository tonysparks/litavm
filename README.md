LitaVM
==
LitaVM is a 32 bit CPU Virtual Machine, created because who doesn't like understanding how low level programming and hardware works?  The name `lita` comes from my grandma who I love dearly.

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
only has 12 registers.
  
The second argument takes 21 bits.  The first bit designates if the argument is a register.  If its set to `1`, then the second bit designates if the value in
the register should be treated as an `address` or `value`.  The remaining 19 bits identify which register.  Although, the format supports up to `2^19` registers, the CPU
only has 12 registers.  

If the first bit is `0`,  then the second bit designates if the value should be treated either as
an immediate value (second bit = 1) or a constant index lookup (second bit = 0).  In the immediate value case, the immediate value is an unsigned value with a max value of
(`2^19`) (`524,288`).  In the constant index lookup case, the remaining 19 bits are used as a constant index to look up in the constant pool.

Instruction Format Table
==

| 0   | 1   | 2   | 3   | 4   | 5   | 6   | 7   | 8   | 9   | 10  | 11  | 12  | 13  | 14  | 15  | 16  | 17  | 18  | 19  | 20  | 21  | 22  | 23  | 24  | 25  | 26  | 27  | 28  | 29  | 30  | 31  |
| --- |:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| op  | op  | op  | op  | op  | op  | Adr | v1  | v1  | v1  | v1  | Reg | Adr | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | 
| op  | op  | op  | op  | op  | op  | Adr | v1  | v1  | v1  | v1  | 0   | Imm | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  | v2  |
| jmp | jmp | jmp | jmp | jmp | jmp | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   | v   |

Registers
==
There are 12 total registers, four reserved and six general purpose.  Registers can contain a 32 bit value (either int or float); use the appropriate `opcode` to interpret the value of the register correctly (`opcodes` come in three flavors `I`, `F`, `B` to parse 32 bit int, 32 bit float and 8 bit bytes respectively.  Additionally, a register can contain a memory address, as all memory addresses are 32 bit.

* `$sp` is the stack pointer and is available for read/write
* `$pc` is the program counter and is available for read
* `$r` stores the return address when invoking a `CALL` instruction; this is available for read/write
* `$h` is the heap pointer that is the address of where heap allocations can begin
* `$a` is a general purpose register for read/write
* `$b` is a general purpose register for read/write
* `$c` is a general purpose register for read/write
* `$d` is a general purpose register for read/write
* `$i` is a general purpose register for read/write
* `$j` is a general purpose register for read/write
* `$k` is a general purpose register for read/write
* `$u` is a general purpose register for read/write

RAM
==
When instantiating the vm (or thru command line arguments) you may specify the amount of RAM that the VM has available.  By default, there is `1 MiB`.  The memory layout consists of:

| Address | Memory type | Notes |
|--------:|------------:|:------|
| 0..x    | Constant Pool | The constant pool stores all string's and number constants |
| x..MaxRam-MaxStackSize | Heap | The heap grows upward, meaning as more memory is allocated to the heap, the memory addresses increase |
| MaxStackSize..MaxRam | Stack | The stack grows downward, meaning as you push things on the stack, the memory addresses decrease |


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
| PUSHI        | 8     | $b        | Push 32 bit int on top of stack (decrements $sp by 4) |
| PUSHF        | 9     | $b        | Push 32 bit float on top of stack (decrements $sp by 4) |
| PUSHB        | 10    | $b        | Push 8 bit byte on top of stack (decrements $sp by 1) |
| POPI         | 11    | $a        | Pops 32 bit int from top of stack (increments $sp by 4) |
| POPF         | 12    | $a        | Pops 32 bit float from top of stack (increments $sp by 4) |
| POPB         | 13    | $a        | Pops 8 bit byte from top of stack (increments $sp by 1) |
| DUPI         | 14    | $a        | Duplicates a 32 bit int on top of the stack (decrements $sp by 4) and places the copied value in $a |
| DUPF         | 15    | $a        | Duplicates a 32 bit float on top of the stack (decrements $sp by 4) and places the copied value in $a |
| DUPB         | 16    | $a        | Duplicates a 8 bit byte on top of the stack (decrements $sp by 1) and places the copied value in $a |
| IFI          | 17    | $a $b     | Skips the next instruction if $a > $b |
| IFF          | 18    | $a $b     | Skips the next instruction if $a > $b |
| IFB          | 19    | $a $b     | Skips the next instruction if $a > $b |
| IFEI         | 20    | $a $b     | Skips the next instruction if $a >= $b |
| IFEF         | 21    | $a $b     | Skips the next instruction if $a >= $b |
| IFEB         | 22    | $a $b     | Skips the next instruction if $a >= $b |
| JMP          | 23    | $v        | Moves the program counter to position $v |
| PRINTI       | 24    | $a        | Prints the value of $a to system out |
| PRINTF       | 25    | $a        | Prints the value of $a to system out |
| PRINTB       | 26    | $a        | Prints the value of $a to system out |
| PRINTC       | 27    | $a        | Prints the value of $a (as an ASCII character) to system out |
| CALL         | 28    | $v        | Stores the current program counter in register `$r` and sets the program counter to $v |
| RET          | 29    | 0         | Sets the program counter to the value stored in register `$r` |
| ADDI         | 30    | $a $b     | Adds two 32 bit int and stores the result in $a = $a + $b |
| ADDF         | 31    | $a $b     | Adds two 32 bit float and stores the result in $a = $a + $b |
| ADDB         | 32    | $a $b     | Adds two 8 bit byte and stores the result in $a = $a + $b |
| SUBI         | 33    | $a $b     | Subtracts two 32 bit int and stores the result in $a = $a - $b |
| SUBF         | 34    | $a $b     | Subtracts two 32 bit float and stores the result in $a = $a - $b |
| SUBB         | 35    | $a $b     | Subtracts two 8 bit byte and stores the result in $a = $a - $b |
| MULI         | 36    | $a $b     | Multiplies two 32 bit int and stores the result in $a = $a * $b |
| MULF         | 37    | $a $b     | Multiples two 32 bit float and stores the result in $a = $a * $b |
| MULB         | 38    | $a $b     | Multiples two 8 bit byte and stores the result in $a = $a * $b |
| DIVI         | 39    | $a $b     | Divides two 32 bit int and stores the result in $a = $a / $b |
| DIVF         | 40    | $a $b     | Divides two 32 bit float and stores the result in $a = $a / $b |
| DIVB         | 41    | $a $b     | Divides two 8 bit byte and stores the result in $a = $a / $b |
| MODI         | 42    | $a $b     | Remainder of dividing two 32 bit int and stores the result in $a = $a % $b |
| MODF         | 43    | $a $b     | Remainder of dividing two 32 bit float and stores the result in $a = $a % $b |
| MODB         | 44    | $a $b     | Remainder of dividing two 8 bit byte and stores the result in $a = $a % $b |
| ORI          | 45    | $a $b     | Bitwise OR of two 32 bit int and stores the result in $a = $a | $b |
| ORB          | 46    | $a $b     | Bitwise OR of two 8 bit byte and stores the result in $a = $a | $b |
| ANDI         | 47    | $a $b     | Bitwise AND of two 32 bit int and stores the result in $a = $a & $b |
| ANDB         | 48    | $a $b     | Bitwise AND of two 8 bit byte and stores the result in $a = $a & $b |
| NOTI         | 49    | $a $b     | Bitwise NOT of a 32 bit int and stores the result in $a = ~$b |
| NOTB         | 50    | $a $b     | Bitwise NOT of a 8 bit byte and stores the result in $a = ~$b |
| XORI         | 51    | $a $b     | Bitwise Exclusive OR of two 32 bit int and stores the result in $a = $a ^ $b |
| XORB         | 52    | $a $b     | Bitwise Exclusive OR of two 8 bit byte and stores the result in $a = $a ^ $b |
| SZRLI        | 53    | $a $b     | Bitwise Shift Zero Right Logical of a 32 bit int and stores the result in $a = $a >>> $b |
| SZRLB        | 54    | $a $b     | Bitwise Shift Zero Right Logical of a 8 bit byte and stores the result in $a = $a >>> $b |
| SRLI         | 55    | $a $b     | Bitwise Shift Right Logical of a 32 bit int and stores the result in $a = $a >> $b |
| SRLB         | 56    | $a $b     | Bitwise Shift Right Logical of a 8 bit byte and stores the result in $a = $a >> $b |
| SLLI         | 57    | $a $b     | Bitwise Shift Left Logical of a 32 bit int and stores the result in $a = $a << $b |
| SLLB         | 58    | $a $b     | Bitwise Shift Left Logical of a 8 bit byte and stores the result in $a = $a << $b |


Assembly Language
==
The LitaVM assembly language syntax is pretty standard.  

| Assembly Code       | Purpose |
|:-------------------:|---------|
| .favre 4            | Creates a constant named `favre` that can be referenced in instruction arguments, when it is referenced, the number 4 is used|
| .brett "Brett"      | Creates a constant named `brett` that can be referenced in instruction arguments, when it is referenced, the memory address of the start of the `"Brett"` string is used |
| pushi .favre        | This will use the constant `4` and push that onto the stack |
| pushi #12           | This will use the immediate value of `12`(format in base 10) and push that onto the stack. Only integers are allowed in immediate values |
| pushi #0x01         | This will use the immediate value of `1` (format in hexidecimal) and push that onto the stack. Only integers are allowed in immediate values |
| pushi #0b1001       | This will use the immediate value of `9` (format in binary) and push that onto the stack. Only integers are allowed in immediate values |
| ; this is a comment | Creates a comment, anything after the `;` is ignored |
| :label              | Creates a label that a `jmp` or `call` instruction can jump to.  |
| jmp :label          | Moves the program counter to the location of `:label` |
| movi $a $b          | The `$a` retrieves the value in the register |
| ifb &$a $b          | The `&` in front of a register means to treat the value in the register as a memory address, and go to that position in memory and return that value |


Sample Assembly
==

Hello World program, which prints out "Hello World" to system out
```asm

;; Create a constant
.text "Hello World"


ldca $a .text        ;; Load the constant address in $a
pushi $a             ;; push the address on the stack, so that print_string can use it
call :print_string   ;; call the print_string subroutine

jmp :exit            ; exit out of the program
printi #11           ; shouldn't get invoked, because we are jumping to the :exit label

;;
;; Prints the supplied string to sysout
;;
;; input:
;;    <address> to string constant, retreived from top of the stack
;; output:
;;    <void>
;;
:print_string        
        popi $a          ;; stores the address of the string constant
    :print_loop
        ifb &$a #0       ;; loops until the value at address $a = 0; strings are null terminated
        jmp :print_end_loop
        printc &$a       ;; prints out the ASCII byte character
        addi $a #1       ;; increments to the next character byte
        jmp :print_loop
    :print_end_loop    
        ret              ;; Stores return $pc in $r register, RET sets the $pc to value of $r
       
:exit
```

# Compiler-for-Sysy
A compiler to translate sysy to LLVM IR

We refer to the constructor of LLVM IR and implements optimizations below

* Function In Line
* DeadCode Delete
* Mem2Reg

Usage: `java compiler.jar -o main.ll main.C [-O1]`

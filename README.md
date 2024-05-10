## Compiler for Co2

Our Compiler converts and executes the code, following the language of CO2. The compiler was a semester-long project for CSCE434 at Texas A&M, taught by Dr. Nguyen (https://khanhtn.engr.tamu.edu/). This language allows for features such as if statements, for loops, while loops, functions, and more. Our compiler takes in an input file and pipelines the inputs to a scanner, parser, type checker, optimizer, and a code generator. The compiler can optimize the code by running constant folding, constant propagation, copy propagation, orphan function elimination, common subexpression elimination, and dead code elimination. Our code compiles and generates machine code for the DLX architecture, which contains a maximum of 24 general use registers, 7 special registers, and memory. The final project is around 11,600 lines of code and 750 comments.

## Implementation
#### Scanner
To implement our scanner, we used simplified maximal munch in order to generate our token stream.
While simplified maximal munch may have some issues with certain inputs, we can assume that for
the most part, this algorithm will work. There were some issues with generating extra error tokens,
but our error tokens were only generated extras if there was already an error token. So this does
not create any errors.
#### Parser
Once we are given the token stream from the scanner. To solve this, we used recursive descent
technique, which broke this down into methods for each grammar rule, and made sure that the
token stream correctly follows the grammar. A lot of these methods were straightforward, but some
of the declarations created issues. We output any errors we might get, which means that our input
code is nonconforming to our grammar.
#### Type Checker
Our type checker is self explanatory and checks for the correct type expressions (i.e. adding ints
to ints, comparing booleans compared to ints). We used specific checkings for each type in our
AST. For example, our checking for if statements first looked at the relation to make sure it has
the correct typing, and then looked at the following blocks. There was also a need for function
checking, to make sure that all the arguments that get passed are in the correct type, as well as
the return type being the same as well. Similarly, we also implemented a spaghetti stack for our
symbol table, which allowed us to confirm that all variables were also correctly initialized and in
scope.
#### Optimizer and IR Generation
We converted our abstract syntax tree into immediate representation. This immediate
representation of three address code allowed us to have our code represented in assembly
adjacent language, where each instruction has a destination, and some expression. The expression
can either be an addition, subtraction, function call, or similar. We chose not to use SSA, as
generating our code in SSA was rather difficult, and we could not get it working well. We figured
the benefits outweigh the possible errors that could have been created from our conversion algorithm.
Our optimization process was simple, and followed a very similar algorithm for all of our
different features. All of our optimizations had a kill operator, a gen operator, and apply operator.
For example, our common subexpression elimination held a set of subexpressions that can be
replaced. If we find a new subexpression, we can add it to the set. If we use a reassign a variable, x,
we kill all subexpressions that contain the value x. Finally, if we find an expression that is already
in our set, we can replace that expression with the variable that already contains that same value.
We ran these optimizations on each basic block, and carried over the set if needed.
#### Register Allocation
Our register allocation runs a scan of our variables, and determines what registers should hold each
variable. To do this, we construct a Register Interference Graph that determines which registers
will interfere others. We say that an interference occurs when a register’s live variable analysis
overlaps with a different register’s live variable. After we have this graph constructed, we can run
our graph coloring algorithm to see if any registers are spilled. We have specified Register 25 for the
destination, and Register 26 for the left value and Register 27 for the right value. In other words,
if our instruction was t0 = t1 + t2, t0 would be in Register 25, t1 would be Register 26, t2 would
be Register 27. While Register 25 is also used for the return value, we expect that there will be no
overlap, as we save the value of the destination back into memory before marking it as "free" for
the next instruction (which might be a return).
#### Code Generation
We use a similar "visitor" pattern that is used in the previous steps. Each node in the CFG has
a specific method, which generates the instruction, or instructions that is necessary for a specific
instruction. If a variable is spilled, we need to add specific instructions that load the value into the
designated register, and specific instructions to load the destination back into memory if its also
spilled. To handle branching, we mark the starting position of each block, and add the specific
branch instruction to that starting position. We do a similar algorithm for function call, and always
return to the address held in Register 31.
> There are known issues in branching which affects loops.

## Compilation
### Java Compilation
This project was compiled and tested using JVM (https://en.wikipedia.org/wiki/Java_virtual_machine). We compiled the Java to Class files using 
> javac -cp ../compiler-project/lib/commons-cli-1.6.0.jar src/co2/*.java src/ast/*.java src/types/*.java src/ir/cfg/*.java src/ir/tac/*.java

and can be run using
> java co2.CompilerTester -src src/test/regAlloc/test000.txt -reg 5


> java co2.CompilerTester -src src/test/optimization/test001.txt -o cp -o cf -loop


> java co2.CompilerTester -src src/test/ast/test000.txt -ast -cfg screen -in src/test/ast/test000.in -max 


### Command line options
| Command   | Option | Argument |
| ------------- | ------------- | ------------- |
| Source File  | s, src | File path to source file
| Data File  | i, in  | File path to data file (input for readInt, readBool, and readFloat functions)
| Number of Registers | nr, reg | Number of registers, r, where 2 <= r <= 24
| Display AST | a, astOut | No Argument
| Display CFG | cfg | Specify `screen` or `file` 
| Order Specific Optimization | o | Specific Optimization
| Loop all Optimizations | loop, convergence | No Argument
| Run All Optimizations | max, maxOpt | No Argument
| Display Assembly | asm, assembly | No Argument

> Displaying the CFG into a file will output the code to `SourceFileName_cfg.dot`. Outputting the CFG will be in a DOT format (https://graphviz.org/doc/info/lang.html), and can easily be viewed thanks to the helpful SketchViz tool (https://sketchviz.com/new) 
> The Optimizations can be specified by the arguments cp (Constant Propagation), cf (Constant Folding), cpp (Copy Propagation), cse (Common Subexpression Elimiation), dce (Dead Code Elimination).
> Options could have multiple options for the same meaning, such as `s` and `src`.





## Folder Structure

The workspace contains:

- `src`: the folder to maintain sources
- `src/ast`: the folder to maintain classes for the AST (abstract syntax tree) structure
- `src/co2`: the folder to maintain classes for compiler features and compiler testers
- `src/ir/cfg`: the folder to maintain classes for the CFG (tree for tac)
- `src/ir/tac`: the folder to maintain classes for for the TAC (three instruction code)
- `src/test`: the folder to maintain test files that can be used as input
- `src/types`: the folder to maintain classes used for the Type Checker
- `lib`: the folder to maintain dependencies (contains an Apache Commons CLI for command line options)

Meanwhile, the compiler will make optimizations, and report what changes were made to the code. THe output will be in a file named: "YYYY.MM.DD.HH.MM.SSoptimization-outputs.txt"



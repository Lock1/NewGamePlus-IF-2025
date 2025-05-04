# IF2211 - Algorithm Strategy
Introductory class assignment. Seemingly boring simple brute force practice turns out to be pretty fun playground to optimize performance via non-big O approach. Given 2 set of word, find a solution in form of `Map<Character,Int0to10>` that satisfies sum(S1) == S2 after character in-place substitution. Unlike the original problem statement which only ask for 1 solution, this NG+ deliberately exhaust all of the possible solution.

My original solution [(link: github.com/Lock1/IF2211)](https://github.com/Lock1/IF2211-Stima-Tucil-1-2/blob/main/Tugas%20Kecil%201%20-%20Cryptarithms/Deliverable/src/main.c) uses messy C code.



## Haskell
First time actually using Haskell for something bigger than CP problem set grind. Pretty sure my implementation sucks.
I think there's some bug as well, number of the solution seems too low?
But yes, certainly there's a problem with lazy/strict pipeline control as I keep getting OOM for bigger test case.
For the smaller case, it beat my friend's Python solution by order of magnitude.

Because it was my first time, I just write the "easiest" solution in my mind. Which, for some reason, drags me to design some parser with sum-type. Haven't get used with the type system yet, so I was a struggling a bit to write a code that contain correct semantic.
I also decided to omit IO processing because I couldn't care less with it.
But anyway, it's pretty fun experience overall. Still need to learn FP's debugging, profiling, monadic IO, lazy/eager, the list goes on.



## Java
With Java 24 + preview features, modern Java turns out to be pretty decent. This one is more like immutability cost & jerry-rig FP test.
I used Java actively at the time writing this at `$DAYJOB`, so this one just basically some quick modeling prototype practice.
The end result work pretty well I guess. Relatively fast, easy to tune, and actually succeed achieving the original design goal.

Primarily designed to be revolving around "`Stream` pipeline" & FP-oriented construct.
Initial list of string will get transformed into `Expression` and lazy `Stream<PossibleSolution>`.
Then each `PossibleSolution` are filtered by it's validity before get added to the counter.
Due to recursive nature of state space enumeration & non-interference property required by `Stream`,
each step of enumeration creates a new copy of state space before get passed into next recursion.

Man, I keep making `Functionals` whenever I write some modern Java nowadays. Maybe someone should push it to upstream JDK.

End result work pretty fast using Ryzen 4800H. Plus this shows how much `parallelStream()` can affect the performance;
turns out it's pretty a lot for natural fork-join parallel task.

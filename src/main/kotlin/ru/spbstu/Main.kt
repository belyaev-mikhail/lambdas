package ru.spbstu

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import ru.spbstu.kparsec.parse

val stdLib = """
    i = \x.x
    k = \x _.x
    s = \x y z.x z (y z)
    y = \f.(\x.f (x x)) (\x.f (x x))
    
    true = \x _.x
    false = \_ y.y
    if = \c t f.c t f
    and = \x y.x y false
    or = \x y.x true y
    not = \x.x false true
    
    0 = \f z.z
    succ = \n f z.f (n f z)
    1 = succ 0
    2 = succ 1
    3 = succ 2
    4 = succ 3
    5 = succ 4
    6 = succ 5
    7 = succ 6
    8 = succ 7
    9 = succ 8
    isZero = \n.n (\_.false) true
    + = \m n f z.m f (n f z)
    pred = \n f z.n (\g h.h (g f)) (\_.z) (\u.u)
    - = \m n.n pred m
    * = \m n.n (+ m) 0
    ieq = \m n.and (isZero (- m n)) (isZero (- n m))

    pair = \a b f.f a b
    fst = \p.p (\a _.a)
    snd = \p.p (\_ b.b)

    isNil = \p.p (\_ _ _. false) true
    nil = \_ y.y
    cons = pair
    head = fst
    tail = snd
    
    fold = y (\ rec f acc lst . if(isNil lst) acc (rec f (f acc (head lst)) (tail lst)))
"""

class Main : CliktCommand() {
    val evalSteps by option(help = "Number of steps to evaluate").int().default(1000)

    override fun run() {
        val state = EvalState(System.out)
        for (binding in stdLib.lines()) if(binding.isNotBlank()) {
            StmtParser.parse(binding).orThrow().eval(state, evalSteps)
        }
        while(true) {
            try {
                val stmt = StmtParser.parse(readLine() ?: break).orThrow()
                stmt.eval(state, evalSteps)
            } catch (ex: Exception) {
                System.err.println(ex.message)
            }
        }
    }
}

fun main(args: Array<String>) = Main().main(args)


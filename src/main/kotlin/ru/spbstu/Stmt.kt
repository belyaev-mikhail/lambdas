package ru.spbstu

import ru.spbstu.wheels.joinTo
import ru.spbstu.wheels.joinToString

sealed class Stmt
data class Binding(val symbol: Symbol, val expr: Expr): Stmt()
data class Eval(val expr: Expr): Stmt()
data class EvalAsInt(val expr: Expr): Stmt()
data class EvalAsList(val expr: Expr): Stmt()
data class EvalAsBoolean(val expr: Expr): Stmt()
object Debug : Stmt()

class EvalState(val output: Appendable,
                val bindings: MutableMap<Symbol, Expr> = mutableMapOf()) {
    override fun toString(): String = bindings.joinToString("\n")
}

fun Expr.run(state: EvalState, evalSteps: Int) = replace(state.bindings).eval(evalSteps)

fun Expr.reifyBoolean(stepLimit: Int = 1000): Boolean {
    val t = FreeVar("#T")
    val f = FreeVar("#F")
    when(this(t, f).eval(stepLimit)) {
        t -> return true
        f -> return false
        else -> throw IllegalArgumentException("Expression $this is not a boolean")
    }
}

fun Expr.reifyInt(stepLimit: Int = 1000): Int {
    val f = FreeVar("#F")
    val z = FreeVar("#Z")
    var res = this(f, z).eval(stepLimit)
    var total = 0
    loop@ while(true) {
        when(res) {
            is FreeVar -> {
                if(res != z) break@loop
                return total
            }
            is App -> {
                val func = res.function
                if(func != f) break@loop
                res = res.argument
                ++total
            }
            else -> break@loop
        }
    }
    throw IllegalArgumentException("Expression $this is not an integer")
}

fun Expr.reifyList(stepLimit: Int = 1000): List<Expr> {
    val f = FreeVar("#F")
    val gravestone = FreeVar("#G")
    var current = this
    val result = mutableListOf<Expr>()
    loop@ while(true) {
        when(val decons = current(f, gravestone).eval()) {
            gravestone -> return result
            is App -> {
                val (f1, a2) = decons
                if(a2 != gravestone || f1 !is App) break@loop
                val (f2, a1) = f1
                if(f2 !is App) break@loop
                val (f3, a0) = f2
                if(f3 != f) break@loop
                result += a0
                current = a1
            }
            else -> break@loop
        }
    }
    throw IllegalArgumentException("Expression $this is not a list")
}

fun Stmt.eval(state: EvalState, evalSteps: Int) {
    when (this) {
        is Binding -> state.bindings.put(symbol, expr.run(state, evalSteps))
        is Eval -> expr.run(state, evalSteps).let { state.output.appendln("$it") }
        is EvalAsInt -> expr.run(state, evalSteps).reifyInt(evalSteps).let { state.output.appendln("$it") }
        is EvalAsBoolean -> expr.run(state, evalSteps).reifyBoolean(evalSteps).let { state.output.appendln("$it") }
        is EvalAsList -> expr.run(state, evalSteps).reifyList(evalSteps).map {
            try { it.reifyInt(evalSteps) } catch (ex: Exception) { it }
        }.let { state.output.appendln("$it") }
        is Debug -> state.bindings.joinTo(state.output, "\n")
    }
}

fun EvalState.addBinding(binding: Binding, evalSteps: Int) = binding.eval(this, evalSteps)

fun Iterable<Stmt>.eval(state: EvalState, evalSteps: Int) = forEach { it.eval(state, evalSteps) }

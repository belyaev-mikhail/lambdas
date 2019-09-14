package ru.spbstu

import ru.spbstu.wheels.*
import ru.spbstu.wheels.Option.Companion.empty
import ru.spbstu.wheels.Option.Companion.just

sealed class Stmt
data class Binding(val symbol: Symbol, val expr: Expr): Stmt()
enum class EvalFormat { OBJECT, INTEGER, BOOLEAN, LIST }
data class Eval(val expr: Expr, val format: EvalFormat = EvalFormat.OBJECT): Stmt()
object Debug : Stmt()

class EvalState(val output: Appendable,
                val bindings: MutableMap<Symbol, Expr> = mutableMapOf()) {
    override fun toString(): String = bindings.joinToString("\n")
}

fun Expr.run(state: EvalState, evalSteps: Int) = replace(state.bindings).eval(evalSteps)

fun Expr.reifyBoolean(stepLimit: Int = 1000): Option<Boolean> = run {
    val t = FreeVar("#T")
    val f = FreeVar("#F")
    when(this(t, f).eval(stepLimit)) {
        t -> just(true)
        f -> just(false)
        else -> empty()
    }
}

fun Expr.reifyInt(stepLimit: Int = 1000): Option<Int> {
    val f = FreeVar("#F")
    val z = FreeVar("#Z")
    var res = this(f, z).eval(stepLimit)
    var total = 0
    loop@ while(true) {
        when(res) {
            is FreeVar -> {
                if(res != z) break@loop
                return just(total)
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
    return empty()
}

fun Expr.reifyList(stepLimit: Int = 1000): Option<List<Expr>> {
    val f = FreeVar("#F")
    val gravestone = FreeVar("#G")
    var current = this
    val result = mutableListOf<Expr>()
    loop@ while(true) {
        when(val decons = current(f, gravestone).eval(stepLimit)) {
            gravestone -> return just(result)
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
    return empty()
}

fun Expr.reifyAsAnything(stepLimit: Int): Any =
    reifyInt(stepLimit)
        .orElse { reifyBoolean(stepLimit) }
        .orElse { reifyList(stepLimit).map { list -> list.map { it.reifyAsAnything(stepLimit) } } }
        .getOrElse { this }

fun Stmt.eval(state: EvalState, evalSteps: Int) {
    when (this) {
        is Binding -> state.bindings.put(symbol, expr.run(state, evalSteps))
        is Eval -> {
            val e = expr.run(state, evalSteps)
            when(format) {
                EvalFormat.OBJECT -> e as Any
                EvalFormat.INTEGER -> e.reifyInt(evalSteps)
                    .getOrElse { throw Exception("Expression $e is not an integer") }
                EvalFormat.BOOLEAN -> e.reifyBoolean(evalSteps)
                    .getOrElse { throw Exception("Expression $e is not a boolean") }
                EvalFormat.LIST -> e.reifyList(evalSteps).map { list ->
                    list.map { it.reifyAsAnything(evalSteps) }
                }.getOrElse { throw Exception("Expression $e is not a list") }
            }.let { state.output.appendln("$it") }
        }
        is Debug -> state.bindings.joinTo(state.output, "\n")
    }
}

fun EvalState.addBinding(binding: Binding, evalSteps: Int) = binding.eval(this, evalSteps)

fun Iterable<Stmt>.eval(state: EvalState, evalSteps: Int) = forEach { it.eval(state, evalSteps) }

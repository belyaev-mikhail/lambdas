package ru.spbstu

import kotlin.reflect.KProperty

sealed class Expr {
    override fun toString(): String = print()
}
sealed class Symbol: Expr() {
    abstract val name: String
}
data class FreeVar(override val name: String): Symbol() {
    companion object {
        operator fun getValue(thisRef: Any?, prop: KProperty<*>) = FreeVar(prop.name)
    }
    override fun toString(): String = super.toString()
}
data class BoundVar(override val name: String, val id: Int = ++freeId): Symbol() {
    companion object {
        var freeId = 0
    }
    override fun toString(): String = super.toString()
}

data class App(val function: Expr, val argument: Expr): Expr() {
    fun shallowCopy(function: Expr = this.function,
                    argument: Expr = this.argument) = when {
        function === this.function && argument === this.argument -> this
        else -> App(function, argument)
    }
    override fun toString(): String = super.toString()
}
operator fun Expr.invoke(vararg args: Expr) =
    args.fold(this, ::App)

data class Lambda(val argument: BoundVar, val body: Expr): Expr() {
    fun shallowCopy(argument: Symbol = this.argument, body: Expr = this.body) = when {
        argument === this.argument && body === this.body -> this
        else -> Lambda(argument, body)
    }
    override fun toString(): String = super.toString()
}
fun Lambda(symbol: Symbol, body: Expr) = run {
    val bv = BoundVar(symbol.name)
    Lambda(bv, body.replace(symbol, bv))
}
class MultiSymbol(val contents: MutableList<BoundVar> = mutableListOf()) {
    private fun getOrCreate(index: Int): Symbol {
        if(contents.size <= index) {
            contents.addAll((contents.size..index).map { BoundVar("#arg$it") })
        }
        return contents[index]
    }
    operator fun component1(): Symbol = getOrCreate(0)
    operator fun component2(): Symbol = getOrCreate(1)
    operator fun component3(): Symbol = getOrCreate(2)
    operator fun component4(): Symbol = getOrCreate(3)
    operator fun component5(): Symbol = getOrCreate(4)
    operator fun component6(): Symbol = getOrCreate(5)
    operator fun component7(): Symbol = getOrCreate(6)
    operator fun component8(): Symbol = getOrCreate(7)
    operator fun component9(): Symbol = getOrCreate(8)
}
inline fun Lambda(body: (Symbol) -> Expr) = run {
    val bv = BoundVar("it")
    Lambda(bv, body(bv))
}
inline fun MultiLambda(body: (MultiSymbol) -> Expr) = run {
    val ms = MultiSymbol()
    val bd = body(ms)
    ms.contents.foldRight(bd) { arg, acc -> Lambda(arg, acc) }
}

interface ExprVisitor<R> {
    fun visit(e: Expr): R = e.accept(this)
    fun visit(sym: Symbol): R
    fun visit(app: App): R
    fun visit(lambda: Lambda): R
}
open class RecursiveVisitor : ExprVisitor<Expr> {
    override fun visit(sym: Symbol): Expr = sym
    override fun visit(app: App): Expr = app.shallowCopy(function = visit(app.function), argument = visit(app.argument))
    override fun visit(lambda: Lambda): Expr = lambda.shallowCopy(body = visit(lambda.body))
}

fun <R> Expr.accept(visitor: ExprVisitor<R>): R = when(this) {
    is Symbol -> visitor.visit(sym = this)
    is App -> visitor.visit(app = this)
    is Lambda -> visitor.visit(lambda = this)
}

class Replacer(private val original: Symbol,
               private val replacement: Expr) : RecursiveVisitor() {
    companion object {
        var counter = 0
    }

    override fun visit(sym: Symbol): Expr = when {
        sym == original -> replacement
        else -> sym
    }
    override fun visit(lambda: Lambda): Expr = when {
        lambda.argument == original -> lambda /* shadowing */
        else -> super.visit(lambda)
    }
}

fun Expr.replace(original: Symbol, replacement: Expr): Expr =
    Replacer(original, replacement).visit(this)

fun Expr.replace(mapping: Map<Symbol, Expr>): Expr =
    mapping.entries.fold(this) { e, (k, v) -> e.replace(k, v) }

object Evaluator : RecursiveVisitor() {
    override fun visit(sym: Symbol): Expr = sym
    override fun visit(lambda: Lambda): Expr = lambda
    override fun visit(app: App): Expr = when(app.function) {
        is Lambda -> app.function.body.replace(app.function.argument, app.argument)
        else -> super.visit(app)
    }
}

fun Expr.evalStep(): Expr = Evaluator.visit(this)
fun Expr.eval(stepLimit: Int = 1000): Expr {
    var result = this
    for(i in 0 until stepLimit) {
        val previous = result
        result = previous.evalStep()
        if(previous === result) break
    }
    return result
}

class Printer(val to: Appendable) : ExprVisitor<Unit> {
    fun paren(e: Expr) { to.append('(').also { visit(e) }.append(')') }

    override fun visit(sym: Symbol) {
        to.append(sym.name)
    }

    override fun visit(app: App) {
        when(app.function) {
            is Lambda -> paren(app.function)
            else -> visit(app.function)
        }
        to.append(' ')
        when(app.argument) {
            is Symbol -> visit(app.argument)
            else -> paren(app.argument)
        }
    }

    override fun visit(lambda: Lambda) {
        to.append('λ')
        visit(lambda.argument)
        to.append('.')
        visit(lambda.body)
    }
}

fun Expr.print() = Printer(StringBuilder()).apply { visit(this@print) }.to.toString()

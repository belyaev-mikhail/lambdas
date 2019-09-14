package ru.spbstu

import ru.spbstu.kparsec.*
import ru.spbstu.kparsec.examples.manyOneToString
import ru.spbstu.kparsec.parsers.*
import kotlin.or

object StmtParser : StringsAsParsers, DelegateParser<Char, List<Stmt>> {
    val identifier =
        lexeme(char { it.isDefined() && !it.isWhitespace() && it !in "()\\.λ" }.manyOneToString())

    val paren = -'(' + defer { expr } + -')'

    val symbol = identifier.map(::FreeVar)

    val app = (symbol or paren).manyOne().map { it.reduce(::App) }

    val lambdaArguments = (-'\\' or -'λ') + symbol.manyOne() + -'.'

    val lambda = zip(lambdaArguments, defer { expr }) { args, body ->
        args.foldRight(body) { arg, acc -> Lambda(arg, acc) }
    }

    val expr: Parser<Char, Expr> by lazy { app or symbol or lambda or paren }

    val assignee = symbol + -"="

    val binding = zip(assignee, expr, ::Binding)

    val eval = expr.map { Eval(it) }

    val evalAsInt = (-":i" + expr).map { Eval(it, EvalFormat.INTEGER) }
    val evalAsBoolean = (-":b" + expr).map { Eval(it, EvalFormat.BOOLEAN) }
    val evalAsList = (-":l" + expr).map { Eval(it, EvalFormat.LIST) }

    val debug = (+":debug").map { Debug }

    val stmt = debug or evalAsInt or evalAsBoolean or evalAsList or binding or eval

    override val self: Parser<Char, List<Stmt>> = stmt.manyOne()
}

fun <T, R> ParseResult<T, R>.orThrow(): R = when (this) {
    is Success -> result
    is NoSuccess -> throw IllegalArgumentException("$this")
}

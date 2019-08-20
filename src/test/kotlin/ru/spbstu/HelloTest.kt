package ru.spbstu

import org.junit.Test
import ru.spbstu.kparsec.parse
import kotlin.test.assertEquals

class HelloTest {

    val evalSteps = 1000
    val sb = StringBuilder()
    val state = EvalState(sb).apply {
        for (binding in stdLib.lines()) if(binding.isNotBlank()) {
            StmtParser.parse(binding).orThrow().eval(this, evalSteps)
        }
    }

    fun expr(e: String) = StmtParser.expr.parse(e).orThrow().run(state, evalSteps)

    fun assertExprEquals(expected: String, actual: String) =
        assertEquals(expr(expected), expr(actual))

    @Test
    fun smokeTest() {
        assertExprEquals("x", "x")

        val map_ = expr("""\ rec f lst . if (isNil lst) nil (cons (f (head lst)) (rec f (tail lst)))""")

        val y by FreeVar
        val isNil by FreeVar
        val nil by FreeVar
        val cons by FreeVar
        val `if` by FreeVar
        val head by FreeVar
        val tail by FreeVar

        val lm = y(MultiLambda { (rec, f, lst) ->
            `if`(isNil(lst), nil, cons(f(head(lst)), rec(f, tail(lst))))
        })

        println(lm)

    }

}

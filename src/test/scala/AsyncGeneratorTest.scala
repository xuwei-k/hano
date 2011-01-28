

// Copyright Shunsuke Sogame 2010.
// Distributed under the terms of an MIT-style license.


package com.github.okomok.hanotest


import com.github.okomok.hano
import junit.framework.Assert._

import hano.Generator


class AsyncGeneratorTest extends org.scalatest.junit.JUnit3Suite {


    def testEmpty: Unit = {
        val tr = Generator.Async[Int] { * =>
            999
            *.end()
        }
        assertTrue(tr.isEmpty)
        assertTrue(tr.isEmpty) // run again.
    }

    def makeValuesTo(n: Int)(y: Generator.Async.Env[Int]): Unit = {
        for (i <- 1 to n) {
            y(i)
        }
        y.end()
    }

    def withMakeValuesTo(n: Int): Unit = {
        val tr = Generator.Async(makeValuesTo(n))
        assertEquals(hano.Iter.from(1 to n), hano.Iter.from(tr))
        assertEquals(hano.Iter.from(1 to n), hano.Iter.from(tr)) // run again.
    }

    def testTrivial: Unit = {
        withMakeValuesTo(1)
        withMakeValuesTo(2)
        withMakeValuesTo(3)
        withMakeValuesTo(9)
        withMakeValuesTo(11)
        withMakeValuesTo(19)
        withMakeValuesTo(20)
        withMakeValuesTo(21)
        withMakeValuesTo(25)
        withMakeValuesTo(30)
        withMakeValuesTo(60)
        withMakeValuesTo(67)
        withMakeValuesTo(80)
        withMakeValuesTo(82)
        withMakeValuesTo(300)
        withMakeValuesTo(310)
    }

    def testTrivial2 {
        def example = Generator.Async[Any] { * =>
            *("first")
            for (i <- 1 until 4) {
                *(i)
            }
            *("last")
            *.end()
            *.end() // idempotent (illegal, but accepted.).
        }
        for (a <- example) {
            //println(a)
        }
    }

    class IgnoredError extends RuntimeException("ignore me")
    class ForwardedError extends RuntimeException("forward me")

    def testExceptionForwarding: Unit = {
        def throwSome(y: Generator.Async.Env[Int]): Unit = {
            for (i <- 1 to 27) {
                y(i)
            }
            throw new ForwardedError
        }

        val tr = Generator.Async(throwSome)

        var thrown = false
        val arr = new java.util.ArrayList[Int]

        try {
            val it = tr.iterator
            while (it.hasNext) {
                arr.add(it.next)
            }
        } catch {
            case _: ForwardedError => thrown = true
        }
        assertTrue(thrown)
        assertEquals(hano.Iter.from(1 to 27), hano.Iter.from(arr))
    }


    def testExceptionForwardingEmpty: Unit = {
        def throwImmediately(y: Generator.Async.Env[Int]) {
            throw new ForwardedError
        }
        val tr = Generator.Async(throwImmediately)

        var thrown = false
        val arr = new java.util.ArrayList[Int]
        try {
            val it = tr.iterator
            while (it.hasNext) {
                arr.add(it.next)
            }
        } catch {
            case _: ForwardedError => thrown = true
        }
        assertTrue(thrown)
        assertTrue(arr.isEmpty)
    }

    def testThrowButEnough {
        def sample = Generator.Async[Int] { y =>
            for (i <- 0 until 25) {
                y(i)
            }
            throw new ForwardedError
        }
        val ret = new java.util.ArrayList[Int]
        val it = sample.iterator
        for (_ <- 0 until 25) { // cf. Cursor will prefetch one element.
            val e = it.next
            ret.add(e)
        }
        assertEquals(hano.Iter.from(0 until 25), hano.Iter.from(ret))

        intercept[ForwardedError] {
            it.hasNext
        }
    }

    def testToIterable {
        val sample = hano.Act().loop.generate(0 until 20).toIterable
        assertEquals(hano.Iter.from(0 until 20), hano.Iter.from(sample))
    }
/*
    def testTraverse {
        val sample = Generator.Async.traverse(0 until 20)
        assertEquals(hano.Iter.from(0 until 20), hano.Iter.from(sample))
    }
*/
    def testThrowAfterEnd {
        val sample = Generator.Async[Int] { * =>
            *(1)
            *(2)
            *(3)
            *.end()
            throw new IgnoredError // abandoned.
        }
        val ret = new java.util.ArrayList[Int]
         sample.foreach(ret.add(_))
        expect(hano.Iter(1,2,3))(hano.Iter.from(ret))
    }
}

/*
class Generator.AsyncLockCompile extends Benchmark {
    val b = new AsyncGeneratorTest
    val tr = iterative.Generator.Async(b.makeValuesTo(100000))
    override def run = {
        val a = tr.size
        ()
    }
}
*/

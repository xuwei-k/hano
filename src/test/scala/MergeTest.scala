

// Copyright Shunsuke Sogame 2010-2011.
// Distributed under the terms of an MIT-style license.


package com.github.okomok.hanotest


import com.github.okomok.hano


import junit.framework.Assert._


class MergeTest extends org.scalatest.junit.JUnit3Suite {

    def testTrivial: Unit = {
        val r1 = hano.Seq(1,2,3)
        val r2 = hano.Seq(4,5)
        val out = new java.util.ArrayList[Int]
        for (x <- r1 merge r2) {
            out.add(x)
        }
        assertEquals(hano.Iter(1,2,3,4,5), hano.Iter.from(out))
    }

    def testNonTrivial: Unit = {
        val r1 = hano.Seq(1,2,3)
        val r2 = hano.Seq(4,5)
        val r3 = hano.Seq(6,7,8,9)
        val out = new java.util.ArrayList[Int]
        for (x <- r1 merge r2 merge r3) {
            out.add(x)
        }
        assertEquals(hano.Iter(1,2,3,4,5,6,7,8,9), hano.Iter.from(out))
    }

    def testDuplicate {
        val r = hano.Seq(1,2,3,4,5)
        val (r1, r2) = r.duplicate
        val out = new java.util.ArrayList[Int]
        for (x <- r1 merge r2) {
            out.add(x)
        }
        assertEquals(hano.Iter(1,1,2,2,3,3,4,4,5,5), hano.Iter.from(out))
    }

    def testEnd {
        val xs = hano.async.pull(0 to 5)
        val ys = hano.async.pull(6 to 9)
        val out = new Array[Int](10)
        var ends = false
        val gate = new java.util.concurrent.CountDownLatch(1)
        var i = 0
        var exitCount = 0
        for (x <- (xs merge ys).onExit{ case hano.Exit.Success => { exitCount += 1; gate.countDown() }; case _ => () }) {
            out(i) = x
            i += 1
        }
        gate.await()
        expect(1)(exitCount)
        java.util.Arrays.sort(out)
        assertEquals(hano.Iter(0,1,2,3,4,5,6,7,8,9), hano.Iter.from(out))
    }

/* TODO
    def testWhenThrown {
        val xs = hano.asyncBy(1000).pull(0 until 5)
        val ys = hano.asyncBy(1000).pull(5 until 1000)
        val out = new java.util.ArrayList[Int]
        var ends = false
        val gate = new java.util.concurrent.CountDownLatch(1)
         var exitCount = 0
        for (x <- (xs merge ys).onExit{ case hano.Exit.Failure(_) => { exitCount += 1; gate.countDown() }; case _ => () }) {
            //println(x)
            //println(Thread.currentThread)
            if (x == 2) {
                throw new Error("catch me, report me")
            }
            Thread.sleep(3) // out.size will be smaller.
            out.add(x)
        }
        gate.await()
        expect(1)(exitCount)
        println(hano.Iter.from(out))
        assert(out.size < 900)
        expect(None)(hano.Iter.from(out).able.find(_ == 3))
    }
*/
}

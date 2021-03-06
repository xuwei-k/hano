

// Copyright Shunsuke Sogame 2010-2011.
// Distributed under the terms of an MIT-style license.


package com.github.okomok.hanotest


import com.github.okomok.hano


import junit.framework.Assert._


class ForkTest extends org.scalatest.junit.JUnit3Suite {
    def testTrivial: Unit = {
        val r = hano.Seq(1,2,3,4,5,6)
        val out = new java.util.ArrayList[Int]
        r.
            fork{r => r.onEach(e => out.add(e *  2)).start()}.
            fork{r => r}.
            fork{r => r.onEach(e => out.add(e + 10)).start()}.
            fork{r => r}.
            start

        assertEquals(hano.Iter(2,11,4,12,6,13,8,14,10,15,12,16), hano.Iter.from(out))
    }

    def testDoing: Unit = {
        val r = hano.Seq(1,2,3,4,5,6)
        val out = new java.util.ArrayList[Int]
        r.
            onEach(e => out.add(e *  2)).
            onEach(e => out.add(e + 10)).
            start

        assertEquals(hano.Iter(2,11,4,12,6,13,8,14,10,15,12,16), hano.Iter.from(out))
    }

    /*
    def testToIterable: Unit = {
        val r = hano.async.pull(0 until 100)
        val v1, v2 = new hano.Val[Iterator[Int]]
        r.fork { xs =>
            v1() = xs.iterator() // deadlock, offcourse.
        } fork { xs =>
            v2() = xs.iterator()
        } start()

        expect(hano.Iter.from(0 until 100))(hano.Iter.from(v1()))
        expect(hano.Iter.from(0 until 100))(hano.Iter.from(v2()))
    }
    */
}

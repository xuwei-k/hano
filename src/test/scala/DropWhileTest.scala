

// Copyright Shunsuke Sogame 2010-2011.
// Distributed under the terms of an MIT-style license.


package com.github.okomok.hanotest


import com.github.okomok.hano


import junit.framework.Assert._


class DropWhileTest extends org.scalatest.junit.JUnit3Suite {
    def testTrivial0: Unit = {
        val a = hano.Seq(1,2,3,4,5,6)
        val b = new java.util.ArrayList[Int]
        for (x <- a.dropWhile(_ <= 4)) {
            b.add(x)
        }
        assertEquals(hano.Iter(5,6), hano.Iter.from(b))
    }

    def testAll: Unit = {
        val a = hano.Seq(1,2,3,4,5,6)
        val b = new java.util.ArrayList[Int]
        for (x <- a.dropWhile(_ <= 10)) {
            b.add(x)
        }
        assertTrue(b.isEmpty)
    }

    def testEmpty: Unit = {
        val a = hano.Empty.of[Int]
        val b = new java.util.ArrayList[Int]
        for (x <- a.dropWhile(_ <= 10)) {
            b.add(x)
        }
        assertTrue(b.isEmpty)
    }

    def testNone: Unit = {
        val a = hano.Seq(1,2,3,4,5,6)
        val b = new java.util.ArrayList[Int]
        for (x <- a.dropWhile(_ > 10)) {
            b.add(x)
        }
        assertEquals(hano.Iter(1,2,3,4,5,6), hano.Iter.from(b))
    }
}

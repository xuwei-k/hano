

// Copyright Shunsuke Sogame 2010-2011.
// Distributed under the terms of an MIT-style license.


package com.github.okomok.hanotest


import com.github.okomok.hano


import junit.framework.Assert._


class FilterTest extends org.scalatest.junit.JUnit3Suite {
    def testTrivial: Unit = {
        val s = new java.util.ArrayList[Int]
        for (x <- hano.Seq(0,1,2,3,4) if x % 2 == 0) {
            s.add(x)
        }
        assertEquals(hano.Iter(0,2,4), hano.Iter.from(s))
    }

    def testEmpty: Unit = {
        val s = new java.util.ArrayList[Int]
        for (x <-hano.Empty.of[Int] if x % 2 == 0) {
            s.add(x)
        }
        assertTrue(s.isEmpty)
    }
}



// Copyright Shunsuke Sogame 2008-2010.
// Distributed under the terms of an MIT-style license.


package com.github.okomok.madatest.sequencetest.reactivetest.example.fortest


// See also: https://gist.github.com/616638


import com.github.okomok.hano
import junit.framework.Assert._
import scala.util.continuations.reset


class ForTest extends org.scalatest.junit.JUnit3Suite {

    def testTrivial {
        val out = new java.util.ArrayList[Int]
        reset {
            val x = hano.BlockEnv.each(hano.Seq.from(1 to 10))
            if (x % 2 == 0) {
                out.add(x)
            }
        }
        assertEquals(hano.util.Iterable(2,4,6,8,10), hano.util.Iterable.from(out))
    }

}

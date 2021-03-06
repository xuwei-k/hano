

// Copyright Shunsuke Sogame 2010-2011.
// Distributed under the terms of an MIT-style license.


package com.github.okomok.hanotest


import com.github.okomok.hano




class BlockTest extends org.scalatest.junit.JUnit3Suite {

    def testTrivial {
        val a = new java.util.ArrayList[Int]
        hano.cps {
            for (x <- hano.Seq(1,2,3).!?) {
                a.add(x); ()
            }
            a.add(99); ()
        }
        expect(hano.Iter(1,2,3,99))(hano.Iter.from(a))
    }

    def testValueDiscarding {
        val a = new java.util.ArrayList[Int]
        hano.cps {
            for (x <- hano.Seq(1,2,3).!?) {
                a.add(x)
                "discard me"
            }
            a.add(99)
            "discard me"
        }
        expect(hano.Iter(1,2,3,99))(hano.Iter.from(a))
    }

    def testNested {
        val a = new java.util.ArrayList[Int]
        hano.cps {
            for (x <- hano.Seq(1,2,3).!?) {
                a.add(x)
                for (y <- hano.Seq(10+x,20+x).!?) {
                    a.add(y); ()
                }
                a.add(98); ()
            }
            a.add(99); ()
        }
        expect(hano.Iter(1,11,21,98,2,12,22,98,3,13,23,98,99))(hano.Iter.from(a))
    }

    def testNestedValueDiscarding {
        val a = new java.util.ArrayList[Int]
        hano.cps {
            for (x <- hano.Seq(1,2,3).!?) {
                a.add(x)
                for (y <- hano.Seq(10+x,20+x).!?) {
                    a.add(y)
                    "discard me"
                }
                a.add(98)
                "discard me"
            }
            a.add(99)
            "discard me"
        }
        expect(hano.Iter(1,11,21,98,2,12,22,98,3,13,23,98,99))(hano.Iter.from(a))
    }

    def testRequire {
        val a = new java.util.ArrayList[(Int, Int)]
        hano.cps {
            val x = hano.Seq(1,2,3).!
            val y = hano.Seq(2,3,4).!
            hano.cps.require(x + y == 5)
            a.add((x, y))
        }
        expect(hano.Iter((1,4),(2,3),(3,2)))(hano.Iter.from(a))
    }

    def testRequire2 {
        val a = new java.util.ArrayList[(Int, Int)]
        hano.cps {
            val x = hano.Seq(1,2,3).!
            val y = hano.Seq(2,3,4).!
            hano.cps.require(x + y == 5)
            hano.cps.require(x == 2)
            a.add((x, y))
        }
        expect(hano.Iter((2,3)))(hano.Iter.from(a))
    }

}



// Copyright Shunsuke Sogame 2010.
// Distributed under the terms of an MIT-style license.


package com.github.okomok.hanotest


import com.github.okomok.hano

import hano.Reactor
import junit.framework.Assert._
import scala.actors.Actor


class TakeUntilTest extends org.scalatest.junit.JUnit3Suite {

    def testTrivial: Unit = {
        val out = new java.util.ArrayList[Int]

        val b = Reactor.singleThreaded()
        val a = Reactor.singleThreaded { r =>
            r collect {
                case e: Int => e
            } takeUntil {
                b
            } onEach { x =>
                out.add(x)
            } start
        }

        a ! 1
        a ! 2
        a ! 3
        b ! "go"
        a ! 4
        a ! 5
        assertEquals(hano.Iter(1,2,3), hano.Iter.from(out))
    }

    def testParallel {
        val xs = new hano.Set[Unit](100)
        val ys = new hano.Channel[Unit]

        val suite = new ParallelSuite(10)
        suite.add(100) {
            xs member ()
        }
        suite.add(10) {
            Thread.sleep(100)
            ys write ()
        }
        val zs = xs takeUntil ys
        for (z <- zs) {
        }
        suite.start()
    }
}



// Copyright Shunsuke Sogame 2010-2011.
// Distributed under the terms of an MIT-style license.


package com.github.okomok.hanotest


import com.github.okomok.hano
import org.testng.annotations._
//import scala.util.control.TailCalls


class ChannelTest extends org.scalatest.junit.JUnit3Suite {

    /* Equivalent to
        hano.cps { * =>
            while (true) {
                val x = ch.read()
                q.offer(x)
            }
        }
    */
    def loopRec[A](xs: hano.Seq[A])(f: A => Unit) {
        def g(): Unit = xs.foreach { x =>
            f(x)
            g()
        }
        g()
    }


    /*
    object bboo {

        import scala.util.continuations._

        sealed trait TailRec [A]
        case class Return [A]( result : A) extends TailRec [A]
        case class Call [A]( thunk : () => TailRec [A] @ suspendable )
        extends TailRec [A]

        @scala.annotation.tailrec
        def tailrec [A]( comp : TailRec [A]): A @ suspendable = comp match {
            case Call ( thunk ) => tailrec ( thunk () )
            case Return (x) => x
        }
    }
    */

    def loopRecSeq[A](xs: hano.Seq[A]) = new hano.Seq[A] {
        override def process = xs.process
        override def forloop(f: hano.Reaction[A]) {
            var g: hano.Reaction[A] = null
            g = new hano.Reaction[A] {
                override protected def rawEnter(p: hano.Exit) = ()
                override protected def rawApply(x: A) = synchronized { f(x); xs.forloop(g) }
                override protected def rawExit(q: hano.Exit.Status) = synchronized {  }
            }
            xs.forloop(g)
        }
    }

    sealed trait TailRec[A]
    case class Return[A](result: A) extends TailRec[A]
    case class Call[A](thunk: () => hano.Seq[TailRec[A]]) extends TailRec[A]

    //@annotation.tailrec
    def tailrec[A](comp: TailRec[A])(f: A => Unit) {
        comp match {
            case Call(thunk) => thunk().foreach(x => tailrec(x)(f))
            case Return(x) => f(x)
        }
    }

    /*
    def tailrec[A](comp: TailRec[A]): hano.Seq[A] = new hano.Seq[A] {
        override def forloop(f: hano.Reaction[A]) {
            comp match {
                case Call(thunk) => thunk().foreach(x => tailrec(x))
                case Return(x) => f(x)
            }
        }
    }
    def loopTail(comp: hano.Seq[Unit]) {
        def f(): hano.Seq[TailRec[Unit]] = {
            comp.map { _ =>
                Call { () =>
                    f()
                }
            }
        }
        f().foreach { t =>
            tailrec(t)
        }
    }
    */

    def loopTail2[A](xs: hano.Seq[A])(f: A => Unit) {
        def g(): hano.Seq[TailRec[Unit]] = {
            xs.map { x =>
                Call { () =>
                    f(x)
                    g()
                }
            }
        }
        g().foreach { t =>
            tailrec(t){ _ => () }
        }
    }

    def loop2[A](xs: hano.Seq[A])(f: A => Unit) {
        def g(): hano.Seq[() => Unit] = {
            xs.map { x =>
                f(x)
                () => {
                    g().foreach { df =>
                        df()
                    }
                }
            }
        }

        g().foreach { df =>
            df()
        }
    }

    def testTrivial {
        val ch = new hano.Channel[Int]

        val suite = new ParallelSuite(10)
        val i = new java.util.concurrent.atomic.AtomicInteger(0)
        suite.add(50) {
            ch write i.incrementAndGet
        }

        val q = new java.util.concurrent.ConcurrentLinkedQueue[Int]
        suite.add(10) {
            for (x <- ch.cycle) {
                q.offer(x)
            }
        }

        suite.start()

        Thread.sleep(2000)

        /*
        too early.
        import scala.actors.Actor
        val cur = Actor.self
        object OK
        ctx.foreach { _ =>
            cur ! OK
        }
        Actor.receive {
            case OK =>
        }*/

        val arr = new java.util.ArrayList[Int]
        for (x <- hano.Iter.from(q).able) {
            arr.add(x)
        }
        java.util.Collections.sort(arr, implicitly[Ordering[Int]])
        expect(hano.Iter.from(1 to 50))(hano.Iter.from(arr))
    }


    def testTake {
        val ch = new hano.Channel[Int]

        val suite = new ParallelSuite(10)
        val i = new java.util.concurrent.atomic.AtomicInteger(0)
        suite.add(50) {
            ch write i.incrementAndGet
        }

        val q = new java.util.concurrent.ConcurrentLinkedQueue[Int]
        suite.add(10) {
            for (x <- ch.cycle.take(4)) {
                q.offer(x)
            }
        }

        suite.start()
        Polling.expect(40, q.size)
    }

    def testOutput {
        val ch = new hano.Channel[Int]

        val xs = hano.async.pull(Seq(5,1,3,6,2,0,4))
        ch << xs.reduceLeft(_ min _) << xs.reduceLeft(_ max _)

        expect(0)(ch.read())
        expect(6)(ch.read())
    }

    def testClose {
        val ch = new hano.Channel[Int]

        val xs = hano.async.pull(Seq(5,1,3,6,2,0,4))
        ch << xs.reduceLeft(_ min _) << xs.reduceLeft(_ max _)

        expect(0)(ch.read())
        expect(6)(ch.read())

        ch.close()
        intercept[hano.Channel.ClosedException] {
            ch write 10
        }
    }

    /*
    def testReadAll {
        val ch = new hano.Channel[Int]

        val p = hano.async
        val xs = p.pull(Seq(1,2,3,4,5))
        val ys = p.pull(Seq(10,20,30,40,50))
        ch << xs << ys close()
        Thread.sleep(2000)
        expect(hano.Iter(1,10,2,20,3,30,4,40,5,50))(hano.Iter.from(ch.readAll()))
    }
*/
    def testOutputSeq {
        val ch = new hano.Channel[Int]
        val p = ch.process

        val xs = p.pull(0 until 3)
        val ys = p.pull(10 until 13)

        ch << xs << ys

        var out: List[Int] = Nil
        for (_ <- 0 until 6) {
            out :+= ch.read()
        }

        expect(hano.Iter(0,1,2,10,11,12))(hano.Iter.from(out.sortWith(_ < _)))
    }

}

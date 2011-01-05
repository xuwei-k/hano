

// Copyright Shunsuke Sogame 2010.
// Distributed under the terms of an MIT-style license.


package com.github.okomok
package hano


import java.util.TimerTask
import scala.actors.Actor


/**
 * Marker trait for a context
 * A context is one-element sequence of the Unit.
 */
trait Context


object Context {


    /**
     * In the call-site
     */
    val self: Seq[Unit] = new Self()

    /**
     * In the thread-pool
     */
    def async: Seq[Unit] = new Async()

    /**
     * In the event-dispatch-thread
     */
    val inEdt: Seq[Unit] = new InEdt()

    /**
     * In a timer
     */
    def inTimer(schedule: TimerTask => Unit): Seq[Unit] = new InTimer(schedule)

    /**
     * Evaluates `body` in a context.
     */
    def eval(ctx: Seq[Unit])(body: => Unit) {
        assert(ctx.isInstanceOf[Context])
        ctx.foreach(_ => body) // Exit is ignored.
    }


    private[hano]
    def upper(xs1: Seq[_], xs2: Seq[_]): Seq[Unit] = {
        val ctx1 = xs1.context
        val ctx2 = xs2.context
        if (ctx1 eq self) {
            if (ctx2 eq self) {
                ctx1
            } else {
                ctx2
            }
        } else {
            ctx1
        }
    }


    private def origin(eval: (=> Unit) => Unit): Seq[Unit] = origin(eval)

    private class Origin(_1: (=> Unit) => Unit) extends Seq[Unit] with Context {
        override def context: Seq[Unit] = this
        override def forloop(f: Reaction[Unit]) {
            _1 {
                f.tryRethrow(context) {
                    f()
                }
                f.exit(Exit.End)
            }
        }
    }


    private class Self() extends Seq[Unit] with Context {
        override def context: Seq[Unit] = this
        override def forloop(f: Reaction[Unit]) {
            f.tryRethrow(context) {
                f()
            }
            f.exit(Exit.End)
        }
    }

    /*

    Something bad happens.

    private class Self() extends SeqProxy[Unit] with Context {
        override def context = this
        override lazy val self = origin { body =>
            body
        }
    }

    private class Async() extends SeqProxy[Unit] with Context {
        override def context = this
        override def close() {
            a ! Exit.Closed
        }
        override val self = origin { body =>
            a ! Task(() => body)
        }
        private val a = new Actor {
            override def act = {
                Actor.loop {
                    react {
                        case Task(f) => f()
                        case q: Exit => Actor.exit
                    }
                }
            }
        }
        a.start()
    }

    private class InEdt() extends SeqProxy[Unit] with Context {
        override def context = this
        override lazy val self = origin { body =>
            javax.swing.SwingUtilities.invokeLater {
                new Runnable {
                    override def run() = body
                }
            }
        }
    }
    */

    case class Task(_1: () => Unit)

    private class Async() extends Seq[Unit] with Context {
        override def context = this
        //override def close() {
        //    a ! Exit.Closed
        //}
        override def forloop(f: Reaction[Unit]) {
            a ! Task { () =>
                var thrown = false
                try {
                    f.tryRethrow(context) {
                        f()
                    }
                } catch {
                    case t: Throwable => {
                        println("Error in reaction: " + t) // TODO
                        thrown = true
                    }
                }
                if (!thrown) {
                    f.exit(Exit.End)
                }
            }
        }
        private val a = new Actor {
            override def act = {
                Actor.loop {
                    react {
                        case Task(f) => f()
                        case q: Exit => Actor.exit
                    }
                }
            }
        }
        a.start()
    }


    private class InEdt() extends Seq[Unit] with Context {
        override def context = this
        override def forloop(f: Reaction[Unit]) {
            javax.swing.SwingUtilities.invokeLater {
                new Runnable {
                    override def run() {
                        f.tryRethrow(context) {
                            f()
                        }
                        f.exit(Exit.End)
                    }
                }
            }
        }
    }


    private class InTimer(_2: TimerTask => Unit) extends SeqProxy[Unit] with Context {
        override def context = this
        override val self = origin { body =>
            var l: TimerTask = null
            l = new TimerTask {
                override def run() = {
                    body
                    l.cancel()
                }
            }
            _2(l)
        }
    }

}



// Copyright Shunsuke Sogame 2010.
// Distributed under the terms of an MIT-style license.


package com.github.okomok
package hano


object Reaction {

    def apply[A](f: A => Unit, k: Exit => Unit): Reaction[A] = new Apply(f, k)

    @Annotation.returnThat
    def from[A](that: Reaction[A]): Reaction[A] = that

    implicit def fromFunction[A](from: A => Unit): Reaction[A] = new FromFunction(from)
    implicit def fromVar[A](from: Var[A]): Reaction[A] = from.toReaction
//    implicit def fromRist[A](from: Rist[A]): Reaction[A] = from.toReaction

    private class Apply[A](_1: A => Unit, _2: Exit => Unit) extends CheckedReaction[A] {
        override protected def checkedApply(x: A) = _1(x)
        override protected def checkedExit(q: Exit) = _2(q)
    }

    private class FromFunction[A](_1: A => Unit) extends CheckedReaction[A] {
        override protected def checkedApply(x: A) = _1(x)
        override protected def checkedExit(q: Exit) = ()
    }

}


/**
 * Triggered by Seq.forloop
 */
trait Reaction[-A] { self =>

    /**
     * Reacts on each element.
     */
    def apply(x: A): Unit

    /**
     * Reacts on the exit.
     */
    def exit(q: Exit): Unit

    @Annotation.equivalentTo("exit(Exit.End)")
    final def end(): Unit = exit(Exit.End)

    @Annotation.equivalentTo("exit(Exit.Closed)")
    final def closed(): Unit = exit(Exit.Closed)

    @Annotation.equivalentTo("exit(Exit.Failed(why))")
    final def failed(why: Throwable): Unit = exit(Exit.Failed(why))

    private[hano]
    final def tryRethrow(ctx: Seq[Unit] = Context.self)(body: => Unit) {
        assert(ctx.isInstanceOf[Context])
        try {
            body
        } catch {
            case t: Throwable => {
                ctx.eval {
                    exit(Exit.Failed(t)) // informs Reaction-site
                }
                throw t // handled in Seq-site
            }
        }
    }

}



// Copyright Shunsuke Sogame 2010.
// Distributed under the terms of an MIT-style license.


package com.github.okomok
package hano


/**
 * Yielding thread is not blocked; elements are buffered.
 */
object AsyncGenerator extends detail.GeneratorCommon {

    override def iterator[A](xs: Seq[A]): Iterator[A] = {
        val ch = new Channel[Msg]
        detail.AsyncForloop(xs)(new ReactionImpl[A](ch))
        new IteratorImpl[A](ch).concrete
    }

    private case class Msg(msg: Any)

    private class ReactionImpl[A](ch: Channel[Msg]) extends Reaction[A] {
        override def apply(x: A) {
            ch write Msg(x)
        }
        override def exit(q: Exit) {
            ch write Msg(q)
        }
    }

    private class IteratorImpl[A](ch: Channel[Msg]) extends detail.AbstractIterator[A] {
        private[this] var v: Option[A] = None
        ready()

        override def isEnd = v.isEmpty
        override def deref = v.get
        override def increment() = ready()

        private def ready() {
            v = ch.read match {
                case Msg(Exit.Failed(t)) => throw t
                case Msg(q: Exit) => None
                case Msg(x) => Some(x.asInstanceOf[A])
            }
        }
    }
}

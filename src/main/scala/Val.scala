

// Copyright Shunsuke Sogame 2010.
// Distributed under the terms of an MIT-style license.


package com.github.okomok
package hano


// See: ScalaFlow
//   at http://github.com/hotzen/ScalaFlow/raw/master/thesis.pdf


import java.util.concurrent
import detail.CountDown


/**
 * Single-assignment value as single-element sequence
 */
final class Val[A](override val context: Context = async) extends Seq[A] {

    require(context ne Self)
    require(context ne Unknown)

    private[this] val v = new concurrent.atomic.AtomicReference[Either[Throwable, A]](null)
    private[this] val fs = new concurrent.ConcurrentLinkedQueue[Reaction[A]]

    // subscription order is NOT preserved.
    override def forloop(f: Reaction[A]) {
        if (v.get != null) {
            _eval(f, v.get)
        } else {
            fs.offer(f)
            if (v.get != null && fs.remove(f)) {
                _eval(f, v.get)
            }
        }
    }

    private def _set(tx: Either[Throwable, A]): Boolean = {
        if (v.compareAndSet(null, tx)) {
            while (!fs.isEmpty) {
                val f = fs.poll
                if (f != null) {
                    _eval(f, tx)
                }
            }
            true
        } else {
            false
        }
    }

    /**
     * Sets the value.
     */
    def set(x: A): Boolean = _set(Right(x))

    /**
     * Gets the value
     */
    def get: A = future.apply()

    /**
     * Fails to produce a value.
     */
    def fail(why: Throwable): Boolean = _set(Left(why))

    /**
     * `Val` assignment
     */
    def assign(that: Seq[A]) = that.forloop(toReaction)

    /**
     * Gets the value in the future.
     */
    def future: () => A = new Val.ToFuture(this)

    /**
     * Equivalent to `set(x)`, but throws if the value is different.
     */
    def update(x: A) {
        if (!set(x)) {
            v.get match {
                case Right(y) if x != y => {
                    throw new Val.MultipleAssignmentException(y, x)
                }
                case _ => ()
            }
        }
    }

    @annotation.aliasOf("get")
    def apply(): A = get

    @annotation.aliasOf("assign")
    def :=[B <: A](that: Seq[B]): Unit = assign(that)

    @annotation.conversion
    def toReaction: Reaction[A] = new Val.ToReaction(this)

    private def _eval(f: Reaction[A], tx: Either[Throwable, A]) {
        context onEach { _ =>
            tx match {
                case Left(t) => f.exit(Exit.Failed(t))
                case Right(x) => f(x)
            }
        } onExit {
            f.exit(_)
        } start()
    }

    // REMOVE ME
    def onAssign(f: A => Unit): Seq[A] = new Val.OnAssign(this, f)
}


object Val {

    /**
     * Thrown in case multiple assignment
     */
    class MultipleAssignmentException[A](expected: A, actual: A) extends
        RuntimeException("expected: " + expected + ", but actual: " + actual)

    /**
     * Creates a `Val` with initial value.
     */
    def apply[A](x: A): Val[A] = {
        val v = new Val[A]
        v() = x
        v
    }

    @annotation.equivalentTo("new Val[A]")
    def apply[A]: Val[A] = new Val[A]

    // REMOVE ME.
    def length(xs: Seq[_]): Val[Option[Int]] = {
        val v = new Val[Option[Int]](xs.context upper async)
        var acc = 0
        xs onEach { x =>
            acc += 1
        } onExit {
            case Exit.End => v() = Some(acc)
            case q => v() = None
        } start()
        v
    }

    // REMOVE ME.
    private class OnAssign[A](_1: Seq[A], _2: A => Unit) extends SeqProxy[A] {
        override val self = _1.onHead {
            case Some(x) => _2(x)
            case None => ()
        }
    }

    private class ToReaction[A](_1: Val[A]) extends Reaction[A] {
        override protected def rawApply(x: A) = _1.set(x)
        override protected def rawExit(q: Exit) = q match {
            case Exit.Failed(t) => _1.fail(t)
            case Exit.Closed => _1.fail(new NoSuchElementException("source sequence was closed before Val.set"))
            case _ => ()
        }
    }

    private class ToFuture[A](_1: Seq[A]) extends (() => A) {
        private[this] var v: Either[Throwable, A] = null
        private[this] val c = new java.util.concurrent.CountDownLatch(1)

        _1 onEach { x =>
            CountDown(c) {
                v = Right(x)
            }
        } onExit { q =>
            CountDown(c) {
                q match {
                    case Exit.Failed(t) if v == null => v = Left(t)
                    case _ => ()
                }
            }
        } start()

        override def apply(): A = {
            c.await()
            if (v == null) {
                throw new NoSuchElementException("Val.future.apply()")
            }
            v match {
                case Left(t) => throw t
                case Right(r) => r
            }
        }
    }
}



// Copyright Shunsuke Sogame 2010-2011.
// Distributed under the terms of an MIT-style license.


package com.github.okomok
package hano


object Arm {
    def apply[A](x: A, c: A => Unit): Arm[A] = new Apply(x, c)

    @annotation.returnThat
    def from[A](that: Arm[A]): Arm[A] = that

    implicit def fromJCloseable[A <: java.io.Closeable](from: A): Arm[A] = new FromJCloseable(from)
    implicit def fromJLock[A <: java.util.concurrent.locks.Lock](from: A): Arm[A] = new FromJLock(from)

    private class Apply[A](_1: A, _2: A => Unit) extends Arm[A] {
        override def open = _1
        override def close() = _2(_1)
    }

    private class FromJCloseable[A <: java.io.Closeable](_1: A) extends Arm[A] {
        override def open = _1
        override def close() = _1.close()
    }

    private class FromJLock[A <: java.util.concurrent.locks.Lock](_1: A) extends Arm[A] {
        override def open = { _1.lock; _1 }
        override def close() = _1.unlock
    }

    /* BROKEN
    private class UsedBy[A, B](_1: Arm[A], _2: A => Seq[B]) extends Seq[B] {
        override def close() = _1.close()
        override def process = Self

        @annotation.pre("f is synchronous")
        override def forloop(f: Reaction[B]) {
            val r = _1.open
            _2(r) onEach { y =>
                for (_ <- from(_1: java.io.Closeable)) {
                    f(y)
                }
            } onExit { q =>
                f.exit(q)
                close()
            } start()
        }
    }
    */
}


/**
 * Mixin for automatic resource management
 */
trait Arm[+A] extends Seq[A] with java.io.Closeable {
    def open: A
    final override def process = Self

    @annotation.pre("f is synchronous")
    override def forloop(f: Reaction[A]) {
        val r = open
        var primary: Throwable = null

        f.enter()
        try {
            f(r)
        } catch {
            case t: Throwable => {
                primary = t
                f.fail(t)
                throw t
            }
        } finally {
            if (primary ne null) {
                try {
                    close()
                } catch {
                    case s: Exception => /*primary.addSuppressedException(s)*/
                }
            } else {
                f.exit()
                close()
            }
        }
    }

    //def usedBy[B](f: A => Seq[B]): Seq[B] = new Arm.UsedBy(this, f)
}

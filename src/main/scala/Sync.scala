

// Copyright Shunsuke Sogame 2010.
// Distributed under the terms of an MIT-style license.


package com.github.okomok
package hano


import detail.For


/**
 * Contains synchronous algorithms.
 */
object Sync {

    final class Val[A] extends Reaction[A] with Reaction.Checked[A] { self =>
        private[this] var v: Either[Throwable, A] = null
        private[this] val c = new java.util.concurrent.CountDownLatch(1)

        override protected def applyChecked(x: A) {
            if (v != null) {
                return
            }
            try {
                v = Right(x)
            } finally {
                c.countDown
            }
        }
        override protected def exitChecked(q: Exit) {
            try {
                q match {
                    case Exit.Failed(t) => if (v == null) { v = Left(t) }
                    case _ => ()
                }
            } finally {
                c.countDown
            }
        }

        def apply(): A = {
            c.await()
            if (v == null) {
                throw new NoSuchElementException("Sync.Val.apply()")
            }
            v match {
                case Right(x) => x
                case Left(t) => throw t
            }
        }

        def toFunction = new Function0[A] {
            override def apply = self()
        }
    }

    def length[_](xs: Seq[_]): Function0[Int] = {
        val v = new Val[Int]
        var acc = 0
        For(xs) { x =>
            acc += 1
        } AndThen {
            case Exit.End => v(acc)
            case q => v.exit(q)
        }
        v.toFunction
    }

    def head[A](xs: Seq[A]): Function0[A] = {
        val v = new Val[A]
        var go = true
        For(xs) { x =>
            if (go) {
                go = false
                v(x)
                xs.close()
            }
        } AndThen {
            v.exit(_)
        }
        v.toFunction
    }

    def last[A](xs: Seq[A]): Function0[A] = {
        val v = new Val[A]
        var acc: Option[A] = None
        For(xs) { x =>
            acc = Some(x)
        } AndThen {
            case Exit.End => if (acc.isEmpty) v.exit(Exit.End) else v(acc.get)
            case q => v.exit(q)
        }
        v.toFunction
    }

    def foldLeft[A, B](xs: Seq[A])(z: B)(op: (B, A) => B): Function0[B] = {
        val v = new Val[B]
        var acc = z
        For(xs) { x =>
            acc = op(acc, x)
        } AndThen {
            case Exit.End => v(acc)
            case q => v.exit(q)
        }
        v.toFunction
    }

    def reduceLeft[A](xs: Seq[A])(op: (A, A) => A): Function0[A] = {
        val v = new Val[A]
        var acc: Option[A] = None
        For(xs) { x =>
            if (acc.isEmpty) {
                acc = Some(x)
            } else {
                acc = Some(op(acc.get, x))
            }
        } AndThen {
            case Exit.End => if (acc.isEmpty) v.exit(Exit.End) else v(acc.get)
            case q => v.exit(q)
        }
        v.toFunction
    }

    def min[A](xs: Seq[A])(implicit c: Ordering[A]): Function0[A] = {
        reduceLeft(xs)(c.min(_, _))
    }

    def max[A](xs: Seq[A])(implicit c: Ordering[A]): Function0[A] = {
        reduceLeft(xs)(c.max(_, _))
    }

}


// Copyright Shunsuke Sogame 2010.
// Distributed under the terms of an MIT-style license.


package com.github.okomok
package hano


import scala.util.continuations.{cpsParam, reset, shift}


object Block {

    def apply[A](ctx: Env => A @cpsParam[A, Any]): Unit = reset(ctx(Env))

    private val Env = new Env{}
    trait Env {
        def each[A](xs: Seq[A]): A @cpsParam[Any, Unit] = xs.toCps

        def head[A](xs: Seq[A]): A @cpsParam[Any, Unit] = xs.take(1).toCps

        def nth[A](xs: Seq[A])(n: Int): A @cpsParam[Any, Unit] = xs.drop(n).take(1).toCps

        def find[A](xs: Seq[A])(p: A => Boolean): A @cpsParam[Any, Unit] = xs.dropWhile(!p(_)).take(1).toCps

        def in[A](xs: Seq[A]): In[A] = new In(xs)

        sealed class In[A](xs: Seq[A]) {
            def foreach(g: A => Any @cpsParam[Unit, Unit]): Exit @cpsParam[Any, Unit] = new Seq[Exit] {
                override def forloop(cp: Exit => Unit, k: Exit => Unit) {
                    xs.onExit(q => cp(q)).forloop(x => reset{g(x);()}, k)
                }
            } toCps
        }

        def require(cond: => Boolean): Unit @cpsParam[Any, Unit] =  (if (cond) Seq.single(()) else Seq.empty).toCps

        def use[A](xs: Arm[A]): A @cpsParam[Any, Unit] = xs.toCps

        def amb[A](xs: util.Iter[A]): A @cpsParam[Any, Unit] = each(xs)
    }

}

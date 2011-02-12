

// Copyright Shunsuke Sogame 2010-2011.
// Distributed under the terms of an MIT-style license.


package com.github.okomok


package object hano {

    /**
     * Creates an asynchronous(thread-pool) context
     */
    def async: Context = new detail.Async()

    /**
     * By-name sequence
     */
    def byName[A](body: => Seq[A]): Seq[A] = new detail.ByName(() => body)

    /**
     * A reaction as set of reactions
     */
    def multi[A](xs: Seq[Reaction[A]]): Reaction[A] = new detail.Multi(xs)

    /**
     * Builds single-or-empty sequence from an expression.
     */
    def optional[A](body: => A): Seq[A] = new detail.Optional(() => body)
}
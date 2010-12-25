

// Copyright Shunsuke Sogame 2010.
// Distributed under the terms of an MIT-style license.


package com.github.okomok
package hano
package detail


private[hano]
class OnEach[A](_1: Seq[A], _2: A => Unit) extends Forwarder[A] {
    override protected val delegate = _1.react(Reaction(_2, _ => ()))
}

private[hano]
class OnEachMatch[A](_1: Seq[A], _2: PartialFunction[A, Unit]) extends Forwarder[A] {
    override protected val delegate = _1.react(Reaction(x => if (_2.isDefinedAt(x)) _2(x), _ => ()))
}
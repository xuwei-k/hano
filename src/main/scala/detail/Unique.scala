

// Copyright Shunsuke Sogame 2010.
// Distributed under the terms of an MIT-style license.


package com.github.okomok
package hano
package detail


private[hano]
class Unique[A](_1: Seq[A]) extends SeqProxy[A] {
    override val self = _1.uniqueBy(_ == _)
    override def unique: Seq[A] = this // unique.unique fusion
}

private[hano]
class UniqueBy[A](_1: Seq[A], _2: (A, A) => Boolean) extends SeqAdapter[A] {
    override protected val underlying = _1
    override def forloop(f: Reaction[A]) {
        var prev: Option[A] = None
        _1 onEach { x =>
            if (prev.isEmpty || !_2(prev.get, x)) {
                f(x)
            }
            prev = Some(x)
        } onExit {
            f.exit(_)
        } start()
    }
}

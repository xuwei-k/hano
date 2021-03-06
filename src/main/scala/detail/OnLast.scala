

// Copyright Shunsuke Sogame 2010-2011.
// Distributed under the terms of an MIT-style license.


package com.github.okomok
package hano
package detail


private[hano]
class OnLast[A](_1: Seq[A], _2: Option[A] => Unit) extends SeqAdapter.Of[A](_1) {
    override def forloop(f: Reaction[A]) {
        var acc: Option[A] = None

        _1.onEnter {
            f.enter(_)
        } onEach { x =>
            acc = Some(x)
        } onExit {
            case Exit.Success => _2(acc)
            case _ => _2(None)
        } forloop(f)
    }
}

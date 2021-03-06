

// Copyright Shunsuke Sogame 2010-2011.
// Distributed under the terms of an MIT-style license.


package com.github.okomok
package hano
package detail


private[hano]
class OnHead[A](_1: Seq[A], _2: Option[A] => Unit) extends SeqAdapter.Of[A](_1) {
    override def forloop(f: Reaction[A]) {
        var go = true

        _1.onEnter {
            f.enter(_)
        } onEach { x =>
            if (go) {
                go = false
                _2(Some(x))
            }
        } onExit { _ =>
            if (go) {
                _2(None)
            }
        } forloop(f)
    }
}

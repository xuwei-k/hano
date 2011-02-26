

// Copyright Shunsuke Sogame 2010.
// Distributed under the terms of an MIT-style license.


package com.github.okomok
package hano
package detail


private[hano]
class DropWhile[A](_1: Seq[A], _2: A => Boolean) extends SeqAdapter.Class[A](_1) {
    override def forloop(f: Reaction[A]) {
        var go = false

        _1.onEnter {
            f.enter
        } onEach { x =>
            if (!go && !_2(x)) {
                go = true
            }
            if (go) {
                f(x)
            }
        } onExit {
            f.exit
        } start()
    }
}

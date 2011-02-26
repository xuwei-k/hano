

// Copyright Shunsuke Sogame 2010-2011.
// Distributed under the terms of an MIT-style license.


package com.github.okomok
package hano
package detail


private[hano]
class Last[A](_1: Seq[A]) extends SeqAdapter.Of[A](_1) {
    override def forloop(f: Reaction[A]) {
        var acc: Option[A] = None

        _1.onEnter {
            f.enter(_)
        } onEach { x =>
            acc = Some(x)
        } onExit {
            case Exit.End => {
                if (acc.isEmpty) {
                    f.exit(Exit.Failed(new NoSuchElementException("Seq.last")))
                } else {
                    f(acc.get)
                }
            }
            case q => f.exit(q)
        } start()
    }
}

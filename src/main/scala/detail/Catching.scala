

// Copyright Shunsuke Sogame 2010.
// Distributed under the terms of an MIT-style license.


package com.github.okomok
package hano
package detail


private[hano]
class Catching[A](_1: Seq[A], _2: PartialFunction[Throwable, Unit]) extends Seq[A] {
    override def close() = _1.close()
    override def context = _1.context
    override def forloop(f: Reaction[A]) {
        For(_1) { x =>
            try {
                f(x)
            } catch {
                case t: Throwable => {
                    if (_2.isDefinedAt(t)) {
                        _2(t)
                    } else {
                        throw t
                    }
                }
            }
        } AndThen {
            f.exit(_)
        }
    }
}

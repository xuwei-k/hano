

// Copyright Shunsuke Sogame 2010.
// Distributed under the terms of an MIT-style license.


package com.github.okomok
package hano
package detail


private[hano]
class React[A](_1: Seq[A], _2: Reaction[A]) extends Seq[A] {
    override def close() = _1.close()
    override def context = _1.context
    override def forloop(f: Reaction[A]) {
        _1 `for` { x =>
            _2(x)
            f(x)
        } exit { q =>
            _2.exit(q)
            f.exit(q)
        }
    }
}

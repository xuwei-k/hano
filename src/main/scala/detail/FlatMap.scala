

// Copyright Shunsuke Sogame 2010.
// Distributed under the terms of an MIT-style license.


package com.github.okomok
package hano
package detail


private[hano]
class FlatMap[A, B](_1: Seq[A], _2: A => Seq[B]) extends Seq[B] {
    override def close() = _1.close()
    override def forloop(f: Reaction[B]) {
        val _k = CallOnce[Exit] { q => f.exit(q);close() }

        For(_1) { x =>
            if (!_k.isDone) {
                For(_2(x)) { y =>
                    f(y)
                } AndThen {
                    case Exit.End => ()
                    case q => _k(q)
                }
            }
        } AndThen {
            _k
        }
    }
}
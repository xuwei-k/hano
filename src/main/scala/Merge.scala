

// Copyright Shunsuke Sogame 2010.
// Distributed under the terms of an MIT-style license.


package com.github.okomok
package hano


private class Merge[A](_1: Seq[A], _2: Seq[A]) extends Seq[A] {
    override def close() = { _1.close(); _2.close() }
    override def forloop(f: A => Unit, k: Exit => Unit) {
        val _k = CallOnce[Exit] { q => k(q) }
        val _ok = IfFirst[Exit] { _ => () } Else { q => _k(q) }
        val _no = CallOnce[Exit] { q => _k(q);close() }
        val lock = new AnyRef{}

        LockedFor(_1, lock) { x =>
            if (!_k.isDone) {
                f(x)
            }
        } AndThen {
            case Exit.End => _ok(Exit.End)
            case q => _no(q)
        }

        LockedFor(_2, lock) { y =>
            if (!_k.isDone) {
                f(y)
            }
        } AndThen {
            case Exit.End => _ok(Exit.End)
            case q => _no(q)
        }
    }
}

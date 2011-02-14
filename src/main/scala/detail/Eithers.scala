

// Copyright Shunsuke Sogame 2010.
// Distributed under the terms of an MIT-style license.


package com.github.okomok
package hano
package detail


private[hano]
class Eithers[A](_1: Seq[A]) extends SeqAdapter[Either[Exit, A]] {
    override protected val underlying = _1
    override def forloop(f: Reaction[Either[Exit, A]]) {
        _1 onEach { x =>
            f(Right(x))
        } onExit { q =>
            f(Left(q))
        } start()
    }
}

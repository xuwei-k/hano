

// Copyright Shunsuke Sogame 2010-2011.
// Distributed under the terms of an MIT-style license.


package com.github.okomok
package hano
package detail


private[hano]
class Cycle[A](_1: Seq[A]) extends SeqProxy[A] {
    override val self = _1.repeatWhile {
        case Some(Exit.Success) => true
        case Some(Exit.Failure(_)) => false
        case None => true
    }
}

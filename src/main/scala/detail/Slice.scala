

// Copyright Shunsuke Sogame 2010-2011.
// Distributed under the terms of an MIT-style license.


package com.github.okomok
package hano
package detail


private[hano]
class Slice[A](_1: Seq[A], _2: Int, _3: Int) extends SeqProxy[A] {
    Require.nonnegative(_2, "slice start position")
    Require.range(_2, _3, "slice")
    override val self = _1.drop(_2).take(_3 - _2)
}

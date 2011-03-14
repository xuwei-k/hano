

// Copyright Shunsuke Sogame 2010-2011.
// Distributed under the terms of an MIT-style license.


package com.github.okomok
package hano
package detail


import scala.collection.immutable.{IndexedSeq, Vector}
import scala.collection.JavaConversions


private[hano]
class Buffered[A](_1: Seq[A], _2: Int) extends SeqAdapter.Of[IndexedSeq[A]](_1) {
    override def forloop(f: Reaction[IndexedSeq[A]]) {
        val buf = new AdjacentBuffer[A](_2)

        _1.onEnter {
            f.enter(_)
        } onEach { x =>
            buf.addLast(x)
            if (buf.isFull) {
                f(buf.toIndexedSeq)
                buf.removeFirst()
            }
        } onExit {
            f.exit(_)
        } start()
    }
}


private[hano]
class AdjacentBuffer[A](capacity: Int) {
    Pre.positive(capacity, "buffered")

    private[this] val impl = new java.util.ArrayList[A](capacity)

    def isFull: Boolean = impl.size == capacity

    def removeFirst() = impl.remove(0)

    def addLast(x: A) {
        assert(!isFull)
        impl.add(x)
    }

    def toIndexedSeq: IndexedSeq[A] = {
        import JavaConversions._
        Vector.empty ++ impl
    }
}
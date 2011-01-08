

// Copyright Shunsuke Sogame 2010.
// Distributed under the terms of an MIT-style license.


package com.github.okomok
package hano


// See: ScalaFlow
//   at http://github.com/hotzen/ScalaFlow/raw/master/thesis.pdf


import java.util.concurrent.locks.ReentrantLock


/**
 * Asynchronous channel
 */
final class Channel[A](override val context: Context = Context.async) extends Seq[A] {
    require(context ne Context.self)

    private class Node[A] {
        val value = new Val[A](context) // shares context.
        var next: Node[A] = null
    }

    private[this] var readNode = new Node[A]
    private[this] var writeNode = readNode

    private[this] val readLock = new ReentrantLock
    private[this] val writeLock = new ReentrantLock

    override def forloop(f: Reaction[A]) {
        readLock.lock()
        val v = try {
            if (readNode.next == null) {
                writeLock.lock()
                try {
                    if (readNode.next == null) {
                        readNode.next = new Node[A]
                    }
                } finally {
                    writeLock.unlock()
                }
            }

            val w = readNode.value
            readNode = readNode.next
            w
        } finally {
            readLock.unlock()
        }
        v.forloop(f)
    }

    def read = toCps

    def write(x: A) {
        writeLock.lock()
        val v = try {
            val w = writeNode.value
            if (writeNode.next == null) {
                writeNode.next = new Node[A]
            }
            writeNode = writeNode.next
            w
        } finally {
            writeLock.unlock()
        }
        v := x
    }

    def loop: Seq[A] = {
        new Channel.LoopAsync(this)
        /*
        if (context eq Context.self) {
            new Channel.LoopSelf(this)
        } else {
            new Channel.LoopAsync(this)
        }
        */
    }
}


object Channel {

    private class LoopAsync[A](_1: Channel[A]) extends Seq[A] {
        assert(_1.context ne Context.self)
        override def context = _1.context
        override def forloop(f: Reaction[A]) {
            val _k = detail.ExitOnce { q => f.exit(q) }

            def g() {
                detail.For(_1) { x =>
                    _k.beforeExit {
                        f(x)
                        g()
                    }
                } AndThen {
                    case q @ Exit.Failed(t) => f.exit(q)
                    case _ => ()
                }
            }
            g()
        }
    }

    /*
    // Avoid stack-overflow in case write is bazillion before read.
    private class LoopSelf[A](_1: Channel[A]) extends Seq[A] {
        assert(_1.context eq Context.self)
        override def context = _1.context
        override def forloop(f: Reaction[A]) {
            val _k = detail.ExitOnce { q => f.exit(q) }

            @annotation.tailrec
            def g() {
                var y: Option[A] = None
                detail.For(_1) { x =>
                    _k.beforeExit {
                        y = Some(x)
                    }
                } AndThen {
                    case q @ Exit.Failed(t) => f.exit(q)
                    case _ => ()
                }
                if (!y.isEmpty) {
                    f(y.get)
                    g()
                }
            }
            g()
        }
    }
    */
}



// Copyright Shunsuke Sogame 2010.
// Distributed under the terms of an MIT-style license.


package com.github.okomok
package hano


/**
 * Call-site context
 */
object Self extends Context {
    override def close() = ()

    override def forloop(f: Reaction[Unit]) {
        f.enter()
        f._do {
            f()
        }
        f.exit(Exit.End)
    }
}

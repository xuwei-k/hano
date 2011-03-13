

// Copyright Shunsuke Sogame 2010-2011.
// Distributed under the terms of an MIT-style license.


package com.github.okomok
package hano


import java.util.concurrent.TimeUnit


/**
 * Period
 */
sealed abstract class Within {
    def toMillis: Long
}

object Within {
    /**
     * The infinite time
     */
    case object Inf extends Within {
        override def toMillis: Long = throw new UnsupportedOperationException("Inf.toMillis")
    }

    /**
     * Specified by `TimeUnit`.
     */
    case class Elapse(duration: Long, unit: TimeUnit) extends Within {
        override def toMillis: Long = unit.toMillis(duration)
    }

    @annotation.returnThat
    def from(that: Within): Within = that

    @annotation.compatibleConversion
    implicit def fromMillis(t: Long): Within = Elapse(t, TimeUnit.MILLISECONDS)

    @annotation.equivalentTo("Elapse(_duration, _unit)")
    def apply(_duration: Long, _unit: TimeUnit): Within = Elapse(_duration, _unit)
}

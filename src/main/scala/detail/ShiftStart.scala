

// Copyright Shunsuke Sogame 2010-2011.
// Distributed under the terms of an MIT-style license.


package com.github.okomok
package hano
package detail


import scala.actors.Actor


private[hano]
class ShiftStart[A](_1: Seq[A], _2: Seq[_]) extends SeqProxy[A] {
    Require.notUnknown(_2.process, "shiftStart")

    override val self: Seq[A] = {
        if (_1.process eq Self) {
            new ShiftStartFromSelf(_1, _2)
        } else {
            new ShiftStartFromOther(_1, _2)
        }
    }

    override def shiftStart(that: Seq[_]): Seq[A] = { // shiftStart.shiftStart fusion
        if (that.process eq _2.process) {
            _1.shiftStart(_2)
        } else {
            super.shiftStart(that)
        }
    }
}


private[hano]
class ShiftStartFromOther[A](_1: Seq[A], _2: Seq[_]) extends SeqAdapter.Of[A](_1) {
    assert(_1.process ne Self)

    override def forloop(f: Reaction[A]) {
        _2.process.invoke {
            _1.forloop(f)
        }
    }
}


private[hano]
class ShiftStartFromSelf[A](_1: Seq[A], _2: Seq[_]) extends SeqAdapter.Of[A](_1) {
    assert(_1.process eq Self)

    override def forloop(f: Reaction[A]) {
        val cur = Actor.self

        _2.process.invoke {
            _1.onEnter { p =>
                cur ! Action {
                    f.enter {
                        Exit { q =>
                            cur ! Close
                        }
                    }
                    f.enter(p)
                }
            } onEach { x =>
                f.beforeExit {
                    cur ! Action {
                        f(x)
                    }
                }
            } onExit { q =>
                f.beforeExit {
                    cur ! Action {
                        f.exit(q)
                    }
                }
            } start()
        }

        var go = true
        while (go) {
            Actor.receive {
                case Action(f) => f()
                case Close => go = false
            }
        }
    }
}

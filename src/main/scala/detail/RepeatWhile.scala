

// Copyright Shunsuke Sogame 2010-2011.
// Distributed under the terms of an MIT-style license.


package com.github.okomok
package hano
package detail


private[hano]
class RepeatWhile[A](_1: Seq[A], _2: Option[Exit.Status] => Boolean) extends SeqProxy[A] {
    override val self = {
        if (_1.process eq Self) {
            new RepeatWhileSelf(_1, _2)
        } else {
            new RepeatWhileOther(_1, _2)
        }
    }
}


private[hano]
class RepeatWhileOther[A](_1: Seq[A], _2: Option[Exit.Status] => Boolean) extends SeqAdapter.Of[A](_1) {
    assert(_1.process ne Self)

    override def forloop(f: Reaction[A]) {
        val loop = new Loop

        def rec() {
            var _p: Exit = null

            _1.onEnter { p =>
                loop.begin {
                    f.enter(loop.exit)
                    if (!_2(None)) {
                        f.exit()
                    }
                }
                _p = p
                f.enter(_p)
                loop.breakable(f)
            } onEach { x =>
                f.beforeExit {
                    f(x)
                    loop.breakable(f)
                }
            } onExit { q =>
                f.beforeExit {
                    if (_2(Some(q))) {
                        loop.breakable(f)
                        if (!loop.breaks) {
                            f.unenter(_p) // keeps Exit.Queue small.
                            rec()
                        }
                    } else {
                        f.exit(q)
                    }
                }
            } start()
        }

        rec()
    }
}


// Specialized to avoid stack-overflow.
private[hano]
class RepeatWhileSelf[A](_1: Seq[A], _2: Option[Exit.Status] => Boolean) extends SeqAdapter.Of[A](_1) {
    assert(_1.process eq Self)

    override def forloop(f: Reaction[A]) {
        val loop = new Loop

        var go = true
        while (go) {
            go = false
            var _p: Exit = null

            _1.onEnter { p =>
                loop.begin {
                    f.enter(loop.exit)
                    if (!_2(None)) {
                        f.exit()
                    }
                }
                _p = p
                f.enter(_p)
                loop.breakable(f)
            } onEach { x =>
                f.beforeExit {
                    f(x)
                    loop.breakable(f)
                }
            } onExit { q =>
                f.beforeExit {
                    if (_2(Some(q))) {
                        loop.breakable(f)
                        if (!loop.breaks) {
                            f.unenter(_p)
                            go = true
                        }
                    } else {
                        f.exit(q)
                    }
                }
            } start()
        }
    }
}

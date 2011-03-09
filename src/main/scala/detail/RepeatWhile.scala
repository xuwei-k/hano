

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
        @volatile var status = Exit.Success.asStatus
        @volatile var isActive = true
        val begin = new DoOnce

        def rec() {
            _1.onEnter { p =>
                f.enter {
                    Exit { q =>
                        p(q)
                        status = Exit.Failure(Exit.ByOther(q))
                        isActive = false
                    }
                }
                begin {
                    if (!_2(None)) {
                        f.exit(Exit.Success)
                    }
                }
            } onEach { x =>
                f.beforeExit {
                    f(x)
                    if (!isActive) {
                        f.exit(status) // exit immediately
                    }
                }
            } onExit { q =>
                f.beforeExit {
                    if (_2(Some(q))) {
                        if (isActive) {
                            rec()
                        } else {
                            f.exit(status)
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
        @volatile var status = Exit.Success.asStatus
        @volatile var isActive = true
        val begin = new DoOnce

        var go = true
        while (go) {
            go = false
            _1.onEnter { p =>
                f.enter {
                    Exit { q =>
                        p(q)
                        status = Exit.Failure(Exit.ByOther(q))
                        isActive = false
                    }
                }
                begin {
                    if (!_2(None)) {
                        f.exit(Exit.Success)
                    }
                }
            } onEach { x =>
                f.beforeExit {
                    f(x)
                    if (!isActive) {
                        f.exit(status) // exit immediately
                    }
                }
            } onExit { q =>
                f.beforeExit {
                    if (_2(Some(q))) {
                        if (isActive) {
                            go = true
                        } else {
                            f.exit(status)
                        }
                    } else {
                        f.exit(q)
                    }
                }
            } start()
        }
    }
}
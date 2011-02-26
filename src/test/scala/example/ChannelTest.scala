
package com.github.okomok.hanotest.example
import com.github.okomok.hano

/**
 * `Channel` is a mutable single-element sequence whose value varies every foreach you call.
 * `Channel` methods are thread-safe.
 */
class ChannelTest extends org.scalatest.junit.JUnit3Suite {

    /**
     * `write` updates the `Channel` value.
     */
    def testWrite {
        val ch = new hano.Channel[Int]
        ch write 1

        locally {
            // `Channel` is a single-element sequence.
            var out: Option[Int] = None
            for (x <- ch) {
                out = Some(x)
            }
            Thread.sleep(500) // `Channel` context is asynchronous.
            expect(1)(out.get)
        }

        ch write 2
        ch write 3

        locally {
            // `Channel` is regarded as mutable, for its value is changed.
            var out: List[Int] = Nil
            for (x <- ch) {
                out :+= x
            }
            for (x <- ch) {
                out :+= x
            }
            Thread.sleep(500)
            expect(List(2,3))(out)
        }
    }

    /**
     * Even better, `read`(blocking) can retrieve the values.
     */
    def testRead {
        val ch = new hano.Channel[Int]
        ch write 1
        ch write 2
        ch write 3

        var out: List[Int] = Nil
        out :+= ch.read
        out :+= ch.read
        out :+= ch.read

        expect(List(1,2,3))(out)
    }

    /**
     * Recall `loop` cycles a sequence infinitely. So that, ...
     */
    def testLoop {
        val ch = new hano.Channel[Int]
        ch write 1
        ch write 2
        ch write 3

        var out: List[Int] = Nil
        ch.loop take {
            3
        } onEach { x =>
            out :+= x
        } await()

        expect(List(1,2,3))(out)
    }

    /**
     * Like `Val`, you can write a single-element sequence into `Channel`.
     */
    def testOutput {
        val xs = hano.async.loop.pull(1 until 6)

        val ch = new hano.Channel[Int]
        xs fork { xs =>
            ch << xs.reduceLeft(_ + _)
        } fork { xs =>
            ch << xs.reduceLeft(_ * _)
        } start()

        var out: List[Int] = Nil
        out :+= ch.read
        out :+= ch.read

        expect(List(1+2+3+4+5,1*2*3*4*5))(out)
    }
}
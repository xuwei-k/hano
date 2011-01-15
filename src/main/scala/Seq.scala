

// Copyright Shunsuke Sogame 2010.
// Distributed under the terms of an MIT-style license.


package com.github.okomok
package hano


import scala.util.continuations


object Seq extends detail.Conversions with detail.PseudoMethods {

    /**
     * Creates a sequence initially containing the specified elements.
     */
    def apply[A](from: A*): Seq[A] = from

    /**
     * The empty sequence
     */
    val empty: Seq[Nothing] = new detail.Empty()

    /**
     * The empty sequence without the exit reaction
     */
    val never: Seq[Nothing] = new detail.Never()

    /**
     * A single-element sequence
     */
    def single[A](x: A): Seq[A] = new detail.Single(x)

    /**
     * Turns into a by-name expression.
     */
    def byName[A](xs: => Seq[A]): Seq[A] = new detail.ByName(xs)

    @Annotation.equivalentTo("from(util.optional(body))")
    def optional[A](body: => A): Seq[A] = from {
        try {
            Some(body)
        } catch {
            case _ => None
        }
    }

//    @Annotation.equivalentTo("from(util.optionalErr(body))")
//    def optionalErr[A](body: => A): Seq[A] = from(util.optionalErr(body))
}


/**
 * Seq sequence, which is built upon asynchronous foreach.
 */
trait Seq[+A] extends java.io.Closeable {


    @Annotation.returnThis
    final def of[B >: A]: Seq[B] = this


// kernel

    /**
     * (Possibly) asynchronous foreach with end reaction.
     */
    def forloop(f: Reaction[A]): Unit

    /**
     * Context where Reactions are invoked.
     */
    def context: Context

    /**
     * Should be thread-safe and idempotent.
     */
    override def close(): Unit = ()

    @Annotation.equivalentTo("forloop(Reaction(f, _ => ()))")
    final def foreach(f: A => Unit) = forloop(Reaction(f, Exit.defaultHandler))

    @Annotation.equivalentTo("foreach(_ => ())")
    final def start(): Unit = foreach(_ => ())

    /**
     * Builds a forloop expression cute way.
     */
    final def `for`(f: A => Unit) = new detail.For(this, f)


// combinator

    def append[B >: A](that: Seq[B]): Seq[B] = new detail.Append[B](this, that)

    @Annotation.aliasOf("append")
    final def ++[B >: A](that: Seq[B]): Seq[B] = append(that)

    def merge[B >: A](that: Seq[B]): Seq[B] = new detail.Merge[B](this, that)

    def map[B](f: A => B): Seq[B] = new detail.Map(this, f)

    /**
     * Exit.End isn't sent.
     */
    def flatMap[B](f: A => Seq[B]): Seq[B] = new detail.FlatMap(this, f)

    def filter(p: A => Boolean): Seq[A] = new detail.Filter(this, p)

    final def withFilter(p: A => Boolean): Seq[A] = filter(p)

    def collect[B](f: PartialFunction[A, B]): Seq[B] = new detail.Collect(this, f)

    def remove(p: A => Boolean): Seq[A] = new detail.Remove(this, p)

    def partition(p: A => Boolean): (Seq[A], Seq[A]) = duplicate match {
        case (xs, ys) => (xs.filter(p), ys.remove(p))
    }

    def scanLeft[B](z: B)(op: (B, A) => B): Seq[B] = new detail.ScanLeft(this, z, op)

    def scanLeft1[B >: A](op: (B, A) => B): Seq[B] = new detail.ScanLeft1(this, op)

    final def scanl[B](z: B): _ScanlBy[B] = new _ScanlBy(z)
    sealed class _ScanlBy[B](z: B) {
        @Annotation.equivalentTo("scanLeft(z)(op)")
        def by(op: (B, A) => B): Seq[B] = scanLeft(z)(op)
    }

    def tail: Seq[A] = new detail.Tail(this)

    def init: Seq[A] = new detail.Init(this)

    def take(n: Int): Seq[A] = new detail.Take(this, n)

    def drop(n: Int): Seq[A] = new detail.Drop(this, n)

    def slice(n: Int, m: Int): Seq[A] = new detail.Slice(this, n, m)

    def takeWhile(p: A => Boolean): Seq[A] = new detail.TakeWhile(this, p)

    def dropWhile(p: A => Boolean): Seq[A] = new detail.DropWhile(this, p)

    def span(p: A => Boolean): (Seq[A], Seq[A]) = duplicate match {
        case (xs, ys) => (xs.takeWhile(p), ys.dropWhile(p))
    }

    def splitAt(n: Int): (Seq[A], Seq[A]) = {
        detail.Pre.nonnegative(n, "splitAt")
        duplicate match {
            case (xs, ys) => (xs.take(n), ys.drop(n))
        }
    }

    def flatten[B](implicit pre: Seq[A] <:< Seq[Seq[B]]): Seq[B] = new detail.Flatten(pre(this))

    /**
     * Steps by the specified stride.
     */
    def step(n: Int): Seq[A] = new detail.Step(this, n)

    /**
     * Steps by the specified time-span(millisecond).
     */
    def stepTime(i: Long): Seq[A] = new detail.StepTime(this, i)

    /**
     * Removes duplicates using <code>==</code>.
     */
    def unique: Seq[A] = new detail.Unique(this)

    /**
     * Removes duplicates using the predicate.
     */
    def uniqueBy(p: (A, A) => Boolean): Seq[A] = new detail.UniqueBy(this, p)

    /**
     * Flattens <code>vs</code>, each Seq appending <code>sep</code> except the last one.
     */
    def unsplit[B](sep: Seq[B])(implicit pre: Seq[A] <:< Seq[Seq[B]]): Seq[B] = new detail.Unsplit(pre(this), sep)

    def zip[B](that: Seq[B]): Seq[(A, B)] = new detail.Zip(this, that)
    def zipWithIndex: Seq[(A, Int)] = new detail.ZipWithIndex(this)

    def unzip[B, C](implicit pre: Seq[A] <:< Seq[(B, C)]): (Seq[B], Seq[C]) = pre(this).duplicate match {
        case (xs, ys) => (xs.map(_._1), ys.map(_._2))
    }


// conversion

    @Annotation.conversion
    def breakOut[To](implicit bf: scala.collection.generic.CanBuildFrom[Nothing, A, To]): To = Sync.copy(this)(bf)()

    @Annotation.conversion @Annotation.pre("synchronous")
    def toTraversable: scala.collection.Traversable[A] = new detail.ToTraversable(this)

    @Annotation.conversion
    def toIterable: Iterable[A] = new detail.ToIterable(this)

    @Annotation.conversion
    def toIter: Iter[A] = Iter.from(toIterable)

    @Annotation.conversion
    def toResponder: Responder[A] = new detail.ToResponder(this)

    @Annotation.conversion
    final def toCps: A @continuations.cpsParam[Any, Unit] = {
        continuations.shift {
            (cont: A => Any) => foreach(new detail.DiscardValue(cont))
        }
    }

    def actor: scala.actors.Actor = scala.actors.Actor.actor(start)


// misc

    /**
     * Loops with evaluating `f`.
     */
    def react(f: Reaction[A]): Seq[A] = new detail.React(this, f)

    /**
     * Calls `f` on the exit of sequence.
     */
    def onExit(k: Exit => Unit): Seq[A] = new detail.OnExit(this, k)

    /**
     * Loops with evaluating `f`.
     */
    def onEach(f: A => Unit): Seq[A] = new detail.OnEach(this, f)

    /**
     * Loops with evaluating `f`.
     */
    def onEachMatch(f: PartialFunction[A, Unit]): Seq[A] = new detail.OnEachMatch(this, f)

    @Annotation.equivalentTo("onEach(_ => f)")
    final def doing(f: => Unit): Seq[A] = onEach(_ => f)

    /**
     * Forks.
     */
    def fork(f: Seq[A] => Seq[_]): Seq[A] = new detail.Fork(this, f)

    /**
     * Creates a duplicate.
     */
    def duplicate: (Seq[A], Seq[A]) = { val b = new detail.Duplicate(this); (b, b) }

    /**
     * Skips trailing forks.
     */
    def break: Seq[A] = new detail.Break(this)

    /**
     * Takes elements until `that` starts. `that` may be closed.
     */
    def takeUntil(that: Seq[_]): Seq[A] = new detail.TakeUntil(this, that)

    /**
     * Drops elements until `that` starts. `that` may be closed.
     */
    def dropUntil(that: Seq[_]): Seq[A] = new detail.DropUntil(this, that)

    /**
     * Calls `f` on the head of sequence.
     */
    def onHead(f: A => Unit): Seq[A] = new detail.OnHead(this, f)

    /**
     * Calls `f` on the nth of sequence.
     */
    def onNth(n: Int)(f: A => Unit): Seq[A] = new detail.OnNth(this, n, f)

    /**
     * Calls `f` on the closing of sequence.
     */
    def onClose(f: => Unit): Seq[A] = new detail.OnClose(this, f)

    /**
     * Pseudo catch-statement
     */
    def catching(f: PartialFunction[Throwable, Unit]): Seq[A] = new detail.Catching(this, f)

    /**
     * Attach a resource.
     */
    def using(c: java.io.Closeable): Seq[A] = new detail.Using(this, c)

    @Annotation.aliasOf("using(this)")
    final def used: Seq[A] = using(this)

    /**
     * Ignores `close()` call.
     */
    def protect: Seq[A] = new detail.Protect(this)

    /*
     * Tokenized by Peg.
    def tokenize[B >: A](p: Peg[B]): Seq[Vector[B]] = new detail.Tokenize[B](this, p)
     */

    /**
     * Retrieves adjacent sequences.
     */
    def adjacent(n: Int): Seq[scala.collection.immutable.IndexedSeq[A]] = new detail.Adjacent(this, n)

    /**
     * Replaces elements by those of `it`. The length of this sequence never becomes longer.
     */
    def generate[B](it: => Iter[B]): Seq[B] = new detail.Generate(this, it)

    /**
     * Replaces elements by those of `it`. The length of this sequence never be changed.
     */
    def replace[B >: A](it: => Iter[B]): Seq[B] = new detail.Replace[B](this, it)

    /**
     * Replaces elements by those of `it`. The length of this sequence never be changed.
     */
    def replaceRegion[B >: A](n: Int, m: Int, it: => Iter[B]): Seq[B] = new detail.ReplaceRegion[B](this, n, m, it)

    @Annotation.equivalentTo("replace(Stream.from(0))")
    def indices: Seq[Int] = new detail.Indices(this)

    /**
     * Reactions are invoked in the context of `that`.
     */
    def shift(that: Seq[_]): Seq[A] = new detail.Shift(this, that)

    /**
     * Elements with a break function.
     */
    def breakable: Seq[(A, () => Unit)] = new detail.Breakable(this)

    /**
     * Turns into a closeable infinite sequence of the first elements.
     */
    def loop: Seq[A] = new detail.Loop(this)

    /**
     * Turns into a closeable infinite sequence of the first elements.
     */
    def loopBy(grainSize: Int): Seq[A] = new detail.Loop(this, grainSize)

}

package unzimm

import monocle.Lens
import reftree._
import zipper._

object ZipperDiagrams {
  implicit def elideParent[A](implicit default: ToRefTree[Zipper[A]]): ToRefTree[Zipper[A]] =
    default.suppressField(3)
}

object LensDiagrams {
  trait Example[A] {
    def exemplify: A
  }

  object Example {
    def apply[A](f: ⇒ A): Example[A] = new Example[A] {
      def exemplify = f
    }
    implicit val `Int Example`: Example[Int] = Example[Int](42)
    implicit val `Long Example`: Example[Long] = Example[Long](42)
    implicit val `Char Example`: Example[Char] = Example[Char]('?')
    implicit val `String Example`: Example[String] = Example[String]("specimen")

    import shapeless._

    implicit val `HNil Example`: Example[HNil] = Example[HNil](HNil)

    implicit def `HCons Example`[H: Example, T <: HList: Example]: Example[H :: T] = Example[H :: T] {
      implicitly[Example[H]].exemplify :: implicitly[Example[T]].exemplify
    }

    implicit def `Generic Example`[A, L <: HList](
      implicit generic: Generic.Aux[A, L], hListExample: Lazy[Example[L]]
    ): Example[A] = Example[A] {
      generic.from(hListExample.value.exemplify)
    }
  }
  
  trait Marker[A] {
    def mark(value: A): A
  }

  object Marker {
    def apply[A](f: A ⇒ A): Marker[A] = new Marker[A] {
      def mark(value: A) = f(value)
    }
    implicit val `Int Marker` = Marker[Int](x ⇒ x + 1)
    implicit val `Long Marker` = Marker[Long](x ⇒ x + 1L)
    implicit val `Char Marker` = Marker[Char] { case '?' ⇒ '!'; case _ ⇒ '?' }
    implicit val `String Marker` = Marker[String](x ⇒ x + " ")

    import shapeless._

    implicit def `HCons Marker`[H: Marker, T <: HList]: Marker[H :: T] = Marker[H :: T] {
      case h :: t ⇒ implicitly[Marker[H]].mark(h) :: t
    }

    implicit def `Generic Marker`[A, L <: HList](
      implicit generic: Generic.Aux[A, L], hListMarker: Lazy[Marker[L]]
    ): Marker[A] = Marker[A] { value ⇒
      generic.from(hListMarker.value.mark(generic.to(value)))
    }
  }

  implicit val `RefTree Unzip`: Unzip[RefTree] = new Unzip[RefTree] {
    def unzip(node: RefTree): List[RefTree] = node match {
      case RefTree.Ref(_, _, children, _) ⇒ children.toList
      case _ ⇒ List.empty
    }
    def zip(node: RefTree, children: List[RefTree]): RefTree = node match {
      case ref: RefTree.Ref ⇒ ref.copy(children = children)
      case t ⇒ t
    }
  }

  // dogfooding is strong with this one:
  // we use a Zipper to provide a RefTree instance for Lens
  implicit def `Lens RefTree`[A, B](
    implicit exampleA: Example[A], refTreeA: ToRefTree[A],
    exampleB: Example[B], refTreeB: ToRefTree[B],
    marker: Marker[B]
  ): ToRefTree[Lens[A, B]] = new ToRefTree[Lens[A, B]] {
    def refTree(value: Lens[A, B]): RefTree = {
      // obtain a random value of type A
      val before = exampleA.exemplify
      // modify it using the lens and the marker function
      val after = value.modify(marker.mark)(before)
      // an example RefTree for type B used to detect similar RefTrees
      val example = exampleB.exemplify.refTree
      // compare the RefTrees before and after modification
      iterate(Zipper(before.refTree), Zipper(after.refTree), example)
    }

    def next[T](zipper: Zipper[T]): Zipper.MoveResult[T] =
      zipper.tryMoveRight.orElse(_.tryMoveUp.flatMap(next))

    def childOrNext[T](zipper: Zipper[T]): Zipper.MoveResult[T] =
      zipper.tryMoveDownLeft.orElse(next)

    def iterate(zipper1: Zipper[RefTree], zipper2: Zipper[RefTree], example: RefTree): RefTree = {
      def iterateOrCommit(updated: Zipper[RefTree], move: Zipper.Move[RefTree]) =
        (move(updated), move(zipper2)) match {
          case (Zipper.MoveResult.Success(z1), Zipper.MoveResult.Success(z2)) ⇒ iterate(z1, z2, example)
          case _ ⇒ updated.commit
        }
      (example, zipper1.focus, zipper2.focus) match {
        case (_: RefTree.Val, x: RefTree.Val, y: RefTree.Val) if x != y ⇒
          // the example is a Val, and we found two mismatching Val trees
          val updated = zipper1.set(x.copy(highlight = true))
          iterateOrCommit(updated, next)

        case (RefTree.Ref(name, _, _, _), x: RefTree.Ref, y: RefTree.Ref) if x != y && x.name == name ⇒
          // the example is a Ref, and we found two mismatching Ref trees with the same name
          val updated = zipper1.set(x.copy(highlight = true))
          iterateOrCommit(updated, next)

        case _ ⇒
          iterateOrCommit(zipper1, childOrNext)
      }
    }
  }
}

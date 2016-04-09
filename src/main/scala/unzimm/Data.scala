package unzimm

import monocle.PLens
import scalaz.Functor
import scala.language.higherKinds

object Data {
  case class Employee(
    name: String,
    salary: Long
  )

  case class Startup(
    name: String,
    ceo: Employee,
    team: List[Employee]
  )

  case class Hierarchy(
    employee: Employee,
    team: List[Hierarchy]
  )

  case class Company(
    name: String,
    hierarchy: Hierarchy
  )

  case class Tree(x: Int, c: List[Tree] = Nil)

  val vowelLens = new PLens[String, String, Char, Char] {
    def get(s: String): Char = ???
    def set(b: Char): String ⇒ String = ???
    def modifyF[F[_]: Functor](f: Char ⇒ F[Char])(s: String): F[String] = ???
    def modify(f: Char ⇒ Char): String ⇒ String = { string ⇒
      string map {
        case v @ ('A' | 'E' | 'I' | 'O' | 'U' | 'a' | 'e' | 'i' | 'o' | 'u') ⇒ f(v)
        case x ⇒ x
      }
    }
  }
}

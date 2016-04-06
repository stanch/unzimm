package unzimm

import org.scalacheck.{Arbitrary, Gen}
import scalaz.scalacheck.ScalaCheckBinding._
import scalaz.std.list._
import com.thoughtworks.each.Monadic._
import Data._

object Generators {
  implicit val arbitraryString: Arbitrary[String] = Arbitrary(Gen.oneOf("example", "specimen"))

  def employees(minSalary: Long = 1000L, maxSalary: Long = 2000): Gen[Employee] = monadic[Gen] {
    def name = faker.Name.first_name
    val salary = Gen.choose(minSalary, maxSalary).each
    Employee(name, salary)
  }

  implicit val arbitraryEmployees: Arbitrary[Employee] = Arbitrary(employees())

  def startups(revenue: Long = 10000L): Gen[Startup] = monadic[Gen] {
    def name = faker.Company.name
    val ceo = employees(revenue / 3, revenue / 2).each
    val remainder = revenue - ceo.salary
    val num = Gen.choose(4, 8).each
    val team = Gen.listOfN(num, employees(remainder / num - 300, remainder / num + 300)).each
    Startup(name, ceo, team)
  }

  implicit val arbitraryStartups: Arbitrary[Startup] = Arbitrary(startups())

  def hierarchies(depth: Int, minSalary: Long = 1000L, maxSalary: Long = 2000): Gen[Hierarchy] = monadic[Gen] {
    val boss = employees(minSalary, maxSalary).each
    val team = if (depth < 2) List.empty else {
      val num = Gen.chooseNum(1, 2).each
      Gen.listOfN(num, hierarchies(depth - 1, minSalary * 2 / 3, boss.salary)).each
    }
    Hierarchy(boss, team)
  }

  def companies(depth: Int = 3, minSalary: Long = 1000L, maxSalary: Long = 2000): Gen[Company] = monadic[Gen] {
    def name = faker.Company.name
    val hierarchy = hierarchies(depth, minSalary, maxSalary).each
    Company(name, hierarchy)
  }.retryUntil(_.name.length < 20)

  def simpleTrees(depth: Int, x: Int = 1): Gen[Tree] = monadic[Gen] {
    if (depth < 2) Tree(x) else {
      val frequencies = List.tabulate(depth * 2) { i ⇒ (i + 1) → Gen.const(i) }
      val count = Gen.frequency(frequencies: _*).each
      val indices = List.tabulate(count)(identity)
      val children = for (i ← indices.monadicLoop) yield simpleTrees(depth - 1, x * 10 + i + 1).each
      Tree(x, children)
    }
  }
}

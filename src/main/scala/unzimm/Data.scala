package unzimm

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
}

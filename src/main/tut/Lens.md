## Lens

### Example

Updating immutable data structures can be tricky.
For case classes Scala gives us the `copy` method:

```scala
case class Employee(
  name: String,
  salary: Long
)
```

```tut
import unzimm.Data._
import unzimm.Generators._

val employee = employees().sample.get
val raisedEmployee = employee.copy(salary = employee.salary + 10)
```

However once composition comes into play, the resulting nested immutable data structures
would require a lot of `copy` calls: 

```scala
case class Employee(
  name: String,
  salary: Long
)

case class Startup(
  name: String,
  ceo: Employee,
  team: List[Employee]
)
```

```tut
val startup = startups().sample.get
val raisedCeo = startup.copy(
  ceo = startup.ceo.copy(
    salary = startup.ceo.salary + 10
  )
)
```

```tut:invisible
import reftree._
import ToRefTree.Simple.list
import java.nio.file.Paths
val path = Paths.get("images/lens")
import unzimm.LensDiagrams._
```

```tut:invisible
DotPlotter(path.resolve("startup.png")).plot(startup, raisedCeo)
```

<p align="center"><img src="images/lens/startup.png" width="50%" /></p>

Ouch!

A common solution to this problem is a “lens”.
In the simplest case a lens is a pair of functions to get and set a value of type `B` inside a value of type `A`.
It’s called a lens because it focuses on some part of the data and allows to update it.
For example, here is a lens that focuses on an employee’s salary
(using the excellent [Monocle library](https://github.com/julien-truffaut/Monocle)):

```tut
import monocle.macros.GenLens

val salaryLens = GenLens[Employee](_.salary)

salaryLens.get(startup.ceo)
salaryLens.modify(s => s + 10)(startup.ceo)
```

```tut:invisible
DotPlotter(path.resolve("salaryLens.png")).plot(salaryLens)
```

<p align="center"><img src="images/lens/salaryLens.png" width="50%" /></p>

We can also define a lens that focuses on the startup’s CEO:

```tut
val ceoLens = GenLens[Startup](_.ceo)

ceoLens.get(startup)
```

```tut:invisible
DotPlotter(path.resolve("ceoLens.png")).plot(ceoLens)
```

<p align="center"><img src="images/lens/ceoLens.png" width="50%" /></p>

It’s not apparent yet how this would help, but the trick is that lenses can be composed:

```tut
val ceoSalaryLens = ceoLens composeLens salaryLens

ceoSalaryLens.get(startup)
ceoSalaryLens.modify(s => s + 10)(startup)
```

```tut:invisible
DotPlotter(path.resolve("ceoSalaryLens.png")).plot(ceoSalaryLens)
```

<p align="center"><img src="images/lens/ceoSalaryLens.png" width="50%" /></p>

One interesting thing is that lenses can focus on anything, not just direct attributes of the data.
Here is a lens that focuses on all vowels in a string:

```tut:silent
import unzimm.LensDiagrams.vowelLens
```

```tut:invisible
DotPlotter(path.resolve("vowelLens.png")).plot(vowelLens)
```

<p align="center"><img src="images/lens/vowelLens.png" width="50%" /></p>

We can use it to give our CEO a funny name:

```tut
val employeeNameLens = GenLens[Employee](_.name)
val ceoVowelLens = ceoLens composeLens employeeNameLens composeLens vowelLens

ceoVowelLens.modify(v => v.toUpper)(startup)
```

```tut:invisible
DotPlotter(path.resolve("ceoVowelLens.png")).plot(ceoVowelLens)
```

<p align="center"><img src="images/lens/ceoVowelLens.png" width="50%" /></p>

So far we have replaced the `copy` boilerplate with a number of lens declarations.
However most of the time our goal is just to update data.

In Scala there is a great library called [quicklens](https://github.com/adamw/quicklens)
that allows to do exactly that, creating all the necessary lenses under the hood:

```tut
import com.softwaremill.quicklens._

val raisedCeo = startup.modify(_.ceo.salary).using(s => s + 10)
```

You might think this is approaching the syntax for updating mutable data,
but actually we have already surpassed it, since lens are much more flexible:


```tut
import com.softwaremill.quicklens._

val raisedEveryone = startup.modifyAll(_.ceo.salary, _.team.each.salary).using(s => s + 10)
```

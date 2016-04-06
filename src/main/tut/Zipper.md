## Zipper

### Example

In our domain models we are often faced with recursive data structures.
Consider this example:

```scala
case class Employee(
  name: String,
  salary: Long
)

case class Hierarchy(
  employee: Employee,
  team: List[Hierarchy]
)

case class Company(
  name: String,
  hierarchy: Hierarchy
)
```

The `Hierarchy` class refers to itself.
Let’s grab a random company object and display its hierarchy as a tree:

```tut:invisible
import reftree._
import ToRefTree.Simple.list
import java.nio.file.Paths
val path = Paths.get("images/zipper")
```

```tut:silent
import unzimm.Data._
import unzimm.Generators._

val company = companies(depth = 3).sample.get
val hierarchy = company.hierarchy
```

```tut:invisible
DotPlotter(path.resolve("company.png")).plot(hierarchy)
```

<p align="center"><img src="images/zipper/company.png" width="50%" /></p>

What if we want to navigate through this tree and modify it along the way?
We can use [lens](Lens.md), but the recursive nature of the tree allows for a better solution.

This solution is called a “Zipper”, and was introduced by Gérard Huet in 1997.
It consists of a “cursor” pointing to a location anywhere in the tree — “current focus”.
The cursor can be moved freely with operations like `moveDownLeft`, `moveRight`, `moveUp`, etc.
Current focus can be updated, deleted, or new nodes can be inserted to its left or right.
Zippers are immutable, and every operation returns a new Zipper.
All the changes made to the tree can be committed, yielding a new modified version of the original tree.

Here is how we would insert a new employee into the hierarchy:

```tut:silent
import zipper._

val newEmployee = Hierarchy(employees().sample.get, team = List.empty)
val updatedHierarchy = Zipper(hierarchy).moveDownRight.moveDownRight.insertRight(newEmployee).commit
```

```tut:invisible
DotPlotter(path.resolve("updatedHierarchy.png")).plot(hierarchy, updatedHierarchy)
```

<p align="center"><img src="images/zipper/updatedHierarchy.png" width="50%" /></p>

My [zipper library](https://github.com/stanch/zipper#zipper--an-implementation-of-huets-zipper)
provides a few useful movements and operations.

### How it works

Let’s consider a simpler recursive data structure:

```scala
case class Tree(x: Int, c: List[Tree] = List.empty)
```

and a simple tree:

```tut:silent
val tree1 = Tree(
  1, List(
    Tree(2),
    Tree(3),
    Tree(4),
    Tree(5)
  )
)
```

```tut:invisible
DotPlotter(path.resolve("tree1.png")).plot(tree1)
```

<p align="center"><img src="images/zipper/tree1.png" width="50%" /></p>

When we wrap a Zipper around this tree, it does not look very interesting yet:

```tut:silent
import zipper._

val zipper1 = Zipper(tree1)
```

```tut:invisible
DotPlotter(path.resolve("zipper1.png")).plot(tree1, zipper1)
```

<p align="center"><img src="images/zipper/zipper1.png" width="50%" /></p>

We can see that it just points to the original tree and has some other empty fields.
More specifically, a Zipper consists of four pointers:

```scala
case class Zipper[A](
  focus: A,                // the current focus
  left: List[A],           // left siblings of the focus
  top: Option[Zipper[A]],  // the parent zipper
  right: List[A]           // right siblings of the focus
)
```

In this case the focus is the root of the tree, which has no siblings,
and the parent zipper does not exist, since we are at the top level.

One thing we can do right away is modify the focus:

```tut:silent
val zipper2 = zipper1.update(focus ⇒ focus.copy(x = focus.x + 99))
```

```tut:invisible
DotPlotter(path.resolve("zipper2.png")).plot(tree1, zipper1, zipper2)
```

<p align="center"><img src="images/zipper/zipper2.png" width="50%" /></p>

We just created a new tree! To obtain it, we have to commit the changes:

```tut:silent
val tree2 = zipper2.commit
```

```tut:invisible
DotPlotter(path.resolve("tree2.png")).plot(tree1, tree2)
```

<p align="center"><img src="images/zipper/tree2.png" width="50%" /></p>

If you were following closely,
you would notice that nothing spectacular happened yet:
we could’ve easily obtained the same result by modifying the tree directly:

```tut:silent
val tree2b = tree1.copy(x = tree1.x + 99)

assert(tree2b == tree2)
```

The power of Zipper becomes apparent when we go one or more levels deep.
To move down the tree, we “unzip” it, separating the child nodes into
the focused node and its left and right siblings:

```tut:silent
val zipper2 = zipper1.moveDownLeft
```

```tut:invisible
DotPlotter(path.resolve("zipper1+2.png")).plot(zipper1, zipper2)
```

<p align="center"><img src="images/zipper/zipper1+2.png" width="50%" /></p>

The new Zipper links to the old one,
which will allow us to return to the root of the tree when we are done applying changes.
This link however prevents us from seeing the picture clearly.
Let’s elide the parent field:

```tut:silent
import unzimm.ZipperDiagrams.elideParent
```

```tut:invisible
DotPlotter(path.resolve("zipper2b.png")).plot(zipper2)
```

<p align="center"><img src="images/zipper/zipper2b.png" width="50%" /></p>

Great! We have `2` in focus and `3, 4, 5` as right siblings. What happens if we move right a bit?

```tut:silent
val zipper3 = zipper2.moveRightBy(2)
```

```tut:invisible
DotPlotter(path.resolve("zipper3.png")).plot(zipper3)
```

<p align="center"><img src="images/zipper/zipper3.png" width="50%" /></p>

This is interesting! Notice that the left siblings are “inverted”.
This allows to move left and right in constant time, because the sibling
adjacent to the focus is always at the head of the list.

This also allows us to insert new siblings easily:

```tut:silent
val zipper4 = zipper3.insertLeft(Tree(34))
```

```tut:invisible
DotPlotter(path.resolve("zipper4.png")).plot(zipper4)
```

<p align="center"><img src="images/zipper/zipper4.png" width="50%" /></p>

And, as you might know, we can delete nodes and update the focus:

```tut:silent
val zipper5 = zipper4.deleteAndMoveRight.set(Tree(45))
```

```tut:invisible
DotPlotter(path.resolve("zipper5.png")).plot(zipper5)
```

<p align="center"><img src="images/zipper/zipper5.png" width="50%" /></p>

Finally, when we move up, the siblings at the current level are “zipped”
together and their parent node is updated:

```tut:silent
val zipper6 = zipper5.moveUp
```

```tut:invisible
DotPlotter(path.resolve("zipper6.png")).plot(zipper6)
```

<p align="center"><img src="images/zipper/zipper6.png" width="50%" /></p>

You can probably guess by now that `.commit` is a shorthand for going
all the way up (applying all the changes) and returning the focus:

```tut:silent
val tree3a = zipper5.moveUp.focus
val tree3b = zipper5.commit

assert(tree3a == tree3b)
```

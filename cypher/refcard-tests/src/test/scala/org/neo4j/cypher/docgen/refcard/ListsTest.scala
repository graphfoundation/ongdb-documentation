/*
 * Copyright (c) 2002-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.docgen.refcard

import org.neo4j.cypher.QueryStatisticsTestSupport
import org.neo4j.cypher.docgen.RefcardTest
import org.neo4j.cypher.internal.runtime.InternalExecutionResult

class ListsTest extends RefcardTest with QueryStatisticsTestSupport {
  val graphDescription = List("A KNOWS B")
  val title = "Lists"
  override val linkId = "syntax/lists"

  override def assert(name: String, result: InternalExecutionResult) {
    name match {
      case "returns-two" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 2)
      case "returns-one" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 1)
      case "returns-none" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 0)
    }
  }

  override def parameters(name: String): Map[String, Any] =
    name match {
      case "parameters=name" =>
        Map("value" -> "Bob")
      case "parameters=names" =>
        Map("names" -> List("A", "B"))
      case "parameters=list" =>
        Map("list" -> List(1, 2, 3))
      case "parameters=range" =>
        Map("firstNum" -> 1, "lastNum" -> 10, "step" -> 2)
      case "parameters=subscript" =>
        Map("startIdx" -> 1, "endIdx" -> -1, "idx" -> 0)
      case "" =>
        Map()
    }

  override val properties: Map[String, Map[String, Any]] = Map(
    "A" -> Map("name" -> "Alice", "list" -> Array(1, 2, 3), "age" -> 30),
    "B" -> Map("name" -> "Bob", "list" -> Array(1, 2, 3), "age" -> 40))

  def text = """
###assertion=returns-one
RETURN

['a', 'b', 'c'] AS list

###

Literal lists are declared in square brackets.

###assertion=returns-one parameters=list
RETURN

size($list) AS len, $list[0] AS value

###

Lists can be passed in as parameters.

###assertion=returns-one parameters=range
RETURN

range($firstNum, $lastNum, $step) AS list

###

`range()` creates a list of numbers (`step` is optional), other functions returning lists are:
`labels()`, `nodes()`, `relationships()`, `filter()`, `extract()`.

###assertion=returns-one
//

MATCH p = (a)-[:KNOWS*]->()
RETURN relationships(p) AS r

###

The list of relationships comprising a variable length path can be returned using named paths and `relationships()`.

###assertion=returns-two
MATCH (matchedNode)

RETURN matchedNode.list[0] AS value,
       size(matchedNode.list) AS len

###

Properties can be lists of strings, numbers or booleans.

###assertion=returns-one parameters=subscript
WITH [1, 2, 3] AS list
RETURN

list[$idx] AS value,
list[$startIdx..$endIdx] AS slice

###

List elements can be accessed with `idx` subscripts in square brackets.
Invalid indexes return `null`.
Slices can be retrieved with intervals from `start_idx` to `end_idx`, each of which can be omitted or negative.
Out of range elements are ignored.

###assertion=returns-one parameters=names
//

UNWIND $names AS name
MATCH (n {name: name})
RETURN avg(n.age)

###

With `UNWIND`, any list can be transformed back into individual rows.
The example matches all names from a list of names.

###assertion=returns-two parameters=names
//

MATCH (a)
RETURN [(a)-->(b) WHERE b.name = 'Bob' | b.age]

###

Pattern comprehensions may be used to do a custom projection from a match directly into a list.

###assertion=returns-two parameters=names
//

MATCH (person)
RETURN person { .name, .age}

###

Map projections may be easily constructed from nodes, relationships and other map values.
"""
}

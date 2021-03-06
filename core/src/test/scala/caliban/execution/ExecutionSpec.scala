package caliban.execution

import caliban.GraphQL._
import caliban.Macros.gqldoc
import caliban.RootResolver
import caliban.TestUtils._
import caliban.parsing.adt.Value.{ BooleanValue, StringValue }
import zio.test.Assertion._
import zio.test._

object ExecutionSpec
    extends DefaultRunnableSpec(
      suite("ExecutionSpec")(
        testM("skip directive") {
          val interpreter = graphQL(resolver)
          val query       = gqldoc("""
            query test {
              amos: character(name: "Amos Burton") {
                name
                nicknames @skip(if: true)
              }
            }""")

          assertM(
            interpreter.execute(query).map(_.toString),
            equalTo("""{"amos":{"name":"Amos Burton"}}""")
          )
        },
        testM("simple query with fields") {
          val interpreter = graphQL(resolver)
          val query       = gqldoc("""
            {
              characters {
                name
              }
            }""")

          assertM(
            interpreter.execute(query).map(_.toString),
            equalTo(
              """{"characters":[{"name":"James Holden"},{"name":"Naomi Nagata"},{"name":"Amos Burton"},{"name":"Alex Kamal"},{"name":"Chrisjen Avasarala"},{"name":"Josephus Miller"},{"name":"Roberta Draper"}]}"""
            )
          )
        },
        testM("arguments") {
          val interpreter = graphQL(resolver)
          val query       = gqldoc("""
            {
              characters(origin: MARS) {
                name
                nicknames
              }
            }""")

          assertM(
            interpreter.execute(query).map(_.toString),
            equalTo(
              """{"characters":[{"name":"Alex Kamal","nicknames":[]},{"name":"Roberta Draper","nicknames":["Bobbie","Gunny"]}]}"""
            )
          )
        },
        testM("arguments with list coercion") {
          val interpreter = graphQL(resolver)
          val query       = gqldoc("""
            {
              charactersIn(names: "Alex Kamal") {
                name
              }
            }""")

          assertM(
            interpreter.execute(query).map(_.toString),
            equalTo("""{"charactersIn":[{"name":"Alex Kamal"}]}""")
          )
        },
        testM("aliases") {
          val interpreter = graphQL(resolver)
          val query       = gqldoc("""
             {
               amos: character(name: "Amos Burton") {
                 name
                 nicknames
               }
             }""")

          assertM(
            interpreter.execute(query).map(_.toString),
            equalTo("""{"amos":{"name":"Amos Burton","nicknames":[]}}""")
          )
        },
        testM("fragment") {
          val interpreter = graphQL(resolver)
          val query       = gqldoc("""
             {
               amos: character(name: "Amos Burton") {
                 ...info
               }
             }
               
             fragment info on Character {
               name
             }""")

          assertM(
            interpreter.execute(query).map(_.toString),
            equalTo("""{"amos":{"name":"Amos Burton"}}""")
          )
        },
        testM("inline fragment") {
          val interpreter = graphQL(resolver)
          val query       = gqldoc("""
             {
               amos: character(name: "Amos Burton") {
                 name
                 role {
                   ... on Mechanic {
                     shipName
                   }
                 }
               }
             }""")

          assertM(
            interpreter.execute(query).map(_.toString),
            equalTo("""{"amos":{"name":"Amos Burton","role":{"shipName":"Rocinante"}}}""")
          )
        },
        testM("effectful query") {
          val interpreter = graphQL(resolverIO)
          val query       = gqldoc("""
               {
                 characters {
                   name
                 }
               }""")

          assertM(
            interpreter.execute(query).map(_.toString),
            equalTo(
              """{"characters":[{"name":"James Holden"},{"name":"Naomi Nagata"},{"name":"Amos Burton"},{"name":"Alex Kamal"},{"name":"Chrisjen Avasarala"},{"name":"Josephus Miller"},{"name":"Roberta Draper"}]}"""
            )
          )
        },
        testM("mutation") {
          val interpreter = graphQL(resolverWithMutation)
          val query       = gqldoc("""
               mutation {
                 deleteCharacter(name: "Amos Burton")
               }""")

          assertM(interpreter.execute(query).map(_.toString), equalTo("""{"deleteCharacter":{}}"""))
        },
        testM("variable") {
          val interpreter = graphQL(resolver)
          val query       = gqldoc("""
             query test($name: String!){
               amos: character(name: $name) {
                 name
               }
             }""")

          assertM(
            interpreter.execute(query, None, Map("name" -> StringValue("Amos Burton"))).map(_.toString),
            equalTo("""{"amos":{"name":"Amos Burton"}}""")
          )
        },
        testM("skip directive") {
          val interpreter = graphQL(resolver)
          val query       = gqldoc("""
             query test{
               amos: character(name: "Amos Burton") {
                 name
                 nicknames @skip(if: true)
               }
             }""")

          assertM(interpreter.execute(query).map(_.toString), equalTo("""{"amos":{"name":"Amos Burton"}}"""))
        },
        testM("include directive") {
          val interpreter = graphQL(resolver)
          val query       = gqldoc("""
             query test($included: Boolean!){
               amos: character(name: "Amos Burton") {
                 name
                 nicknames @include(if: $included)
               }
             }""")

          assertM(
            interpreter.execute(query, None, Map("included" -> BooleanValue(false))).map(_.toString),
            equalTo("""{"amos":{"name":"Amos Burton"}}""")
          )
        },
        testM("test Map") {
          case class Test(map: Map[Int, String])
          val interpreter = graphQL(RootResolver(Test(Map(3 -> "ok"))))
          val query       = gqldoc("""
             {
               map {
                 key
                 value
               }
             }""")

          assertM(interpreter.execute(query).map(_.toString), equalTo("""{"map":[{"key":3,"value":"ok"}]}"""))
        },
        testM("test Either") {
          case class Test(either: Either[Int, String])
          val interpreter = graphQL(RootResolver(Test(Right("ok"))))
          val query       = gqldoc("""
             {
               either {
                 left
                 right
               }
             }""")

          assertM(interpreter.execute(query).map(_.toString), equalTo("""{"either":{"left":null,"right":"ok"}}"""))
        }
      )
    )

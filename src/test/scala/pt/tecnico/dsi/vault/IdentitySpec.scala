package pt.tecnico.dsi.vault

import cats.effect.IO
import org.scalatest.{EitherValues, OptionValues}
import pt.tecnico.dsi.vault.secretEngines.identity.{AliasCRUD, BaseEndpoints}
import pt.tecnico.dsi.vault.secretEngines.identity.models._

class IdentitySpec extends Utils with EitherValues with OptionValues {
  import client.secretEngines.identity

  def baseEndpoints[T <: Base](endpoints: BaseEndpoints[IO, T], name: String, article: String, create: (String, Map[String, String]) => IO[String]): Unit = {
    import endpoints._
    s"list $name" in {
      import cats.implicits._
      val names = List.tabulate(5)(i => s"list$i")
      for {
        createdIds <- names.traverse(create(_, Map.empty))
        listedIds <- listById
        listedNames <- list
      } yield {
        listedIds should contain allElementsOf createdIds
        listedNames should contain allElementsOf names
      }
    }

    s"get $article $name" in {
      val name = "get"
      for {
        empty <- get(name)
        id <- create(name, Map.empty)
        entity <- get(name)
        entityById <- getById(id)
        _ <- delete(name) // Just so we can re-run the tests
      } yield {
        empty.isEmpty shouldBe true
        entity shouldBe entityById
      }
    }

    s"apply $article $name" in {
      import org.http4s.client.UnexpectedStatus
      val name = "apply"
      for {
        failure <- apply(name).attempt
        _ <- create(name, Map.empty)
        entity <- apply(name)
        _ <- delete(name) // Just so we can re-run the tests
      } yield {
        failure.left.value shouldBe a[UnexpectedStatus]
        failure.left.value.getMessage should include ("Not Found")
        entity.name shouldBe name
      }
    }

    s"create $article $name" in {
      val name = "create"
      for {
        id <- create(name, Map("a" -> "a"))
        id2 <- create(name, Map("b" -> "b"))
        entity <- apply(name)
        _ <- deleteById(id) // Just so we can re-run the tests
      } yield {
        id shouldBe id2
        // Create also updates
        entity.metadata should not contain ("a" -> "a")
        entity.metadata should contain ("b" -> "b")
      }
    }

    s"findOrCreate $article $name" in {
      val name = "findOrCreate"
      for {
        empty <- get(name)
        first <- findOrCreate(name)
        second <- findOrCreate(name)
        _ <- deleteById(first.id) // Just so we can re-run the tests
      } yield {
        empty.isEmpty shouldBe true
        first.id shouldBe second.id
      }
    }

    s"delete $article $name" in {
      for {
        _ <- create("first", Map.empty)
        _ <- delete("first").idempotently(_ shouldBe ())
        id <- create("second", Map.empty)
        result <- deleteById(id).idempotently(_ shouldBe ())
      } yield result
    }
  }

  "identity - entity" should {
    import identity.entity._

    behave like baseEndpoints[Entity](identity.entity, "entity", "an", (name, metadata) => create(name, metadata = metadata))

    "update an entity" in {
      val name = "update"
      for {
        id <- create(name, metadata = Map("a" -> "a"))
        entity <- apply(name)
        newName = name * 2
        _ <- update(id, newName, metadata = Map("c" -> "c")).idempotently(_ shouldBe ())
        firstUpdate <- apply(newName)
        _ <- update(entity.copy(policies = List("policy1"))).idempotently(_ shouldBe ())
        secondUpdate <- apply(name)
        _ <- deleteById(id) // Just so we can re-run the tests
      } yield {
        firstUpdate.name shouldBe newName
        firstUpdate.metadata should not contain ("a" -> "a")
        firstUpdate.metadata should contain ("c" -> "c")
        secondUpdate.name shouldBe name
        secondUpdate.policies should contain ("policy1")
      }
    }

    "addPolicies an entity" in {
      val name = "addPolicies"
      for {
        empty <- get(name)
        first <- addPolicies(name, name)
        second <- addPolicies(name, "another-policy")
        entity <- apply(name)
        _ <- delete(name) // Just so we can re-run the tests
      } yield {
        empty.isEmpty shouldBe true
        first shouldBe second
        entity.policies should contain.allOf(name, "another-policy")
      }
    }
  }

  "identity - group" should {
    import identity.group._

    behave like baseEndpoints[Group](identity.group, "group", "a", (name, metadata) => create(name, metadata = metadata))

    "update a group" in {
      val name = "update"
      for {
        id <- create(name, metadata = Map("a" -> "a"))
        entity <- apply(name)
        newName = name * 2
        _ <- update(id, newName, metadata = Map("c" -> "c")).idempotently(_ shouldBe ())
        firstUpdate <- apply(newName)
        _ <- update(entity.copy(policies = List("policy1"))).idempotently(_ shouldBe ())
        secondUpdate <- apply(name)
        _ <- deleteById(id) // Just so we can re-run the tests
      } yield {
        firstUpdate.name shouldBe newName
        firstUpdate.metadata should not contain ("a" -> "a")
        firstUpdate.metadata should contain ("c" -> "c")
        secondUpdate.name shouldBe name
        secondUpdate.policies should contain ("policy1")
      }
    }

    "addPolicies a group" in {
      val name = "addPolicies"
      for {
        empty <- get(name)
        first <- addPolicies(name, name)
        second <- addPolicies(name, "another-policy")
        entity <- apply(name)
        _ <- delete(name) // Just so we can re-run the tests
      } yield {
        empty.isEmpty shouldBe true
        first shouldBe second
        entity.policies should contain.allOf(name, "another-policy")
      }
    }

    "addMembers a group" in {
      val name = "addMembers"
      for {
        empty <- get(name)
        member1Id <- identity.entity.create("member1")
        member2Id <- identity.entity.create("member2")
        first <- addMembers(name, member1Id)
        second <- addMembers(name, member2Id)
        entity <- apply(name)
        _ <- delete(name) // Just so we can re-run the tests
      } yield {
        empty.isEmpty shouldBe true
        first shouldBe second
        entity.members should contain.allOf(member1Id, member2Id)
      }
    }

    "addSubgroups a group" in {
      val name = "addMembers"
      for {
        empty <- get(name)
        subgroup1Id <- findOrCreate("subgroup1").map(_.id)
        subgroup2Id <- findOrCreate("subgroup2").map(_.id)
        first <- addSubgroups(name, subgroup1Id)
        second <- addSubgroups(name, subgroup2Id)
        entity <- apply(name)
        _ <- delete(name) // Just so we can re-run the tests
      } yield {
        empty.isEmpty shouldBe true
        first shouldBe second
        entity.subgroups should contain.allOf(subgroup1Id, subgroup2Id)
      }
    }
  }

  def aliasCRUD[T <: Alias](aliasCrud: AliasCRUD[IO, T], create: String => IO[String], mountAccessor: IO[String]): Unit = {
    def withSubT(name: String): IO[(String, String, String)] = for {
      accessor <- mountAccessor
      id <- create(name)
      aliasId <- aliasCrud.create(name, id, accessor)
    } yield (id, accessor, aliasId)

    "list aliases" in withSubT("list-aliases").flatMap { case (_, _, aliasId) =>
      aliasCrud.list.idempotently(_ should contain (aliasId))
    }

    "get alias (existing id)" in withSubT("get-alias").flatMap { case (id, accessor, aliasId) =>
      aliasCrud.get(aliasId).idempotently { alias =>
        alias.value.id shouldBe aliasId
        alias.value.canonicalId shouldBe id
        alias.value.name shouldBe "get-alias"
        alias.value.mountAccessor shouldBe accessor
      }
    }
    "get alias (non-existing id)" in {
      aliasCrud.get("potato").idempotently(_.isEmpty shouldBe true)
    }

    "apply alias" in withSubT("apply-alias").flatMap { case (id, _, aliasId) =>
      aliasCrud(aliasId).idempotently(_.canonicalId shouldBe id)
    }

    "update alias" in {
      val newName = "new-name"
      for {
        (id, accessor, aliasId) <- withSubT("update-alias")
        _ <- aliasCrud.update(aliasId, newName, id, accessor).idempotently(_ shouldBe ())
        t <- aliasCrud(aliasId)
        _ <- aliasCrud.delete(aliasId) // So we can run the suite multiple times
      } yield t.name shouldBe newName
    }

    "delete alias" in withSubT("delete-alias").flatMap { case (_, _, aliasId) =>
      aliasCrud.delete(aliasId).idempotently(_ shouldBe())
    }
  }

  val mountAccessor: IO[String] = client.sys.auth.list.map { mountedAuths => mountedAuths("token/").accessor }
  "identity - entityAlias" should {
    behave like aliasCRUD[EntityAlias](identity.entityAlias, identity.entity.create(_), mountAccessor)
  }

  // Group Alias can only be set on external groups. We would need to mount Ldap or Github auth for the tests
  /*"identity - groupAlias" should {
    behave like aliasCRUD[GroupAlias](identity.groupAlias, identity.group.create(_), mountAccessor)
  }*/
}

package pt.tecnico.dsi.vault

import cats.effect.IO
import org.scalatest.{EitherValues, OptionValues}
import pt.tecnico.dsi.vault.secretEngines.identity.{AliasCRUD, BaseEndpoints}
import pt.tecnico.dsi.vault.secretEngines.identity.models.*

class IdentitySpec extends Utils with EitherValues with OptionValues {
  import client.secretEngines.identity

  def baseEndpoints[T <: Base](endpoints: BaseEndpoints[IO, T], name: String, article: String, create: (String, Map[String, String]) => IO[T]): Unit = {
    import endpoints.*
    s"list $name" in {
      import cats.implicits.*
      val names = List.tabulate(5)(i => s"list$i")
      for {
        createdModels <- names.traverse(create(_, Map.empty))
        listedIds <- listById
        listedNames <- list
      } yield {
        listedIds should contain allElementsOf createdModels.map(_.id)
        listedNames should contain allElementsOf names
      }
    }

    s"get $article $name" in {
      val name = "get"
      for {
        empty <- get(name)
        model <- create(name, Map.empty)
        entity <- get(name)
        entityById <- getById(model.id)
        _ <- delete(name) // Just so we can re-run the tests
      } yield {
        empty.isEmpty shouldBe true
        entity shouldBe entityById
      }
    }

    s"apply $article $name" in {
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
        model1 <- create(name, Map("a" -> "a"))
        model2 <- create(name, Map("b" -> "b"))
        entity <- apply(name)
        _ <- deleteById(model1.id) // Just so we can re-run the tests
      } yield {
        model1.id shouldBe model2.id
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
        model <- create("second", Map.empty)
        result <- deleteById(model.id).idempotently(_ shouldBe ())
      } yield result
    }
  }

  "identity - entity" should {
    import identity.entity.*

    behave like baseEndpoints[Entity](identity.entity, "entity", "an", (name, metadata) => create(name, metadata = metadata))

    "update an entity" in {
      val name = "update"
      for {
        model <- create(name, metadata = Map("a" -> "a"))
        entity <- apply(name)
        newName = name * 2
        _ <- update(model.id, newName, metadata = Map("c" -> "c")).idempotently(_ shouldBe ())
        firstUpdate <- apply(newName)
        _ <- update(entity.copy(policies = List("policy1"))).idempotently(_ shouldBe ())
        secondUpdate <- apply(name)
        _ <- deleteById(model.id) // Just so we can re-run the tests
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
    import identity.group.*

    behave like baseEndpoints[Group](identity.group, "group", "a", (name, metadata) => create(name, metadata = metadata))

    "update a group" in {
      val name = "update"
      for {
        model <- create(name, metadata = Map("a" -> "a"))
        entity <- apply(name)
        newName = name * 2
        _ <- update(model.id, newName, metadata = Map("c" -> "c")).idempotently(_ shouldBe ())
        firstUpdate <- apply(newName)
        _ <- update(entity.copy(policies = List("policy1"))).idempotently(_ shouldBe ())
        secondUpdate <- apply(name)
        _ <- deleteById(model.id) // Just so we can re-run the tests
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
        member1 <- identity.entity.create("member1")
        member2 <- identity.entity.create("member2")
        first <- addMembers(name, member1.id)
        second <- addMembers(name, member2.id)
        entity <- apply(name)
        _ <- delete(name) // Just so we can re-run the tests
      } yield {
        empty.isEmpty shouldBe true
        first shouldBe second
        entity.members should contain.allOf(member1.id, member2.id)
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

  def aliasCRUD[T <: Alias, B <: Base](aliasCrud: AliasCRUD[IO, T], create: String => IO[B], mountAccessor: IO[String]): Unit = {
    def withSubT(name: String): IO[(String, String, String)] = for {
      accessor <- mountAccessor
      model <- create(name)
      aliasId <- aliasCrud.create(name, model.id, accessor)
    } yield (model.id, accessor, aliasId)

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
    behave like aliasCRUD(identity.entityAlias, identity.entity.create(_), mountAccessor)
  }

  // Group Alias can only be set on external groups. We would need to mount Ldap or Github auth for the tests
  /*"identity - groupAlias" should {
    behave like aliasCRUD[GroupAlias](identity.groupAlias, identity.group.create(_), mountAccessor)
  }*/
}

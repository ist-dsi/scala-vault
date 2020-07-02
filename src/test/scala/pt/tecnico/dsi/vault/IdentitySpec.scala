package pt.tecnico.dsi.vault

import org.http4s.client.UnexpectedStatus
import org.scalatest.EitherValues
import cats.implicits._

class IdentitySpec extends Utils with EitherValues {
  import client.secretEngines.identity

  "identity - entity" should {
    "list entities" in {
      val names = (0 to 5).map(i => s"list$i")
      for {
        createdIds <- names.toList.traverse(identity.entity.create(_))
        listedIds <- identity.entity.listById()
        listedNames <- identity.entity.list()
      } yield {
        listedIds should contain allElementsOf createdIds
        listedNames should contain allElementsOf names
      }
    }

    "get an entity" in {
      val name = "get"
      for {
        empty <- identity.entity.get(name)
        id <- identity.entity.create(name)
        entity <- identity.entity.get(name)
        entityById <- identity.entity.getById(id)
        _ <- identity.entity.delete(name) // Just so we can re-run the tests
      } yield {
        empty shouldBe empty
        entity should not be empty
        entity shouldBe entityById
      }
    }

    "apply an entity" in {
      val name = "apply"
      for {
        failure <- identity.entity(name).attempt
        _ <- identity.entity.create(name)
        entity <- identity.entity(name)
        _ <- identity.entity.delete(name) // Just so we can re-run the tests
      } yield {
        failure.left.value shouldBe a[UnexpectedStatus]
        failure.left.value.getMessage should include ("Not Found")
        entity.name shouldBe name
      }
    }

    "create/update an entity" in {
      val name = "create"
      for {
        id <- identity.entity.create(name, metadata = Map("a" -> "a"))
        id2 <- identity.entity.create(name, metadata = Map("b" -> "b"))
        entity <- identity.entity(name)
        newName = name * 2
        _ <- identity.entity.update(id, newName, metadata = Map("c" -> "c")).idempotently(_ shouldBe ())
        updatedEntity <- identity.entity(newName)
        _ <- identity.entity.deleteById(id) // Just so we can re-run the tests
      } yield {
        id shouldBe id2
        // Create also updates
        entity.metadata should not contain ("a" -> "a")
        entity.metadata should contain ("b" -> "b")
        // Tests the update
        updatedEntity.metadata should not contain ("b" -> "b")
        updatedEntity.metadata should contain ("c" -> "c")
      }
    }

    "delete an entity by name" in {
      for {
        _ <- identity.entity.create("random")
        r <- identity.entity.delete("random").idempotently(_ shouldBe ())
      } yield r
    }
    "delete an entity by id" in {
      for {
        id <- identity.entity.create("random")
        r <- identity.entity.deleteById(id).idempotently(_ shouldBe ())
      } yield r
    }
  }
}

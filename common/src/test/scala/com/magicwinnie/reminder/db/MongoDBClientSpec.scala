package com.magicwinnie.reminder.db

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.github.nscala_time.time.Imports.{DateTime, Period}
import org.mongodb.scala.MongoCollection
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName

class MongoDBClientSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  val container: MongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"))

  private var client: MongoDBClient = _

  override def beforeAll(): Unit = {
    container.start()
    val mongoUri: String = container.getConnectionString
    client = new MongoDBClient(mongoUri)
  }

  override def afterAll(): Unit = {
    container.stop()
  }

  behavior of "MongoDBClient"

  it should "get created with correct URI" in {
    client.uri shouldBe container.getConnectionString
  }

  it should "get collection with correct name" in {
    val collectionIO: IO[MongoCollection[ReminderModel]] =
      client.getCollection[ReminderModel]("test_collection")

    val collection = collectionIO.unsafeRunSync()

    collection.namespace.getCollectionName shouldBe "test_collection"
  }

  it should "register custom codecs successfully" in {
    val collectionIO: IO[MongoCollection[ReminderModel]] =
      client.getCollection[ReminderModel]("test_collection")

    val collection = collectionIO.unsafeRunSync()

    val registry = collection.codecRegistry
    registry.get(classOf[DateTime]) should not be null
    registry.get(classOf[Period]) should not be null
  }
}

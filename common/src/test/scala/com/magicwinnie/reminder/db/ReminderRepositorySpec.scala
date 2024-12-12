package com.magicwinnie.reminder.db

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.github.nscala_time.time.Imports.DateTime
import org.joda.time.DateTimeZone
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName

class ReminderRepositorySpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  private val container = new MongoDBContainer(DockerImageName.parse("mongo:7.0"))
  private var repository: ReminderRepository[IO] = _
  private var client: MongoDBClient = _

  override def beforeAll(): Unit = {
    container.start()

    client = new MongoDBClient(container.getConnectionString)
    val collectionIO = client.getCollection[ReminderModel]("test_collection")
    repository = collectionIO.flatMap(ReminderRepository.make[IO]).unsafeRunSync()
  }

  override def afterAll(): Unit = {
    container.stop()
  }

  "ReminderRepository" should "create and delete a reminder" in {
    val reminder = ReminderModel(
      chatId = 12345L,
      name = "Test",
      executeAt = DateTime.now(DateTimeZone.UTC),
      repeatIn = None,
      timezoneID = "UTC"
    )

    repository.createReminder(reminder).unsafeRunSync()

    val reminders = repository.getRemindersForChat(12345L).unsafeRunSync()
    reminders.size shouldBe 1

    repository.deleteReminder(reminders.head._id).unsafeRunSync()

    val remindersAfterDelete = repository.getRemindersForChat(12345L).unsafeRunSync()
    remindersAfterDelete.size shouldBe 0
  }
}

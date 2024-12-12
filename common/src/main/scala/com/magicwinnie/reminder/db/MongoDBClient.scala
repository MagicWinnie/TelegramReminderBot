package com.magicwinnie.reminder.db

import cats.effect.IO
import org.bson.codecs.configuration.CodecRegistries.{fromCodecs, fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.{MongoClient, MongoCollection}

import scala.reflect.ClassTag

class MongoDBClient(val uri: String) {
  private val client = MongoClient(uri)

  private val codecRegistry = fromRegistries(
    fromCodecs(NscalaTimeCodecs.dateTimeCodec, NscalaTimeCodecs.periodCodec),
    fromProviders(classOf[ReminderModel]),
    MongoClient.DEFAULT_CODEC_REGISTRY
  )

  private val database: IO[org.mongodb.scala.MongoDatabase] =
    IO(client.getDatabase("reminder_bot").withCodecRegistry(codecRegistry))

  def getCollection[T: ClassTag](name: String): IO[MongoCollection[T]] =
    database.map(_.getCollection[T](name))
}

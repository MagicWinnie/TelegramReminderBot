package com.magicwinnie.reminder.db

import org.bson.codecs.configuration.CodecRegistries.{fromCodecs, fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.{MongoClient, MongoCollection}

import scala.reflect.ClassTag

object MongoDBClient {
  private val client = MongoClient()

  private val codecRegistry = fromRegistries(
    fromCodecs(NscalaTimeCodecs.dateTimeCodec, NscalaTimeCodecs.periodCodec),
    fromProviders(classOf[ReminderModel]),
    MongoClient.DEFAULT_CODEC_REGISTRY
  )

  private val database = client.getDatabase("reminder_bot").withCodecRegistry(codecRegistry)

  def getCollection[T: ClassTag](name: String): MongoCollection[T] =
    database.getCollection[T](name)
}

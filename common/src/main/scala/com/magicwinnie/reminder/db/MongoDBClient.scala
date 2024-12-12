package com.magicwinnie.reminder.db

import cats.effect.IO
import org.bson.codecs.configuration.CodecRegistries.{fromCodecs, fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.{MongoClient, MongoCollection}

import scala.reflect.ClassTag

/** Manages MongoDB connection and collection access.
  *
  * @param uri
  *   Connection string for the MongoDB database
  */
class MongoDBClient(val uri: String) {

  private val client = MongoClient(uri)

  /** Configures codec registry to support custom type serialization.
    *
    * Includes codecs for DateTime and Period, and provides serialization for ReminderModel.
    */
  private val codecRegistry = fromRegistries(
    fromCodecs(NscalaTimeCodecs.dateTimeCodec, NscalaTimeCodecs.periodCodec),
    fromProviders(classOf[ReminderModel]),
    MongoClient.DEFAULT_CODEC_REGISTRY
  )

  /** Initializes the database connection with the configured codec registry.
    *
    * Uses a fixed database name "reminder_bot" for the application.
    */
  private val database: IO[org.mongodb.scala.MongoDatabase] =
    IO(client.getDatabase("reminder_bot").withCodecRegistry(codecRegistry))

  /** Retrieves a specific collection from the database.
    *
    * @tparam T
    *   The type of documents in the collection
    * @param name
    *   The name of the collection to retrieve
    * @return
    *   An IO effect containing the requested MongoDB collection
    */
  def getCollection[T: ClassTag](name: String): IO[MongoCollection[T]] =
    database.map(_.getCollection[T](name))
}

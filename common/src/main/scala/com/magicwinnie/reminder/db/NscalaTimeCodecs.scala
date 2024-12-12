package com.magicwinnie.reminder.db

import com.github.nscala_time.time.Imports.{DateTime, Period}
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}

/** Объект NscalaTimeCodecs предоставляет реализации кодеков для сериализации и десериализации объектов типов DateTime и
  * Period из библиотеки nscala-time.
  */
object NscalaTimeCodecs {

  implicit val dateTimeCodec: Codec[DateTime] = new Codec[DateTime] {

    override def encode(writer: BsonWriter, value: DateTime, encoderContext: EncoderContext): Unit =
      writer.writeDateTime(value.getMillis)

    override def decode(reader: BsonReader, decoderContext: DecoderContext): DateTime =
      new DateTime(reader.readDateTime())

    override def getEncoderClass: Class[DateTime] = classOf[DateTime]
  }

  implicit val periodCodec: Codec[Period] = new Codec[Period] {

    override def encode(writer: BsonWriter, value: Period, encoderContext: EncoderContext): Unit = {
      // Например, "P4D" для периода в 4 дня
      writer.writeString(value.toString)
    }

    override def decode(reader: BsonReader, decoderContext: DecoderContext): Period =
      Period.parse(reader.readString())

    override def getEncoderClass: Class[Period] = classOf[Period]
  }

}

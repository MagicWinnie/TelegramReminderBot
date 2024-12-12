package com.magicwinnie.reminder.db

import com.github.nscala_time.time.Imports.{DateTime, Period}
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}

/** Provides BSON codecs for nscala-time DateTime and Period types. */
object NscalaTimeCodecs {

  /** Codec for encoding and decoding nscala-time DateTime objects. */
  implicit val dateTimeCodec: Codec[DateTime] = new Codec[DateTime] {

    /** Encodes a DateTime as a BSON date. */
    override def encode(writer: BsonWriter, value: DateTime, encoderContext: EncoderContext): Unit =
      writer.writeDateTime(value.getMillis)

    /** Decodes a BSON date to a DateTime. */
    override def decode(reader: BsonReader, decoderContext: DecoderContext): DateTime =
      new DateTime(reader.readDateTime())

    /** Returns the class type for this codec. */
    override def getEncoderClass: Class[DateTime] = classOf[DateTime]
  }

  /** Codec for encoding and decoding nscala-time Period objects. */
  implicit val periodCodec: Codec[Period] = new Codec[Period] {

    /** Encodes a Period as a BSON string. */
    override def encode(writer: BsonWriter, value: Period, encoderContext: EncoderContext): Unit = {
      // Converts the Period to an ISO-8601 string format
      writer.writeString(value.toString)
    }

    /** Decodes a BSON string to a Period. */
    override def decode(reader: BsonReader, decoderContext: DecoderContext): Period =
      Period.parse(reader.readString())

    /** Returns the class type for this codec. */
    override def getEncoderClass: Class[Period] = classOf[Period]
  }

}

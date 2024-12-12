package com.magicwinnie.reminder.db

import com.github.nscala_time.time.Imports.{DateTime, Period}
import org.bson.codecs.{DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}
import org.joda.time.DateTimeZone
import org.mockito.Mockito.{mock, verify, when}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class NscalaTimeCodecsSpec extends AnyFlatSpec with Matchers {

  "dateTimeCodec" should "encode DateTime into BSON date" in {
    val writer = mock(classOf[BsonWriter])
    val context = mock(classOf[EncoderContext])
    val dateTime = DateTime.now(DateTimeZone.UTC)

    NscalaTimeCodecs.dateTimeCodec.encode(writer, dateTime, context)

    verify(writer).writeDateTime(dateTime.getMillis)
  }

  it should "decode BSON date into DateTime" in {
    val reader = mock(classOf[BsonReader])
    val context = mock(classOf[DecoderContext])
    val dateTime = DateTime.now(DateTimeZone.UTC)

    when(reader.readDateTime()).thenReturn(dateTime.getMillis)

    val result = NscalaTimeCodecs.dateTimeCodec.decode(reader, context)

    result.getMillis shouldBe dateTime.getMillis
  }

  "periodCodec" should "encode Period into BSON string" in {
    val writer = mock(classOf[BsonWriter])
    val context = mock(classOf[EncoderContext])
    val period = Period.days(5)

    NscalaTimeCodecs.periodCodec.encode(writer, period, context)

    verify(writer).writeString(period.toString)
  }

  it should "decode BSON string into Period" in {
    val reader = mock(classOf[BsonReader])
    val context = mock(classOf[DecoderContext])
    val period = Period.days(2).toString

    when(reader.readString()).thenReturn(period)

    val result = NscalaTimeCodecs.periodCodec.decode(reader, context)

    result shouldBe Period.days(2)
  }

}

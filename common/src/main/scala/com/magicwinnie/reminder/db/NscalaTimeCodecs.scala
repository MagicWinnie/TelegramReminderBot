package com.magicwinnie.reminder.db

import com.github.nscala_time.time.Imports.{DateTime, Period}
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}

/** Объект NscalaTimeCodecs предоставляет реализации кодеков для сериализации и десериализации объектов типов DateTime и
  * Period из библиотеки nscala-time.
  */
object NscalaTimeCodecs {

  implicit val dateTimeCodec: Codec[DateTime] = new Codec[DateTime] {

    /** Кодирует объект DateTime и записывает его в BsonWriter.
      *
      * @param writer
      *   экземпляр BsonWriter, в который будет записано значение
      * @param value
      *   объект DateTime, который нужно закодировать
      * @param encoderContext
      *   контекст энкодера, содержащий правила кодирования
      * @return
      *   Unit
      */
    override def encode(writer: BsonWriter, value: DateTime, encoderContext: EncoderContext): Unit =
      writer.writeDateTime(value.getMillis)

    /** Декодирует дату и время из предоставленного BsonReader.
      *
      * @param reader
      *   объект BsonReader, используемый для чтения данных.
      * @param decoderContext
      *   контекст декодирования, предоставляющий дополнительную информацию для процесса декодирования.
      * @return
      *   объект DateTime, созданный на основе данных, считанных из BsonReader.
      */
    override def decode(reader: BsonReader, decoderContext: DecoderContext): DateTime =
      new DateTime(reader.readDateTime())

    /** Возвращает класс кодировщика, связанного с объектами типа DateTime.
      *
      * @return
      *   Класс кодировщика для объектов DateTime.
      */
    override def getEncoderClass: Class[DateTime] = classOf[DateTime]
  }

  implicit val periodCodec: Codec[Period] = new Codec[Period] {

    /** Кодирует объект типа Period в строковое представление (например, "P4D" для периода в 4 дня) и записывает его в
      * BsonWriter.
      *
      * @param writer
      *   экземпляр BsonWriter, в который записывается закодированное значение
      * @param value
      *   объект типа Period, который необходимо закодировать
      * @param encoderContext
      *   контекст энкодера, содержащий правила кодирования
      * @return
      *   ничего не возвращает, так как метод имеет тип Unit
      */
    override def encode(writer: BsonWriter, value: Period, encoderContext: EncoderContext): Unit =
      writer.writeString(value.toString)

    /** Декодирует период из предоставленного BsonReader.
      *
      * @param reader
      *   объект BsonReader, используемый для чтения данных.
      * @param decoderContext
      *   контекст декодирования, предоставляющий дополнительную информацию для процесса декодирования.
      * @return
      *   объект Period, созданный на основе данных, считанных из BsonReader.
      */
    override def decode(reader: BsonReader, decoderContext: DecoderContext): Period =
      Period.parse(reader.readString())

    /** Возвращает класс кодировщика, связанного с объектами типа Period.
      *
      * @return
      *   Класс кодировщика для объектов Period.
      */
    override def getEncoderClass: Class[Period] = classOf[Period]
  }

}

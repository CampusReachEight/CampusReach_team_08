package com.android.sample.model.date // Or any other appropriate package

import java.util.Date
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// By declaring this as an 'object', you are creating a singleton.
// This makes it a compile-time constant that can be used in annotations.
object DateSerializer : KSerializer<Date> {

  override val descriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)

  // Converts the Date object to its millisecond timestamp (a Long) for serialization.
  override fun serialize(encoder: Encoder, value: Date) {
    encoder.encodeLong(value.time)
  }

  // Converts the Long timestamp back into a Date object during deserialization.
  override fun deserialize(decoder: Decoder): Date {
    return Date(decoder.decodeLong())
  }
}

package com.android.sample.model.serializers

import android.net.Uri
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import androidx.core.net.toUri

object UriSerializer : KSerializer<Uri> {

  override val descriptor = PrimitiveSerialDescriptor("Uri", PrimitiveKind.STRING)

  // Converts the Uri object to its String representation for serialization.
  override fun serialize(encoder: Encoder, value: Uri) {
    encoder.encodeString(value.toString())
  }

  // Converts the String back into a Uri object during deserialization.
  override fun deserialize(decoder: Decoder): Uri {
    return decoder.decodeString().toUri()
  }
}
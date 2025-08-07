package com.auto.daemon.config;

import com.auto.daemon.config.enums.EnumInterface;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class EnumSerializer extends JsonSerializer {
    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(((EnumInterface)value).getType());
    }
}
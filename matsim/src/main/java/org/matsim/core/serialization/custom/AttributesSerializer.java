package org.matsim.core.serialization.custom;

import org.apache.fury.Fury;
import org.apache.fury.memory.MemoryBuffer;
import org.apache.fury.serializer.Serializer;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

public class AttributesSerializer extends Serializer<Attributes> {
    public AttributesSerializer(Fury fury, Class<Attributes> type) {
        super(fury, type, false, true);
    }

    @Override
    public void write(MemoryBuffer buffer, Attributes value) {
        // Dont serialize anything
    }

    @Override
    public Attributes read(MemoryBuffer buffer) {
        return new AttributesImpl();
    }
}

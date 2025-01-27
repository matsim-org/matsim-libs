package org.matsim.core.serialization.custom;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.apache.fury.Fury;
import org.apache.fury.memory.MemoryBuffer;
import org.apache.fury.serializer.Serializer;

public class IntArrayListSerializer extends Serializer<IntArrayList> {

    public IntArrayListSerializer(Fury fury, Class<IntArrayList> type) {
        super(fury, type, false, false);
    }


    @Override
    public void write(MemoryBuffer buffer, IntArrayList value) {
        buffer.writeInt32(value.size());
        buffer.writePrimitiveArray(value.elements(), 0, value.size() * Integer.BYTES);
    }

    @Override
    public IntArrayList read(MemoryBuffer buffer) {

        int size = buffer.readInt32();
        int[] data = new int[size];

        buffer.readToUnsafe(data, 0, size * Integer.BYTES);

        return IntArrayList.wrap(data);
    }
}

package org.matsim.core.serialization.custom;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.apache.fory.context.ReadContext;
import org.apache.fory.context.WriteContext;
import org.apache.fory.serializer.collection.CollectionSerializer;

public class IntArrayListSerializer extends CollectionSerializer<IntArrayList> {

	public IntArrayListSerializer(org.apache.fory.resolver.TypeResolver resolver, Class<IntArrayList> type) {
		super(resolver, type);
	}


	@Override
	public void write(WriteContext context, IntArrayList value) {
		var buffer = context.getBuffer();
		buffer.writeInt32(value.size());
		buffer.writePrimitiveArray(value.elements(), 0, value.size() * Integer.BYTES);
	}

	@Override
	public IntArrayList read(ReadContext readContext) {

		var buffer = readContext.getBuffer();
		int size = buffer.readInt32();
		int[] data = new int[size];

		buffer.readToUnsafe(data, 0, size * Integer.BYTES);

		return IntArrayList.wrap(data);
	}
}

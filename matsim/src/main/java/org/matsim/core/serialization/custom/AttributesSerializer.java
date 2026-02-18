package org.matsim.core.serialization.custom;

import org.apache.fory.Fory;
import org.apache.fory.memory.MemoryBuffer;
import org.apache.fory.serializer.Serializer;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

public class AttributesSerializer extends Serializer<Attributes> {
	public AttributesSerializer(Fory fory, Class<Attributes> type) {
		super(fory, type, false, true);
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

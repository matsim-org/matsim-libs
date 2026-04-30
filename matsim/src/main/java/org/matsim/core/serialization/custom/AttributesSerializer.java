package org.matsim.core.serialization.custom;

import org.apache.fory.context.ReadContext;
import org.apache.fory.context.WriteContext;
import org.apache.fory.serializer.Serializer;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

public class AttributesSerializer extends Serializer<Attributes> {
	public AttributesSerializer(org.apache.fory.config.Config foryConfig, Class<Attributes> type) {
		super(foryConfig, type);
	}

	@Override
	public void write(WriteContext writeContext, Attributes value) {
		// Dont serialize anything
	}

	@Override
	public Attributes read(ReadContext readContext) {
		return new AttributesImpl();
	}
}

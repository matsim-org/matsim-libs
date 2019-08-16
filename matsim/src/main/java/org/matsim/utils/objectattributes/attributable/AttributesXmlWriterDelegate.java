package org.matsim.utils.objectattributes.attributable;

import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.io.XmlUtils;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.ObjectAttributesConverter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

/**
 * @author thibautd
 */
public class AttributesXmlWriterDelegate {
	private final ObjectAttributesConverter converter = new ObjectAttributesConverter();

	public final void writeAttributes(final String indentation, final BufferedWriter writer, final Attributes attributes) {
		if (attributes.size() == 0) {
			return;
		}

		try {
			writer.write(indentation);
			writer.write("<attributes>");
			writer.newLine();

			// write attributes
			for (Map.Entry<String, Object> objAttribute : attributes.getAsMap().entrySet()) {
				Class<?> clazz = objAttribute.getValue().getClass();
				String converted = converter.convertToString(objAttribute.getValue());
				if (converted != null) {
					writer.write(indentation + "\t");
					writer.write("<attribute name=\"" + XmlUtils.encodeAttributeValue(objAttribute.getKey()) + "\" ");
					writer.write("class=\"" + clazz.getName() + "\">");
					writer.write(XmlUtils.encodeContent(converted));
					writer.write("</attribute>");
					writer.newLine();
				}
			}

			writer.write(indentation);
			writer.write("</attributes>");
			writer.newLine();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void putAttributeConverters(Map<Class<?>, AttributeConverter<?>> converters) {
		this.converter.putAttributeConverters(converters);
	}
}

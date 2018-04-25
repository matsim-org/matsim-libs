package org.matsim.utils.objectattributes.attributable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.jaxb.lanedefinitions20.ObjectFactory;
import org.matsim.jaxb.lanedefinitions20.XMLAttributeType;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.ObjectAttributesConverter;
import org.matsim.utils.objectattributes.attributeconverters.BooleanConverter;
import org.matsim.utils.objectattributes.attributeconverters.DoubleConverter;
import org.matsim.utils.objectattributes.attributeconverters.FloatConverter;
import org.matsim.utils.objectattributes.attributeconverters.IntegerConverter;
import org.matsim.utils.objectattributes.attributeconverters.LongConverter;
import org.matsim.utils.objectattributes.attributeconverters.StringConverter;

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
					writer.write("<attribute name=\"" + objAttribute.getKey() + "\" ");
					writer.write("class=\"" + clazz.getCanonicalName() + "\" >");
					writer.write(converted);
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

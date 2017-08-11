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

	private final static Logger log = Logger.getLogger(AttributesXmlWriterDelegate.class);

	private final Map<Class<?>, AttributeConverter<?>> converters = new HashMap<>();
	private final Set<Class<?>> missingConverters = new HashSet<>();

	public AttributesXmlWriterDelegate() {
		this.converters.put(String.class, new StringConverter());
		this.converters.put(Integer.class, new IntegerConverter());
		this.converters.put(Float.class, new FloatConverter());
		this.converters.put(Double.class, new DoubleConverter());
		this.converters.put(Boolean.class, new BooleanConverter());
		this.converters.put(Long.class, new LongConverter());
	}

	public final void writeAttributes(final String indentation, final BufferedWriter writer, final Attributes attributes) {
		if (attributes.keys.length == 0)
			return;
		try {
			writer.write(indentation);
			writer.write("<attributes>");
			writer.newLine();

			// sort attributes by name
			Map<String, Object> objAttributes = new TreeMap<>();
			for (int i = 0; i < attributes.keys.length; i++) {
				objAttributes.put(attributes.keys[i], attributes.values[i]);
			}

			// write attributes
			for (Map.Entry<String, Object> objAttribute : objAttributes.entrySet()) {

				// log.warn("key=" + objAttribute.getKey() + ", value=" + objAttribute.getValue() );

				Class<?> clazz = objAttribute.getValue().getClass();
				AttributeConverter<?> conv = this.converters.get(clazz);
				if (conv != null) {
					writer.write(indentation + "\t");
					writer.write("<attribute name=\"" + objAttribute.getKey() + "\" ");
					writer.write("class=\"" + clazz.getCanonicalName() + "\" >");
					writer.write(conv.convertToString(objAttribute.getValue()));
					writer.write("</attribute>");
					writer.newLine();
				} else {
					if (missingConverters.add(clazz)) {
						log.warn("No AttributeConverter found for class " + clazz.getCanonicalName() + ". Not all attribute values will be written.");
					}
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
		this.converters.putAll(converters);
	}

	/**
	 * Fill the given list of XMLAttributeType with all given custom attributes.
	 * The factory is used to create the objects of type XMLAttributeType.
	 * 
	 * This method is needed for xsd format file writer like for lanes and signals. 
	 */
	public final void writeAttributesToXSDFormat(List<XMLAttributeType> attributeList, Attributes attributes, ObjectFactory fac) {
		if (attributes.keys.length == 0)
			return;

		// sort attributes by name
		Map<String, Object> objAttributes = new TreeMap<>();
		for (int i = 0; i < attributes.keys.length; i++) {
			objAttributes.put(attributes.keys[i], attributes.values[i]);
		}

		// write attributes
		for (Map.Entry<String, Object> objAttribute : objAttributes.entrySet()) {
			Class<?> clazz = objAttribute.getValue().getClass();
			AttributeConverter<?> conv = this.converters.get(clazz);
			if (conv != null) {
				XMLAttributeType att = fac.createXMLAttributeType();
				att.setKey(objAttribute.getKey());
				att.setValue(conv.convertToString(objAttribute.getValue()));
				att.setClazz(clazz.getCanonicalName());
				attributeList.add(att);
			} else {
				if (missingConverters.add(clazz)) {
					log.warn("No AttributeConverter found for class " + clazz.getCanonicalName() + ". Not all attribute values will be written.");
				}
			}
		}
	}
}

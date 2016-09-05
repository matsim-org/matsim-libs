package org.matsim.utils.objectattributes.attributable;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.utils.objectattributes.AttributeConverter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author thibautd
 */
public class AttributesXmlWriterDelegate {

	private final static Logger log = Logger.getLogger( AttributesXmlWriterDelegate.class);

	private final Map<String, AttributeConverter<?>> converters = new HashMap<>();
	private final Set<Class<?>> missingConverters = new HashSet<>();

	public final void writeAttributes(final String indentation,
									  final BufferedWriter writer,
									  final Attributes attributes) {
		try {
			writer.write(indentation);
			writer.write("<attributes>");
			writer.newLine();

			// sort attributes by name
			Map<String, Object> objAttributes = new TreeMap<>();
			for (Map.Entry<String, Object> objAttribute : attributes.map.entrySet()) {
				objAttributes.put(objAttribute.getKey(), objAttribute.getValue());
			}

			// write attributes
			for (Map.Entry<String, Object> objAttribute : objAttributes.entrySet()) {
				Class<?> clazz = objAttribute.getValue().getClass();
				AttributeConverter<?> conv = this.converters.get(clazz.getCanonicalName());
				if (conv != null) {
					writer.write( indentation + "\t" );
					writer.write( "<attribute name=\"" + objAttribute.getKey() + "\" " );
					writer.write( "class=\"" + clazz.getCanonicalName() + "\" > " );
					writer.write( conv.convertToString( objAttribute.getValue() ) );
					writer.write( "<attribute/>" );
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
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}
}

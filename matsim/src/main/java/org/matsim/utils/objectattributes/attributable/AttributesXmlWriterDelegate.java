package org.matsim.utils.objectattributes.attributable;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.attributeconverters.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author thibautd
 */
public class AttributesXmlWriterDelegate {

	private final static Logger log = Logger.getLogger( AttributesXmlWriterDelegate.class);

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

	public final void writeAttributes(final String indentation,
									  final BufferedWriter writer,
									  final Attributes attributes) {
		if ( attributes.keys.length == 0 ) return;
		try {
			writer.write(indentation);
			writer.write("<attributes>");
			writer.newLine();

			// sort attributes by name
			Map<String, Object> objAttributes = new TreeMap<>();
			for ( int i=0; i < attributes.keys.length; i++ ) {
				objAttributes.put( attributes.keys[ i ] , attributes.values[ i ] );
			}

			// write attributes
			for (Map.Entry<String, Object> objAttribute : objAttributes.entrySet()) {
				
//				log.warn("key=" + objAttribute.getKey() + ", value=" + objAttribute.getValue() );
				
				Class<?> clazz = objAttribute.getValue().getClass();
				AttributeConverter<?> conv = this.converters.get(clazz);
				if (conv != null) {
					writer.write( indentation + "\t" );
					writer.write( "<attribute name=\"" + objAttribute.getKey() + "\" " );
					writer.write( "class=\"" + clazz.getCanonicalName() + "\" >" );
					writer.write( conv.convertToString( objAttribute.getValue() ) );
					writer.write( "</attribute>" );
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

	public void putAttributeConverters(Map<Class<?>, AttributeConverter<?>> converters) {
		this.converters.putAll( converters );
	}
}

package org.matsim.utils.objectattributes.attributable;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.utils.objectattributes.AttributeConverter;

import java.util.*;

/**
 * @thibautd
 */
public class MatsimXmlWriterWithAttributes extends MatsimXmlWriter {

	private final static Logger log = Logger.getLogger(MatsimXmlWriterWithAttributes.class);

	/*package*/ final static String TAG_ATTRIBUTES = "attributes";
	/*package*/ final static String TAG_ATTRIBUTE = "attribute";
	/*package*/ final static String ATTR_ATTRIBUTENAME = "name";
	/*package*/ final static String ATTR_ATTRIBUTECLASS = "class";

	private final Map<String, AttributeConverter<?>> converters = new HashMap<>();
	private final Set<Class<?>> missingConverters = new HashSet<>();

	protected final void writeAttributes( final Attributes attributes ) {

		writeStartTag(TAG_ATTRIBUTES, Collections.<Tuple<String,String>>emptyList() );

		// sort attributes by name
		Map<String, Object> objAttributes = new TreeMap<>();
		for ( int i=0; i < attributes.keys.length; i++ ) {
			objAttributes.put( attributes.keys[ i ] , attributes.values[ i ] );
		}

		// write attributes
		for (Map.Entry<String, Object> objAttribute : objAttributes.entrySet()) {
			Class<?> clazz = objAttribute.getValue().getClass();
			AttributeConverter<?> conv = this.converters.get(clazz.getCanonicalName());
			if (conv != null) {
				List<Tuple<String, String>> xmlAttributes = new ArrayList<>();

				xmlAttributes.add(super.createTuple(ATTR_ATTRIBUTENAME, objAttribute.getKey()));
				xmlAttributes.add(super.createTuple(ATTR_ATTRIBUTECLASS, clazz.getCanonicalName()));

				writeStartTag(TAG_ATTRIBUTE, xmlAttributes);

				writeContent(conv.convertToString(objAttribute.getValue()), false);

				writeEndTag(TAG_ATTRIBUTE);
			} else {
				if (missingConverters.add(clazz)) {
					log.warn("No AttributeConverter found for class " + clazz.getCanonicalName() + ". Not all attribute values will be written.");
				}
			}
		}

		writeEndTag(TAG_ATTRIBUTES);
	}
}

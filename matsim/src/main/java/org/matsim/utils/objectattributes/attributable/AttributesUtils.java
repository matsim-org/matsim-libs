package org.matsim.utils.objectattributes.attributable;

import org.matsim.utils.objectattributes.AttributeConverter;

import java.util.Map;

/**
 * @author thibautd
 */
public class AttributesUtils {
	public static final String ATTRIBUTES = "attributes";
	public static final String ATTRIBUTE = "attribute";

	/**
	 * Adds the mappings from "from" to "to". Nothing is done to copy the Object themselves,
	 * which should be fine for 99.9% of the usecases of Attributes (value objects)
	 */
	public static void copyTo( Attributes from , Attributes to ) {
		for ( Map.Entry<String, Object> entry : from.getAsMap().entrySet() ) {
			to.putAttribute( entry.getKey() , entry.getValue() );
		}
	}

	/**
	 * Adds the mappings from "from" to "to". Nothing is done to copy the Object themselves,
	 * which should be fine for 99.9% of the usecases of Attributes (value objects)
	 */
	public static <T extends Attributable> void copyAttributesFromTo( T from , T to ) {
		copyTo( from.getAttributes() , to.getAttributes() );
	}

	/**
	 * @deprecated use {@link Attributes#isEmpty()} instead
	 * @param attributes collection of attributes
	 * @return <code>true</code> if the attributes collection does not contain any attribute
	 */
	@Deprecated
	public static boolean isEmpty(Attributes attributes) {
		return attributes.size() == 0;
	}
}

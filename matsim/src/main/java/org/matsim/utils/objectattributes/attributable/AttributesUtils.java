package org.matsim.utils.objectattributes.attributable;

import org.matsim.utils.objectattributes.AttributeConverter;

import java.util.Map;

/**
 * @author thibautd
 */
public class AttributesUtils {
	/**
	 * Adds the mappings from "from" to "to". Nothing is done to copy the Object themselves,
	 * which should be fine for 99.9% of the usecases of Attributes (value objects)
	 */
	public static void copyTo( Attributes from , Attributes to ) {
		for ( int i=0; i < from.keys.length; i++ ) {
			to.putAttribute( from.keys[ i ] , from.values[ i ] );
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
	 * @param attributes collection of attributes
	 * @return <code>true</code> if the attributes collection does not contain any attribute
	 */
	public static boolean isEmpty(Attributes attributes) {
		return attributes.size() == 0;
	}
}

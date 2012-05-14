package org.matsim.utils.objectattributes;

/**
 * Converts an attribute to a String (for being written out) or from a String
 * (after being read in).
 *
 * @author mrieser
 */
public interface AttributeConverter<T> {

	public T convert(final String value);

	public String convertToString(final Object o);

}
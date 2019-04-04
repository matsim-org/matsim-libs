package org.matsim.utils.objectattributes;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.utils.objectattributes.attributeconverters.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Object that converts arbitrary objects to and from strings based on the logic defined by {@AttributeConverter}s
 * @author thibautd
 */
public class ObjectAttributesConverter {
	private static final Logger log = Logger.getLogger(ObjectAttributesConverter.class);
	private final Map<String, AttributeConverter<?>> converters = new HashMap<>();

	private final Set<String> missingConverters = new HashSet<>();

	@Inject
	public ObjectAttributesConverter(final Map<Class<?>, AttributeConverter<?>> converters) {
		this();
		this.putAttributeConverters(converters);
	}

	public ObjectAttributesConverter() {
		this.converters.put(String.class.getName(), new StringConverter() );
		this.converters.put(Integer.class.getName(), new IntegerConverter() );
		this.converters.put(Float.class.getName(), new FloatConverter() );
		this.converters.put(Double.class.getName(), new DoubleConverter() );
		this.converters.put(Boolean.class.getName(), new BooleanConverter() );
		this.converters.put(Long.class.getName(), new LongConverter() );
		this.converters.put(double[].class.getName(), new DoubleArrayConverter());
	}

	public Object convert(String className, String value) {
		AttributeConverter converter = getConverter(className);
		return converter == null ? null : converter.convert(value);
	}

	private AttributeConverter getConverter(String className) {
		if (converters.containsKey(className)) return converters.get(className);

		try {
			Class<?> clazz = Class.forName(className);
			if (clazz.isEnum()) {
				AttributeConverter converter = new EnumConverter(clazz);
				converters.put(className, converter);
				return converter;
			}
			if (missingConverters.add(className)) {
				log.warn("No AttributeConverter found for class " + className + ". Not all attribute values can be converted.");
			}
		}
		catch (ClassNotFoundException e) {
			if (missingConverters.add(className)) {
				log.warn("No AttributeConverter found for class " + className + ", and class is not on classpath. Not all attribute values can be converted.");
			}
		}

		return null;
	}

	public String convertToString(Object o) {
		AttributeConverter converter = getConverter(o.getClass().getName());
		// is returning null the right approach there?
		return converter == null ? null : converter.convertToString(o);
	}

    /**
	 * Sets the converter for reading attributes of the specified class.
	 *
	 * @param clazz
	 * @param converter
	 * @return the previously registered converter for this class, or <code>null</code> if none was set before.
	 */
	public AttributeConverter putAttributeConverter(final Class<?> clazz, final AttributeConverter converter) {
		return this.converters.put(clazz.getName(), converter);
	}

	public void putAttributeConverters( final Map<Class<?>, AttributeConverter<?>> converters ) {
		for ( Map.Entry<Class<?>, AttributeConverter<?>> e : converters.entrySet() ) {
			putAttributeConverter( e.getKey() , e.getValue() );
		}
	}

	/**
	 * Removes the converter for reading attributes of the specified class.
	 *
	 * @param clazz
	 * @return the previously registered converter for this class, of <code>null</code> if none was set.
	 */
	public AttributeConverter removeAttributeConverter(final Class<?> clazz) {
		return this.converters.remove(clazz.getName());
	}
}

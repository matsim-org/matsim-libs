
/* *********************************************************************** *
 * project: org.matsim.*
 * ObjectAttributesConverter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

 package org.matsim.utils.objectattributes;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.utils.objectattributes.attributeconverters.*;
import org.matsim.api.core.v01.Coord;
import org.matsim.vehicles.PersonVehicles;

import java.util.*;

/**
 * Object that converts arbitrary objects to and from strings based on the logic defined by {@AttributeConverter}s
 *
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
		this.converters.put(String.class.getName(), new StringConverter());
		this.converters.put(Integer.class.getName(), new IntegerConverter());
		this.converters.put(Float.class.getName(), new FloatConverter());
		this.converters.put(Double.class.getName(), new DoubleConverter());
		this.converters.put(Boolean.class.getName(), new BooleanConverter());
		this.converters.put(Long.class.getName(), new LongConverter());
		this.converters.put(double[].class.getName(), new DoubleArrayConverter());
		this.converters.put(Map.class.getName(), new StringStringMapConverter());
		this.converters.put(Collection.class.getName(), new StringCollectionConverter());
		this.converters.put(Coord.class.getName(), new CoordConverter());
		this.converters.put(Coord[].class.getName(), new CoordArrayConverter());
		this.converters.put(PersonVehicles.class.getName(), new PersonVehiclesAttributeConverter());
	}

	//this is for reading
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

			if(Map.class.isAssignableFrom(clazz)) return this.converters.get(Map.class.getName());
			if(Collection.class.isAssignableFrom(clazz)) return this.converters.get(Collection.class.getName());

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

		//handle map and collection converter - check for string elements
		//we pass in a lot of maps here that we can and (maybe) do not want to write
		{
			if(converter instanceof StringStringMapConverter){
				Map<Object, Object> map = ((Map<Object, Object>) o);
				if (! map.isEmpty()){
					Map.Entry firstEntry = map.entrySet().iterator().next();
					if(! (firstEntry.getKey() instanceof String && firstEntry.getValue() instanceof String) ) return null;
				}
			}
			if(converter instanceof StringCollectionConverter){
				Collection collection = ((Collection) o);
				if(! collection.isEmpty()){
					if(! ( collection.iterator().next() instanceof String) ) return null;
				}
			}
		}

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

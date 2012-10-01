/* *********************************************************************** *
 * project: org.matsim.*
 * ReflectiveModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.config.Module;

/**
 * A module using reflection for easy implementation of config groups.
 * To implement a config group, extend this class and:
 *
 * <ul>
 * <li> create fields for your values. The name of those fields will be used as parameter name in the xml file.
 * The field should be set to a default value
 * <li> implement getters and setters for your fields. Each field should have a setter
 * accepting a string as argument, and a getter. naming conventions are usual,
 * an case independant.
 * <li> if the string encoding in the xml file is different of <tt>""+value</tt>
 * where <tt>value</tt> is the field value, a getter <tt>get---AsString</tt> should
 * be implemented.
 * </ul>
 *
 * The string value of all parameters not specified by a field will be accessible
 * as with the base Module (ie all string values are remembered).
 *
 * @author thibautd
 */
public class ReflectiveModule extends Module {
	private static final Logger log =
		Logger.getLogger(ReflectiveModule.class);

	private final Map<String, Field> fields;
	private final Map<String, Method> setters;
	private final Map<String, Method> stringGetters;
	private final Map<String, Method> getters;

	// /////////////////////////////////////////////////////////////////////////
	// Construction
	// /////////////////////////////////////////////////////////////////////////
	public ReflectiveModule(final String name) {
		super(name);
		fields = getFields();
		setters = getSetters();
		stringGetters = getStringGetters();
		getters = getGetters();
	}

	private Map<String, Method> getStringGetters() {
		Map<String, Method> gs = new HashMap<String, Method>();
		Class c = getClass();

		Method[] allMethods = c.getDeclaredMethods();

		for (Method m : allMethods) {
			if ( m.getName().toLowerCase().matches( "get.*asstring" ) &&
					m.getParameterTypes().length == 0 &&
					m.getReturnType().equals( String.class ) ) {
				gs.put( m.getName().toLowerCase() , m );
			}
		}

		return gs;
	}

	private Map<String, Method> getGetters() {
		Map<String, Method> gs = new HashMap<String, Method>();
		Class c = getClass();

		Method[] allMethods = c.getDeclaredMethods();

		for (Method m : allMethods) {
			if ( m.getName().toLowerCase().matches( "get.*" ) &&
					m.getParameterTypes().length == 0 ) {
				gs.put( m.getName().toLowerCase() , m );
			}
		}

		return gs;
	}

	private Map<String, Method> getSetters() {
		Map<String, Method> ss = new HashMap<String, Method>();
		Class c = getClass();

		Method[] allMethods = c.getDeclaredMethods();

		for (Method m : allMethods) {
			if ( m.getName().toLowerCase().matches( "set.*" ) &&
					m.getParameterTypes().length == 1 &&
					m.getParameterTypes()[0].equals( String.class ) ) {
				ss.put( m.getName().toLowerCase() , m );
			}
		}

		return ss;
	}

	private Map<String, Field> getFields() {
		Map<String, Field> fs = new HashMap<String, Field>();
		Class c = getClass();

		for (Field f : c.getDeclaredFields()) {
			if ( !Modifier.isStatic( f.getModifiers() ) ) {
				fs.put( f.getName() , f );
			}
		}

		return fs;
	}

	// /////////////////////////////////////////////////////////////////////////
	// module methods
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public void addParam(
			final String param_name,
			final String value) {
		Field f = fields.get( param_name );

		if (f == null) {
			log.warn( "unknown parameter "+param_name+" for group "+getName()+". Here are the valid parameter names: "+fields.keySet() );
			log.warn( "Only the string value will be remembered" );
			super.addParam( param_name , value );
			return;
		}

		Method setter = setters.get( "set"+f.getName().toLowerCase() );

		if (setter == null) {
			log.warn( "field "+param_name+" for group "+getName()+" found but no setter found!" );
			return;
		}

		try {
			setter.invoke( this , value );
			log.info( "value "+value+" successfully set for field "+param_name+" for group "+getName() );
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

	@Override
	public String getValue(final String param_name) {
		Method getter = stringGetters.get( "get"+param_name.toLowerCase()+"asstring" );
		try {
			if (getter != null) {
				return ""+getter.invoke( this );
			}

			log.warn( "no string getter found for param "+param_name+": trying regular getter" );

			getter = getters.get( "get"+param_name.toLowerCase() );

			if (getter != null) {
				return ""+getter.invoke( this );
			}

			log.warn( "no getter found at all for param "+param_name+": trying parent method" );
			return super.getValue( param_name );
		} catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

	@Override
	public Map<String, String> getParams() {
		Map<String, String> map = super.getParams();

		for (String f : fields.keySet()) {
			addParameterToMap( map , f );
		}

		return map;
	}
}


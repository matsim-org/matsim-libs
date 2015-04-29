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
package org.matsim.core.config.experimental;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ConfigGroup;

/**
 * A module using reflection for easy implementation of config groups.
 * <br>
 * <br>
 * This class takes care of all the housekeeping tasks that you normally have
 * to manually implement when extending {@link ConfigGroup} (usually forgetting half of it).
 * <br>
 * <br>
 * For each field in the xml file, just implement a setter taking one of the following types as parameter:
 * <ul>
 * <li> a String,</li>
 * <li> a primitive data type or a "primitive wrapper" type (such as {@link Double}
 * or {@link Integer}),</li>
 * <li> an enumeration type</li>
 * </ul>
 * and a getter returning an Object or primitive type which String representation
 * must be written to the xml file, and annotate them with the {@link StringSetter}
 * and {@link StringGetter} annotation types.
 * <br>
 * Those annotations take a mandatory String argument, which is the parameter name
 * in the xml.
 * <br>
 * <br>
 * In most of the cases (for Strings, primitive types and enums),
 * annotating the actual setters and getters is fine.
 * Sometimes (When the {@link Object#toString()} method returns the right String representation,
 * for instance for {@link Id}s), one needs to implement a specific
 * "String" setter, but can annotate the normal getter.
 * Sometimes (for instance when handling collections) one needs specific conversion bothways.
 * In those cases, just separate actual setters/getters and "string" setters/getters.
 * <br>
 * Note that there is no restriction on access modifiers for those methods: they
 * can even be private, if for instance you do not want to confuse the users with dozens
 * of methods of the type "<tt>getStringRepresentationOfParameterX()</tt>" or
 * "<tt>setParameterXFromStringRepresentation( String v )</tt>".
 * <br>
 * A commented example can be found at {@link tutorial.programming.reflectiveConfigGroup.MyConfigGroup}.
 * <br>
 * <br>
 * If something is wrong (missing setter or getter, wrong parameter or return type),
 * an {@link InconsistentModuleException} will be thrown at construction.
 *
 * @author thibautd
 */
public abstract class ReflectiveConfigGroup extends ConfigGroup {
	private static final Logger log =
		Logger.getLogger(ReflectiveConfigGroup.class);

	private final boolean storeUnknownParameters;

	private final Map<String, Method> setters;
	private final Map<String, Method> stringGetters;

	// /////////////////////////////////////////////////////////////////////////
	// Construction
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Creates an instance which will crash if an unknown parameter name
	 * is given.
	 *
	 * @param name the name of the module in the config file.
	 */
	public ReflectiveConfigGroup(final String name) {
		this( name , false );
	}

	/**
	 * Creates an instance, giving the choice on whether unknown parameter names result
	 * in a crash or just in the parameter value being stored as a String.
	 *
	 * @param name the name of the module in the config file.
	 * @param storeUnknownParametersAsStrings if true, when no annotated getter
	 * or setter is found for a parameter name, the parameters are stored using
	 * the default {@link ConfigGroup} behavior. This is not that safe, so be careful.
	 */
	public ReflectiveConfigGroup(
			final String name,
			final boolean storeUnknownParametersAsStrings) {
		super(name);
		this.storeUnknownParameters = storeUnknownParametersAsStrings;
		setters = getSetters();
		stringGetters = getStringGetters();

		if ( !setters.keySet().equals( stringGetters.keySet() ) ) {
			throw new InconsistentModuleException( "setters and getters inconsistent" );
		}

		checkConvertNullAnnotations();
	}

	private void checkConvertNullAnnotations() {
		final Class<? extends ReflectiveConfigGroup> c = getClass();

		final Method[] allMethods = c.getDeclaredMethods();

		for (Method m : allMethods) {
			final StringGetter annotation = m.getAnnotation( StringGetter.class );
			if ( annotation != null ) {
				final Method g = getStringGetters().get( annotation.value() );

				if ( m.isAnnotationPresent( DoNotConvertNull.class ) != g.isAnnotationPresent( DoNotConvertNull.class ) ) {
					throw new InconsistentModuleException( "Inconsistent annotation of getter and setter with ConvertNull in "+getClass().getName() );
				}
			}
		}
	}

	private Map<String, Method> getStringGetters() {
		final Map<String, Method> gs = new HashMap<String, Method>();
		final Class<? extends ReflectiveConfigGroup> c = getClass();

		final Method[] allMethods = c.getDeclaredMethods();

		for (Method m : allMethods) {
			final StringGetter annotation = m.getAnnotation( StringGetter.class );
			if ( annotation != null ) {
				checkGetterValidity( m );
				final Method old = gs.put( annotation.value() , m );
				if ( old != null ) {
					throw new InconsistentModuleException( "several string getters for "+annotation.value() );
				}
			}
		}

		return gs;
	}

	private static void checkGetterValidity(final Method m) {
		if ( m.getParameterTypes().length > 0 ) {
			throw new InconsistentModuleException( "getter "+m+" has parameters" );
		}

		if ( m.getReturnType().equals( Void.TYPE ) ) {
			throw new InconsistentModuleException( "getter "+m+" has void return type" );
		}
	}

	private Map<String, Method> getSetters() {
		final Map<String, Method> ss = new HashMap<String, Method>();
		final Class<? extends ReflectiveConfigGroup> c = getClass();

		final Method[] allMethods = c.getDeclaredMethods();

		for (Method m : allMethods) {
			final StringSetter annotation = m.getAnnotation( StringSetter.class );
			if ( annotation != null ) {
				checkSetterValidity( m );
				final Method old = ss.put( annotation.value() , m );
				if ( old != null ) {
					throw new InconsistentModuleException( "several string setters for "+annotation.value() );
				}
			}
		}

		return ss;
	}

	private static void checkSetterValidity(final Method m) {
		final Class<?>[] params = m.getParameterTypes();

		if (params.length != 1) {
			throw new InconsistentModuleException( "setter "+m+" has "+params.length+" parameters instead of one" );
		}

		final Collection<Class<?>> allowedParameterTypes =
			Arrays.<Class<?>>asList(
					String.class,
					Float.class, Double.class, Integer.class, Long.class, Boolean.class, Character.class, Byte.class, Short.class,
					Float.TYPE, Double.TYPE, Integer.TYPE, Long.TYPE, Boolean.TYPE, Character.TYPE, Byte.TYPE, Short.TYPE);
        if ( !allowedParameterTypes.contains( params[ 0 ] ) && !params[ 0 ].isEnum() ) {
			throw new InconsistentModuleException( "setter "+m+" gets a "+params[ 0 ]+". Valid types are String, primitive types and their wrapper classes, and enumerations. "+
					"Other types are fine as parameters, but you will need to implement conversion strategies in the String setters." );
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// module methods
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public final void addParam(
			final String param_name,
			final String value) {
		final Method setter = setters.get( param_name );

		if (setter == null) {
			if ( !storeUnknownParameters ) {
				throw new IllegalArgumentException(
						"Module "+getName()+" of type "+getClass().getName()+
						" doesn't accept unkown parameters. Parameter "+param_name+
						" is not part of the valid parameters: "+setters.keySet() );
			}
			log.warn( "unknown parameter "+param_name+" for group "+getName()+". Here are the valid parameter names: "+setters.keySet() );
			log.warn( "Only the string value will be remembered" );
			super.addParam( param_name , value );
			return;
		}

		try {
			invokeSetter( setter , value );
			log.trace( "value "+value+" successfully set for field "+param_name+" for group "+getName() );
		}
		catch (InvocationTargetException e) {
			// this exception wraps Throwables intercepted in the invocation of the setter.
			// Avoid multiple wrappings (exception wrapped in InvocationTargetException
			// itself wrapped in a RuntimeException), as it makes error messages
			// messy.
			final Throwable cause = e.getCause();
			if ( cause instanceof RuntimeException ) {
				throw (RuntimeException) cause;
			}

			if ( cause instanceof Error ) {
				throw (Error) cause;
			}

			throw new RuntimeException( cause );
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException( e );
		}
	}

	private void invokeSetter(
			final Method setter,
			final String value) throws IllegalAccessException, InvocationTargetException {
		// do not care about access modifier:
		// if a method is tagged with the StringSetter
		// annotation, we are supposed to access it.
		// This *is* safe.
		final boolean accessible = setter.isAccessible();
		setter.setAccessible( true );

		final Class<?>[] params = setter.getParameterTypes();
		assert params.length == 1; // already checked at constr.

		final Class<?> type = params[ 0 ];

		if ( value.equals( "null" ) && !setter.isAnnotationPresent( DoNotConvertNull.class ) ) {
			setter.invoke( this , new Object[]{ null } );
		}
		else if ( type.equals( String.class ) ) {
			setter.invoke( this , value );
		}
		else if ( type.equals( Float.class ) || type.equals( Float.TYPE ) ) {
			setter.invoke( this , Float.parseFloat( value ) );
		}
		else if ( type.equals( Double.class ) || type.equals( Double.TYPE ) ) {
			setter.invoke( this , Double.parseDouble( value ) );
		}
		else if ( type.equals( Integer.class ) || type.equals( Integer.TYPE ) ) {
			setter.invoke( this , Integer.parseInt( value ) );
		}
		else if ( type.equals( Long.class ) || type.equals( Long.TYPE ) ) {
			setter.invoke( this , Long.parseLong( value ) );
		}
		else if ( type.equals( Boolean.class ) || type.equals( Boolean.TYPE ) ) {
			setter.invoke( this , Boolean.parseBoolean( value ) );
		}
		else if ( type.equals( Character.class ) || type.equals( Character.TYPE ) ) {
			if ( value.length() != 1 ) throw new IllegalArgumentException( value+" is not a single char!" );
			setter.invoke( this , value.toCharArray()[ 0 ] );
		}
		else if ( type.equals( Byte.class ) || type.equals( Byte.TYPE ) ) {
			setter.invoke( this , Byte.parseByte( value ) );
		}
		else if ( type.equals( Short.class ) || type.equals( Short.TYPE ) ) {
			setter.invoke( this , Short.parseShort( value ) );
		}
		else if ( type.isEnum() ) {
			try {
				setter.invoke(
						this,
						Enum.valueOf(
							type.asSubclass( Enum.class ),
							value ) );
			}
			catch (IllegalArgumentException e) {
				// happens when the string does not correspond to any enum values.
				// Surprisingly, the default error message does not print the possible
				// values: do it here, so that the user gets an idea of what went wrong
				final StringBuilder comment =
					new StringBuilder(
							"Error trying to set value "+value+
							" for type "+type.getName()+
							": possible values are " );

				final Object[] consts = type.getEnumConstants();
				for ( int i = 0; i < consts.length; i++ ) {
					comment.append( consts[ i ].toString() );
					if ( i < consts.length - 1 ) comment.append( ", " );
				}

				throw new IllegalArgumentException( comment.toString() , e );
			}
		}
		else {
			throw new RuntimeException( "no method to handle type "+type );
		}

		setter.setAccessible( accessible );
	}

	@Override
	public final String getValue(final String param_name) {
		final Method getter = stringGetters.get( param_name );

		try {
			if (getter != null) {
				// do not care about access modifier:
				// if a method is tagged with the StringGetter
				// annotation, we are supposed to access it.
				// This *is* safe.
				final boolean accessible = getter.isAccessible();
				getter.setAccessible( true );
				final Object result = getter.invoke( this );
				getter.setAccessible( accessible );

				if ( result == null ) {
					if ( getter.isAnnotationPresent( DoNotConvertNull.class ) ) {
						log.error( "getter for parameter "+param_name+" of module "+getName()+" returned null." );
						log.error( "This is not allowed for this getter." );

						throw new NullPointerException( "getter for parameter "+param_name+" of module "+getClass().getName()+" ("+getName()+") returned null." );
					}
					return null;
				}

				final String value = ""+result;

				if ( value.equals( "null" ) && !getter.isAnnotationPresent( DoNotConvertNull.class ) ) {
					throw new RuntimeException( "parameter "+param_name+" understands null pointers for IO. As a consequence, the \"null\" String is not a valid value for "+getter.getName() );
				}

				return value;
			}
		}
		catch (InvocationTargetException e) {
			// this exception wraps Throwables intercepted in the invocation of the getter.
			// Avoid multiple wrappings (exception wrapped in InvocationTargetException
			// itself wrapped in a RuntimeException), as it makes error messages
			// messy.
			final Throwable cause = e.getCause();
			if ( cause instanceof RuntimeException ) {
				throw (RuntimeException) cause;
			}

			if ( cause instanceof Error ) {
				throw (Error) cause;
			}

			throw new RuntimeException( cause );
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException( e );
		}

		if ( !storeUnknownParameters ) {
			throw new IllegalArgumentException(
					"Module "+getName()+" of type "+getClass().getName()+
					" doesn't store unkown parameters. Parameter "+param_name+
					" is not part of the valid parameters: "+stringGetters.keySet() );
		}

		log.warn( "no getter found for param "+param_name+": trying parent method" );
		return super.getValue( param_name );
	}

	@Override
	public final Map<String, String> getParams() {
		final Map<String, String> map = super.getParams();

		for (String f : setters.keySet()) {
			addParameterToMap( map , f );
		}

		return map;
	}

	/**
	 * Comments for parameters which setter get an enum type are automatically generated,
	 * containing a list of possible values. They can be overriden by subclasses without
	 * problems.
	 *
	 * <br>
	 * it is recommended for subclasses to get this map using <tt>super.getComments()</tt>
	 * and fill it with additional comments, rather than generate an empty map.
	 */
	@Override
	public Map<String, String> getComments() {
		// generate some default comments.
		final Map<String, String> comments = super.getComments();

		for ( Map.Entry<String, Method> entry : setters.entrySet() ) {
			final String paramName = entry.getKey();
			if ( comments.containsKey( paramName ) ) {
				// at the time of implementation, this is not possible,
				// but who knows? Do not override something already there.
				continue;
			}

			final Method setter = entry.getValue();

			final Class<?>[] params = setter.getParameterTypes();
			assert params.length == 1; // already checked at constr.

			final Class<?> type = params[ 0 ];

			if ( type.isEnum() ) {
				// generate an automatic comment containing the possible values for enum types
				final StringBuilder comment = new StringBuilder( "Possible values: " );
				final Object[] consts = type.getEnumConstants();

				for ( int i = 0; i < consts.length; i++ ) {
					comment.append( consts[ i ].toString() );
					if ( i < consts.length - 1 ) comment.append( ", " );
				}

				comments.put( paramName , comment.toString() );
			}
		}

		return comments;
	}

	// /////////////////////////////////////////////////////////////////////////
	// annotations
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * use to annotate the methods which should be used to read the string
	 * values.
	 * See the class description for a description of the valid signature of the
	 * annotated method.
	 */
	@Documented
	@Retention( RetentionPolicy.RUNTIME )
	public static @interface StringSetter {
		/**
		 * the name of the field in the XML document
		 */
		String value();
	}

	/**
	 * use to annotate the methods which should be used to get the string
	 * values.
	 * the methods must take no parameter and return an Object or primitive
	 * data type which string representations is the one which should appear in the
	 * xml.
	 */
	@Documented
	@Retention( RetentionPolicy.RUNTIME )
	public static @interface StringGetter {
		/**
		 * the name of the field in the XML document
		 */
		String value();
	}

	/**
	 * Setters for which the "null" string should NOT be converted
	 * to the <tt>null</tt> pointer, and getter from which a <tt>null</tt>
	 * pointer should NOT be accepted and converted to the "null" string,
	 * should be annotated with this.
	 * <br>
	 * Note that both the setter and the getter for a given parameter,
	 * or none of them, must be annotated. If not, an {@link InconsistentModuleException}
	 * will be thrown.
	 */
	@Documented
	@Retention( RetentionPolicy.RUNTIME )
	public static @interface DoNotConvertNull {}

	public static class InconsistentModuleException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private InconsistentModuleException(final String msg) {
			super( msg );
		}
	}
}


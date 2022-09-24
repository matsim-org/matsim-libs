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
package org.matsim.core.config;

import static java.util.stream.Collectors.joining;

import java.io.Serial;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimExtensionPoint;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

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
 * A commented example can be found at {@link org.matsim.codeexamples.config.reflectiveConfigGroup.MyConfigGroup}.
 * <br>
 * <br>
 * If something is wrong (missing setter or getter, wrong parameter or return type),
 * an {@link InconsistentModuleException} will be thrown at construction.
 *
 * @author thibautd
 */
public abstract class ReflectiveConfigGroup extends ConfigGroup implements MatsimExtensionPoint {
	private static final Logger log = LogManager.getLogger(ReflectiveConfigGroup.class);

	private static final Set<Class<?>> ALLOWED_PARAMETER_TYPES = Set.of(String.class, Float.class, Double.class,
			Integer.class, Long.class, Boolean.class, Character.class, Byte.class, Short.class, Float.TYPE, Double.TYPE,
			Integer.TYPE, Long.TYPE, Boolean.TYPE, Character.TYPE, Byte.TYPE, Short.TYPE);

	private final boolean storeUnknownParameters;

	private final Map<String, Method> setters;
	private final Map<String, Method> stringGetters;
	private final Map<String, Field> paramFields;
	private final Set<String> registeredParams;// accessible via getters/setters or as fields

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
		this(name, false);
	}

	/**
	 * Creates an instance, giving the choice on whether unknown parameter names result
	 * in a crash or just in the parameter value being stored as a String.
	 *
	 * @param name                            the name of the module in the config file.
	 * @param storeUnknownParametersAsStrings if true, when no annotated getter
	 *                                        or setter is found for a parameter name, the parameters are stored using
	 *                                        the default {@link ConfigGroup} behavior. This is not that safe, so be careful.
	 */
	public ReflectiveConfigGroup(final String name, final boolean storeUnknownParametersAsStrings) {
		super(name);
		this.storeUnknownParameters = storeUnknownParametersAsStrings;
		setters = getSetters();
		stringGetters = getStringGetters();
		paramFields = getParamFields();
		registeredParams = Sets.union(stringGetters.keySet(), paramFields.keySet());

		checkModuleConsistency(setters.keySet().equals(stringGetters.keySet()), "setters and getters inconsistent");
		checkModuleConsistency(paramFields.keySet().stream().noneMatch(setters::containsKey),
				"Use either StringGetter/Setter or Parameter annotations");
	}

	private Map<String, Method> getStringGetters() {
		final Map<String, Method> gs = new HashMap<>();

		final var allMethods = getDeclaredMethodsInSubclasses();
		for (Method m : allMethods) {
			final StringGetter annotation = m.getAnnotation(StringGetter.class);
			if (annotation != null) {
				checkGetterValidity(m);
				final Method old = gs.put(annotation.value(), m);
				checkModuleConsistency(old == null, "several string getters for: %s", annotation.value());
			}
		}

		return gs;
	}

	private static void checkGetterValidity(final Method m) {
		checkModuleConsistency(m.getParameterTypes().length == 0, "getter %s has parameters", m);
		checkModuleConsistency(!m.getReturnType().equals(Void.TYPE), "getter %s has void return type", m);
	}

	private Map<String, Method> getSetters() {
		final Map<String, Method> ss = new HashMap<>();

		final var allMethods = getDeclaredMethodsInSubclasses();
		for (Method m : allMethods) {
			final StringSetter annotation = m.getAnnotation(StringSetter.class);
			if (annotation != null) {
				checkSetterValidity(m);
				final Method old = ss.put(annotation.value(), m);
				checkModuleConsistency(old == null, "several string setters for: %s", annotation.value());
			}
		}

		return ss;
	}

	private List<Method> getDeclaredMethodsInSubclasses() {
		// in order to support multi-level inheritance of ReflectiveConfigGroup, we need to collect all methods from
		// all levels of inheritance below ReflectiveConfigGroup
		var methods = new ArrayList<Method>();
		for (Class<?> c = getClass(); c != ReflectiveConfigGroup.class; c = c.getSuperclass()) {
			Collections.addAll(methods, c.getDeclaredMethods());
		}
		return methods;
	}

	private static void checkSetterValidity(final Method m) {
		final Class<?>[] params = m.getParameterTypes();
		checkModuleConsistency(params.length == 1, "setter %s has %s parameters instead of 1.", m, params.length);

		var param = params[0];
		checkModuleConsistency(ALLOWED_PARAMETER_TYPES.contains(param) || param.isEnum(),
				"setter %s takes a %s argument."
						+ " Valid types are String, primitive types and their wrapper classes, and enumerations."
						+ " Other types are fine as parameters, but you will need to implement conversion strategies in the String setters.",
				m, param);
	}

	private Map<String, Field> getParamFields() {
		final Map<String, Field> pf = new HashMap<>();

		final var allFields = getDeclaredFieldsInSubclasses();
		for (Field f : allFields) {
			Parameter annotation = f.getAnnotation(Parameter.class);
			if (annotation != null) {
				checkParamFieldValidity(f);
				var paramName = annotation.value().isEmpty() ? f.getName() : annotation.value();
				Field old = pf.put(paramName, f);
				checkModuleConsistency(old == null, "several parameter fields for: %s", paramName);
			}
		}

		return pf;
	}

	private List<Field> getDeclaredFieldsInSubclasses() {
		// in order to support multi-level inheritance of ReflectiveConfigGroup, we need to collect all fields from
		// all levels of inheritance below ReflectiveConfigGroup
		var fields = new ArrayList<Field>();
		for (Class<?> c = getClass(); c != ReflectiveConfigGroup.class; c = c.getSuperclass()) {
			Collections.addAll(fields, c.getDeclaredFields());
		}
		return fields;
	}

	private static void checkParamFieldValidity(Field field) {
		var type = field.getType();
		checkModuleConsistency(ALLOWED_PARAMETER_TYPES.contains(type) || type.isEnum(), "field %s is of type %s."
						+ " Valid types are String, primitive types and their wrapper classes, and enumerations."
						+ " Other types are fine as parameters, but you will need to implement conversion strategies in the String setters.",
				field, type);
	}

	// /////////////////////////////////////////////////////////////////////////
	// module methods
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public final void addParam(final String param_name, final String value) {
		var setter = setters.get(param_name);
		if (setter != null) {
			invokeSetter(setter, value);
			log.trace("value {} successfully set for field {} for group {}", value, param_name, getName());
			return;
		}

		var field = paramFields.get(param_name);
		if (field != null) {
			setParamField(field, value);
			log.trace("value {} successfully set for field {} for group {}", value, param_name, getName());
			return;
		}

		Preconditions.checkArgument(storeUnknownParameters, "Module %s of type %s doesn't accept unknown parameters."
						+ " Parameter %s is not part of the valid parameters: %s", getName(), getClass().getName(), param_name,
				setters.keySet());

		log.warn(
				"Unknown parameter {} for group {}. Here are the valid parameter names: {}. Only the string value will be remembered.",
				param_name, getName(), registeredParams);
		super.addParam(param_name, value);
	}

	private void invokeSetter(final Method setter, final String value) {
		boolean accessible = enforceAccessible(setter);
		try {
			var type = setter.getParameterTypes()[0];
			setter.invoke(this, fromString(value, type));
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw convertToUnchecked(e);
		} finally {
			setter.setAccessible(accessible);
		}
	}

	private void setParamField(Field paramField, String value) {
		boolean accessible = enforceAccessible(paramField);
		try {
			var type = paramField.getType();
			paramField.set(this, fromString(value, type));
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} finally {
			paramField.setAccessible(accessible);
		}
	}

	private boolean enforceAccessible(AccessibleObject object) {
		// do not care about access modifier:
		// if a method is tagged with the StringSetter
		// annotation, we are supposed to access it.
		// This *is* safe.
		boolean accessible = object.isAccessible();
		object.setAccessible(true);
		return accessible;
	}

	private RuntimeException convertToUnchecked(InvocationTargetException e) {
		// this exception wraps Throwables intercepted in the invocation of the setter.
		// Avoid multiple wrappings (exception wrapped in InvocationTargetException
		// itself wrapped in a RuntimeException), as it makes error messages
		// messy.
		var cause = e.getCause();
		if (cause instanceof Error error) {
			throw error;
		}
		return (cause instanceof RuntimeException runtimeException) ? runtimeException : new RuntimeException(cause);
	}

	private Object fromString(String value, Class<?> type) {
		if (value.equals("null")) {
			return null;
		} else if (type.equals(String.class)) {
			return value;
		} else if (type.equals(Float.class) || type.equals(Float.TYPE)) {
			return Float.parseFloat(value);
		} else if (type.equals(Double.class) || type.equals(Double.TYPE)) {
			return Double.parseDouble(value);
		} else if (type.equals(Integer.class) || type.equals(Integer.TYPE)) {
			return Integer.parseInt(value);
		} else if (type.equals(Long.class) || type.equals(Long.TYPE)) {
			return Long.parseLong(value);
		} else if (type.equals(Boolean.class) || type.equals(Boolean.TYPE)) {
			Preconditions.checkArgument(value.equals("true") || value.equals("false"),
					"Incorrect value of the boolean parameter: %s", value);
			return Boolean.parseBoolean(value);
		} else if (type.equals(Character.class) || type.equals(Character.TYPE)) {
			Preconditions.checkArgument(value.length() == 1, "%s is not a single char!", value);
			return value.toCharArray()[0];
		} else if (type.equals(Byte.class) || type.equals(Byte.TYPE)) {
			return Byte.parseByte(value);
		} else if (type.equals(Short.class) || type.equals(Short.TYPE)) {
			return Short.parseShort(value);
		} else if (type.isEnum()) {
			try {
				return Enum.valueOf(type.asSubclass(Enum.class), value);
			} catch (IllegalArgumentException e) {
				// happens when the string does not correspond to any enum values.
				// Surprisingly, the default error message does not print the possible
				// values: do it here, so that the user gets an idea of what went wrong
				String comment = "Error trying to set value "
						+ value
						+ " for type "
						+ type.getName()
						+ ". Possible values are: "
						+ Arrays.stream(type.getEnumConstants()).map(Object::toString).collect(joining(","));
				throw new IllegalArgumentException(comment, e);
			}
		} else {
			throw new RuntimeException("Unsupported type: " + type);
		}
	}

	@Override
	public final String getValue(final String param_name) {
		var getter = stringGetters.get(param_name);
		if (getter != null) {
			return invokeGetter(getter);
		}

		var field = paramFields.get(param_name);
		if (field != null) {
			return getParamField(field);
		}

		Preconditions.checkArgument(storeUnknownParameters, "Module %s of type %s doesn't store unknown parameters."
						+ " Parameter %s is not part of the valid parameters: %s", getName(), getClass().getName(), param_name,
				registeredParams);

		log.warn("no getter found for param {}: trying parent method", param_name);
		return super.getValue(param_name);
	}

	private String invokeGetter(Method getter) {
		boolean accessible = enforceAccessible(getter);
		try {
			var result = getter.invoke(this);
			return result == null ? null : result + "";
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw convertToUnchecked(e);
		} finally {
			getter.setAccessible(accessible);
		}
	}

	private String getParamField(Field paramField) {
		boolean accessible = enforceAccessible(paramField);
		try {
			var result = paramField.get(this);
			return result == null ? null : result + "";
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} finally {
			paramField.setAccessible(accessible);
		}
	}

	@Override
	public final Map<String, String> getParams() {
		final Map<String, String> map = super.getParams();
		registeredParams.forEach(f -> addParameterToMap(map, f));
		return map;
	}

	/**
	 * Comments for parameters of an enum type are automatically generated,
	 * containing a list of possible values. They can be overriden by subclasses.
	 *
	 * <br>
	 * it is recommended for subclasses to get this map using <tt>super.getComments()</tt>
	 * and fill it with additional comments, rather than generate an empty map.
	 */
	@Override
	public Map<String, String> getComments() {
		// generate some default comments.
		final Map<String, String> comments = super.getComments();

		for (String paramName : registeredParams) {
			if (comments.containsKey(paramName)) {
				// at the time of implementation, this is not possible,
				// but who knows? Do not override something already there.
				continue;
			}

			final Class<?> type;
			var setter = setters.get(paramName);
			if (setter != null) {
				type = setter.getParameterTypes()[0];
			} else {
				var field = paramFields.get(paramName);

				Comment annotation = field.getAnnotation(Comment.class);
				if (annotation != null) {
					comments.put(paramName, annotation.value());
					continue;
				}

				type = field.getType();
			}

			if (type.isEnum()) {
				// generate an automatic comment containing the possible values for enum types
				var comment = "Possible values: " + Arrays.stream(type.getEnumConstants())
						.map(Object::toString)
						.collect(joining(","));
				comments.put(paramName, comment);
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
	@Retention(RetentionPolicy.RUNTIME)
	public @interface StringSetter {
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
	@Retention(RetentionPolicy.RUNTIME)
	public @interface StringGetter {
		/**
		 * the name of the field in the XML document
		 */
		String value();
	}

	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Parameter {
		/**
		 * the name of the field in the XML document
		 * <p>
		 * If empty string (e.g. value is not provided), the name of the annotated field is used instead.
		 */
		String value() default "";
	}

	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Comment {
		/**
		 * The comment of the field in the XML document
		 */
		String value() default "";
	}

	private static void checkModuleConsistency(boolean condition, String messageTemplate, Object... args) {
		if (!condition) {
			throw new InconsistentModuleException(Strings.lenientFormat(messageTemplate, args));
		}
	}

	public static class InconsistentModuleException extends RuntimeException {
		@Serial
		private static final long serialVersionUID = 1L;

		private InconsistentModuleException(final String msg) {
			super(msg);
		}
	}
}


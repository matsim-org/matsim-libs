
/* *********************************************************************** *
 * project: org.matsim.*
 * QSimComponentsConfig.java
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

 package org.matsim.core.mobsim.qsim.components;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.inject.Key;
import com.google.inject.name.Names;

/**
 * Contains information about which QSim components should be used in the
 * simulation and in which order they are registered with the QSim.
 *
 * @deprecated -- one can achive many (all?) of the same functionality at the config level, see {@link QSimComponentsConfigGroup}.  Doing it through
 * the config group is consistent with other places where we have similar functionality (which is to bind things by Guice, but activate them
 * separately), e.g. related to {@link org.matsim.core.config.groups.ReplanningConfigGroup}.  I think that the parallel functionality here just makes
 * it more difficult, since maintainers need to learn another dialect.  kai, jan'25
 */
final public class QSimComponentsConfig {
	private final List<Object> components = new LinkedList<>();
	private final Set<Key<?>> keys = new HashSet<>();


	/**
	 * @deprecated see javadoc of {@link QSimComponentsConfig}
	 */
	@Deprecated
	public void addComponent(Class<? extends Annotation> annotation) {
		addComponent(Key.get(Object.class, annotation));
		components.add(annotation);
	}

	/**
	 * @deprecated see javadoc of {@link QSimComponentsConfig}
	 */
	@Deprecated
	public void addComponent(Annotation annotation) {
		addComponent(Key.get(Object.class, annotation));
		components.add(annotation);
	}

	private void addComponent(Key<?> componentKey) {
		if (keys.contains(componentKey)) {
			throw new IllegalStateException(keyToString(componentKey) + " is already registered.");
		}
		keys.add(componentKey);
	}

	/**
	 * @deprecated see javadoc of {@link QSimComponentsConfig}
	 */
	@Deprecated
	public void addNamedComponent(String name) {
		addComponent(Names.named(name));
	}

	/**
	 * @deprecated see javadoc of {@link QSimComponentsConfig}
	 */
	@Deprecated
	public void removeComponent(Class<? extends Annotation> annotation) {
		addComponent(Key.get(Object.class, annotation));
		components.remove(annotation);
	}

	/**
	 * @deprecated see javadoc of {@link QSimComponentsConfig}
	 */
	@Deprecated
	public void removeComponent(Annotation annotation) {
		removeComponent(Key.get(Object.class, annotation));
		components.remove(annotation);
	}

	private void removeComponent(Key<?> componentKey) {
		if (!keys.remove(componentKey)) {
			throw new IllegalStateException(keyToString(componentKey) + " is not registered.");
		}
	}

	/**
	 * @deprecated see javadoc of {@link QSimComponentsConfig}
	 */
	@Deprecated
	public void removeNamedComponent(String name) {
		removeComponent(Names.named(name));
	}

	/**
	 * @deprecated see javadoc of {@link QSimComponentsConfig}
	 */
	@Deprecated
	public boolean hasComponent(Class<? extends Annotation> annotation) {
		return hasComponent(Key.get(Object.class, annotation));
	}

	/**
	 * @deprecated see javadoc of {@link QSimComponentsConfig}
	 */
	@Deprecated
	public boolean hasComponent(Annotation annotation) {
		return hasComponent(Key.get(Object.class, annotation));
	}

	private boolean hasComponent(Key<?> componentKey) {
		return keys.contains(componentKey);
	}

	/**
	 * @deprecated see javadoc of {@link QSimComponentsConfig}
	 */
	@Deprecated
	public boolean hasNamedComponent(String name) {
		return hasComponent(Names.named(name));
	}

	private String keyToString(Key<?> componentKey) {
		return "Annotation" + componentKey.getAnnotation() != null ?
				" " + componentKey.getAnnotation() :
				"Type " + componentKey.getAnnotationType();
	}

	/**
	 * @deprecated see javadoc of {@link QSimComponentsConfig}
	 */
	@Deprecated
	public void clear() {
		components.clear();
		keys.clear();
	}

	/**
	 * @deprecated see javadoc of {@link QSimComponentsConfig}
	 */
	@Deprecated
	public List<Object> getActiveComponents() {
		return Collections.unmodifiableList(components);
	}
}

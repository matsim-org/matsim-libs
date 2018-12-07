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
 */
final public class QSimComponentsConfig {
	private final List<Object> components = new LinkedList<>();
	private final Set<Key<?>> keys = new HashSet<>();

	// looks like a manually maintained map to me; would it be possible to explain that design decision?  kai, dec'18

	public void addComponent(Class<? extends Annotation> annotation) {
		addComponent(Key.get(Object.class, annotation));
		components.add(annotation);
	}

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

	public void addNamedComponent(String name) {
		addComponent(Names.named(name));
	}

	public void removeComponent(Class<? extends Annotation> annotation) {
		addComponent(Key.get(Object.class, annotation));
		components.remove(annotation);
	}

	public void removeComponent(Annotation annotation) {
		removeComponent(Key.get(Object.class, annotation));
		components.remove(annotation);
	}

	private void removeComponent(Key<?> componentKey) {
		if (!keys.remove(componentKey)) {
			throw new IllegalStateException(keyToString(componentKey) + " is not registered.");
		}
	}

	public void removeNamedComponent(String name) {
		removeComponent(Names.named(name));
	}

	public boolean hasComponent(Class<? extends Annotation> annotation) {
		return hasComponent(Key.get(Object.class, annotation));
	}

	public boolean hasComponent(Annotation annotation) {
		return hasComponent(Key.get(Object.class, annotation));
	}

	private boolean hasComponent(Key<?> componentKey) {
		return keys.contains(componentKey);
	}

	public boolean hasNamedComponent(String name) {
		return hasComponent(Names.named(name));
	}

	private String keyToString(Key<?> componentKey) {
		return "Annotation" + componentKey.getAnnotation() != null ?
				" " + componentKey.getAnnotation() :
				"Type " + componentKey.getAnnotationType();
	}

	public void clear() {
		components.clear();
		keys.clear();
	}

	List<Object> getActiveComponents() {
		return Collections.unmodifiableList(components);
	}

	@Override
	public String toString() {
		StringBuilder strb = new StringBuilder() ;
		strb.append( super.toString() + "\n") ;
		for ( Object cc : components ){
			strb.append( " " + cc.toString() +"\n");
		}
		for ( Key<?> key : keys ) {
			strb.append( "" + key.toString() +"\n") ;
		}
		return strb.toString() ;
	}
}

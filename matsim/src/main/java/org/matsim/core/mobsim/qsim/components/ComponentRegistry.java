package org.matsim.core.mobsim.qsim.components;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;

public class ComponentRegistry {
	private final Logger logger = Logger.getLogger(ComponentRegistry.class);

	private final Map<Key<?>, List<Key<? extends QSimComponent>>> componentsByKey = new HashMap<>();

	ComponentRegistry() {
	}

	public void register(Key<? extends QSimComponent> component) {
		if (component.getAnnotationType() == null) {
			logger.warn(String.format(
					"Ignoring QSimComponent '%s' because annotation is missing. This may raise an exception in the future.",
					component.getTypeLiteral()));
			return;
		}

		Key<?> key = component.getAnnotation() != null ? //
				Key.get(Object.class, component.getAnnotation()) //
				: Key.get(Object.class, component.getAnnotationType());
		componentsByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(component);
	}

	public List<Key<? extends QSimComponent>> getOrderedComponents(QSimComponents config) {
		List<Key<? extends QSimComponent>> orderedComponents = new LinkedList<>();

		for (Object annotation : config.getActiveComponents()) {
			Key<?> key = annotation instanceof Annotation ? //
					Key.get(Object.class, (Annotation) annotation) //
					: Key.get(Object.class, (Class<? extends Annotation>) annotation);

			List<Key<? extends QSimComponent>> components = componentsByKey.get(key);
			if (components != null) {
				orderedComponents.addAll(components);
			}
		}
		return orderedComponents;
	}

	static public ComponentRegistry create(Injector injector) {
		ComponentRegistry registry = new ComponentRegistry();

		for (Map.Entry<Key<?>, Binding<?>> entry : injector.getAllBindings().entrySet()) {
			if (QSimComponent.class.isAssignableFrom(entry.getKey().getTypeLiteral().getRawType())) {
				registry.register((Key<? extends QSimComponent>) entry.getKey());
			}
		}

		return registry;
	}
}

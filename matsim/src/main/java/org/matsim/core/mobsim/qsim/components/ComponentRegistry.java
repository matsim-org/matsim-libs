package org.matsim.core.mobsim.qsim.components;

import java.lang.annotation.Annotation;
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
	
	private final Map<Annotation, List<Key<? extends QSimComponent>>> componentsByAnnotation = new HashMap<>();
	private final Map<Class<? extends Annotation>, List<Key<? extends QSimComponent>>> componentsByAnnotationType = new HashMap<>();

	ComponentRegistry() {
	}

	public void register(Key<? extends QSimComponent> component) {
		if (!componentsByAnnotation.containsKey(component.getAnnotation())) {
			componentsByAnnotation.put(component.getAnnotation(), new LinkedList<>());
		}

		if (!componentsByAnnotationType.containsKey(component.getAnnotationType())) {
			componentsByAnnotationType.put(component.getAnnotationType(), new LinkedList<>());
		}

		componentsByAnnotation.get(component.getAnnotation()).add(component);
		componentsByAnnotationType.get(component.getAnnotationType()).add(component);

		if (component.getAnnotation() == null) {
			logger.info("Registered " + component.getTypeLiteral() + " with annotation " + component.getAnnotation());
		} else {
			logger.warn("Registered " + component.getTypeLiteral() + " without annotation");	
		}
	}

	public List<Key<? extends QSimComponent>> getComponentsByAnnotation(Annotation annotation) {
		if (componentsByAnnotation.containsKey(annotation)) {
			return componentsByAnnotation.get(annotation);
		} else {
			throw new IllegalArgumentException(
					String.format("No components with annotation %s registered", annotation));
		}
	}

	public List<Key<? extends QSimComponent>> getComponentsByAnnotationType(
			Class<? extends Annotation> annotationType) {
		if (componentsByAnnotationType.containsKey(annotationType)) {
			return componentsByAnnotationType.get(annotationType);
		} else {
			throw new IllegalArgumentException(
					String.format("No components with annotation type %s registered", annotationType));
		}
	}

	public List<Key<? extends QSimComponent>> getOrderedComponents(QSimComponents config) {
		List<Key<? extends QSimComponent>> orderedComponents = new LinkedList<>();

		for (Object annotation : config.getActiveComponents()) {
			if (annotation instanceof Annotation) {
				orderedComponents.addAll(getComponentsByAnnotation((Annotation) annotation));
			} else {
				orderedComponents.addAll(getComponentsByAnnotationType((Class<? extends Annotation>) annotation));
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

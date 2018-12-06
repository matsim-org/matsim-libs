package org.matsim.core.mobsim.qsim.components;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import org.apache.log4j.Logger;

public class ComponentRegistry {
	private static final Logger log = Logger.getLogger( ComponentRegistry.class ) ;

	private final Map<Key<?>, List<Key<? extends QSimComponent>>> componentsByKey = new HashMap<>();

	ComponentRegistry() {
	}

	public boolean register(Key<? extends QSimComponent> component) {
		if (component.getAnnotationType() == null) {
			return false;
		}

		Key<?> key = component.getAnnotation() != null ? //
				Key.get(Object.class, component.getAnnotation()) //
				: Key.get(Object.class, component.getAnnotationType());
		componentsByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(component);
		return true ;
	}

	public List<Key<? extends QSimComponent>> getOrderedComponents(QSimComponentsConfig config) {
		List<Key<? extends QSimComponent>> orderedComponents = new LinkedList<>();

		for (Object annotation : config.getActiveComponents()) {
			Key<?> key = annotation instanceof Annotation ? //
					Key.get(Object.class, (Annotation) annotation) //
					: Key.get(Object.class, (Class<? extends Annotation>) annotation);

			List<Key<? extends QSimComponent>> components = componentsByKey.get(key);
			if (components != null) {
				orderedComponents.addAll(components);
			} else {
				throw new RuntimeException("no component registered under key=" + key ) ;
			}
		}
		return orderedComponents;
	}

	static public ComponentRegistry create(Injector injector) {
		ComponentRegistry registry = new ComponentRegistry();

		for (Map.Entry<Key<?>, Binding<?>> entry : injector.getAllBindings().entrySet()) {
			if (QSimComponent.class.isAssignableFrom(entry.getKey().getTypeLiteral().getRawType())) {
				boolean result = registry.register( (Key<? extends QSimComponent>) entry.getKey() );
//				if ( entry.getValue().toString().contains( "Mock" ) ){
				if ( result ){
					log.warn("") ;
					log.warn( "did register the following QSimComponent:" );
					log.warn( "key=" + entry.getKey() );
					log.warn( "value=" + entry.getValue() );
				} else {
					log.warn("") ;
					log.warn("did NOT register the following QSimComponent:") ;
					log.warn( "key=" + entry.getKey() );
					log.warn( "value=" + entry.getValue() );
				}
			}
		}

		return registry;
	}
}

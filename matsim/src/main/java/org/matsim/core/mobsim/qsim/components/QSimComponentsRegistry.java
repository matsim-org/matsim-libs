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

public final class QSimComponentsRegistry{
	private static final Logger log = Logger.getLogger( QSimComponentsRegistry.class ) ;

	private final Map<Key<?>, List<Key<? extends QSimComponent>>> componentsByKey = new HashMap<>();

	QSimComponentsRegistry() {
	}

	/**
	 * Registers the component, using its annotation, or if not available its annotation type, as key.  If neither is available, registration fails.
	 */
	public boolean register(Key<? extends QSimComponent> component) {
		if (component.getAnnotationType() == null) {
			log.warn("not registering QSimComponent because it has no annotation: " + component ) ;
			return false;
		}

		Key<?> key = component.getAnnotation() != null ? //
				Key.get(Object.class, component.getAnnotation()) //
				: Key.get(Object.class, component.getAnnotationType());
		componentsByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(component);
		return true ;
	}

	public List<Key<? extends QSimComponent>> getOrderedComponents( QSimComponentAnnotationsRegistry annotationsRegistry ) {
		log.warn("") ;
		log.warn("entering getOrderedComponents ... ") ;

		List<Key<? extends QSimComponent>> orderedComponents = new LinkedList<>();

		log.warn("") ;
		log.warn("componentsByKey=" ) ;
		for( Key<?> key : componentsByKey.keySet() ){
			log.warn(key) ;
		}
		log.warn("") ;
		for( List<Key<? extends QSimComponent>> value : componentsByKey.values() ){
			log.warn(value) ;
		}
		log.warn("") ;

		for (Object annotation : annotationsRegistry.getActiveComponentAnnotations()) {
			log.warn("annotation=" + annotation ) ;
			Key<?> key = annotation instanceof Annotation ? //
					Key.get(Object.class, (Annotation) annotation) //
					: Key.get(Object.class, (Class<? extends Annotation>) annotation);
			log.warn("key=" + key ) ;
			List<Key<? extends QSimComponent>> components = componentsByKey.get(key);
			log.warn("components=" + components) ;
			if (components != null) {
				orderedComponents.addAll(components);
			} else {
				throw new RuntimeException("no component registered under key=" + key ) ;
			}
		}
		log.warn("... leaving getOrderedComponents.") ;
		log.warn("") ;
		return orderedComponents;
	}

	static public QSimComponentsRegistry create( Injector injector ) {
		QSimComponentsRegistry registry = new QSimComponentsRegistry();

		for (Map.Entry<Key<?>, Binding<?>> entry : injector.getAllBindings().entrySet()) {
			if (QSimComponent.class.isAssignableFrom(entry.getKey().getTypeLiteral().getRawType())) {
				log.warn("") ;
				boolean result = registry.register( (Key<? extends QSimComponent>) entry.getKey() );
//				if ( entry.getValue().toString().contains( "Mock" ) ){
				if ( result ){
					log.warn( "did register the following QSimComponent:" );
					log.warn( "key=" + entry.getKey() );
					log.warn( "value=" + entry.getValue() );
				} else {
					log.warn("did NOT register the following QSimComponent:") ;
					log.warn( "key=" + entry.getKey() );
					log.warn( "value=" + entry.getValue() );
				}
			}
		}

		return registry;
	}
}

package org.matsim.core.mobsim.qsim.components;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Named;

public class NamedComponentUtils {
	static public <T> Map<String, T> instantiate(Injector injector, Map<String, Key<T>> componentKeys) {
		Map<String, T> components = new HashMap<>();
		componentKeys.forEach((name, key) -> components.put(name, injector.getInstance(key)));
		return components;
	}

	static public <T> Map<String, Key<T>> find(Injector injector, Class<T> componentType) {
		Map<String, Key<T>> components = new HashMap<>();

		for (Map.Entry<Key<?>, Binding<?>> entry : injector.getAllBindings().entrySet()) {
			if (entry.getKey().getTypeLiteral().getRawType().equals(componentType)) {
				if (entry.getKey().hasAttributes()) {
					Annotation annotation = entry.getKey().getAnnotation();

					if (annotation != null && annotation instanceof Named) {
						String name = ((Named) annotation).value();
						components.put(name, entry.getKey().ofType(componentType));
					}
				}
			}
		}

		return components;
	}
}

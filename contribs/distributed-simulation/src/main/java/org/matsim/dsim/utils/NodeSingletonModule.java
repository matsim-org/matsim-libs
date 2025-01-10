package org.matsim.dsim.utils;

import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import org.matsim.core.mobsim.dsim.NodeSingleton;

import java.util.Map;

/**
 * This module provides the classes annotated with {@link org.matsim.core.mobsim.dsim.NodeSingleton} from the parent injector.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class NodeSingletonModule extends AbstractModule {
	private final Injector parent;

	public NodeSingletonModule(Injector parent) {
		this.parent = parent;
	}

	@Override
	protected void configure() {

		for (Map.Entry<Key<?>, Binding<?>> e : parent.getAllBindings().entrySet()) {

			Key key = e.getKey();
			Binding<?> binding = e.getValue();

			if (isSingleton(binding)) {
				Object instance = parent.getInstance(key);
				bind(key).toInstance(instance);
			}

		}
	}

	private static boolean isSingleton(Binding<?> binding) {
		Key<?> key = binding.getKey();
		Class<?> type = key.getTypeLiteral().getRawType();

		return type.isAnnotationPresent(NodeSingleton.class);
	}
}

package org.matsim.core.mobsim.dsim;

/**
 * Utility for detecting whether an object's class (or any class or interface in its hierarchy) is annotated with {@link NodeSingleton}.
 * <p>
 * The check intentionally walks the full hierarchy so that classes implementing an annotated interface (e.g. {@code DrtOptimizer}) are detected as
 * node singletons even when the implementing class itself carries no annotation.
 */
public final class NodeSingletons {

	private NodeSingletons() {
	}

	public static boolean isNodeSingleton(Object o) {
		var clazz = o.getClass();
		while (clazz != null) {
			if (clazz.isAnnotationPresent(NodeSingleton.class))
				return true;
			for (var iface : clazz.getInterfaces()) {
				if (isNodeSingletonInterface(iface))
					return true;
			}
			clazz = clazz.getSuperclass();
		}
		return false;
	}

	private static boolean isNodeSingletonInterface(Class<?> iface) {
		if (iface.isAnnotationPresent(NodeSingleton.class))
			return true;
		for (Class<?> parent : iface.getInterfaces()) {
			if (isNodeSingletonInterface(parent))
				return true;
		}
		return false;
	}
}

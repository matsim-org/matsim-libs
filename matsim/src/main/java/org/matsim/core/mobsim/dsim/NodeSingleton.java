package org.matsim.core.mobsim.dsim;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for components that are singletons within a single simulation node.
 * Such components are created once per node and shared between all partitions.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NodeSingleton {
}

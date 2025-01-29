package org.matsim.application;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a dependency to be used in a {@link CommandSpec}.
 * This annotation is not used directly on other classes.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_USE)
public @interface Dependency {

	/**
	 * Analysis command required by this command.
	 */
	Class<? extends MATSimAppCommand> value();

	/**
	 * List of file names that are used as input for this class.
	 */
	String[] files() default {};

	/**
	 * Whether this dependency is required.
	 */
	boolean required() default false;

}

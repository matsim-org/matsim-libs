package org.matsim.application;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the specification for a command usable by external tools.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandSpec {

	/**
	 * List of file names that are used as input for this class.
	 */
	String[] requires() default {};

	/**
	 * Whether a network is required as input.
	 */
	boolean requireNetwork() default false;

	/**
	 * Whether a population is required as input.
	 */
	boolean requirePopulation() default false;

	/**
	 * Whether an events file is required as input.
	 */
	boolean requireEvents() default false;

	/**
	 * Whether a count file is required as input.
	 */
	boolean requireCounts() default false;

	/**
	 * Whether a run directory is required as input.
	 */
	boolean requireRunDirectory() default false;

	/**
	 * List of files names that are produces by this command and accessible by others as input.
	 */
	String[] produces() default {};

	/**
	 * Other commands that produce input needed by this command.
	 */
	Dependency[] dependsOn() default {};

	/**
	 * Group name / identifier. Will use the package name if this is not changed here.
	 */
	String group() default "";

}

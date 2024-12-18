package org.matsim.application;

public @interface Dependency {

	Class<? extends MATSimAppCommand> value();

	String[] files() default {};

	boolean required() default false;

}

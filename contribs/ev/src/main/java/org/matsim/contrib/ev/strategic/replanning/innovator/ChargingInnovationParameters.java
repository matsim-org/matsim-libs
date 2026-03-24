package org.matsim.contrib.ev.strategic.replanning.innovator;

import org.matsim.core.config.ReflectiveConfigGroup;

import com.google.common.base.Preconditions;

public abstract class ChargingInnovationParameters extends ReflectiveConfigGroup {
	static public final String PREFIX = "innovation:";

	public ChargingInnovationParameters(String name) {
		super(name);
		Preconditions.checkArgument(name.startsWith(PREFIX));
	}

	public enum ConstraintErrorMode {
		none, printWarning, throwException
	}

	public enum ConstraintFallbackBehavior {
		returnRandom, returnNone
	}

	@Parameter
	private int constraintIterations = 0;

	@Parameter
	private ConstraintErrorMode constraintErrorMode = ConstraintErrorMode.printWarning;

	@Parameter
	private ConstraintFallbackBehavior constraintFallbackBehavior = ConstraintFallbackBehavior.returnNone;

	public int getConstraintIterations() {
		return constraintIterations;
	}

	public void setConstraintIterations(int val) {
		constraintIterations = val;
	}

	public ConstraintErrorMode getConstraintErrorMode() {
		return constraintErrorMode;
	}

	public void setConstraintErrorMode(ConstraintErrorMode val) {
		constraintErrorMode = val;
	}

	public ConstraintFallbackBehavior getConstraintFallbackBehavior() {
		return constraintFallbackBehavior;
	}

	public void setConstraintFallbackBehavior(ConstraintFallbackBehavior val) {
		constraintFallbackBehavior = val;
	}
}

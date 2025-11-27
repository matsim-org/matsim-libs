package org.matsim.contrib.ev.strategic.replanning.innovator;

import org.matsim.core.config.ReflectiveConfigGroup;

import com.google.common.base.Preconditions;

public abstract class ChargingInnovationParameters extends ReflectiveConfigGroup {
	static public final String PREFIX = "innovation:";

	public ChargingInnovationParameters(String name) {
		super(name);
		Preconditions.checkArgument(name.startsWith(PREFIX));
	}

	public enum ErrorMode {
		none, printWarning, throwException
	}

	@Parameter
	private int constraintIterations = 0;

	@Parameter
	private ErrorMode constraintErrorMode = ErrorMode.printWarning;

	public int getConstraintIterations() {
		return constraintIterations;
	}

	public void setConstraintIterations(int val) {
		constraintIterations = val;
	}

	public ErrorMode getConstraintErrorMode() {
		return constraintErrorMode;
	}

	public void setConstraintErrorMode(ErrorMode val) {
		constraintErrorMode = val;
	}
}

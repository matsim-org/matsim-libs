package org.matsim.contribs.discrete_mode_choice.model.nested;

public class DefaultNest implements Nest {
	private final String name;
	private double scaleParameter;

	public DefaultNest(String name, double scaleParameter) {
		this.name = name;
		this.scaleParameter = scaleParameter;
	}

	public String getName() {
		return name;
	}

	@Override
	public double getScaleParameter() {
		return scaleParameter;
	}

	public void setScaleParameter(double scaleParameter) {
		this.scaleParameter = scaleParameter;
	}
}
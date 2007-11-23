/* *********************************************************************** *
 * project: org.matsim.*
 * PlansConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.marcel.config.groups;

public class PlansConfigGroup extends AbstractFileIOConfigGroup {

	public static final String GROUP_NAME = "plans";

	public static final String PLANS_STREAMING = "usePlansStreaming";
	public static final String OUTPUT_SAMPLE = "outputSample";
	
	private boolean usePlansStreaming = false;
	private double outputSample = 1.0;
	
	public PlansConfigGroup() {
		super();
		this.keyset.add(PLANS_STREAMING);
		this.keyset.add(OUTPUT_SAMPLE);
	}
	
	@Override
	public String getName() {
		return GROUP_NAME;
	}
	
	public String getValue(String key) {
		if (key.equals(PLANS_STREAMING)) {
			return (usePlansStreaming() ? "true" : "false");
		} else if (key.equals(OUTPUT_SAMPLE)) {
			return Double.toString(getOutputSample());
		} else {
			return super.getValue(key);
		}
	}

	public void setValue(String key, String value) {
		if (key.equals(PLANS_STREAMING)) {
			usePlansStreaming(value.equals("true") || value.equals("yes"));
		} else if (key.equals(OUTPUT_SAMPLE)) {
			setOutputSample(Double.parseDouble(value));
		} else {
			super.setValue(key, value);
		}
	}

	
	/* direct access */

	public boolean usePlansStreaming() {
		return this.usePlansStreaming;
	}
	
	public void usePlansStreaming(boolean useStreaming) {
		this.usePlansStreaming = useStreaming;
	}
	
	public double getOutputSample() {
		return outputSample;
	}
	
	public void setOutputSample(double sample) {
		this.outputSample = sample;
	}
	
}

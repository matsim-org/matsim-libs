/* *********************************************************************** *
 * project: org.matsim.*
 * SnowballAttributes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.sna.snowball;

/**
 * Container class to store snowball related attributes.
 * 
 * @author illenberger
 * 
 */
public class SnowballAttributes {

	private Integer detected;

	private Integer sampled;

	/**
	 * Sets this element as detected in <tt>interation</tt>, i.e. another
	 * element has reported this element (its existence is known) but it has not
	 * been sampled yet.
	 * 
	 * @param iteration
	 *            the iteration in which this element has been detected.
	 */
	public void detect(Integer iteration) {
		detected = iteration;
	}

	/**
	 * Returns the iteration in which this element was sampled.
	 * 
	 * @return the iteration in which this element was sampled, or -1 if this
	 *         element has not been detected yet.
	 */
	public Integer getIterationDeteted() {
		return detected;
	}

	/**
	 * Returns whether this element is detected.
	 * 
	 * @return <tt>true</tt> if this element is detected, <tt>false</tt>
	 *         otherwise.
	 */
	public boolean isDetected() {
		return detected != null;
	}

	/**
	 * Set this element as sampled in <tt>iteration</tt>.
	 * 
	 * @param iteration
	 *            the iteration in which this element has been sampled.
	 */
	public void sample(Integer iteration) {
		sampled = iteration;
	}

	/**
	 * Returns the iteration in which this element has been sampled.
	 * 
	 * @return the iteration in which this element has been sampled.
	 */
	public Integer getIterationSampled() {
		return sampled;
	}

	/**
	 * Returns whether this element is sampled.
	 * 
	 * @return <tt>true</tt> if this element is sampled, <tt>false</tt>
	 *         otherwise.
	 */
	public boolean isSampled() {
		return sampled != null;
	}
}

/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetgen.config;

import java.util.Map;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author nagel
 *
 */
public class SocnetgenConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "socnetgen" ;
	public SocnetgenConfigGroup() {
		super(GROUP_NAME) ;
	}
	// ---
	private static final String ITERATIONS = "iterations" ;
	private static final String LOG_INTERVAL = "loginterval" ;
	private static final String SAMPLE_INTERVAL = "sampleinterval" ;
	private static final String OUTPUT = "output" ;
	private static final String THETA_DISTANCE = "theta_distance" ;
	private static final String THETA_GENDER = "theta_gender" ;
	private static final String THETA_AGE = "theta_age" ;
	private static final String THETA_TRIANGLES = "theta_triangles" ;
	private static final String CONSERVE_PK = "conservePk" ;
	@Override public final Map<String, String> getComments() {
		Map<String, String> map = super.getComments() ;
		
		return map ;
	}
	// ---
	private Long sngIterations = (long) 0 ;
	@StringGetter(ITERATIONS) 
	public final Long getSngIterations() {
		return this.sngIterations;
	}
	@StringSetter(ITERATIONS) 
	public final void setSngIterations(Long sngIterations) {
		this.sngIterations = sngIterations;
	}
	// ---
	private Long logInterval = (long) 0 ;
	@StringGetter(LOG_INTERVAL)
	public final Long getLogInterval() {
		return this.logInterval;
	}
	@StringSetter(LOG_INTERVAL)
	public final void setLogInterval(Long logInterval) {
		this.logInterval = logInterval;
	}
	// ---
	private Long sampleInterval = (long) 0 ;
	@StringGetter(SAMPLE_INTERVAL)
	public final Long getSampleInterval() {
		return this.sampleInterval;
	}
	@StringSetter(SAMPLE_INTERVAL)
	public final void setSampleInterval(Long sampleInterval) {
		this.sampleInterval = sampleInterval;
	}
	// ---
	private String output = null ;
	@StringGetter(OUTPUT)
	public final String getOutput() {
		return this.output;
	}
	@StringSetter(OUTPUT)
	public final void setOutput(String output) {
		this.output = output;
	}
	// ---
	private Double thetaDistance = null ;
	@StringGetter(THETA_DISTANCE)
	public final Double getThetaDistance() {
		return this.thetaDistance;
	}
	@StringSetter(THETA_DISTANCE)
	public final void setThetaDistance(Double theta_distance) {
		this.thetaDistance = theta_distance;
	}
	// ---
	private Double thetaGender = null ;
	@StringGetter(THETA_GENDER)
	public final Double getThetaGender() {
		return this.thetaGender;
	}
	@StringSetter(THETA_GENDER)
	public final void setThetaGender(Double thetaGender) {
		this.thetaGender = thetaGender;
	}
	// ---
	private Double thetaAge = null ;
	@StringGetter( THETA_AGE )
	public final Double getThetaAge() {
		return this.thetaAge;
	}
	@StringSetter( THETA_AGE )
	public final void setThetaAge(Double thetaAge) {
		this.thetaAge = thetaAge;
	}
	// ---
	private Double thetaTriangles = null ;
	@StringGetter( THETA_TRIANGLES )
	public final Double getThetaTriangles() {
		return this.thetaTriangles;
	}
	@StringSetter( THETA_TRIANGLES )
	public final void setThetaTriangles(Double thetaTriangles) {
		this.thetaTriangles = thetaTriangles;
	}
	// ---
	private boolean conservingPk ;
	@StringGetter(CONSERVE_PK)
	public final boolean isConservingPk() {
		return this.conservingPk;
	}
	@StringSetter(CONSERVE_PK)
	public final void setConservingPk(boolean conservePk) {
		this.conservingPk = conservePk;
	}
}

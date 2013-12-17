/* *********************************************************************** *
 * project: org.matsim.*
 * PseudoSimConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.mobsim;

import org.matsim.core.config.experimental.ReflectiveModule;

/**
 * @author thibautd
 */
public class PseudoSimConfigGroup extends ReflectiveModule {
	public static final String GROUP_NAME = "pseudoSim";

	private boolean isUsePSimAtAll = false;
	private int nPSimIters = 5;
	private int period = 3;
	private int nThreads = 1;

	public PseudoSimConfigGroup() {
		super( GROUP_NAME );
	}

	@StringGetter( "isUsePSimAtAll" )
	public boolean isIsUsePSimAtAll() {
		return this.isUsePSimAtAll;
	}

	@StringSetter( "isUsePSimAtAll" )
	public void setIsUsePSimAtAll(boolean isUsePSimAtAll) {
		this.isUsePSimAtAll = isUsePSimAtAll;
	}

	@StringGetter( "nPSimIters" )
	public int getNPSimIters() {
		return this.nPSimIters;
	}

	@StringSetter( "nPSimIters" )
	public void setNPSimIters(int nPSimIters) {
		this.nPSimIters = nPSimIters;
	}

	@StringGetter( "period" )
	public int getPeriod() {
		return this.period;
	}

	@StringSetter( "period" )
	public void setPeriod(int period) {
		this.period = period;
	}

	@StringGetter( "nThreads" )
	public int getNThreads() {
		return this.nThreads;
	}

	@StringSetter( "nThreads" )
	public void setNThreads(int nThreads) {
		this.nThreads = nThreads;
	}
}


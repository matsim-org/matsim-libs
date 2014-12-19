/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ExperimentResource.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.mzilske.populationsize;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


public class ExperimentResource {

	private final String wd;

	public ExperimentResource(String wd) {
		this.wd = wd;
	}

	public Collection<String> getRegimes() {
		final Set<String> REGIMES = new HashSet<String>();
		REGIMES.add("uncongested");
		REGIMES.add("congested");
		return REGIMES;
	}

	public RegimeResource getRegime(String regime) {
		return new RegimeResource(wd + "regimes/" + regime, regime);
	}

}

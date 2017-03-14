/* *********************************************************************** *
 * project: org.matsim.*
 * ScoringFromEmissions.java
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
package playground.benjamin.internalization;

import org.apache.log4j.Logger;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.deprecated.scoring.ScoringFunctionAccumulator.BasicScoring;
import org.matsim.core.scoring.functions.ScoringParameters;


/**
 * @author benjamin
 *
 */
@Deprecated
public class ScoringFromEmissions implements BasicScoring, WarmEmissionEventHandler, ColdEmissionEventHandler{
	
	private static final Logger logger = Logger.getLogger(ScoringFromEmissions.class);

	ScoringParameters params;

	public ScoringFromEmissions(ScoringParameters params) {
		this.params = params;
		logger.info("using " + ScoringFromEmissions.class.getName() + "...");
	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub
	}

	@Override
	public double getScore() {
		return 0;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
		// TODO Auto-generated method stub
		
	}

}

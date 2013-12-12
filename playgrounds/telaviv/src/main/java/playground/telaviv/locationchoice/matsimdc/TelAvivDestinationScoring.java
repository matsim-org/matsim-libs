/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.telaviv.locationchoice.matsimdc;

import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;


public class TelAvivDestinationScoring extends org.matsim.contrib.locationchoice.bestresponse.scoring.DestinationScoring { 
	private DestinationChoiceBestResponseContext dcContext;
		
	public TelAvivDestinationScoring(DestinationChoiceBestResponseContext dcContext) {
		super(dcContext);
		this.dcContext = dcContext;
	}
	
	public double getZonalScore(PlanImpl plan, ActivityImpl act) {
		return 0.0;
	}
}

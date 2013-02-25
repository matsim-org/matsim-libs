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

package org.matsim.contrib.locationchoice.bestresponse.scoring;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.locationchoice.BestReplyDestinationChoice;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;

public class DCActivityWOFacilitiesScoringFunction extends CharyparNagelActivityScoring {
	static final Logger log = Logger.getLogger(DCActivityWOFacilitiesScoringFunction.class);	
	private DestinationScoring destinationChoiceScoring;	
	private Plan plan ;
	
	public DCActivityWOFacilitiesScoringFunction(Plan plan, DestinationChoiceBestResponseContext lcContext) {
		super(lcContext.getParams());
		this.destinationChoiceScoring = new DestinationScoring(lcContext);
		this.plan = plan ;
	}
	
	@Override
	public void finish() {		
		
		super.finish();

		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				this.score += destinationChoiceScoring.getDestinationScore((PlanImpl)plan, (ActivityImpl)pe, BestReplyDestinationChoice.useScaleEpsilonFromConfig);
			}
		}
	}
}

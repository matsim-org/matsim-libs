/* *********************************************************************** *
 * project: org.matsim.*
 * DgFlightScoringFunction
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
package air.archive;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.pt.PtConstants;


/**
 * @author dgrether
 * @deprecated not needed anymore, functionality is now core matsim
 */
@Deprecated
public class DgFlightCharyparNagelActivityScoringFunction extends CharyparNagelActivityScoring {
	
	private static final String PT_INTERACTION_ACTIVITY_TYPE = PtConstants.TRANSIT_ACTIVITY_TYPE;

	private final double lineSwitch;
	
	private int noOfLineSwitch = 0;

//	private double ptInteractionStartTime;

	public DgFlightCharyparNagelActivityScoringFunction(final CharyparNagelScoringParameters params, double utilityOfLineSwitch) {
		super(params);
		this.lineSwitch = utilityOfLineSwitch;
	}


	@Override
	public void startActivity(double time, Activity activity) {
		if (PT_INTERACTION_ACTIVITY_TYPE.equalsIgnoreCase(activity.getType())){
//			this.ptInteractionStartTime = activity.getStartTime();
		}
		else {
			super.startActivity(time, activity);
		}
	}

	@Override
	public void endActivity(double time, Activity activity) {
		if (PT_INTERACTION_ACTIVITY_TYPE.equalsIgnoreCase(activity.getType())){
//			double ptInteractionDuration = (time - this.ptInteractionStartTime);
//			this.score  += (ptInteractionDuration * this.lineSwitch);
			this.noOfLineSwitch++;
		}
		else {
			super.endActivity(time, activity);
		}
	}

	@Override
	public void finish(){
		super.finish();
		if (this.noOfLineSwitch > 2){
			this.score += (this.noOfLineSwitch - 2) * this.lineSwitch;
		}
	}

}

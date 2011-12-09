/* *********************************************************************** *
 * project: org.matsim.*
 * AggloControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.anhorni.agglo.run;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;
import org.matsim.locationchoice.analysis.DistanceStats;
import org.matsim.locationchoice.bestresponse.scoring.MixedScoringFunctionFactory;
import org.matsim.locationchoice.bestresponse.scoring.ScaleEpsilon;
import org.matsim.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.locationchoice.facilityload.FacilityPenalty;
import org.matsim.locationchoice.utils.ActTypeConverter;
import org.matsim.locationchoice.utils.ActivitiesHandler;

public class AggloControler extends Controler {
				
	public AggloControler(final String[] args) {
		super(args);	
	}

    public static void main (final String[] args) { 
    	AggloControler controler = new AggloControler(args);
    	controler.setOverwriteFiles(true);
    	controler.run();
    }
    
    @Override
    protected void setUp() {
      super.setUp();    	  
    	ActivitiesHandler defineFlexibleActivities = new ActivitiesHandler(this.config.locationchoice());
  		ScaleEpsilon scaleEpsilon = defineFlexibleActivities.createScaleEpsilon();
  		
  		ActTypeConverter actTypeConverter = defineFlexibleActivities.createActivityTypeConverter();
           
  		MixedScoringFunctionFactory mixedScoringFunctionFactory =
			new MixedScoringFunctionFactory(this.config, this, scaleEpsilon, actTypeConverter);
  	
		this.setScoringFunctionFactory(mixedScoringFunctionFactory);
		this.addControlerListener(new DistanceStats(this.config, "best", "s", actTypeConverter));
		this.addControlerListener(new DistanceStats(this.config, "best", "l", actTypeConverter));
		
			
		// add retailer relocation choice
		TreeMap<Id, FacilityPenalty> facilityPenalties = new TreeMap<Id, FacilityPenalty>(); 
		this.addControlerListener(new FacilitiesLoadCalculator(facilityPenalties));
		this.addControlerListener(new RetailerRelocation(facilityPenalties));
	}    
}

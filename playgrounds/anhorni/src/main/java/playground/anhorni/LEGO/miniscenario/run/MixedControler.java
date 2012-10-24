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

package playground.anhorni.LEGO.miniscenario.run;

import org.matsim.contrib.locationchoice.analysis.DistanceStats;
import org.matsim.contrib.locationchoice.bestresponse.scoring.MixedScoringFunctionFactory;
import org.matsim.contrib.locationchoice.bestresponse.scoring.ScaleEpsilon;
import org.matsim.contrib.locationchoice.utils.ActTypeConverter;
import org.matsim.contrib.locationchoice.utils.ActivitiesHandler;
import org.matsim.core.controler.Controler;

public class MixedControler extends Controler {
				
	public MixedControler(final String[] args) {
		super(args);	
	}

    public static void main (final String[] args) { 
    	MixedControler controler = new MixedControler(args);
    	controler.setOverwriteFiles(true);
    	controler.run();
    }
    
    @Override
    protected void setUp() {
      super.setUp();
      
      // do not score load
      //double fLoad = Double.parseDouble(config.findParam(LCEXP, "scoreElementFLoad"));       
      //if (!(fLoad > 0.00000001)) {
    	  this.getConfig().setParam("locationchoice", "restraintFcnFactor", "0.0");
      //}
    	  
    	ActivitiesHandler defineFlexibleActivities = new ActivitiesHandler(this.config.locationchoice());
  		ScaleEpsilon scaleEpsilon = defineFlexibleActivities.createScaleEpsilon();
  		
  		ActTypeConverter actTypeConverter = defineFlexibleActivities.getConverter();
           
  		MixedScoringFunctionFactory mixedScoringFunctionFactory =
			new MixedScoringFunctionFactory(this.config, this, scaleEpsilon, actTypeConverter, defineFlexibleActivities.getFlexibleTypes());
  	
		this.setScoringFunctionFactory(mixedScoringFunctionFactory);
		//this.addControlerListener(new FacilitiesLoadCalculator(this.getFacilityPenalties()));
		this.addControlerListener(new DistanceStats(this.config, "best", "s", actTypeConverter));
		this.addControlerListener(new DistanceStats(this.config, "best", "l", actTypeConverter));
	}    
}

/* *********************************************************************** *
 * project: org.matsim.*
 * LCControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

import org.matsim.core.controler.Controler;
import playground.anhorni.LEGO.miniscenario.run.analysis.CalculatePlanTravelStats;
import playground.anhorni.LEGO.miniscenario.run.scoring.MixedScoringFunctionFactory;


public class MixedControler extends Controler {
	
	private final String LCEXP = "locationchoiceExperimental";
			
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
      
      double fLoad = Double.parseDouble(config.findParam(LCEXP, "scoreElementFLoad"));
           
      if (!(fLoad > 0.00000001)) {
    	  this.getConfig().setParam("locationchoice", "restraintFcnFactor", "0.0");
      }
           
      MixedScoringFunctionFactory mixedScoringFunctionFactory =
			new MixedScoringFunctionFactory(this.config.planCalcScore(), this);
  	
		this.setScoringFunctionFactory(mixedScoringFunctionFactory);
		//this.addControlerListener(new FacilitiesLoadCalculator(this.getFacilityPenalties()));
		this.addControlerListener(new CalculatePlanTravelStats(this.config, "best", "s"));
		this.addControlerListener(new CalculatePlanTravelStats(this.config, "best", "l"));
	}    
}

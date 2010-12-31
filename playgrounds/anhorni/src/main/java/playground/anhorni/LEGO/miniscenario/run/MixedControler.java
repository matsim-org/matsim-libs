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
//import org.matsim.locationchoice.facilityload.FacilitiesLoadCalculator;

import playground.anhorni.LEGO.miniscenario.ConfigReader;
import playground.anhorni.LEGO.miniscenario.run.analysis.CalculatePlanTravelStats;
import playground.anhorni.LEGO.miniscenario.run.scoring.DestinationChoicePreviousScoreComputer;
import playground.anhorni.LEGO.miniscenario.run.scoring.MixedScoringFunctionFactory;


public class MixedControler extends Controler {
	
	private ConfigReader configReader = new ConfigReader();
		
	public MixedControler(final String[] args) {
		super(args);	
	}

    public static void main (final String[] args) { 
    	MixedControler controler = new MixedControler(args);
    	controler.setOverwriteFiles(true);
    	controler.setWriteEventsInterval(250);
    	controler.run();
    }
    
    @Override
    protected void setUp() {
      super.setUp();
      
      configReader.read();
      
      if (!(configReader.getScoreElementFLoad() > 0.00000001)) {
    	  this.getConfig().setParam("locationchoice", "restraintFcnFactor", "0.0");
      }
      
      DestinationChoicePreviousScoreComputer previousScoreComputer = new DestinationChoicePreviousScoreComputer();
      
      MixedScoringFunctionFactory mixedScoringFunctionFactory =
			new MixedScoringFunctionFactory(this.config.charyparNagelScoring(), this, this.configReader, previousScoreComputer);
  	
		this.setScoringFunctionFactory(mixedScoringFunctionFactory);
		//this.addControlerListener(new FacilitiesLoadCalculator(this.getFacilityPenalties()));
		this.addControlerListener(new CalculatePlanTravelStats(configReader, "best", "s"));
		this.addControlerListener(new CalculatePlanTravelStats(configReader, "best", "l"));
		//this.addControlerListener(previousScoreComputer);
	}    
}

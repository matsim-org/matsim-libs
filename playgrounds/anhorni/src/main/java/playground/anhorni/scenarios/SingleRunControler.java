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

package playground.anhorni.scenarios;

import org.matsim.core.controler.Controler;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.anhorni.LEGO.miniscenario.run.analysis.CalculatePlanTravelStats;
import playground.anhorni.LEGO.miniscenario.run.scoring.MixedScoringFunctionFactory;
import playground.anhorni.scenarios.analysis.ShoppingCalculator;

public class SingleRunControler extends Controler {
	
	private ObjectAttributes personAttributes;
		
	public SingleRunControler(final String[] args) {
		super(args);	
	}
	
	public void setPersonAttributes(ObjectAttributes personAttributes) {
		this.personAttributes = personAttributes;
	}
		
    public static void main (final String[] args) { 
    	SingleRunControler controler = new SingleRunControler(args);
    	controler.run();
    }
    
    public void run() {
    	super.setOverwriteFiles(true);
    	super.addControlerListener(new ShoppingCalculator(this.personAttributes));
    	super.run();
    }
    
    @Override
    protected void setUp() {
      super.setUp();
            
      MixedScoringFunctionFactory mixedScoringFunctionFactory =
			new MixedScoringFunctionFactory(this.config.planCalcScore(), this);
  	
		this.setScoringFunctionFactory(mixedScoringFunctionFactory);
		this.addControlerListener(new CalculatePlanTravelStats(this.config, "best", "s"));
	}  
}

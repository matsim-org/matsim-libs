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

package playground.anhorni.PLOC;

import org.matsim.core.controler.Controler;
import org.matsim.locationchoice.analysis.DistanceStats;
import org.matsim.locationchoice.bestresponse.scoring.MixedScoringFunctionFactory;
import org.matsim.locationchoice.bestresponse.scoring.ScaleEpsilon;
import org.matsim.locationchoice.utils.ActTypeConverter;
import org.matsim.locationchoice.utils.ActivitiesHandler;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.anhorni.PLOC.analysis.ShoppingCalculator;

public class SingleRunControler extends Controler {
	
	private ObjectAttributes personAttributes;
	private int day = -1;
	private boolean tempVar;
		
	public SingleRunControler(final String[] args) {
		super(args);	
	}
	
	public void setDay(int day) {
		this.day = day;
	}
	
	public void setTempVar(boolean tempVar) {
		this.tempVar = tempVar;
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
    	if (this.day > -1) super.addControlerListener(new ShoppingCalculator(this.personAttributes, this.tempVar, this.day));
    	super.run();
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
	}  
}

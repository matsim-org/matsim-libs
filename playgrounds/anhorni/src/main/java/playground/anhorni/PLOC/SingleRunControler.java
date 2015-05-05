/* *********************************************************************** *
 * project: org.matsim.*
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

import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.analysis.DistanceStats;
import org.matsim.contrib.locationchoice.utils.ActTypeConverter;
import org.matsim.contrib.locationchoice.utils.ActivitiesHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.anhorni.PLOC.analysis.ShoppingCalculator;

public class SingleRunControler extends Controler {
	
	private ObjectAttributes personAttributes;
	private int day = -1;
	private boolean tempVar;
	public SingleRunControler(String config) {
		super(config);	
		throw new RuntimeException(Gbl.SET_UP_IS_NOW_FINAL) ;
	}
		
	public SingleRunControler(final Config config) {
		super(config);	
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
    	// load the config, telling it to "materialize" the location choice section:
    	Config config = ConfigUtils.loadConfig( args[0], new DestinationChoiceConfigGroup() ) ;
    	
    	SingleRunControler controler = new SingleRunControler(config);
    	controler.run();
    }
    
         
//    @Override
//    protected void setUp() {
//      super.setUp();
//      super.setOverwriteFiles(true);
//      
//      if (this.day > -1) super.addControlerListener(new ShoppingCalculator(this.personAttributes, this.tempVar, this.day));
//      
//      ActivitiesHandler defineFlexibleActivities = new ActivitiesHandler(
//    		  (DestinationChoiceConfigGroup) this.getConfig().getModule("locationchoice"));
//	  ActTypeConverter actTypeConverter = defineFlexibleActivities.getConverter();
//            
//		this.addControlerListener(new DistanceStats(this.getConfig(), "best", "s", actTypeConverter, "car"));
//		
//		throw new RuntimeException("integrate LC with listener!");
//	}  
}

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

import org.apache.log4j.Logger;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class SingleRunControler {
	static Controler controler ;
	
	private ObjectAttributes personAttributes;
	private int day = -1;
	private boolean tempVar;
	
	public SingleRunControler(String config) {
		controler = new Controler( config ) ;
		throw new RuntimeException( Gbl.RETROFIT_CONTROLER ) ;
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
	    Logger.getLogger("dummy").fatal( Gbl.RETROFIT_CONTROLER ) ;
	    System.exit(-1) ;
	    
	    
    	// load the config, telling it to "materialize" the location choice section:
    	Config config = ConfigUtils.loadConfig( args[0], new DestinationChoiceConfigGroup() ) ;
    	
    controler = new Controler(config);
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

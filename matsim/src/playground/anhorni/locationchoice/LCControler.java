/* *********************************************************************** *
 * project: org.matsim.*
 * LCControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;
import org.matsim.core.scoring.LocationChoiceScoringFunctionFactory;
import org.matsim.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.locationchoice.facilityload.FacilityPenalty;


public class LCControler extends Controler {
	
	private TreeMap<Id, FacilityPenalty> facilityPenalties;
	
	public LCControler(final String[] args) {
		super(args);
		
		this.facilityPenalties = new TreeMap<Id, FacilityPenalty>(); 
		this.addControlerListener(new FacilitiesLoadCalculator(this.facilityPenalties));		
	}

    @Override
    protected void setUp() {
      super.setUp();
      this.scoringFunctionFactory = new LocationChoiceScoringFunctionFactory(this.config.charyparNagelScoring(), this.facilityPenalties);
    }
 
    public static void main (final String[] args) { 
    	LCControler controler = new LCControler(args);
    	controler.run();
    }

}

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

import org.matsim.controler.Controler;

import playground.anhorni.locationchoice.facilityLoad.FacilitiesLoadCalculator;
import playground.anhorni.locationchoice.scoring.LocationChoiceScoringFunctionFactory;

public class LCControler extends Controler {
	
	public LCControler(final String[] args) {
		super(args);
	}

    @Override
    protected void setup() {
      super.setup();
      this.scoringFunctionFactory = new LocationChoiceScoringFunctionFactory();
    }
 
    public static void main (final String[] args) {
      LCControler controler = new LCControler(args);
      controler.addControlerListener(new FacilitiesLoadCalculator());
      controler.run();
    }

}

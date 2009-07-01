/* project: org.matsim.*
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

package playground.ciarif.retailers;

import org.matsim.core.api.experimental.population.PlanElement;
import org.matsim.core.api.facilities.Facility;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.locationchoice.facilityload.FacilitiesLoadCalculator;

public class RetailersControler extends Controler {
	
	public RetailersControler(String[] args) {
		super(args);
	}

	@Override
	protected void shutdown(final boolean unexpected) {
		super.shutdown(unexpected);
		
		//ComputeGravityModelParameters cgmp = new ComputeGravityModelParameters ();// TODO modify this in a way that it is not necessary
		// to override this method, but more simply, modify the method doIterations, and add a parameter which tells how many times the
		// relaxed state should be reached (respectively how many times the gravity model should be computed)
		// Basically the idea is that the number of iterations in the config file are multiplied for the number of time we want to perform
		// the computation of the gravity model. It means that each time the number of iteration is reached instead of shut down the program 
		// the gravity model is computed and than the same number of iterations is performed again.
		//cgmp.computeInitialParameters (this);
		/*for (Facility f:this.getFacilities().getFacilities().values()) {
			for (Person p:this.getPopulation().getPersons().values()) {
				for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
					if (pe instanceof Activity) {
						Activity act = (Activity) pe;
						if (act.getType().equals("shop") && act.getFacility().getId().equals(f.getId())) {
							
						}
					}	
				}
			}
		}*/
	}
	
    public static void main (final String[] args) { 
    	Controler controler = new RetailersControler(args);
    	//controler.addControlerListener(new RetailersParallelLocationListener());
    	controler.addControlerListener(new RetailersSequentialLocationListener()); //TODO Introduce a parameter in config file for the type of 
    	// relocation to be performed, sequential or parallel
    	controler.addControlerListener(new FacilitiesLoadCalculator(controler.getFacilityPenalties()));
    	controler.run();
    }
}

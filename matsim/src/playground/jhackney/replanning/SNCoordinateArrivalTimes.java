package playground.jhackney.replanning;
/* *********************************************************************** *
 * project: org.matsim.*
 * SNCoordinateArrivalTimes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.Hashtable;

import org.matsim.controler.Controler;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.network.NetworkLayer;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.replanning.modules.MultithreadedModuleA;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;
import org.matsim.socialnetworks.mentalmap.TimeWindow;
/**
 * A sample of schedule coordination in a replanning StrategyModule. The departure
 * time for an act is adjusted according to when friends will be at the act.
 *  
 * @author jhackney
 *
 */

public class SNCoordinateArrivalTimes extends MultithreadedModuleA {

	private playground.jhackney.controler.SNController3 controler;

	public SNCoordinateArrivalTimes(playground.jhackney.controler.SNController3 controler) {

    	this.controler=controler;
    }

    public PlanAlgorithm getPlanAlgoInstance() {

    	return new SNAdjustTimes(this.controler);
    }


}



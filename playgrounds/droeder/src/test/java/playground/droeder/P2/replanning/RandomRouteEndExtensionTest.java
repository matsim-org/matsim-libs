/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.P2.replanning;

import java.util.ArrayList;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

import playground.andreas.P2.pbox.Cooperative;
import playground.andreas.P2.plan.PPlan;
import playground.andreas.P2.replanning.PPlanStrategy;
import playground.droeder.P2.PScenarioHelper;



/**
 * @author droeder
 *
 */
public class RandomRouteEndExtensionTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	
	@Test
    public final void testRun() {
		Cooperative coop = PScenarioHelper.createCoop2111to2333();
		PPlan oldPlan = coop.getBestPlan();
		for(TransitStopFacility s: oldPlan.getStopsToBeServed()){
			System.out.println(s.getId());
		}
		
		System.out.println();
		int i = 0;
		for(TransitRoute r: oldPlan.getLine().getRoutes().values()){
			System.out.println("---- " + i + " ------");
			for(TransitRouteStop s: r.getStops()){
				System.out.println(s.getStopFacility().getLinkId());
			}
			i++;
			System.out.println("###");
		}
		
		ArrayList<String> parameters = new ArrayList<String>();
		parameters.add("0.7");
		PPlanStrategy strategy = new RandomRouteEndExtension(parameters);
		coop.init(coop.getRouteProvider(), strategy, 1);
		
		PPlan newPlan = coop.getBestPlan();
		
		for(TransitStopFacility s: newPlan.getStopsToBeServed()){
			System.out.println(s.getId());
		}
		
		System.out.println();
		for(TransitRoute r: newPlan.getLine().getRoutes().values()){
			for(TransitRouteStop s: r.getStops()){
				System.out.println(s.getStopFacility().getLinkId());
			}
			System.out.println("###");
		}
//		
//		coop.init(coop.getRouteProvider(), strategy, 2);
//		
//		newPlan = coop.getBestPlan();
//		
//		for(TransitStopFacility s: newPlan.getStopsToBeServed()){
//			System.out.println(s.getId());
//		}
//		
//		System.out.println();
//		for(TransitRoute r: newPlan.getLine().getRoutes().values()){
//			for(TransitRouteStop s: r.getStops()){
//				System.out.println(s.getStopFacility().getLinkId());
//			}
//			System.out.println("###");
//		}
	}

}

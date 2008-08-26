/* *********************************************************************** *
 * project: org.matsim.*
 * LimitedKnowledge.java
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

package playground.christoph.knowledge;

import org.matsim.network.NetworkLayer;
import org.matsim.population.Plan;
import org.matsim.replanning.modules.ReRouteDijkstra;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;

public class LimitedKnowledge extends ReRouteDijkstra {		

	KnowledgeTravelCost knowledgeCostFunction;
	
	// Erstmal alle Daten weiterleiten... 
	public LimitedKnowledge(NetworkLayer network, TravelCost travelCostCalc, TravelTime travelTimeCalc)
	{	
		super(network, travelCostCalc, travelTimeCalc);

		// Referenz auf die TravelCosts abspeichern - da schicken wir dann die jeweils aktuelle Person hin.
		knowledgeCostFunction = (KnowledgeTravelCost) travelCostCalc;
		System.out.println("----------------------LimitedKnowledge Router runs!----------------------");
		
	}	

	@Override
	public void handlePlan(Plan plan) {
		// TODO Auto-generated method stub
		//Knowledge myKnowledge = plan.getPerson().getKnowledge();
		
//		if(plan != null && plan.getPerson() != null) 
//		{ System.out.println("handlePlan, PersonID: " + plan.getPerson().getId().toString()); }
		
		// Person weiterleiten...
		knowledgeCostFunction.setPerson(plan.getPerson());

		// ... und Plan abarbeiten lassen.
		super.handlePlan(plan);
	}

}

/* *********************************************************************** *
 * project: org.matsim.*
 * KnowledgeTravelCost.java
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

/*
 * Anpassung der TravelCosts basierend auf des Wissensstandes einer Person.
 * -> Überlagerung mit dem globalen Wissen oder gänzliches Überschreiben.
 * 
 */

package playground.christoph.knowledge;

import org.matsim.network.Link;
import org.matsim.population.Person;
import org.matsim.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.router.util.TravelTime;

//public class KnowledgeTravelCost implements TravelCost{
public class KnowledgeTravelCost extends TravelTimeDistanceCostCalculator {

	Person person;
	
	public KnowledgeTravelCost(final TravelTime timeCalculator) {
		super(timeCalculator);
	}
	
//	public KnowledgeTravelCost(final TravelTime timeCalculator) {
//		super(timeCalculator);
//		this.timeCalculator = timeCalculator;
		/* Usually, the travel-utility should be negative (it's a disutility)
		 * but the cost should be positive. Thus negate the utility.
		 */
//		this.travelCostFactor = -Gbl.getConfig().charyparNagelScoring().getTraveling() / 3600.0;
//		this.distanceCost = Gbl.getConfig().charyparNagelScoring().getDistanceCost() / 1000.0;
//	}
	
	public void setPerson(Person p)
	{
		this.person = p;
		System.out.println("KnowledgeTravelCost - new aktive PersonID: " + person.getId().toString());
	}
	
	public double getLinkTravelCost(Link link, double time)
	{
		//System.out.println("----------------------KnowledgeTravelCost runs!----------------------");
		//System.out.println("link: " + link);
		//System.out.println("time: " + time);

		// Erst die Kosten basierend auf dem globalen Wissen besorgen...
		double costs = super.getLinkTravelCost(link, time);

		// ID der aktuell behandelten Person holen
		int ID = Integer.valueOf( person.getId().toString() ).intValue();
		
		// Links 6 & 15
		if(link.getId().toString().equals("6") || link.getId().toString().equals("15"))
		{
			//costs = Double.MAX_VALUE;
			
			// gerade ID? dann Strasse "sperren"
			if (ID % 2 == 0) costs = Double.MAX_VALUE; 
			else costs = 0.0;
		}
		
		// Links 10 & 19
		if(link.getId().toString().equals("10") || link.getId().toString().equals("19"))
		{
			//costs = 0.0;
			// ungerade ID? dann Strasse "sperren"
			if (ID % 2 == 1) costs = Double.MAX_VALUE;
			else costs = 0.0;
		}
		
		// ... dann prüfen, ob sich durch das lokale Wissen des Agenten Änderungen ergeben.
		return costs;
	}
		
}

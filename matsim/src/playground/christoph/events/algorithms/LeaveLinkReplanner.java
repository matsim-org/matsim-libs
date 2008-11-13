/* *********************************************************************** *
 * project: org.matsim.*
 * LeaveLinkReplanner.java
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

package playground.christoph.events.algorithms;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.mobsim.queuesim.PersonAgent;
import org.matsim.mobsim.queuesim.QueueNode;
import org.matsim.mobsim.queuesim.Vehicle;
import org.matsim.network.Link;
import org.matsim.network.Node;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Route;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.christoph.router.KnowledgePlansCalcRoute;

/*
 * As the ActEndReplanner the LeaveLinkReplanner should be called when a
 * LeaveLinkEvent is thrown. At the moment this does not work because
 * such an Event is thrown AFTER a Person left a link and entered a new link.
 * 
 * The current solution is to call the method by hand in the MyQueueNode class 
 * when a Person is at the End of a Link but before leaving the link.
 * 
 * MATSim Routers use Plan as Input Data. To be able to use them, we have to create
 * a new Plan from the current Position to the location of the next Activity.
 * 
 * This Replanner is called, if a person is somewhere on a Route between two Activities.
 * First the current Route is splitted into two parts - the already passed links and
 * the ones which are still to go. 
 * Next a new Plan is created with an Activity at the current Position and an Endposition
 * that is equal to the one from the original plan.
 * This Plan is handed over to the Router and finally the new route is merged with the
 * Links that already have been passed by the Person.
 */

public class LeaveLinkReplanner {

	protected Act nextAct;
	protected Act prevAct;
	protected Leg leg;
	protected double time;
	protected PersonAgent personAgent;
	protected Person person;
	protected Node node;
	protected Plan plan;
	protected QueueNode queueNode;
	protected Vehicle vehicle;
	
	protected PlanAlgorithm replanner; 
	
	private static final Logger log = Logger.getLogger(LeaveLinkReplanner.class);
	
	public LeaveLinkReplanner(QueueNode queueNode, Vehicle vehicle, double time, PlanAlgorithm replanner)
	{
		this.queueNode = queueNode;
		this.node = queueNode.getNode();
		this.time = time;
		this.vehicle = vehicle;
		this.personAgent = vehicle.getDriver();
		this.person = vehicle.getDriver().getPerson();
		this.replanner = replanner;
		
		init();
	}
	
	public LeaveLinkReplanner(QueueNode queueNode, Vehicle vehicle, double time)
	{
		this.queueNode = queueNode;
		this.node = queueNode.getNode();
		this.time = time;
		this.vehicle = vehicle;
		this.personAgent = vehicle.getDriver();
		this.person = vehicle.getDriver().getPerson();

		replanner = (PlanAlgorithm)person.getCustomAttributes().get("Replanner");
		if (replanner == null) log.error("No Replanner found in Person!");
		
		init();
		
	}	// Replanner(...)

	protected void init()
	{
		Plan plan = person.getSelectedPlan();

		leg = personAgent.getCurrentLeg();

		prevAct = (Act)plan.getPreviousActivity(leg);
		nextAct = (Act)plan.getNextActivity(leg);	

		// get the Link from where the Person came
		Link fromLink = getFromLink();
		
		// if there is a next Activity...
		if(nextAct != null)
		{
			if(fromLink != null)
			{
				log.info("LeaveLinkReplanner....................." + this.person.getId());
				// create new Route
				routing(fromLink);
			}
			else
			{
				log.error("No Link to to the current Node found - this should not happen!");
			}
			
		}
		else
		{
			log.error("An agents next activity is null - this should not happen!");
		}
	}
	
	
	/*
	 * Route am Ende jedes Links neu berechnen.
	 * Ansatz:
	 * - Neue Aktivit�t beim aktuellen Link generieren und Zielaktivit�t beibehalten.
	 * - Route generieren 
	 * - Alte Route mit neuer Route mergen
	 *   -> Annahme: ein Link ist in jeder Route nur 1x vorhanden (was auch Sinn macht, da
	 *      "Rundfahrten" wohl kaum die k�rzesten Wege von A nach B darstellen.
	 *   -> K�nnte doch noch Probleme hervorrufen. Agenten k�nnten an Stellen gelangen, wo
	 *      z.B. aufgrund von Staus umkehren sinnvoller w�re, was wiederum duplizierte Links
	 *      in den Routen hervorrufen w�rde :?
	 */
	protected void routing(Link fromLink)
	{	

		// Nodes der derzeit geplanten Route mit dem aktuellen Node vergleichen
		if(!leg.getRoute().getRoute().contains(fromLink.getToNode())) 
		{
			log.error("Node " + fromLink.getToNode().getId() + " nicht Teil der Route!!!");
			//System.exit(0);
		}
		
		// Daten fuer die neue Aktivitaet generieren
		String type = "w";
		
		Act newFromAct = new Act(type, fromLink.getToNode().getCoord(), fromLink);
		newFromAct.setStartTime(time);
		newFromAct.setEndTime(time);
		newFromAct.setDur(0);
		
		Route route = leg.getRoute();
		
		//ArrayList<Node> nodesRoute = route.getRoute();
		// Kopie der ArrayList holen und nicht direkt die ArrayList der Route bearbeiten!
		ArrayList<Node> nodesRoute = new ArrayList<Node>();
		nodesRoute.addAll(route.getRoute());

		ArrayList<Node> nodeBuffer = new ArrayList<Node>();
		Node newStartNode = fromLink.getToNode();
		
		// Alle Nodes aus der Route l�schen, die bereits "befahren" wurden
		// Die gel�schten gleichzeitig in einer ArrayList ablegen - diese werden
		// anschliessend mit der neue generierten Subroute wieder zusammengef�gt!
		
		// Achtung: derzeit k�nnen so keine "Schlaufen" gefahren werden, hierf�r
		// w�rde ein Counter oder dergleichen ben�tigt werden!
		while(nodesRoute.size() > 0)
		{
			Node node = nodesRoute.get(0);
			
			if(!node.equals(newStartNode))
			{
//				log.info(node.getId() + " does not match " + newStartNode.getId());
				nodeBuffer.add(node);
				nodesRoute.remove(0);
			}
			// also ersten Knoten der Route gefunden, der noch nicht befahren wurde 
			else
			{
				break;
			}
		}
			
		// neue, gek�rzte Route erstellen
		Route subRoute = new Route();
		subRoute.setRoute(nodesRoute);

		// die neue Route in neuem Leg hinterlegen
		Leg newLeg = new Leg(leg.getMode());
		newLeg.setNum(0);
		newLeg.setDepartureTime(leg.getDepartureTime());
		newLeg.setTravelTime(leg.getTravelTime());
		newLeg.setArrivalTime(leg.getArrivalTime());
		newLeg.setRoute(subRoute);
		newLeg.setRoute(subRoute);
			
		// aktuell gew�hlter Plan
		Plan currentPlan = person.getSelectedPlan();
		
		// neuen Plan generieren und selektieren
		Plan newPlan = new Plan(person);
		person.setSelectedPlan(newPlan);
			
		// Da sind wir gerade.
		newPlan.addAct(newFromAct);
		
		// Weg von der aktuellen Position bis zum Ziel
		newPlan.addLeg(newLeg);
		
		// da wollen wir immer noch hin :)
		newPlan.addAct(nextAct);
		
		// Route neu planen
//		log.info("Replanning Route for Person ... " + person.getId() + " who is at Node " + node.getId());

		/*
		 *  If it's a PersonPlansCalcRoute Object -> set the current Person.
		 *  The router may need their knowledge (activity room, ...).
		 */
		if (replanner instanceof KnowledgePlansCalcRoute)
		{
			((KnowledgePlansCalcRoute)replanner).setPerson(this.person);
			((KnowledgePlansCalcRoute)replanner).setTime(this.time);
		}
			
		replanner.run(newPlan);			
		
		// neu berechnete Route holen
		Route newRoute = newLeg.getRoute();
			
		// bereits gefahrenen Teil der Route mit der neu erstellten Route zusammenf�hren
		nodeBuffer.addAll(newRoute.getRoute());
		
		Route mergedRoute = new Route();
		mergedRoute.setRoute(nodeBuffer);
				
		// Route ersetzen
//		leg.setRoute(mergedRoute);
		leg.getRoute().getRoute().clear();
		leg.getRoute().getRoute().addAll(mergedRoute.getRoute());
				
		// bisher aktiven Plan wieder aktivieren
		person.setSelectedPlan(currentPlan);
	}
	
	// Holt den Link, auf dem der Agent zum aktuellen Node gefahren ist
	// Eventuell w�re ein einfacher Counter sinnvoller. Es sind Szenarien denkbar, in denen
	// ein Link mehrfach befahren wird :?
	// Replanning sollte mit der aktuellen L�sung funktionieren, allerdings wird der gefahrene
	// Weg allenfalls nicht voll �bernommen (Schlaufen werden rausgeschnitten).
	protected Link getFromLink()
	{		
		if (prevAct.getLink().getToNode().equals(node)) return prevAct.getLink();
		
		// It should never be the first node because that's the same as the "toNode" from the previous Activity
		for(int i = 1; i < leg.getRoute().getRoute().size(); i++)
		{
			if (leg.getRoute().getRoute().get(i).equals(node)) return leg.getRoute().getLinkRoute()[i-1];
		}

		// Should not be needed!
		if (nextAct.getLink().getFromNode().equals(node)) 
		{
			log.error("This me be an error - please check!");
			return nextAct.getLink();
		}

		
		return null;
	}
}
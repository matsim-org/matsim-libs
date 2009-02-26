/* *********************************************************************** *
 * project: org.matsim.*
 * BasicSimulation.java
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

/**
 * Basisimplementierung einer Verkehrssimulation.
 * Die Simulation ist zeitunabh�ngig, die Agenten haben keinen Einfluss auf das 
 * Verkehrsnetz und somit auch nicht aufeinander.
 * 
 * Die Datenstrukturen bzw. Klassen (Personen, Netzwerk, Pl�ne, usw.) werden von
 * MATSim �bernommen.
 */

package playground.christoph.basicmobsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.mobsim.queuesim.Simulation;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Population;

public class BasicSimulation {
	
	private final Config config;
	protected final Population plans;
	protected NetworkLayer networkLayer;

	protected static Events events = null; // TODO [MR] instead of making this static and Links/Nodes using QueueSimulation.getEvents(), Gbl should hold a global events-object

	/**
	 * Includes all vehicle that have transportation modes unknown to
	 * the QueueSimulation (i.e. != "car") or have two activities on the same link
 	 */

	final private static Logger log = Logger.getLogger(BasicSimulation.class);
	
	public BasicSimulation(final NetworkLayer network, final Population plans, final Events events) {
		Simulation.reset();
		this.config = Gbl.getConfig();
		
		setEvents(events);
		this.plans = plans;

		this.networkLayer = network;
	}

	
	protected void simulatePerson(Person person)
	{
		log.info("Simulating Person " + person.getId());
		
		Plan plan = person.getSelectedPlan();
	
		// Zufallsgenerator eindeutig mit der Id der Person initialisieren -> reproduzierbare Ergebnisse!
		Random random = new Random( Long.valueOf(person.getId().toString()) );
		
		Iterator actIterator = plan.getIteratorAct();
		Iterator legIterator = plan.getIteratorLeg();
		
		//while(actIterator.hasNext())
		while(legIterator.hasNext())
		{
			ArrayList<Node> routeNodes = new ArrayList<Node>();
			
			Act act = (Act)actIterator.next();
			
			Leg leg = (Leg)legIterator.next();
			
			if (leg == null)
			{
				log.error("Leg is null!");
				System.exit(0);
			}
			
			Act nextAct = plan.getNextActivity(leg);
			
			// Ziellink holen
			Link destinationLink = nextAct.getLink();
			
			Link currentLink = act.getLink();
			Node currentNode = currentLink.getToNode();
			
			// solange wir das Ziel nicht erreicht habenMap		
			while(!currentLink.equals(destinationLink))
			{				
				// gewaehlten Link speichern, um ihn spaeter im Plan hinterlegen zu koennen
				routeNodes.add(currentLink.getToNode());

				log.info("Current Link: " + currentLink.getId() + " Destination Link: " + destinationLink.getId());
				
				Object[] links = currentNode.getOutLinks().values().toArray();
				int linkCount = links.length;
							
				if (linkCount == 0)
				{
					log.error("Node has no outgoing links - Stopped search!");
					break;
				}
				
				/* Hier nicht noetig - alle Links sind gleich wahscheinlich. Die Verteilung
				 * erfolgt ueber die Random Funktion - diese sollte gleichverteile Zufallswerte
				 * generieren.
				 * 
				// Wahrscheinlichkeit fuer die ausgehenden Links berechnen
				double probabilty = 1/linkCount;
				*/
				
				// Link waehlen
				int nextLink = random.nextInt(linkCount);
				
				// den gewaehlten Link zum neuen CurrentLink machen
				if(links[nextLink] instanceof Link)
				{
					currentLink = (Link)links[nextLink];
					currentNode = currentLink.getToNode();
				}
				else
				{
					log.error("Return object was not from type Link! Class " + links[nextLink] + " was returned!");
					break;
				}
				
				
			}	// while(!currentLink.equals(destinationLink))
			
			// gefahrene Route im aktuellen Leg des Agenten hinterlegen
			((CarRoute) leg.getRoute()).setNodes(routeNodes);
			//Route newRoute = new Route();
			//newRoute.setRoute(routeNodes);
			//leg.setRoute(newRoute);
						
		}	// while(legIterator.hasNext())
		
	}
	
	
	
	public final void run() 
	{
		Collection<Person> persons = plans.getPersons().values();
		Iterator personIterator = persons.iterator();
		
		while(personIterator.hasNext())
		{
			this.simulatePerson((Person)personIterator.next());
		}

		cleanupSim();
	}


	/**
	 * Close any files, etc.
	 */
	protected void cleanupSim() 
	{
		BasicSimulation.events = null; // delete events object to free events handlers, if they are nowhere else referenced
	}

	public static final Events getEvents() {
		return events;
	}

	private static final void setEvents(final Events events) {
		BasicSimulation.events = events;
	}


}
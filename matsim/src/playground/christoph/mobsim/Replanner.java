/* *********************************************************************** *
 * project: org.matsim.*
 * KnowledgeReplaner.java
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

package playground.christoph.mobsim;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.controler.Controler;
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



public class Replanner{

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
	
	protected Controler controler;
	
	private static final Logger log = Logger.getLogger(Replanner.class);
	
	
	public Replanner(QueueNode queueNode, Vehicle vehicle, double time)
	{
		this.queueNode = queueNode;
		this.node = queueNode.getNode();
		this.time = time;
		this.vehicle = vehicle;
		this.personAgent = vehicle.getDriver();
		this.person = vehicle.getDriver().getPerson();

		if (queueNode instanceof MyQueueNode) this.controler = ((MyQueueNode)queueNode).getControler();
		else log.error("No Controler delivered :(");		
		
		//new CharyparEtAlCompatibleLegTravelTimeEstimator();
		//CetinCompatibleLegTravelTimeEstimator estimator = 
		//	new CetinCompatibleLegTravelTimeEstimator(travelTime, null, controler.getNetwork());
		
		Plan plan = person.getSelectedPlan();

		leg = personAgent.getCurrentLeg();

		prevAct = (Act)plan.getPreviousActivity(leg);
		nextAct = (Act)plan.getNextActivity(leg);	

		// Link suchen, von dem der Agent zum aktuellen Node gekommen ist
		Link fromLink = getFromLink();
		
		// neue Route berechnen...
		if(nextAct != null)
		{
			if(fromLink != null)
			{
				// neue Route berechnen...
				Routing(fromLink);
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
		
	}	// Replanner(...)
	
	
	
	/**
	 * Route am Ende jedes Links neu berechnen.
	 * Ansatz:
	 * - Neue Aktivität beim aktuellen Link generieren und Zielaktivität beibehalten.
	 * - Route generieren 
	 * - Alte Route mit neuer Route mergen
	 *   -> Annahme: ein Link ist in jeder Route nur 1x vorhanden (was auch Sinn macht, da
	 *      "Rundfahrten" wohl kaum die kürzesten Wege von A nach B darstellen.
	 *   -> Könnte doch noch Probleme hervorrufen. Agenten könnten an Stellen gelangen, wo
	 *      z.B. aufgrund von Staus umkehren sinnvoller wäre, was wiederum duplizierte Links
	 *      in den Routen hervorrufen würde :?
	 */
	protected void Routing(Link fromLink)
	{	

		// Nodes der derzeit geplanten Route mit dem aktuellen Node vergleichen
		if(!leg.getRoute().getRoute().contains(fromLink.getToNode())) 
		{
			log.error("Node " + fromLink.getToNode().getId() + " nicht Teil der Route!!!");
			//System.exit(0);
		}
		
		// Daten für die neue Aktivität generieren
		String type = "w";
		
		Node toNode = fromLink.getToNode();
		double x = toNode.getCoord().getX();
		double y= toNode.getCoord().getY();
		
		String link = fromLink.getId().toString();
		String startTime = String.valueOf(time);
		String endTime = String.valueOf(time);
		String dur = String.valueOf(0.0);
		String isPrimary = "false";
		
		Act newFromAct = new Act(type, x, y, link, startTime, endTime, dur, isPrimary);
		
		Route route = leg.getRoute();
		
		//ArrayList<Node> nodesRoute = route.getRoute();
		// Kopie der ArrayListe holen und nicht direkt die ArrayList der Route bearbeiten!
		ArrayList<Node> nodesRoute = new ArrayList<Node>();
		nodesRoute.addAll(route.getRoute());

		ArrayList<Node> nodeBuffer = new ArrayList<Node>();
		Node newStartNode = fromLink.getToNode();
		
		// Alle Nodes aus der Route löschen, die bereits "befahren" wurden
		// Die gelöschten gleichzeitig in einer ArrayList ablegen - diese werden
		// anschliessend mit der neue generierten Subroute wieder zusammengefügt!
		
		// Achtung: derzeit können so keine "Schlaufen" gefahren werden, hierfür
		// würde ein Counter oder dergleichen benötigt werden!
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
			
		// neue, gekürzte Route erstellen
		Route subRoute = new Route();
		subRoute.setRoute(nodesRoute);

		// die neue Route in neuem Leg hinterlegen
		Leg newLeg = new Leg(0, leg.getMode(), leg.getDepTime(), leg.getTravTime(), leg.getArrTime());
		newLeg.setRoute(subRoute);
				
		// aktuell gewählten Plan 
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
		log.info("Replanning Route for Person ... " + person.getId() + " who is at Node " + node.getId());
		PlanAlgorithm planAlgorithm = controler.getRoutingAlgorithm();
		planAlgorithm.run(newPlan);
		
		// neu berechnete Route holen
		Route newRoute = newLeg.getRoute();
		
//		log.info("new - already driven");
//		for(int i = 0; i < nodeBuffer.size(); i++) log.info(nodeBuffer.get(i));
		
		
		// bereits gefahrenen Teil der Route mit der neu erstellten Route zusammenführen
		nodeBuffer.addAll(newRoute.getRoute());
		
//		log.info("new - to be driven");
//		for(int i = 0; i < newRoute.getRoute().size(); i++) log.info(newRoute.getRoute().get(i));
		
//		log.info("merged");
//		for(int i = 0; i < nodeBuffer.size(); i++) log.info(nodeBuffer.get(i));
	
//		log.info("old");
//		for(int i = 0; i < leg.getRoute().getRoute().size(); i++) log.info(leg.getRoute().getRoute().get(i));
			
		Route mergedRoute = new Route();
		mergedRoute.setRoute(nodeBuffer);
				
//		log.info("Old Route: " + leg.getRoute());
//		log.info("New Route: " + mergedRoute);	
			
		// Route ersetzen
//		leg.setRoute(mergedRoute);
		leg.getRoute().getRoute().clear();
		leg.getRoute().getRoute().addAll(mergedRoute.getRoute());
				
		// bisher aktiven Plan wieder aktivieren
		person.setSelectedPlan(currentPlan);
	}
	

	// Holt den Link, auf dem der Agent zum aktuellen Node gefahren ist
	protected Link getFromLink()
	{		
		if (prevAct.getLink().getToNode().equals(node)) return prevAct.getLink();
		
		if (nextAct.getLink().getToNode().equals(node)) return nextAct.getLink();
		
		// It should never be the first node because that's the same as the "toNode" from the previous Activity
		for(int i = 0; i < leg.getRoute().getRoute().size(); i++)
		{
			if (leg.getRoute().getRoute().get(i).equals(node)) return leg.getRoute().getLinkRoute()[i-1];
		}
		
		return null;
	}
}
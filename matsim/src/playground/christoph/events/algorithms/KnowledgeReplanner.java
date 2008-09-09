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

package playground.christoph.events.algorithms;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.controler.Controler;
import org.matsim.events.ActEndEvent;
import org.matsim.events.AgentReplanEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.ActEndEventHandler;
import org.matsim.events.handler.AgentReplanEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.network.Link;
import org.matsim.network.Node;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Route;
import org.matsim.population.algorithms.PlanAlgorithm;


public class KnowledgeReplanner implements AgentReplanEventHandler, LinkLeaveEventHandler, ActEndEventHandler{

	protected Controler controler;
	
	protected double time;
	protected Person person;
	protected Act nextAct;
	
	private static final Logger log = Logger.getLogger(KnowledgeReplanner.class);
	
	@Override
	public void handleEvent(ActEndEvent event) {
		// TODO Auto-generated method stub
		log.info("KnowledgeReplaner ActEndEvent.................................");
		Replanning(event);
	}
	
	@Override
	public void handleEvent(AgentReplanEvent event) {
		// TODO Auto-generated method stub
//		System.out.println("KnowledgeReplaner AgentReplanEvent.................................");
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		// TODO Auto-generated method stub
//		System.out.println("KnowledgeReplaner LinkLeaveEvent.................................");
//		Replanning(event);
	}
	
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	public KnowledgeReplanner()
	{
		
	}
	
	// Wofür den Controler?!
	public KnowledgeReplanner(Controler controler)
	{
		this.controler = controler;
	}

	protected void Replanning(ActEndEvent event)
	{
		time = event.time;
		person = event.agent;
		Act act = event.act;
		Link link = event.link;
		
		Plan plan = person.getSelectedPlan();

		Leg nextLeg = plan.getNextLeg(act);

		if(nextLeg != null) nextAct = (Act)plan.getNextActivity(nextLeg);
		else nextAct = null;

//		System.out.println("Agent " + person.getId() + " ends Act at " + Time.writeTime(time));
		
		// neue Route berechnen...
		if(nextAct != null)
		{
			Routing(act, nextLeg);
		}
		else
		{ 
			log.error("An agents next activity is null - this should not happen!");
		}
		
	}	// Replanning(ActEndEvent event)
	

	protected void Replanning(AgentReplanEvent event)
	{
		time = event.time;
		person = event.agent;
		Route replannedRoute = event.replannedRoute;

		Plan plan = person.getSelectedPlan();
	}
	
	protected void Replanning(LinkLeaveEvent event)
	{
		time = event.time;
		person = event.agent;
		Leg leg = event.leg;
		Link link = event.link;
		
		Plan plan = person.getSelectedPlan();
		
		nextAct = (Act)plan.getNextActivity(leg);
		if(nextAct != null)
		{
			// neue Route berechnen...
//			System.out.println("Agent " + person.getId() + " is replanning to get to " + nextAct.getType());
//			Routing(link, leg);
		}
		else
		{
			log.error("An agents next activity is null - this should not happen!");
		}
	}
	
	/**
	 * Ansatz:
	 * Die Router von MATSim generieren jeweils Routen für ganze Pläne. Da wird nichts
	 * an den Routern ändern wollen und auch keine eigenen schreiben wollen, generieren
	 * wir stattdessen einen neuen Plan, welcher nur die Bewegung von der Ist-Position
	 * bis hin zur nächsten Aktivität enthält. Dieser Plan wird dann an den Router
	 * geschickt und das Ergebnis anschliessend beim aktiven Agenten hinterlegt. 
	 * 
	 * Die Routen werden neu geplant, wenn der Agent beschliesst, loszufahren - nicht wenn
	 * er tatsächlich auf den ersten Link einbiegt.
	 * Die Konsequenz daraus lässt sich leicht
	 * am Equil Tutorial mit 100 Personen zeigen. Alle 100 Personen wollen sich um 06:00 auf
	 * den Weg zur Arbeit machen und planen auch zu dieser Uhrzeit ihre Route. Nun ist die zu
	 * diesem Zeitpunkt allerdings noch leer, weshalb alle Agenten dieselbe Route wählen.
	 * Würde das Routing erst beim Effektiven einbiegen auf die Strasse passieren, würden
	 * die Agenten unterschiedliche Routen wählen! 
	 * 
	 * @param plan
	 */
	protected void Routing(Act fromAct, Leg nextLeg)
	{		
		// die geplante Endzeit mit der tatsächlichen Endzeit ersetzen
		fromAct.setEndTime(time);
		
		// aktuell gewählten Plan sichern
		Plan currentPlan = person.getSelectedPlan();
		
		// Neuen Plan generieren und selektieren.
		// Dieser enthält nur den Weg von der letzten bis zur nächten Aktivität.
		// -> Mehr wollen wir schliesslich noch gar nicht neu planen!
		Plan newPlan = new Plan(person);
		person.setSelectedPlan(newPlan);
		
		// Da sind wir gerade.
		newPlan.addAct(fromAct);
		
		// bisheriger Weg zum Ziel, der aber umgeplant werden soll
		newPlan.addLeg(nextLeg);
		
		// da wollen wir immer noch hin :)
		newPlan.addAct(nextAct);
		
		// Route neu planen
		log.info("Replanning Route...");
		PlanAlgorithm planAlgorithm = controler.getRoutingAlgorithm();
		planAlgorithm.run(newPlan);
		
/* Gar nicht nötig - die Änderungen werden direkt am nextLeg durchgeführt und
 * sind somit auch im eigentlich Plan direkt vorhanden! 	
		// neu berechnete Route holen
		Leg leg = newPlan.getNextLeg(newPlan.getFirstActivity());
		Route newRoute = leg.getRoute();
		
		// Route ersetzen
		nextLeg.setRoute(newRoute);
*/
		
		// bisher aktiven Plan wieder aktivieren
		person.setSelectedPlan(currentPlan);
	}
	
	
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
	 *      
	 * Klappt derzeit so nicht - der Event wird erst ausgelöst, NACHDEM der Agent von der
	 * Simulation bereits auf den nächsten Link geschoben wurde -> Replanning via
	 * "Replanner.java", welcher direkt vor dem Verschieben der Fahrzeuge neu plant.
	 */
	protected void Routing(Link fromLink, Leg leg)
	{	
		System.out.println();
		System.out.println();
		log.info("old - unchanged");
		for(int i = 0; i < leg.getRoute().getRoute().size(); i++) log.info(leg.getRoute().getRoute().get(i));
		

		ArrayList<Node> myNodes = leg.getRoute().getRoute();
	
		if(!myNodes.contains(fromLink.getToNode())) 
		{
			log.error("Node " + fromLink.getToNode().getId() + " nicht Teil der Route!!!");
			System.exit(0);
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
		
		if (!nodesRoute.contains(newStartNode)) log.error("Neuer Startnode " + newStartNode.getId() + " nicht in der Route enthalten?!");
		
		// Alle Nodes aus der Route löschen, die bereits "befahren" wurden
		// Die gelöschten gleichzeitig in einer ArrayList ablegen - diese werden
		// anschliessend mit der neue generierten Subroute wieder zusammengefügt!
		while(nodesRoute.size() > 0)
		{
			Node node = nodesRoute.get(0);
			
			if(!node.equals(newStartNode))
			{
				log.info(node.getId() + " does not match " + newStartNode.getId());
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

		// die neue Route hinterlegen in neuem Leg hinterlege
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
		log.info("Replanning Route...");
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
		
		log.info("merged");
		for(int i = 0; i < nodeBuffer.size(); i++) log.info(nodeBuffer.get(i));

		
		log.info("old");
		for(int i = 0; i < leg.getRoute().getRoute().size(); i++) log.info(leg.getRoute().getRoute().get(i));
			
		Route mergedRoute = new Route();
		mergedRoute.setRoute(nodeBuffer);
		
//		if(leg.getRoute().getRoute().size() > 4) System.exit(0);
//		if(nodeBuffer.size() > 4) System.exit(0);
		
//		log.info("Old Route: " + leg.getRoute());
//		log.info("New Route: " + mergedRoute);	
			
		// Route ersetzen
		leg.setRoute(mergedRoute);
//		leg.getRoute().getRoute().clear();
//		leg.getRoute().getRoute().addAll(mergedRoute.getRoute());
		
		// PersonAgent -> nextLink muss noch korrigiert werden!
		
//		log.info("old - now overwritten");
//		for(int i = 0; i < leg.getRoute().getRoute().size(); i++) log.info(leg.getRoute().getRoute().get(i));
		
		// bisher aktiven Plan wieder aktivieren
		person.setSelectedPlan(currentPlan);

	}
	
}
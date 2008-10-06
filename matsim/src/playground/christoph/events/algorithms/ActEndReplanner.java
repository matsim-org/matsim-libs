/* *********************************************************************** *
 * project: org.matsim.*
 * ActEndReplanner.java
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

import org.apache.log4j.Logger;
import org.matsim.events.ActEndEvent;
import org.matsim.events.AgentReplanEvent;
import org.matsim.events.handler.ActEndEventHandler;
import org.matsim.network.Link;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Route;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.christoph.router.KnowledgePlansCalcRoute;

/*
 * This is a EventHandler that replans the Route of a Person every time an
 * Activity has ended.
 * 
 * MATSim Routers use Plan as Input Data. To be able to use them, we have to create
 * a new Plan from the current Position to the location of the next Activity.
 * 
 * If a Person had just ended one Activity (ActEndEvent), a new Plan is created which 
 * contains this and the next Activity and the Leg between them.
 */

public class ActEndReplanner implements ActEndEventHandler {

//	protected Controler controler;
	protected PlanAlgorithm replanner;
	
	protected double time;
	protected Person person;
	protected Act nextAct;
	
	private static final Logger log = Logger.getLogger(ActEndReplanner.class);

	public void handleEvent(ActEndEvent event) {
		// TODO Auto-generated method stub

		// If replanning flag is set in the Person
		boolean replanning = (Boolean)event.agent.getCustomAttributes().get("endActivityReplanning");
		if(replanning) 
		{
			log.info("ActEndReplanner....................." + event.agentId);
			Replanning(event);
		}
	}

	
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
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
	

	
	/*
	 * Ansatz:
	 * Die Router von MATSim generieren jeweils Routen f�r ganze Pl�ne. Da wird nichts
	 * an den Routern �ndern wollen und auch keine eigenen schreiben wollen, generieren
	 * wir stattdessen einen neuen Plan, welcher nur die Bewegung von der Ist-Position
	 * bis hin zur n�chsten Aktivit�t enth�lt. Dieser Plan wird dann an den Router
	 * geschickt und das Ergebnis anschliessend beim aktiven Agenten hinterlegt. 
	 * 
	 * Die Routen werden neu geplant, wenn der Agent beschliesst, loszufahren - nicht wenn
	 * er tats�chlich auf den ersten Link einbiegt.
	 * Die Konsequenz daraus l�sst sich leicht
	 * am Equil Tutorial mit 100 Personen zeigen. Alle 100 Personen wollen sich um 06:00 auf
	 * den Weg zur Arbeit machen und planen auch zu dieser Uhrzeit ihre Route. Nun ist die zu
	 * diesem Zeitpunkt allerdings noch leer, weshalb alle Agenten dieselbe Route w�hlen.
	 * W�rde das Routing erst beim Effektiven einbiegen auf die Strasse passieren, w�rden
	 * die Agenten unterschiedliche Routen w�hlen! 
	 * 
	 * @param plan
	 */
	protected void Routing(Act fromAct, Leg nextLeg)
	{	
		// die geplante Endzeit mit der tats�chlichen Endzeit ersetzen
		fromAct.setEndTime(time);
		
		// aktuell gew�hlten Plan sichern
		Plan currentPlan = person.getSelectedPlan();
		
		// Neuen Plan generieren und selektieren.
		// Dieser enth�lt nur den Weg von der letzten bis zur n�chten Aktivit�t.
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
//		log.info("Replanning Route...");
		
		replanner = (PlanAlgorithm)person.getCustomAttributes().get("Replanner");
		if (replanner == null) log.error("No Replanner found in Person!");
		/*
		 *  If it's a PersonPlansCalcRoute Object -> set the current Person.
		 *  The router may need their knowledge (activity room, ...).
		 */
		if (replanner instanceof KnowledgePlansCalcRoute)
		{
			((KnowledgePlansCalcRoute)replanner).setPerson(this.person);
		}
		replanner.run(newPlan);
		
//		if (replanner == null) log.error("No Replanner found in Person!");
//		PlanAlgorithm planAlgorithm = ((EventControler)controler).getReplanningRouter();
//		planAlgorithm.run(newPlan);
		
		// bisher aktiven Plan wieder aktivieren
		person.setSelectedPlan(currentPlan);
	}
	
}
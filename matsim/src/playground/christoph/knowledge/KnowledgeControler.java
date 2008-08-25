/* *********************************************************************** *
 * project: org.matsim.*
 * KnowledgeControler.java
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

// Enthält hardcodierte LinkIDs - diese beziehen sich auf das Equil Szenario
// aus den Tutorials - also bitte damit laufen lassen :)

package playground.christoph.knowledge;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.controler.Controler;
import org.matsim.network.Link;
import org.matsim.population.Person;
import org.matsim.replanning.StrategyManager;

public class KnowledgeControler extends Controler {

	public KnowledgeControler(String args[])
	{
		super(args);
	}
	
	public static void main(String args[])
	{
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} 
		else {
			final KnowledgeControler knowledgeControler = new KnowledgeControler(args);
			knowledgeControler.run();
		}
		System.exit(0);
	}

	// KnowledgeStrategyManagerConfigLoader
	
	/**
	 * @return A fully initialized StrategyManager for the plans replanning.
	 */
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new StrategyManager();
		KnowledgeStrategyManagerConfigLoader.load(this, this.config, manager);
		return manager;
	}
	
		
	// Workaround!
	// Wo wird der TravelCostCalculator festgelegt? Gibt's dafür ein Feld in der Konfigurationsdatei?
	protected void setup() {
		
		// Diese beiden Befehle müssen sind unabhängig vom initialisieren der CostCalculators.
		// Auch wenn diese via Configfile geladen werden, werden die Zeilen benötigt!
		initKnowledge();	// neu...
		setKnowledge();		// neu...
		
		
		double endTime = this.config.simulation().getEndTime() > 0 ? this.config.simulation().getEndTime() : 30*3600;

		// TravelTimeCalculator initialisieren
		this.travelTimeCalculator = this.config.controler().getTravelTimeCalculator(this.network, (int)endTime);
			
		// Eigenen TravenCostCalculator verwenden...
		this.travelCostCalculator = new KnowledgeTravelCost(this.travelTimeCalculator);
		
		// ... dieser wird von nun folgenden Setup nicht mehr überschrieben.
		super.setup();
	}
	
	// Hier werden die neuen Knowledge-Attribute erzeugt.
	protected void initKnowledge()
	{
		Iterator<Person> PersonIterator = this.getPopulation().iterator();
		while (PersonIterator.hasNext())
		{
			Person p = PersonIterator.next();
			if (p.getKnowledge() == null) 
			{
				p.createKnowledge("Knowledgemodels");
			}
			
			ArrayList<Id> linkIds = new ArrayList<Id>();
			ArrayList<Double> costs = new ArrayList<Double>();
				
			
			//Map<String,Object> newAttrib = this.getPopulation().getPerson("1").getCustomAttributes();
			Map<String,Object> newAttrib = p.getKnowledge().getCustomAttributes();
			newAttrib.put("LinkIDs", linkIds);
			newAttrib.put("Costs", costs);	
		}

	}
	
	// Hier werden die neuen Knowledge-Attribute befüllt.
	// Aktuell händisch erstellt, später aus Configfiles geladen.
	protected void setKnowledge()
	{
		Iterator<Person> PersonIterator = this.getPopulation().iterator();
		while (PersonIterator.hasNext())
		{
			Person p = PersonIterator.next();
			
			// ID der aktuell behandelten Person holen
			//Id ID = p.getId();
			
			// kein Check - wir wissen ja, was drinnen steckt...
			ArrayList<Id> linkIds = (ArrayList<Id>)p.getKnowledge().getCustomAttributes().get("LinkIDs");
			ArrayList<Double> costs = (ArrayList<Double>)p.getKnowledge().getCustomAttributes().get("Costs");
			
			linkIds.add(new IdImpl("6"));
			costs.add(Double.MAX_VALUE);
			
			linkIds.add(new IdImpl("15"));
			costs.add(Double.MAX_VALUE);
			
			linkIds.add(new IdImpl("10"));
			costs.add(0.0);
			
			linkIds.add(new IdImpl("19"));
			costs.add(0.0);
			
		}
	}
}
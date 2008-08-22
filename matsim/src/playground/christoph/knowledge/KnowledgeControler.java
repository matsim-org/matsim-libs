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

package playground.christoph.knowledge;

import org.matsim.controler.Controler;
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
		double endTime = this.config.simulation().getEndTime() > 0 ? this.config.simulation().getEndTime() : 30*3600;

		// TravelTimeCalculator initialisieren
		this.travelTimeCalculator = this.config.controler().getTravelTimeCalculator(this.network, (int)endTime);
			
		// Eigenen TravenCostCalculator verwenden...
		this.travelCostCalculator = new KnowledgeTravelCost(this.travelTimeCalculator);
		
		// ... dieser wird von nun folgenden Setup nicht mehr überschrieben.
		super.setup();
	}
}
/* *********************************************************************** *
 * project: org.matsim.*
 * MoneyEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
 * 
 */
package playground.ikaddoura.busCorridor.version3_moneyEvents_scoring;

import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.handler.AgentMoneyEventHandler;
import org.matsim.core.controler.Controler;

/**
 * @author Ihab
 *
 */
public class MoneyEventHandler implements AgentMoneyEventHandler {
	private Controler controler;
	
	/**
	 * @param controler
	 */
	public MoneyEventHandler(Controler controler) {
		this.controler = controler;
	}

	@Override
	public void reset(int iteration) {

	}

	@Override
	public void handleEvent(AgentMoneyEvent event) {
		System.out.println("AgentMoneyEvent at Time "+event.getTime()+" wird gehandelt.");
		
//		Id personId = event.getPersonId();
//		double amount = event.getAmount();
//		
// 		Das läuft automatisch irgendwo im Hintergrund:
//		this.controler.getPlansScoring().getPlanScorer().getScoringFunctionForAgent(personId).addMoney(amount);
		
	}
}

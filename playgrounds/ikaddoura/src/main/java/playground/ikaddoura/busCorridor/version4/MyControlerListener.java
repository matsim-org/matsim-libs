/* *********************************************************************** *
 * project: org.matsim.*
 * MyControlerListener.java
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

package playground.ikaddoura.busCorridor.version4;

import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;

/**
 * @author Ihab
 *
 */

public class MyControlerListener implements StartupListener, IterationStartsListener, BeforeMobsimListener, AfterMobsimListener, ScoringListener, IterationEndsListener, ShutdownListener {

	private MoneyThrowEventHandler moneyThrowEventHandler;

	@Override
	public void notifyStartup(StartupEvent event) {
		System.out.println("Startup-Event");
		
		// Wenn eine Person in ein Vehicle steigt, wirf MoneyEvent
		this.moneyThrowEventHandler = new MoneyThrowEventHandler(event.getControler().getEvents(), event.getControler().getPopulation());
		event.getControler().getEvents().addHandler(this.moneyThrowEventHandler);
	
	}

	
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyScoring(ScoringEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		// TODO Auto-generated method stub
		
	}

}

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

package playground.ikaddoura.busCorridor.version7test;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;

/**
 * @author Ihab
 *
 */

public class MyControlerListener implements StartupListener, IterationStartsListener, BeforeMobsimListener, AfterMobsimListener, ScoringListener, IterationEndsListener, ShutdownListener {

	private MoneyThrowEventHandler moneyThrowEventHandler;
	private WaitingTimeEventHandler waitingTimeEventHandler;
	private Map<Id, Double> personId2WaitingTime = new HashMap<Id,Double>();
	private Map<Id, Double> personId2InVehicleTime = new HashMap<Id,Double>();

	private double fare;

	public MyControlerListener(double fare){
		this.fare = fare;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		this.moneyThrowEventHandler = new MoneyThrowEventHandler(event.getControler().getEvents(), event.getControler().getPopulation(), this.fare);
		event.getControler().getEvents().addHandler(this.moneyThrowEventHandler);
		
		this.waitingTimeEventHandler = new WaitingTimeEventHandler();
		event.getControler().getEvents().addHandler(this.waitingTimeEventHandler);
	
	}

	
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
				
	}

	@Override
	public void notifyScoring(ScoringEvent event) {		
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		this.personId2WaitingTime = waitingTimeEventHandler.getPersonId2WaitingTime();
		this.personId2InVehicleTime = waitingTimeEventHandler.getPersonId2InVehicleTime();
		System.out.println("Im ControlerListener:"+this.personId2WaitingTime);
		
		MyScoringFunctionFactory scoringfactory = (MyScoringFunctionFactory) event.getControler().getScoringFunctionFactory();
		
		scoringfactory.setPersonId2WaitingTime(personId2WaitingTime);
		scoringfactory.setPersonId2InVehicleTime(personId2InVehicleTime);
		event.getControler().setScoringFunctionFactory(scoringfactory);
//		event.getControler().getScoringFunctionFactory().createNewScoringFunction();
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

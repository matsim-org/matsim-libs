/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.juliakern.distribution.withScoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.scoring.ScoringFunction;

import playground.juliakern.distribution.ResponsibilityEvent;

public class ResponsibilityScoringFunction implements ScoringFunction {
	
	ScoringFunction delegate;
	private Plan plan;
	//double rScore = 0.0;
	EmissionControlerListener ecl;
	// total time in all bins / number of bins
//	private Double averageDurationOfTimeBin = 36*60*60/16/12.;
	private Double averageDurationOfTimeBin = 24*60*60/32/20.;
	
	public ResponsibilityScoringFunction(Plan plan, ScoringFunction scoringFunction, EmissionControlerListener ecl){
		this.plan=plan;
		this.delegate = scoringFunction;
		this.ecl=ecl;
	}

	@Override
	public void handleActivity(Activity activity) {
		delegate.handleActivity(activity);
	}

	@Override
	public void handleLeg(Leg leg) {
		delegate.handleLeg(leg);
	}

	@Override
	public void agentStuck(double time) {
		delegate.agentStuck(time);
	}

	@Override
	public void addMoney(double amount) {
		delegate.addMoney(amount);

	}

	@Override
	public void finish() {
		
		Id personId = plan.getPerson().getId();

		if(!plan.isSelected(plan))System.out.println("++++++++++++++++shouldnt happen");
		Double amount = new Double(.0);
		if(ecl!=null){
			if(ecl.getResp()!=null){
				for(ResponsibilityEvent re: ecl.getResp()){
		
					if(re.getResponsiblePersonId().equals(personId)){
						
						// concentration * duration / average duration
						amount += re.getExposureValue()/averageDurationOfTimeBin;
					}
				}
			}
		}
		System.out.println("score without emissions" +delegate.getScore());
		System.out.println("money amount" + amount);
		delegate.addMoney(-amount);System.out.println("score with emission costs" + delegate.getScore());
		delegate.finish();
		
		

	}

	@Override
	public double getScore() {
//		Id personId = plan.getPerson().getId();
//
//		System.out.println("score before " + plan.getScore());
//		if(!plan.isSelected())System.out.println("++++++++++++++++shouldnt happen");
//		Double amount = new Double(.0);
//		if(ecl!=null){
//			if(ecl.getResp()!=null){
//				for(ResponsibilityEvent re: ecl.getResp()){
//		
//					if(re.getResponsiblePersonId().equals(personId)){
//						
//		
//						amount += re.getExposureValue();
//					}
//				}
//			}
//		}
	
//		Id personId = plan.getPerson().getId();
//
//		Double amount = new Double(.0);
//		if(ecl!=null){
//			if(ecl.getResp()!=null){
//				for(ResponsibilityEvent re: ecl.getResp()){
//		
//					if(re.getResponsiblePersonId().equals(personId)){
//		
//						amount += re.getExposureValue();
//					}
//				}
//			}
//		}
//		
//		if(amount>0.0) delegate.addMoney(-amount); //TODO ueberpruefen, hier muesste immer >=0 sein
//		System.out.println("Person id: " + personId.toString() + " exposure amount " + amount + " resulting score " + delegate.getScore());
		return delegate.getScore();
//		System.out.println("delegatescore " + delegate.getScore() + " amount " + amount);
//		return delegate.getScore() - amount;
	}

	@Override
	public void handleEvent(Event event) {
		delegate.handleEvent(event);

	}

}

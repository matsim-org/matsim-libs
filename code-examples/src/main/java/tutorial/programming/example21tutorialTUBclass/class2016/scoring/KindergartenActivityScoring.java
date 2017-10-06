/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package tutorial.programming.example21tutorialTUBclass.class2016.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.SumScoringFunction.ActivityScoring;

public class KindergartenActivityScoring implements ActivityScoring {

	private double score;
	
	KindergartenArrivalHandler handler;
	Id<Person> personId;
	public KindergartenActivityScoring(Id<Person> personId, KindergartenArrivalHandler handler) {
		this.handler = handler;
		this.personId = personId;
	}
	
	
	@Override
	public void handleActivity(Activity act) {
		if (act.getType().equals("pt interaction")) return;
		if (act.getType().startsWith("kindergarten")){
			if (act.getLinkId().equals(handler.kindergartenLink)){
				if (handler.arrivedOnLinkByCar.contains(personId)){
					this.score -= 3000.;
					handler.arrivedOnLinkByCar.remove(personId);
					System.out.println(personId + " arrived by car");

				}
			}
		}
	}



	/* (non-Javadoc)
	 * @see org.matsim.core.scoring.SumScoringFunction.BasicScoring#getScore()
	 */
	@Override
	public double getScore() {
		// TODO Auto-generated method stub
		return this.score;
	}

	
	/* (non-Javadoc)
	 * @see org.matsim.core.scoring.SumScoringFunction.BasicScoring#finish()
	 */
	@Override
	public void finish() {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.scoring.SumScoringFunction.ActivityScoring#handleFirstActivity(org.matsim.api.core.v01.population.Activity)
	 */
	@Override
	public void handleFirstActivity(Activity act) {
		// TODO Auto-generated method stub
		
	}


	/* (non-Javadoc)
	 * @see org.matsim.core.scoring.SumScoringFunction.ActivityScoring#handleLastActivity(org.matsim.api.core.v01.population.Activity)
	 */
	@Override
	public void handleLastActivity(Activity act) {
		// TODO Auto-generated method stub
		
	}
	
	


}

/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.santiago.population.trying;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;

/**
 * @author amit
 */
public class ActivityClassifierTry {
	private static final Logger log = Logger.getLogger(ActivityClassifierTry.class);

	private Scenario sc ;
	private int zeroDurCount = 0;
	private SortedMap<String, Double> actType2TypDur;
	private Scenario scOut;
	private int skippedPersons = 0;

	public ActivityClassifierTry(Scenario scenario) {
		this.sc = scenario;
		this.actType2TypDur = new TreeMap<String, Double>();
		log.info("Typical durations calculated by this class are rounded down to full hours.");
	}

	public void run(){
		scOut = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population popOut = scOut.getPopulation();
		PopulationFactory popFactory = popOut.getFactory();

		for(Person p : sc.getPopulation().getPersons().values()){
			boolean skipPerson = false;
			Person pOut = popFactory.createPerson(p.getId());
			Plan planOut = popFactory.createPlan();
			pOut.addPlan(planOut);

			List<PlanElement> pes = p.getSelectedPlan().getPlanElements();
			// necessary for zero duration activities
			// TODO: Such activity should rather be a pickup/drop-off activity with zero (or very short) duration
			double timeShift = 0;

			// take out first and last activities, put them together if they are same.
			int planElementsSize = pes.size();
			boolean isFirstAndLastActSame = false;
			double overnightTypDur = Double.NEGATIVE_INFINITY;

			Activity firstAct = (Activity) pes.get(0);
			// Example: H(0) -- Car(1) -- W(2) -- Car(3) -- H(4) has a size of 5. We want H(4), i.e. 5-1.
			Activity lastAct = (Activity) pes.get(planElementsSize - 1);
			if(firstAct == null || lastAct == null)	throw new RuntimeException("First or last plan element is not an instance of activity. Aborting...");
			
			double firstActDur = firstAct.getEndTime();
			if(firstAct.getType().equals(lastAct.getType())){ // same first and last act
				isFirstAndLastActSame = true;
				// If lastAct.getStartTime() is after midnight, it eats into the jointActDur which is what we want.
				// It only becomes implausible if lastAct.getStartTime() > firstAct.getEndTime() which would mean negative sleep.
				double jointActDur = firstActDur +  Time.MIDNIGHT - lastAct.getStartTime();
				if(jointActDur == 0.) throw new RuntimeException("First and last activities are of the same type, yet total duration is 0.0. Aborting...");
				overnightTypDur = Math.max(Math.floor(jointActDur/3600), 0.5) * 3600;
			} else { // different first and last act
				if(firstActDur == 0.) {
					// TODO: This is not true since commented out below. And why should I not be able to leave my home at midnight?
					log.warn("First and last activities are of different type, yet first activity has a duration of 0.0. Setting it to duration of 1800 sec.");
					// However, I agree with shifting it for now, and insert the following:
					// TODO: Does this work?
					timeShift = 1800.;
				}
			}
			for(int ii = 0; ii < pes.size();ii++) {
				PlanElement pe = pes.get(ii);
				if(pe instanceof Leg){
					Leg leg = popFactory.createLeg(((Leg)pe).getMode());
					leg.setDepartureTime(((Leg)pe).getDepartureTime() + timeShift);
					leg.setTravelTime(((Leg)pe).getTravelTime());
					planOut.addLeg(leg);
				} else {
					double typDur = Double.NEGATIVE_INFINITY;
					String actType = null;
					if((ii == 0 || ii  == planElementsSize - 1 )){ //first or last activity
						if(isFirstAndLastActSame){ // same first and last act
							actType = firstAct.getType().substring(0,4).concat(overnightTypDur/3600+"H");
							Activity hAct = popFactory.createActivityFromCoord(actType, firstAct.getCoord());
							if(ii==0) hAct.setEndTime(firstAct.getEndTime()); // first act --> only end time (no need for any time shift for first act)
							else hAct.setStartTime(lastAct.getStartTime() + timeShift); // last act --> only start time
							planOut.addActivity(hAct);
							typDur = overnightTypDur;
						} else { // different first and last act
							if(ii == 0){ // first act
								double dur = firstAct.getEndTime();
								Tuple<Double, Double> durAndTimeShift = durationConsistencyCheck(dur);
								typDur = Math.max(Math.floor(durAndTimeShift.getFirst()/3600), 0.5) * 3600;
								timeShift += durAndTimeShift.getSecond();
								actType = firstAct.getType().substring(0,4).concat(typDur/3600+"H");
								Activity act = popFactory.createActivityFromCoord(actType, firstAct.getCoord());
								act.setEndTime(firstAct.getEndTime() + timeShift); //time shift is also required for first activity, e.g. when activities have a end time of 0.
								planOut.addActivity(act);
							} else { // last act
								if(lastAct.getStartTime() >= Time.MIDNIGHT) {
									// skipping the person, one could skip only this activity (and the connecting leg) which could generate other prob like 
									// home1 - car - home2 - pt - work will reduce to home1 -car- home2 and home1 and home2 are not wrapped.
									skipPerson = true;
									break;
								}
								double dur = Time.MIDNIGHT - lastAct.getStartTime();
								Tuple<Double, Double> durAndTimeShift = durationConsistencyCheck(dur);
								typDur = Math.max(Math.floor(durAndTimeShift.getFirst()/3600), 0.5) * 3600;
								timeShift += durAndTimeShift.getSecond();
								actType = lastAct.getType().substring(0,4).concat(typDur/3600+"H");
								Activity act = popFactory.createActivityFromCoord(actType, lastAct.getCoord());
								act.setStartTime(lastAct.getStartTime()+ timeShift);
								planOut.addActivity(act);
							}
						}
					} else { // all intermediate activities
						Activity currentAct = (Activity) pe;
						Coord cord = currentAct.getCoord();
						if(currentAct.getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)){
							planOut.addActivity(currentAct);
							continue;
//							actType = currentAct.getType();
//							typDur = Time.UNDEFINED_TIME;
						} else {
							double dur = currentAct.getEndTime() - currentAct.getStartTime();
							Tuple<Double, Double> durAndTimeShift = durationConsistencyCheck(dur);
							typDur = Math.max(Math.floor(durAndTimeShift.getFirst()/3600), 0.5) * 3600;
							actType = currentAct.getType().substring(0, 4).concat(typDur/3600+"H");
							Activity a1 = popFactory.createActivityFromCoord(actType, cord);
							a1.setStartTime(currentAct.getStartTime()+ timeShift); // previous time shift
							timeShift += durAndTimeShift.getSecond();
							a1.setEndTime(currentAct.getEndTime() + timeShift); 
							planOut.addActivity(a1);
						}
					}
					actType2TypDur.put(actType, typDur);
				} 
			}
			if(!skipPerson) popOut.addPerson(pOut);
			else skippedPersons++;
		}
	}

	private Tuple<Double, Double> durationConsistencyCheck (double duration){
		double timeShift = 0.;
		double dur = 0.;

		if(Double.isNaN(duration)){
			throw new RuntimeException("Duration of activity is " + duration + "; Please check your start and end times. Aborting ...");
		} else if(duration == 0.) {
			if(zeroDurCount<1){
				log.warn("Duration of activity is zero. Setting it to a duration of 1800.");
				log.warn(Gbl.ONLYONCE);
			}
			zeroDurCount ++;
			dur = 1800.;
			timeShift = 1800.;
		} else if (duration < 0.){
			throw new RuntimeException("Duration of activity is negative. Aborting...");
//			timeShift = - duration + 1800;
//			duration = 1800;
		} else {
			dur = duration;
		}
		return new Tuple<Double, Double>(dur, timeShift);
	}

	/**
	 * @return activity types and their typical durations as reported in the survey (rounded down to half hours)
	 */
	public SortedMap<String, Double> getActivityType2TypicalDuration(){
		return this.actType2TypDur;
	}

	/**
	 * @return population with activities
	 * (1) with typical durations as reported in the survey (rounded down to half hours)
	 * (2) with typical durations of minimum 1800s (if they were shorter in the survey -- including zero and negative durations)
	 * 
	 * Note: persons with an activity starting after midnight have been omitted
	 * TODO: What about the ones starting before, but ending after (yielding negative durations)?
	 */
	public Population getOutPop(){
		return this.scOut.getPopulation();
	}

	void writePlans(String outplans){
		new PopulationWriter(scOut.getPopulation()).write(outplans);
		log.info("File is written to " + outplans);
		if(skippedPersons > 0){
			log.warn("Because their last activity starts after midnight, " + skippedPersons + " persons were skipped.");
		}
	}
}

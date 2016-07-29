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

package playground.santiago.population;

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
public class ActivityClassifier {

	public ActivityClassifier(Scenario scenario) {
		this.sc = scenario;
		actType2TypDur = new TreeMap<String, Double>();
		log.info("Typical durations calculated by this class are rounded down to full hours.");
	}

	public static final Logger log = Logger.getLogger(ActivityClassifier.class.getSimpleName());
	private Scenario sc ;
	private int zeroDurCount =0;
	private SortedMap<String, Double> actType2TypDur;
	private Scenario scOut;
	private int skippedPersons = 0;

	/**
	 * @return activity type to typical and minimum duration respectively
	 */
	public SortedMap<String, Double> getActivityType2TypicalDuration(){
		return actType2TypDur;
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
			double timeShift=0; // necessary for zero duration activities

			int planElementsSize = pes.size();
			// take out first and last activities, put them together if they are same.

			boolean isFirstAndLastActSame = false;
			double homeTypDur = Double.NEGATIVE_INFINITY;

			Activity firstAct = (Activity) pes.get(0);
			Activity lastAct = (Activity) pes.get(planElementsSize-1);

			if(firstAct == null || lastAct == null) throw new RuntimeException("First and last plan elements are not instanceof Activity. Aborting...");

			if(firstAct.getType().equals(lastAct.getType())){ 
				double homeDur = firstAct.getEndTime();
				homeDur = homeDur +  24*3600 - lastAct.getStartTime(); // here 30*00 may not be necessary, because, this step only decide about typical duration and lesser typical duration is better than very high.
				isFirstAndLastActSame = true;

				if(homeDur == 0) throw new RuntimeException("First and last activities are same, yet total duration is 0. Aborting...");

				homeTypDur = Math.max(Math.floor(homeDur/3600), 0.5) * 3600;
			} else {
				if(firstAct.getEndTime() == 0.) {
					/*
					 * If first and last act are not same, 1800 sec will be assigned to first act during "duringConsistencyCheck".
					 * else it will be clubbed with last act and thus, will be scored together.
					 */
					log.warn("First activity has zero end time and first and last activities are different and thus scored differently. Setting a minimum duration of 1800 sec for first activity.");
				}
			}
			for(int ii = 0; ii<pes.size();ii++) {
				PlanElement pe = pes.get(ii);

				if(pe instanceof Leg){

					Leg leg = popFactory.createLeg(((Leg)pe).getMode());
					leg.setDepartureTime(((Leg)pe).getDepartureTime()+timeShift);
					leg.setTravelTime(((Leg)pe).getTravelTime());
					planOut.addLeg(leg);

				} else {

					double typDur = Double.NEGATIVE_INFINITY;
					String actType = null;

					if((ii == 0 || ii  == planElementsSize - 1 )){ //first or last activity

						if(isFirstAndLastActSame){ // same first and last act

							actType = firstAct.getType().substring(0,4).concat(homeTypDur/3600+"H");

							Activity hAct = popFactory.createActivityFromCoord(actType, firstAct.getCoord());

							if(ii==0) hAct.setEndTime(firstAct.getEndTime()); // first act --> only end time (no need for any time shift for first act)
							else hAct.setStartTime(lastAct.getStartTime() + timeShift); // last act --> only start time

							planOut.addActivity(hAct);
							typDur = homeTypDur;

						} else { // different first and last act

							if(ii == 0){ // first

								double dur = firstAct.getEndTime();
								Tuple<Double, Double> durAndTimeShift = durationConsistencyCheck(dur);

								typDur = Math.max(Math.floor(durAndTimeShift.getFirst()/3600), 0.5) * 3600;

								timeShift += durAndTimeShift.getSecond();

								actType = firstAct.getType().substring(0,4).concat(typDur/3600+"H");
								Activity act = popFactory.createActivityFromCoord(actType, firstAct.getCoord());
								act.setEndTime(firstAct.getEndTime()+timeShift); //time shift is required for first activity also, for e.g. activities having zero end time.
								planOut.addActivity(act);
							} else { // last

								if(lastAct.getStartTime() >= 24*3600) {
									// skipping the person, one could skip only this activity (and the connecting leg) which could generate other prob like 
									// home1 -car- home2 -pt- work will reduce to home1 -car- home2 and home1 and home2 are not wrapped.
									skipPerson = true;
									break;
								}

								double dur = 24*3600 - lastAct.getStartTime();

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
//							continue;
							actType = currentAct.getType();
							typDur = Time.UNDEFINED_TIME;
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
		log.info("Population is stored.");
	}

	private Tuple<Double, Double> durationConsistencyCheck (double duration){
		double timeShift = 0.;
		double dur = 0;

		if( Double.isNaN(duration) ) {
			throw new RuntimeException("Start and end time are not defined. Don't know how to calculate duration in absence of them. Aborting ...");
		}
		else if(duration == 0) {
			if(zeroDurCount<1){
				log.warn("Duration of person is zero, it may result in higher utility loss. Thus setting it to minimum dur of 1800.");
				log.warn(Gbl.ONLYONCE);
			}
			zeroDurCount ++;
			dur = 1800;
			timeShift = 1800;
		} else if (duration < 0){
			throw new RuntimeException("Duration is negative. Setting it to minimum dur of 1800");
			//			timeShift = - duration + 1800;
			//			duration = 1800;
		} else {
			dur = duration;
		}
		return new Tuple<Double, Double>(dur, timeShift);
	}

	public Population getOutPop(){
		return this.scOut.getPopulation();
	}

	void writePlans( String outplans){
		new PopulationWriter(scOut.getPopulation()).write(outplans);
		log.info("File is written to "+outplans);
		if(skippedPersons > 0){
			log.warn("Because their last activity starts after midnight, " + skippedPersons + " persons were skipped.");
		}
	}
}

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
package playground.agarwalamit.munich.inputs;

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

import playground.agarwalamit.munich.utils.MunichPersonFilter;
import playground.agarwalamit.munich.utils.MunichPersonFilter.MunichUserGroup;

/**
 * It classifies activities into sub-activities for Munich scenario by assuming stated activities as typical duration.
 * Typical duration is rounded to lower hour value (1 h time bin).
 * 
 * @author amit
 */
 class ActivityClassifier {

	public ActivityClassifier(Scenario scenario) {
		this.sc = scenario;
		actType2TypDur = new TreeMap<>();
		LOG.info("Least integer [Math.floor()] of stated activity duration of an activity is set to typical duration.");
		LOG.info("A person is skipped if first and last acitity are different and last activity starts after mid night.");
		//TODO : might make more sense if I check above for all intermediate activities as well.
	}

	public static final Logger LOG = Logger.getLogger(ActivityClassifier.class.getSimpleName());
	private final Scenario sc ;
	private int zeroDurCount =0;
	private final SortedMap<String, Double> actType2TypDur;
	private Scenario scOut;
	private final MunichPersonFilter pf = new MunichPersonFilter();
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

			if( pf.getUserGroupAsStringFromPersonId(p.getId()).equals(MunichUserGroup.Urban.toString()) ){

				boolean skipPerson = false;

				Person pOut = popFactory.createPerson(p.getId());

				Plan planOut = popFactory.createPlan();
				pOut.addPlan(planOut);

				List<PlanElement> pes = p.getSelectedPlan().getPlanElements();
				double timeShift=0; // necessary for zero duration activities

				int planElementsSize = pes.size();
				// take out first and last activities, put them together if they are same.

				double homeTypDur = Double.NEGATIVE_INFINITY;

				Activity firstAct = (Activity) pes.get(0);
				Activity lastAct = (Activity) pes.get(planElementsSize-1);

				if(firstAct == null || lastAct == null) throw new RuntimeException("First and last plan elements are not instanceof Activity. Aborting...");

				boolean isFirstAndLastActSame = firstAct.getType().equals(lastAct.getType());

				if( isFirstAndLastActSame ){ // only define act type and typ dur

					double homeDur = firstAct.getEndTime();
					homeDur = homeDur +  24*3600 - lastAct.getStartTime(); 
					/* here 30*00 may not be necessary, because, this step only decide about typical duration 
					 * and lesser typical duration is better than very high.
					 */

					if(homeDur == 0) throw new RuntimeException("First and last activities are same, yet total duration is 0. Aborting...");

					homeTypDur = Math.max(Math.floor(homeDur/3600), 0.5) * 3600;

				} else {

					if(firstAct.getEndTime() == 0.) { 
						/*
						 * If first and last act are not same, 1800 sec will be assigned to first act during "durationConsistencyCheck(...)".
						 * else it will be clubbed with last act and thus, will be scored together.
						 */
						LOG.warn("First activity has zero end time and first and last activities are different and thus scored differently. "
								+ "Setting a minimum duration of 1800 sec for first activity.");
					}
				}

				// start adding sub-activities to the plans
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

								// first act --> only end time (no need for any time shift for same first and last act)
								if(ii==0) hAct.setEndTime(firstAct.getEndTime()); 
								// last act --> only start time
								else hAct.setStartTime(lastAct.getStartTime() + timeShift); 

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
									//time shift is required for first activity also, for e.g. activities having zero end time.
									act.setEndTime(firstAct.getEndTime()+timeShift); 
									planOut.addActivity(act);

								} else { // last

									if(lastAct.getStartTime() >= 24*3600) {
										/* skipping the person, one could skip only this activity (and the connecting leg) 
										 * which could generate other prob like home1 -car- home2 -pt- work will reduce to home1 -car- home2 and 
										 * home1 and home2 are not wrapped. 
										 * */
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
							double dur = currentAct.getEndTime() - currentAct.getStartTime();

							Tuple<Double, Double> durAndTimeShift = durationConsistencyCheck(dur);

							typDur = Math.max(Math.floor(durAndTimeShift.getFirst()/3600), 0.5) * 3600;

							actType = currentAct.getType().substring(0, 4).concat(typDur/3600+"H");
							Activity a1 = popFactory.createActivityFromCoord(actType, cord);

							a1.setStartTime(currentAct.getStartTime() + timeShift); 

							timeShift += durAndTimeShift.getSecond();

							a1.setEndTime(currentAct.getEndTime() + timeShift); 
							/* updated time shift --> to incorporate time shift of the current and/or previous activities. 
							 * (Basically, multiple activities with zero duration for same person).
							 * for e.g. see initial plan of 555576.2#10166, 555576.2#14123
							 */
							planOut.addActivity(a1);
						}
						actType2TypDur.put(actType, typDur);
					} 
				}

				if(!skipPerson) popOut.addPerson(pOut);
				else skippedPersons++;

			} else if( pf.getUserGroupAsStringFromPersonId(p.getId()).equals(MunichUserGroup.Rev_Commuter.toString()) ){

				//removing end time from the last act
				Person pOut = popFactory.createPerson(p.getId());

				Plan planOut = popFactory.createPlan();
				pOut.addPlan(planOut);

				List<PlanElement> pes = p.getSelectedPlan().getPlanElements();
				int sizeOfPlanElements = pes.size();

				for (int ii=0; ii < sizeOfPlanElements-1;ii++){
					PlanElement pe = pes.get(ii);

					if (pe instanceof Activity){
						planOut.addActivity((Activity)pe);
					} else if (pe instanceof Leg){
						planOut.addLeg((Leg)pe);
					}
				}

				PlanElement pe = pes.get(sizeOfPlanElements-1);
				Activity act = popFactory.createActivityFromCoord(((Activity)pe).getType(),((Activity)pe).getCoord());
				planOut.addActivity(act);
				popOut.addPerson(pOut);
			} else popOut.addPerson(p); // add freight as it is.
		}
		LOG.info("Population is stored.");
	}

	private Tuple<Double, Double> durationConsistencyCheck (double duration){
		double timeShift = 0.;
		double dur = 0;

		if( Double.isNaN(duration) ) throw new RuntimeException("Start and end time are not defined. "
				+ "Don't know how to calculate duration in absence of them. Aborting ...");
		else if(duration == 0) {
			if(zeroDurCount<1){
				LOG.warn("Duration of person is zero, it may result in higher utility loss if typicalDuration calaculation is set to 'Uniform'. "
						+ "Thus setting it to minimum dur of 1800.");
				LOG.warn(Gbl.ONLYONCE);
			}
			zeroDurCount ++;
			dur = 1800;
			timeShift = 1800;
		} else if (duration < 0){
			throw new RuntimeException("Duration is negative. Aborting...");
		} else dur = duration;

		return new Tuple<>(dur, timeShift);
	}

	public Population getOutPopulation(){
		return this.scOut.getPopulation();
	}

	public void writePlans( String outplans){
		new PopulationWriter(scOut.getPopulation()).write(outplans);
		LOG.info("File is written to "+outplans);
		LOG.warn("Total number of skipped persons are "+skippedPersons+". Because last activity starts after mid night.");
	}
}
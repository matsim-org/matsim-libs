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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;

import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */
public class AddingActivitiesInPlans {

	public AddingActivitiesInPlans(Scenario scenario) {
		this.sc = scenario;
		//		params = sc.getConfig().planCalcScore();
		actType2TypDurMinDur = new TreeMap<String, Tuple<Double,Double>>();
		log.warn("Minimum duration for any sub activity is defined as minimum of minimum duration of parent activity and half of new tyical duration.");
		log.warn("Least integer of actual activity duration of an activity is set to typical duration.");
		log.warn("If a person do not have actual duration, plans for such persons remain unchanged. Because such plans do not have either of act end or start time.");
	}

	public static final Logger log = Logger.getLogger(AddingActivitiesInPlans.class.getSimpleName());
	private Scenario sc ;
	//	private PlanCalcScoreConfigGroup params;
	private int zeroDurCount =0;
	private SortedMap<String, Tuple<Double, Double>> actType2TypDurMinDur;
	private Scenario scOut;

	public static void main(String[] args) {
		String initialPlans = "/Users/aagarwal/Desktop/ils4/agarwal/munich/input/mergedPopulation_All_1pct_scaledAndMode_workStartingTimePeakAllCommuter0800Var2h_gk4.xml.gz";
		String initialConfig = "/Users/aagarwal/Desktop/ils4/agarwal/munich/input/config_munich_1pct_baseCase.xml";
		Scenario sc = LoadMyScenarios.loadScenarioFromPlansAndConfig(initialPlans,initialConfig);
		String outPlans = "/Users/aagarwal/Desktop/ils4/agarwal/munich/input/plans_1pct_subActivities.xml.gz";

		AddingActivitiesInPlans newPlansInfo = new AddingActivitiesInPlans(sc);
		newPlansInfo.run();
		newPlansInfo.writePlans(outPlans);
	}

	/**
	 * @return activity type to typical and minimum duration respectively
	 */
	public SortedMap<String, Tuple<Double, Double>> getActivityType2TypicalAndMinimalDuration(){
		return actType2TypDurMinDur;
	}

	public void run(){

		scOut = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population popOut = scOut.getPopulation();
		PopulationFactory popFactory = popOut.getFactory();

		for(Person p : sc.getPopulation().getPersons().values()){

			Person pOut = popFactory.createPerson(p.getId());
			// TODO [AA] need to remove these methods, and use somethign else.
			((PersonImpl) pOut).setAge(((PersonImpl)p).getAge());
			((PersonImpl) pOut).setSex(((PersonImpl)p).getSex());
			((PersonImpl) pOut).setLicence(((PersonImpl)p).getLicense());
			((PersonImpl) pOut).setCarAvail(((PersonImpl)p).getCarAvail());
			((PersonImpl) pOut).setEmployed(((PersonImpl)p).isEmployed());
			
			popOut.addPerson(pOut);
			Plan planOut = popFactory.createPlan();
			pOut.addPlan(planOut);


			List<PlanElement> pes = p.getSelectedPlan().getPlanElements();
			double timeShift=0;

			for(PlanElement pe : pes){ 
				if(pe instanceof Activity){
					String currentAct = ((Activity)pe).getType();
					Coord cord = ((Activity)pe).getCoord();
					double dur = ((Activity)pe).getEndTime() - ((Activity)pe).getStartTime();

					if(dur==0){
						if(zeroDurCount<1){
							log.warn("Duration of person is zero, it may result in higher utility loss. Thus setting it to minimum dur.");
							log.warn(Gbl.ONLYONCE);
						}
						zeroDurCount ++;
						String actTyp = currentAct.substring(0, 4).concat("0.5H");
						Activity a1 = popFactory.createActivityFromCoord(actTyp, cord);
						a1.setStartTime( ((Activity)pe).getStartTime()+timeShift);

						//						double minDur = Math.min(params.getActivityParams(currentAct).getMinimalDuration(), 1800/2);
						double minDur = 1800/2;
						timeShift = timeShift + minDur ;

						a1.setEndTime( ((Activity)pe).getEndTime()+timeShift);
						planOut.addActivity(a1);

						Tuple<Double, Double> typMinDur = new Tuple<Double, Double>(1800., minDur);
						actType2TypDurMinDur.put(actTyp, typMinDur);

					} else if(dur< Double.POSITIVE_INFINITY && dur>0){

						double typDur = Math.floor(dur/3600);
						if(typDur< 1) typDur = 1800;
						else typDur =typDur*3600;

						//						double minDur = Math.min(params.getActivityParams(currentAct).getMinimalDuration(), typDur/2);
						double minDur = typDur/2;

						String actTyp = currentAct.substring(0, 4).concat(typDur/3600+"H");
						Activity a2 = popFactory.createActivityFromCoord(actTyp, cord);
						a2.setStartTime( ((Activity)pe).getStartTime()+timeShift);
						a2.setEndTime( ((Activity)pe).getEndTime()+timeShift);
						planOut.addActivity(a2);

						Tuple<Double, Double> typMinDur = new Tuple<Double, Double>(typDur, minDur);
						actType2TypDurMinDur.put(actTyp, typMinDur);

					} else if(  ((Activity)pe).getStartTime() > Double.NEGATIVE_INFINITY && ((Activity)pe).getStartTime() < Double.POSITIVE_INFINITY  ){
						dur = 30*3600-((Activity)pe).getStartTime();

						double typDur = Math.floor(dur/3600);

						if(typDur< 1) typDur = 1800;
						else typDur =typDur*3600;

						//						double minDur = Math.min(params.getActivityParams(currentAct).getMinimalDuration(), typDur/2);
						double minDur = typDur/2;

						String actTyp = currentAct.substring(0, 4).concat(typDur/3600+"H");
						Activity a2 = popFactory.createActivityFromCoord(actTyp, cord);
						a2.setStartTime( ((Activity)pe).getStartTime()+timeShift);
						a2.setEndTime( ((Activity)pe).getEndTime()+timeShift);
						planOut.addActivity(a2);

						Tuple<Double, Double> typMinDur = new Tuple<Double, Double>(typDur, minDur);
						actType2TypDurMinDur.put(actTyp, typMinDur);
					}

					else {
						planOut.addActivity((Activity)pe);
					}
				} else if(pe instanceof Leg){
					Leg leg = popFactory.createLeg(((Leg)pe).getMode());
					leg.setDepartureTime(((Leg)pe).getDepartureTime()+timeShift);
					leg.setTravelTime(((Leg)pe).getTravelTime());
					planOut.addLeg(leg);
				}
			}

		}
		log.info("Population is stored.");
	}
	
	public Population getOutPop(){
		return this.scOut.getPopulation();
	}

	private void writePlans( String outplans){
		new PopulationWriter(scOut.getPopulation()).write(outplans);
		log.info("File is written to "+outplans);
	}
}

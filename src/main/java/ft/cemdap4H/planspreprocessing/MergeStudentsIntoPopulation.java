/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
package ft.cemdap4H.planspreprocessing;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class MergeStudentsIntoPopulation {

	public static void main(String[] args) {
		String oldPersonFile ="C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/projekt2/input/population/initial_plans1.0.xml.gz";
		String personsFile = "D:/cemdap-vw/cemdap_output/mergedplans_filtered_1.0.xml.gz";
		String configF = "D:/cemdap-vw/cemdap_output/activityConfig.xml";
		new MergeStudentsIntoPopulation().run(oldPersonFile, personsFile, configF);
	}
	
	public void run(String oldPersonFile, String personsFile, String configF)
	{
		Scenario scenOld = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenPop = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Config config = ConfigUtils.loadConfig(configF);
		Set<String> types = new HashSet<>();
		new PopulationReader(scenOld).readFile(oldPersonFile);
		new PopulationReader(scenPop).readFile(personsFile);
		Random r = MatsimRandom.getRandom();
		for (Person p : scenOld.getPopulation().getPersons().values()){
			if (isStudent(p)){
				if (scenPop.getPopulation().getPersons().containsKey(p.getId())){
					scenPop.getPopulation().getPersons().remove(p.getId());
				}
				scenPop.getPopulation().addPerson(p);
				PersonUtils.setAge(p, r.nextInt(18)+8);
				PersonUtils.setCarAvail(p, "never");
				PersonUtils.setLicence(p, "no");
				if (r.nextBoolean()){PersonUtils.setSex(p, "male");
				} else {PersonUtils.setSex(p, "female");
				} 
				for (Plan plan : p.getPlans()){
				Double lastEndTime = null;
				
				for (PlanElement pe : plan.getPlanElements()){
					if (pe instanceof Leg){
						if (((Leg) pe).getMode().equals(TransportMode.car)){
							((Leg) pe).setMode(TransportMode.ride);
						}
					}
					if (pe instanceof Activity){
						Activity act = (Activity) pe;
						
					if (Time.isUndefinedTime(act.getEndTime())&&(!Time.isUndefinedTime(act.getMaximumDuration()))){
						act.setEndTime(lastEndTime+r.nextInt(3*3600));
						act.setMaximumDuration(Time.UNDEFINED_TIME);
					} else {
						lastEndTime = act.getEndTime();
					}	
					}
					}
				}
				for (Plan plan : p.getPlans()){
					Double firstActivityEndtime = null; 
					Double lastActivityEndtime = null; 
					Coord lastActivityCoord = null;
					int firstLastActivityDuration = 0;
					if (plan.getPlanElements().size()>1){
						for (PlanElement pe : plan.getPlanElements()){
							if (pe instanceof Activity){
								if (firstActivityEndtime == null){
									firstActivityEndtime = ((Activity) pe).getEndTime();
									}
									
								if (lastActivityCoord!=null){
										double traveltime = CoordUtils.calcEuclideanDistance(lastActivityCoord, ((Activity) pe).getCoord())/8.33;
										@SuppressWarnings("null")
										double actStartTime = lastActivityEndtime + traveltime;
										if (!Time.isUndefinedTime(((Activity) pe).getEndTime())){
										int duration = (int) Math.round((((Activity) pe).getEndTime() - actStartTime)/3600);
										if (duration <= 0) duration = 1;
										String type = ((Activity) pe).getType();
										if (type.equals("private")) type = "other";
										type = type+"_"+duration;
										((Activity) pe).setType(type);
										types.add(type);
										} else {
											int duration = (int) Math.round(((24*3600 - actStartTime) + firstActivityEndtime)/3600);
											firstLastActivityDuration = duration;
											if (duration <= 0) duration = 1;
											String type = ((Activity) pe).getType();
											if (type.equals("private")) type = "other";
											type = type+"_"+duration;
											((Activity) pe).setType(type);

											types.add(type);

										}
										
										lastActivityCoord = ((Activity) pe).getCoord();
										lastActivityEndtime = ((Activity) pe).getEndTime();
									} else {
										lastActivityCoord = ((Activity) pe).getCoord();
										lastActivityEndtime = ((Activity) pe).getEndTime();
									}
								}
							}
						Activity act0 = (Activity)(plan.getPlanElements().get(0));
						act0.setType(act0.getType()+"_"+firstLastActivityDuration);
						
						}
					}
			}
		}
		for (String type : types){
			String baseType = type.split("_")[0];
			System.out.println(baseType);
			double duration = Integer.parseInt(type.split("_")[1])*3600;
			ActivityParams t = new ActivityParams();
			t.setActivityType(type);
			t.setTypicalDuration(duration);
			t.setOpeningTime(config.planCalcScore().getActivityParams(baseType).getOpeningTime());
			t.setClosingTime(config.planCalcScore().getActivityParams(baseType).getClosingTime());
			config.planCalcScore().addActivityParams(t);
		}
		new ConfigWriter(config).write(configF);
		new PopulationWriter(scenPop.getPopulation()).write(personsFile);
	}

	/**
	 * @param p
	 * @return
	 */
	private static boolean isStudent(Person p) {
		Plan plan = p.getSelectedPlan();
		for (PlanElement pe : plan.getPlanElements()){
			if (pe instanceof Activity){
				if ((((Activity) pe).getType().equals("school"))|| ((Activity) pe).getType().equals("university")){
					((Activity) pe).setType("education");
					return true;
				}
			}
		}
		return false;
	}
}

/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.eMobility;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.XY2Links;

import playground.droeder.eMobility.energy.ChargingProfiles;
import playground.droeder.eMobility.fleet.EFleet;
import playground.droeder.eMobility.fleet.EVehicle;
import playground.droeder.eMobility.poi.POI;
import playground.droeder.eMobility.poi.PoiInfo;
import playground.droeder.eMobility.population.EActivity;
import playground.droeder.eMobility.population.EPerson;
import playground.droeder.eMobility.population.EPopulation;

/**
 * @author droeder
 *
 */
public class CreateTestScenario {
	
	private EmobilityScenario sc;
	
	public CreateTestScenario(){
		sc = new EmobilityScenario();
	}
	
	private void loadConfig(String file){
		if(file == null){
			this.sc.setSc(ScenarioUtils.createScenario(ConfigUtils.createConfig()));
		}else{
			this.sc.setSc(ScenarioUtils.loadScenario(ConfigUtils.loadConfig(file)));
		}
	}
	
	public EmobilityScenario run(String configFile){
		this.loadConfig(configFile);
		Plan basicPlan = createBasicPlan();
		Plan newPlan;
		PopulationFactory factory = this.sc.getSc().getPopulation().getFactory();
		Person p;
		
		int agtCnt = 10;
		for(int i = 0; i < agtCnt; i++){
			newPlan = this.modifyBasePlan(basicPlan);
			p = factory.createPerson(new IdImpl(EPopulation.IDENTIFIER + i));
			p.addPlan(newPlan);
			this.sc.getSc().getPopulation().addPerson(p);
		}
		this.duration = this.duration/agtCnt;
		this.end = this.end /agtCnt;
		this.start = this.end - this.duration;
		
		System.out.println(this.start + " " + this.duration + " " + this.end);
		
		createEData();
		
		createPoi();
		return this.sc;
	}
	
	private void createPoi() {
		PoiInfo poiInfo = new PoiInfo();
		POI poi;
		for(Link l: this.sc.getSc().getNetwork().getLinks().values()){
			poi = new POI(l.getId(), 2, 3600);
			poiInfo.add(poi);
		}
		this.sc.setPoi(poiInfo);
	}

	private void createEData() {
		EPopulation population = new EPopulation();
		EFleet fleet = new EFleet();
		
		
		ArrayList<EActivity> list;
		for(Person p : this.sc.getSc().getPopulation().getPersons().values()){
			list = new ArrayList<EActivity>();
			Id disCharge;
			Id charge;
			double rnd = MatsimRandom.getRandom().nextDouble();
			if(rnd < 0.3){
				disCharge = new IdImpl("LOW");
				charge = ChargingProfiles.NONE;
			}else if(rnd < 0.6){
				disCharge = new IdImpl("HIGH");
				charge = new IdImpl("SLOW");
			}else{
				disCharge = new IdImpl("MEDIUM");
				charge = new IdImpl("FAST");
			}
			Activity appointment = (Activity) p.getSelectedPlan().getPlanElements().get(6);
			EActivity ea = new EActivity(appointment.getLinkId(), appointment.getStartTime(), appointment.getMaximumDuration(), disCharge, charge);
			list.add(ea);
			
			Activity firstCarLocation = (Activity) p.getSelectedPlan().getPlanElements().get(2);
			EVehicle ev = new EVehicle(p.getId(), 20 * MatsimRandom.getRandom().nextDouble(), this.sc.getSc().getNetwork().getLinks().get(firstCarLocation.getLinkId()), list);
			fleet.addVehicle(ev);
			EPerson ep = new EPerson(p.getId(), ev);
			population.add(ep);
		}
		
		this.sc.setFleet(fleet);
		this.sc.setPopulation(population);
		
	}

	private double duration = 0;
	private double end = 0;
	private double start = 0;
	
	/**
	 * @param basicPlan
	 */
	private Plan modifyBasePlan(Plan basicPlan) {
		double offset = ((2* MatsimRandom.getRandom().nextDouble())-1 )/ 3.;
		
		Plan modified = this.sc.getSc().getPopulation().getFactory().createPlan();
		for(PlanElement p : basicPlan.getPlanElements()){
			if(p instanceof Leg){
				modified.addLeg(new LegImpl((LegImpl)p));
			}else{
				modified.addActivity(new ActivityImpl((Activity) p));
			}
		}
		
		double walkDistance = CoordUtils.calcDistance(((Activity)modified.getPlanElements().get(0)).getCoord(), ((Activity)modified.getPlanElements().get(2)).getCoord());
		walkDistance += CoordUtils.calcDistance(((Activity)modified.getPlanElements().get(4)).getCoord(), ((Activity)modified.getPlanElements().get(6)).getCoord());
		
		double driveDistance = CoordUtils.calcDistance(((Activity)modified.getPlanElements().get(2)).getCoord(), ((Activity)modified.getPlanElements().get(4)).getCoord());
		
		double time2workPlace = offset*((walkDistance/1.5) + driveDistance/10.0);
		
		Activity start = ((Activity)modified.getPlanElements().get(0));
		start.setEndTime(start.getEndTime());
		Activity app = ((Activity)modified.getPlanElements().get(6));
		app.setEndTime(start.getEndTime() + app.getMaximumDuration() + time2workPlace);
		this.duration +=app.getMaximumDuration();
		this.end += app.getEndTime();
		Activity end = ((Activity)modified.getPlanElements().get(12));
		end.setEndTime(Time.UNDEFINED_TIME);
		

		
		return modified;
	}

	public Plan createBasicPlan(){
		PopulationFactory factory = this.sc.getSc().getPopulation().getFactory();
		
		Plan plan = factory.createPlan();
		
		Activity home1 = factory.createActivityFromCoord("home", new CoordImpl(4589495, 5820909));
		home1.setEndTime(8*3600);
		plan.addActivity(home1);

		Leg walk1 = factory.createLeg(TransportMode.walk);
		plan.addLeg(walk1);
		
		Activity change11 = factory.createActivityFromCoord("multiple", new CoordImpl(4589997, 5820720));
		change11.setMaximumDuration(0);
		plan.addActivity(change11);
		
		Leg drive1 = factory.createLeg(TransportMode.car);
		plan.addLeg(drive1);
		
		Activity change12 = factory.createActivityFromCoord("multiple", new CoordImpl(4598755, 5814333));
		change12.setMaximumDuration(0);
		plan.addActivity(change12);
		
		Leg walk2 = factory.createLeg(TransportMode.walk);
		plan.addLeg(walk2);
		
		Activity app1 = factory.createActivityFromCoord("work", new CoordImpl(4598990, 5813548));
		app1.setMaximumDuration(8.5 * 3600);
		plan.addActivity(app1);
		
		plan.addLeg(new LegImpl((LegImpl)walk2));
		plan.addActivity(new ActivityImpl(change12));
		plan.addLeg(new LegImpl((LegImpl)drive1));
		plan.addActivity(new ActivityImpl(change11));
		plan.addLeg(new LegImpl((LegImpl)walk1));
		plan.addActivity(new ActivityImpl(home1));
		
		new XY2Links((ScenarioImpl) this.sc.getSc()).run(plan);
		
		
		return plan;
	}

	public static void main(String[] args){
		
		final String DIR = "D:/VSP/svn/shared/volkswagen_internal/";

		final String CONFIGFILE = DIR + "scenario/config_base_scenario.xml";
		EmobilityScenario sc = new CreateTestScenario().run(CONFIGFILE);
	}
}

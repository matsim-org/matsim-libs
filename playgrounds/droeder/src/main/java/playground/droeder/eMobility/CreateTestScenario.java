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
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;

import playground.droeder.DaFileReader;
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
	
	public EmobilityScenario run(String configFile, String basicPlansFile, String appointmentsFile){
		this.loadConfig(configFile);
		Plan basicPlan = loadBasicPlan(basicPlansFile, configFile);
		PopulationFactory factory = this.sc.getSc().getPopulation().getFactory();
		Person p;
		
		Set<String[]> appointments = DaFileReader.readFileContent(appointmentsFile, "\t", true);
		
		for(String[] app: appointments){
			p = factory.createPerson(new IdImpl(EPopulation.IDENTIFIER + app[0]));
			p.addPlan(basicPlan);
			this.sc.getSc().getPopulation().addPerson(p);
		}
		createEData(appointments);
		createPoi();
		return this.sc;
	}
	
	private void createPoi() {
		PoiInfo poiInfo = new PoiInfo();
		POI poi;
		for(Link l: this.sc.getSc().getNetwork().getLinks().values()){
			poi = new POI(l.getId(), 5, 3600);
			poiInfo.add(poi);
		}
		this.sc.setPoi(poiInfo);
	}

	private void createEData(Set<String[]> appointments) {
		EPopulation population = new EPopulation();
		EFleet fleet = new EFleet();
		
		
		ArrayList<EActivity> list;
		for(String[] app: appointments){
			list = new ArrayList<EActivity>();
			Id disCharge = new IdImpl(app[7]);
			Id charge = new IdImpl(app[6]);
			
			double startTime, endTime;
			String [] time = app[4].split(":");
			startTime = 3600 * Double.parseDouble(time[0]) + 60 * Double.parseDouble(time[1]) + Double.parseDouble(time[2]);
			time = app[5].split(":");
			endTime = 3600 * Double.parseDouble(time[0]) + 60 * Double.parseDouble(time[1]) + Double.parseDouble(time[2]);
			
			
			EActivity ea = new EActivity(new IdImpl(app[3]), startTime, endTime - startTime, disCharge, charge);
			list.add(ea);
			
			EVehicle ev = new EVehicle(new IdImpl(EPopulation.IDENTIFIER + app[0]), Double.parseDouble(app[2]), this.sc.getSc().getNetwork().getLinks().get(new IdImpl(app[1])), list);
			fleet.addVehicle(ev);
			EPerson ep = new EPerson(ev.getId(), ev);
			population.add(ep);
		}
		
		this.sc.setFleet(fleet);
		this.sc.setPopulation(population);
		
	}

//	private double duration = 0;
//	private double end = 0;
//	private double start = 0;
//	
//	/**
//	 * @param basicPlan
//	 */
//	private Plan modifyBasePlan(Plan basicPlan) {
//		double offset = ((2* MatsimRandom.getRandom().nextDouble())-1 )/ 3.;
//		
//		Plan modified = this.sc.getSc().getPopulation().getFactory().createPlan();
//		for(PlanElement p : basicPlan.getPlanElements()){
//			if(p instanceof Leg){
//				modified.addLeg(new LegImpl((LegImpl)p));
//			}else{
//				modified.addActivity(new ActivityImpl((Activity) p));
//			}
//		}
//		
//		double walkDistance = CoordUtils.calcDistance(((Activity)modified.getPlanElements().get(0)).getCoord(), ((Activity)modified.getPlanElements().get(2)).getCoord());
//		walkDistance += CoordUtils.calcDistance(((Activity)modified.getPlanElements().get(4)).getCoord(), ((Activity)modified.getPlanElements().get(6)).getCoord());
//		
//		double driveDistance = CoordUtils.calcDistance(((Activity)modified.getPlanElements().get(2)).getCoord(), ((Activity)modified.getPlanElements().get(4)).getCoord());
//		
//		double time2workPlace = offset*((walkDistance/1.5) + driveDistance/10.0);
//		
//		Activity start = ((Activity)modified.getPlanElements().get(0));
//		start.setEndTime(start.getEndTime());
//		Activity app = ((Activity)modified.getPlanElements().get(6));
//		app.setEndTime(start.getEndTime() + app.getMaximumDuration() + time2workPlace);
//		this.duration +=app.getMaximumDuration();
//		this.end += app.getEndTime();
//		Activity end = ((Activity)modified.getPlanElements().get(12));
//		end.setEndTime(Time.UNDEFINED_TIME);
//		
//
//		
//		return modified;
//	}

	private Plan loadBasicPlan(String basicPlansFile, String configFile){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc).readFile(this.sc.getSc().getConfig().getParam(NetworkConfigGroup.GROUP_NAME, "inputNetworkFile"));
		new MatsimPopulationReader(sc).readFile(basicPlansFile);
		// the basicPlansFile should contain EXACTLY one Person
		return sc.getPopulation().getPersons().values().iterator().next().getSelectedPlan();
	}

	public static void main(String[] args){
		
		final String DIR = "D:/VSP/svn/shared/volkswagen_internal/";

		final String CONFIGFILE = DIR + "scenario/config_base_scenario.xml";
		final String BASICPLAN = DIR + "scenario/input/basicAgent.xml";
		final String APPOINTMENTS = DIR + "scenario/input/testAppointments.txt";
		EmobilityScenario sc = new CreateTestScenario().run(CONFIGFILE, BASICPLAN, APPOINTMENTS);
	}
}

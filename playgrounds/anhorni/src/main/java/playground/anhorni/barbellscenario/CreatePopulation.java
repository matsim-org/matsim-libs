/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.anhorni.barbellscenario;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class CreatePopulation {
	private ScenarioImpl scenario = null;	
	private final static Logger log = Logger.getLogger(CreatePopulation.class);	
				
	public static void main(final String[] args) {
		String networkfilePath = args[0];
		String facilitiesfilePath = args[1];
		int nPersons = Integer.parseInt(args[2]);
		String outpath = args[3];
	
		CreatePopulation plansCreator = new CreatePopulation();
		plansCreator.init(networkfilePath, facilitiesfilePath);
		plansCreator.generatePlans(nPersons);
		plansCreator.writePlans(outpath);
		
		log.info("Creation finished -----------------------------------------");
	}
	
	private void init(final String networkfilePath, final String facilitiesfilePath) {
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(this.scenario).readFile(networkfilePath);
		new FacilitiesReaderMatsimV1(this.scenario).readFile(facilitiesfilePath);
	}
	
	public void writePlans(String outpath) {
		log.info("Writing plans ...");
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(outpath + "/plans.xml");
	}
	
	
	public void generatePlans(int nPersons) {	
		ActivityFacility homeFacility;
		ActivityFacility workFacility;
		
		for (int i = 0; i<nPersons; i++) {
			PersonImpl p = new PersonImpl(new IdImpl(i));
			p.setEmployed(true);
			
			if (i % 3 == 0) {
				homeFacility = this.scenario.getActivityFacilities().getFacilities().get(new IdImpl(0));
				workFacility = this.scenario.getActivityFacilities().getFacilities().get(new IdImpl(3));
			} else if (i % 3 == 1) {
				homeFacility = this.scenario.getActivityFacilities().getFacilities().get(new IdImpl(1));
				workFacility = this.scenario.getActivityFacilities().getFacilities().get(new IdImpl(4));
			} else {
				homeFacility = this.scenario.getActivityFacilities().getFacilities().get(new IdImpl(2));
				workFacility = this.scenario.getActivityFacilities().getFacilities().get(new IdImpl(5));
			}
			double timeOffset = i * (3600.0 / nPersons);
			this.scenario.getPopulation().addPerson(p);
			this.generateWorkPlan(p, homeFacility, workFacility, timeOffset);
		}
	}
		
	private void generateWorkPlan(PersonImpl p, ActivityFacility homeFacility, ActivityFacility workFacility, double timeOffset) {	
		
		double time = 0.0 + timeOffset;
		PlanImpl plan = new PlanImpl();
			
		ActivityImpl actH = new ActivityImpl("h", homeFacility.getLinkId());
		actH.setFacilityId(homeFacility.getId());
		actH.setCoord(this.scenario.getActivityFacilities().getFacilities().get(homeFacility.getId()).getCoord());
		
		actH.setStartTime(time);
		actH.setMaximumDuration(8.0 * 3600.0);
		actH.setEndTime(time + 8.0 * 3600.0);
		
		plan.addActivity(actH);		
		plan.addLeg(new LegImpl("car"));

		ActivityImpl actW = new ActivityImpl("w", workFacility.getLinkId());
		actW.setFacilityId(workFacility.getId());
		actW.setCoord(this.scenario.getActivityFacilities().getFacilities().get(workFacility.getId()).getCoord());
		
		actW.setStartTime(time + 8.0 * 3600.0);
		actW.setMaximumDuration(8.0 * 3600.0);
		plan.addActivity(actW);	
		
		p.addPlan(plan);
	}
}

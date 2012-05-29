/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.droeder.laemmer;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author droeder
 *
 */
public class Population4Arm {
	private static final String DIR = "D:/VSP/projects/laemmer/4arms/";
	private static final String NET = DIR + "network.xml";
	private static final int numberEastWest = 1440;
	
	
	private Scenario sc;
	
	public Population4Arm(String networkFile){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc).readFile(networkFile);
		this.sc = sc;
	}
	
	public void run(int eastWestPerHour, String outDir){
		createNorthSouthPop();
		createEastWestPop(eastWestPerHour);
		new PopulationWriter(this.sc.getPopulation(), this.sc.getNetwork()).write(outDir + "population_" + String.valueOf(eastWestPerHour) +".xml");
	}

	/**
	 * @param eastWestPerHour
	 */
	private void createEastWestPop(double eastWestPerHour) {
		PopulationFactory f = this.sc.getPopulation().getFactory();
		ActivityImpl home, work;
		Leg leg;
		Id e,w;
		Person p;
		Coord eCoord, wCoord;
		
		double gap = 3600 /eastWestPerHour;
		
		for(int i = 0; i < (1.5 * eastWestPerHour); i++){
			e = this.sc.createId("E32");
			eCoord = this.sc.getNetwork().getLinks().get(e).getCoord();
			w = this.sc.createId("W12");
			wCoord = this.sc.getNetwork().getLinks().get(w).getCoord();
			
			// N->S
			Plan eastWest = f.createPlan();
			
			home = (ActivityImpl) f.createActivityFromLinkId("home", e);
			home.setCoord(eCoord);
			home.setEndTime((8 * 3600) + (i * gap));
			eastWest.addActivity(home);
			
			leg = f.createLeg(TransportMode.car);
			leg.setDepartureTime((8 * 3600) + (i * gap));
			eastWest.addLeg(leg);
			
			work = (ActivityImpl) f.createActivityFromLinkId("work", w);
			work.setCoord(wCoord);
			eastWest.addActivity(work);
			
			p = f.createPerson(this.sc.createId("E_" + String.valueOf(i)));
			p.addPlan(eastWest);
			this.sc.getPopulation().addPerson(p);
			
			//S->N
			Plan westEast = f.createPlan();
			
			home = (ActivityImpl) f.createActivityFromLinkId("home", w);
			home.setCoord(wCoord);
			home.setEndTime((8 * 3600) + (i * gap));
			westEast.addActivity(home);
			
			leg = f.createLeg(TransportMode.car);
			leg.setDepartureTime((8 * 3600) + (i * gap));
			westEast.addLeg(leg);
			
			work = (ActivityImpl) f.createActivityFromLinkId("work", e);
			work.setCoord(eCoord);
			westEast.addActivity(work);
			
			p = f.createPerson(this.sc.createId("W_" + String.valueOf(i)));
			p.addPlan(westEast);
			this.sc.getPopulation().addPerson(p);
		}
	}

	/**
	 * 
	 */
	private void createNorthSouthPop() {
		PopulationFactory f = this.sc.getPopulation().getFactory();
		ActivityImpl home, work;
		Leg leg;
		Id n,s;
		Person p;
		double gap = 3600 / 180;
		Coord nCoord, sCoord;
		
		for(int i = 0; i < (1.5 * 180); i++){
			n = this.sc.createId("N23");
			nCoord = this.sc.getNetwork().getLinks().get(n).getCoord();
			s = this.sc.createId("S21");
			sCoord = this.sc.getNetwork().getLinks().get(s).getCoord();
			
			// N->S
			Plan northSouth = f.createPlan();
			
			home = (ActivityImpl) f.createActivityFromLinkId("home", n);
			home.setCoord(nCoord);
			home.setEndTime((8 * 3600) + (i * gap));
			northSouth.addActivity(home);
			
			leg = f.createLeg(TransportMode.car);
			leg.setDepartureTime((8 * 3600) + (i * gap));
			northSouth.addLeg(leg);
			
			work = (ActivityImpl) f.createActivityFromLinkId("work", s);
			work.setCoord(sCoord);
			northSouth.addActivity(work);
			
			p = f.createPerson(this.sc.createId("N_" + String.valueOf(i)));
			p.addPlan(northSouth);
			this.sc.getPopulation().addPerson(p);
			
			//S->N
			Plan southNorth = f.createPlan();
			
			home = (ActivityImpl) f.createActivityFromLinkId("home", s);
			home.setCoord(sCoord);
			home.setEndTime((8 * 3600) + (i * gap));
			southNorth.addActivity(home);
			
			leg = f.createLeg(TransportMode.car);
			leg.setDepartureTime((8 * 3600) + (i * gap));
			southNorth.addLeg(leg);
			
			work = (ActivityImpl) f.createActivityFromLinkId("work", n);
			work.setCoord(nCoord);
			southNorth.addActivity(work);
			
			p = f.createPerson(this.sc.createId("S_" + String.valueOf(i)));
			p.addPlan(southNorth);
			this.sc.getPopulation().addPerson(p);
		}
	}
	
	public static void main(String[] args){
		Population4Arm run = new Population4Arm(NET);
		run.run(numberEastWest, DIR);
	}

}

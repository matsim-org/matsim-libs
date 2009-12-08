/* *********************************************************************** *
 * project: org.matsim.*
 * IncomeAttacher.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mfeil.MDSAM;

import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.world.Layer;
import org.matsim.world.MatsimWorldReader;
import org.matsim.api.basic.v01.Id;
import org.matsim.world.World;
import org.matsim.core.population.PersonImpl;
import org.matsim.api.core.v01.population.Person;
import playground.balmermi.census2000.data.Municipality;
import playground.balmermi.census2000.data.Municipalities;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.geometry.CoordUtils;


/**
 * Class to run attach agents with an income via municipalities' average income
 * @param args
 */
public class IncomeAttacher {
	
	
	public static void main(String[] args) {
		log.info("Process started...");
		
		
		final String facilitiesFilename = "/home/baug/mfeil/data/Zurich10/facilities.xml";
		final String worldFilename = "/home/baug/mfeil/data/Zurich10/world.xml";
		final String worldAddFilename = "/home/baug/mfeil/data/Zurich10/gg25_2001_infos.txt";
		final String networkFilename = "/home/baug/mfeil/data/Zurich10/network.xml";
		final String populationFilename = "/home/baug/mfeil/data/Zurich10/plans.xml";
		final String outputFilename = "/home/baug/mfeil/data/choiceSet/it0/output_plans0930.dat";
		
		/*
		final String populationFilename = "./plans/output_plans.xml";
		final String networkFilename = "./plans/network.xml";
		final String facilitiesFilename = "./plans/facilities.xml";
		final String worldFilename = "./plans/world.xml";
		final String worldAddFilename = "./plans/gg25_2001_infos.txt";
		final String outputFilename = "./plans/output.xls";
		*/				
		
		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);
		new MatsimFacilitiesReader(scenario.getActivityFacilities()).readFile(facilitiesFilename);
		new MatsimPopulationReader(scenario).readFile(populationFilename);
		new MatsimWorldReader(scenario.getWorld()).readFile(worldFilename);

		IncomeAttacher att = new IncomeAttacher(scenario);
		att.run(worldAddFilename);
		log.info("Process finished.");
	}
	
	private static final Logger log = Logger.getLogger(IncomeAttacher.class);
	private HashMap<Id,Double> income;
	private ScenarioImpl scenario;

	
	public IncomeAttacher (ScenarioImpl scenario){
		this.scenario = scenario;
	}
	
	
	private void run (String inputFile){
		
		System.out.println("  parsing additional municipality information... ");
		Municipalities municipalities = new Municipalities(inputFile);
		Layer municipalityLayer = scenario.getWorld().getLayer(new IdImpl(Municipalities.MUNICIPALITY));
		municipalities.parse(municipalityLayer);
		System.out.println("  done.");
		
		this.income = new HashMap<Id,Double>();
		HashMap<Id,Id> listings = new HashMap<Id,Id>();
		int doubleListings = 0;
		int noListing =0;
		
		for (Iterator<? extends Person> iterator = this.scenario.getPopulation().getPersons().values().iterator(); iterator.hasNext();){
			PersonImpl person = (PersonImpl) iterator.next();
			
			ArrayList<Id> munAtts = new ArrayList<Id>();
			
			for (Iterator<Municipality> iteratorMun = municipalities.getMunicipalities().values().iterator(); iteratorMun.hasNext();){
				Municipality muni = iteratorMun.next();
				
				double min_x= muni.getZone().getMin().getX();
				double min_y= muni.getZone().getMin().getY();
				double max_x= muni.getZone().getMax().getX();
				double max_y= muni.getZone().getMax().getY();
				
				double x = ((ActivityImpl)(person.getSelectedPlan().getPlanElements().get(0))).getCoord().getX();
				double y = ((ActivityImpl)(person.getSelectedPlan().getPlanElements().get(0))).getCoord().getY();
				
				if (x >= min_x && x <= max_x && y >= min_y && y <= max_y){
					munAtts.add(muni.getId());
				}
			}
			if (munAtts.size()>1){
				doubleListings++;
				System.out.println("Size of element is "+munAtts.size());
				
				double distance = Double.MAX_VALUE;
				int position = -1;
				for (int j=0;j<munAtts.size();j++){
					double dis = CoordUtils.calcDistance(((ActivityImpl)(person.getSelectedPlan().getPlanElements().get(0))).getCoord(), municipalities.getMunicipality(munAtts.get(j)).getZone().getCoord());
					if (distance>dis){
						distance = dis;
						position = j;
					}
				}
				listings.put(person.getId(), munAtts.get(position));
				this.income.put(person.getId(), municipalities.getMunicipality(munAtts.get(position)).getIncome());
				
			}
			if (munAtts.size()== 0){
				noListing++;
				System.out.println("Size of element is "+munAtts.size());
			}
		}
		System.out.println("There are "+doubleListings+" double-listings and "+noListing+" no-listings.");
	}
}



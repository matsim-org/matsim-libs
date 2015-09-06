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

package playground.anhorni.dummy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesReaderMatsimV1;

public class ScnCreator {
	
	private final static Logger log = Logger.getLogger(ScnCreator.class);
	
	private Scenario origScenario;
	private Scenario newScenario;
	
	private static double sampleRate = 0.1;
	
	Random random = new Random(3838494);
	
	private String facilitiesFile = "C:/l/andreasrep/coding/input/tutorial/zhcut/1pct/facilities.xml.gz";
	private String networkFile = "C:/l/andreasrep/coding/input/tutorial/zhcut/network.xml";
	private String plansFile = "C:/l/andreasrep/coding/input/tutorial/zhcut/1pct/plans.xml.gz";
	private String municipalitiesFile = "C:/l/andreasrep/coding/input/tutorial/swiss_municipalities.txt";
	
	private String businessCensusOutFile = "C:/l/andreasrep/coding/input/tutorial/business_census.txt";
	private String censusOutFile = "C:/l/andreasrep/coding/input/tutorial/census.txt";
	private String pusPersonsOutFile = "C:/l/andreasrep/coding/input/tutorial/travelsurvey_persons.txt";
	private String pusTripsOutFile = "C:/l/andreasrep/coding/input/tutorial/travelsurvey_trips.txt";
	
	private TreeMap<Id<Coord>, Coord> municipalities = new TreeMap<>();
	
	// --------------------------------------------------------------------------
	public static void main(String[] args) {
		ScnCreator creator = new ScnCreator();
		creator.run();		
	}
	
	private void run() {
		this.init();
		this.smearFacilities();
		this.smearPlans();	
		this.writeFacilities();
		List<Id<Person>> sampledPersons = this.writePUSPersons();
		this.writePUSTrips(sampledPersons);
		this.writeCensus();
		log.info("Creation finished ......................");
	}
	
	private void init() {
		this.origScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.newScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		new MatsimNetworkReader(this.origScenario).readFile(networkFile);
		new FacilitiesReaderMatsimV1(this.origScenario).readFile(this.facilitiesFile);
		MatsimPopulationReader populationReader = new MatsimPopulationReader(this.origScenario);
		populationReader.readFile(this.plansFile);
		log.info("Original population size " + this.origScenario.getPopulation().getPersons().size());
		
		this.readMunicipalities();
	}
	
	private void readMunicipalities() {
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(this.municipalitiesFile));
			String line = bufferedReader.readLine(); //skip header
					
			while ((line = bufferedReader.readLine()) != null) {
				String parts[] = line.split("\t");
				Id<Coord> id = Id.create(parts[0], Coord.class);
				Coord coord = new Coord(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
				this.municipalities.put(id, coord);;
			}
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void smearPlans() {
		int cnt = 0;
		for (Person p : this.origScenario.getPopulation().getPersons().values()) {
            ((PersonImpl) p).setId(Id.create(cnt, Person.class));
            Plan plan = p.getSelectedPlan();
			
			ActivityImpl homeAct = (ActivityImpl)plan.getPlanElements().get(0);
			Coord homeLocation = homeAct.getCoord();
			Coord newHomeLocation = new Coord(homeLocation.getX() + this.randomize(), homeLocation.getY() + this.randomize());
			
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					ActivityImpl act = (ActivityImpl)pe;
					if (act.getType().startsWith("h")) {
						act.setCoord(newHomeLocation);
					}
					else {
						Coord newCoord = new Coord(act.getCoord().getX() + this.randomize(), act.getCoord().getY() + this.randomize());
						act.setCoord(newCoord);
					}
				}
			}
			cnt++;
		}
	}
		
	private void smearFacilities() {	
		ActivityFacilities newFacilities = this.newScenario.getActivityFacilities();
		ActivityFacilitiesFactory newFactory = newFacilities.getFactory();
		for (ActivityFacility facility : this.origScenario.getActivityFacilities().getFacilities().values()) {
			// do not add home facilities
			if (facility.getActivityOptions().size() == 1 && facility.getActivityOptions().containsKey("home")) continue;
			Coord coord = facility.getCoord();
			double xNew = coord.getX() + this.randomize();
			double yNew = coord.getY() + this.randomize();
			Coord newCoord = new Coord(xNew, yNew);

			ActivityFacility newFacility = newFactory.createActivityFacility(facility.getId(), newCoord);
			newFacilities.addActivityFacility(newFacility);

			for (ActivityOption actOption : facility.getActivityOptions().values()) {
				ActivityOption ao = newFactory.createActivityOption(actOption.getType());
				newFacility.addActivityOption(ao);
			}
		}
	}
	
	private double randomize() {
		double sign = random.nextDouble() - 0.5;
		double deltaL = random.nextInt(2000);
		double dp = random.nextDouble();
		
		return sign * deltaL + dp;
	}
	
	private void writeCensus() {		
		String header = "KANT\t" +
				"ZGDE\t" +
				"GEBAEUDE_ID\t" +
				"HHNR\t" +
				"PERSON_ID\t" +
				"GEMT\t" +
				"ALTJ\t" +
				"GORTAUS\t" +
				"AGDE\t" +
				"AWBUS\t" +
				"XH\t" +
				"YH";
		try {	
			final BufferedWriter out = IOUtils.getBufferedWriter(this.censusOutFile);
			out.write(header);
			out.newLine();	
			
			int cnt = 0;
			for (Person p : ((ScenarioImpl) this.origScenario).getPopulation().getPersons().values()) {
				
				// sample population
				//if (random.nextDouble() > sampleRate) continue;
				
				int kant = random.nextInt(26);
				int zgde = random.nextInt(10);
				int gebId = random.nextInt(10000);
				int hhnr = random.nextInt(10000000);
				Id<Person> personId = Id.create(cnt, Person.class);
				int gemt = random.nextInt(1000);
				int age  = 1 + random.nextInt(100);
				int gorthaus = random.nextInt(1000);				
				int agde = this.findMunicipality(p.getSelectedPlan());
				int awbus = random.nextInt(10000);
				
				Coord homeCoord = ((Activity)p.getSelectedPlan().getPlanElements().get(0)).getCoord();
				out.write(
						kant + "\t" +
						zgde + "\t" +
						gebId + "\t" +
						hhnr + "\t" +
						personId + "\t" +
						gemt + "\t" +
						age + "\t" +
						gorthaus + "\t" +
						agde + "\t" +
						awbus + "\t" +
						homeCoord.getX() + "\t" + 
						homeCoord.getY());
				out.newLine();
					
					cnt++;
			}
			out.flush();
			out.flush();			
			out.flush();
			out.close();
		}catch (final IOException e) {
					throw new RuntimeException(e);
		}
		log.info("Census population written");
	}
	
	private int findMunicipality(Plan plan) {
		int municipality = -1;
		
		Coord workCoord = null;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				ActivityImpl act = (ActivityImpl)pe;
				if (act.getType().startsWith("w")) {
					workCoord = act.getCoord();
				}
			}
		}
		if (workCoord != null) {
			double minDistance = 999999999999999999.0;
			Id<Coord> closestMunicipality = this.municipalities.firstKey();
			for (Id<Coord> id : this.municipalities.keySet()) {
				Coord coord = this.municipalities.get(id);
				if (CoordUtils.calcDistance(coord, workCoord) < minDistance) {
					minDistance = CoordUtils.calcDistance(coord, workCoord);
					closestMunicipality = id;
				}
			}
			municipality = Integer.parseInt(closestMunicipality.toString());
		}
		return municipality;
	}
	
	private List<Id<Person>> writePUSPersons() {
		
		// only take every third person! PUS is smaller than census!
		List<Id<Person>> sampledPersons = new ArrayList<>();
		
		log.info("Writing PUS persons");
		try {	
			String header = "PersonId\tsurvey_day";
			final BufferedWriter out = IOUtils.getBufferedWriter(this.pusPersonsOutFile);
			out.write(header);
			out.newLine();	
				
			for (Person p : ((ScenarioImpl) this.origScenario).getPopulation().getPersons().values()) {
				if (random.nextDouble() <= sampleRate) {
					sampledPersons.add(p.getId());
					out.write(p.getId().toString() + "\t" + random.nextInt(365));
					out.newLine();
				}
			}
			out.flush();
			out.flush();			
			out.flush();
			out.close();
		}catch (final IOException e) {
					throw new RuntimeException(e);
		}
		log.info("Number of PUS persons after sampling: " + sampledPersons.size());
		return sampledPersons;
	}
			
	private void writePUSTrips(List<Id<Person>> sampledPersons) {
		log.info("Writing PUS trips");
		try {	
			String header = "PersonId\tTripId\txCoordOrigin\tyCoordOrigin\txCoordDestination\tyCoordDestination\tactivityDuration\tmode\tactivityType";
			final BufferedWriter out = IOUtils.getBufferedWriter(this.pusTripsOutFile);
			out.write(header);
			out.newLine();	
				
			for (Person p : ((ScenarioImpl) this.origScenario).getPopulation().getPersons().values()) {	
				if (!(sampledPersons.contains(p.getId()))) continue;
				
				PlanImpl plan = (PlanImpl) p.getSelectedPlan();
				
				int cnt = 0;
				for (PlanElement pe : plan.getPlanElements()) {
					String line = p.getId().toString() + "\t" + cnt + "\t";
					if (pe instanceof Leg) {
						LegImpl leg = (LegImpl)pe;
						Activity previousActivity = plan.getPreviousActivity(leg);
						Activity nextAct = plan.getNextActivity(leg);
						line += previousActivity.getCoord().getX() + "\t" + previousActivity.getCoord().getY() + "\t";
						line += nextAct.getCoord().getX() + "\t" + nextAct.getCoord().getY() + "\t";
						
						double duration = nextAct.getMaximumDuration();
						if (!(duration > 0.0)) {
							duration = Math.max(0.5 * 3600.0, 24.0 * 3600.0 - previousActivity.getEndTime());
						}
						line += duration + "\t";
						line += leg.getMode() + "\t";
						line += this.convert(nextAct.getType());
						out.write(line);
						out.newLine();
						cnt++;
					}
				}				
			}
			out.flush();
			out.flush();			
			out.flush();
			out.close();
		}catch (final IOException e) {
					throw new RuntimeException(e);
		}
	}
	
	private String convert(String type) {
		if (type.startsWith("h")) return "home";
		else if (type.startsWith("s")) return "shop";
		else if (type.startsWith("l")) return "leisure";
		else if (type.startsWith("w")) return "work";
		else return "education";
	}
		
	private void writeFacilities() {
		log.info("Writing " + ((ScenarioImpl) this.newScenario).getActivityFacilities().getFacilities().values().size() + " facilities");
		try {	
			String header = "Id\txCoord\tyCoord\tTypes";
			final BufferedWriter out = IOUtils.getBufferedWriter(this.businessCensusOutFile);
			out.write(header);
			out.newLine();	
			
			int counter = 0;
			for (ActivityFacility facility : ((ScenarioImpl) this.newScenario).getActivityFacilities().getFacilities().values()) {
				out.write(counter + "\t" + facility.getCoord().getX() + "\t" + facility.getCoord().getY() + "\t");
				
				int cnt = 0;
				for (ActivityOption actOption : facility.getActivityOptions().values()) {
					if (cnt > 0) out.write(",");
					out.write(actOption.getType());
					cnt++;
				}
				out.newLine();
				counter++;
			}
			out.flush();
			out.flush();			
			out.flush();
			out.close();
		}catch (final IOException e) {
					throw new RuntimeException(e);
		}
	}
}

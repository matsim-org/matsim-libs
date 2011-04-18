/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package herbie.creation.freight;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import utils.BuildTrees;

public class CreateFreightTraffic {
	
	private final static Logger log = Logger.getLogger(CreateFreightTraffic.class);
	private DecimalFormat formatter = new DecimalFormat("0.00");
	
	private Random randomNumberGenerator;
	private int freightOffset;
	private String lkwFile;
	private String liFile;
	private String networkfilePath;
	private String facilitiesfilePath;
	private String zonesfilePath;
	private String outpath;
	private double radius;
	private TreeMap<Id, Zone> zones = new TreeMap<Id, Zone>();
	private final int numberOfZones = 1341;
	private Id [] zoneIds = new Id[numberOfZones];
	private double roundingLimit;
	
	private QuadTree<ActivityFacility> facilityTree;
	private Statistics stats = new Statistics();
	private ScenarioImpl scenario;
	
	public static void main(String[] args) {
		if (args.length == 0) {
			log.info("Please specify a config file path!"); 
			return;
		}
		CreateFreightTraffic creator = new CreateFreightTraffic();				
		creator.init(args[0]);
		creator.create();
		creator.write();
		log.info("Creation finished ...");
	}
	
	private void init(String file) {
		Config config = new Config();
    	MatsimConfigReader matsimConfigReader = new MatsimConfigReader(config);
    	matsimConfigReader.readFile(file);   	
		long seed = Long.parseLong(config.findParam("freight", "randomSeed"));
		this.randomNumberGenerator = new Random(seed);
		this.freightOffset = Integer.parseInt(config.findParam("freight", "offset"));
		this.lkwFile = config.findParam("freight", "lkwFile");
		this.liFile = config.findParam("freight", "liFile");	
		this.networkfilePath = config.findParam("freight", "networkfilePath");
		this.facilitiesfilePath = config.findParam("freight", "facilitiesfilePath");
		this.zonesfilePath = config.findParam("freight", "zonesfilePath");
		this.outpath = config.findParam("freight", "output");
		this.radius = Double.parseDouble(config.findParam("freight", "radius"));
		this.roundingLimit = Double.parseDouble(config.findParam("freight", "roundingLimit"));
		
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(this.scenario).readFile(this.networkfilePath);
		new FacilitiesReaderMatsimV1(this.scenario).readFile(this.facilitiesfilePath);
		
		BuildTrees util = new BuildTrees();
		this.facilityTree = util.createActivitiesTree("all", this.scenario);
    }
	
	/*
	 * TODO: Get the zones (Zonierung) starting from: 900000000 (outside of KVMZH region)
	 */
	public void create() {
		this.createZones();
		// -----------------------------------------------------
		double[][] odMatrix = this.sumMatrices(this.readODMatrix(lkwFile), this.readODMatrix(liFile));
		this.createPersonsFromODMatrices(odMatrix);
		// -----------------------------------------------------
		List<ODRelation> relations = this.getHeavyRelations(odMatrix);
		CSShapeFileWriter gisWriter = new CSShapeFileWriter();
		gisWriter.writeODRelations(this.outpath, relations);
		// -----------------------------------------------------
	}
	
	private List<ODRelation> getHeavyRelations(double [][] od) {
		List<ODRelation> odRelations = new Vector<ODRelation>();
		int cnt = 0;
		for (int i = 0; i < numberOfZones; i++) {
			for (int j = 0; j < numberOfZones; j++) {
				Coord coordStart = this.zones.get(this.zoneIds[i]).getCentroidCoord();
				Coord coordEnd = this.zones.get(this.zoneIds[j]).getCentroidCoord();
				
				if (od[i][j] > this.roundingLimit) {
					odRelations.add(new ODRelation(new IdImpl(cnt), coordStart, coordEnd, od[i][j], this.zones.get(this.zoneIds[i]).getName(),
							this.zones.get(this.zoneIds[j]).getName(), this.zoneIds[i], this.zoneIds[j]));
					cnt++;
				}
			}
		}
		Collections.sort(odRelations);
		
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(this.outpath + "/heavyRelations.txt");
			for (ODRelation relation : odRelations) {
				out.write(formatter.format(relation.getWeight()) + "\tfrom: (" + relation.getOriginId().toString() + ")\t" + relation.getOriginName() + 
						"\tto (" + relation.getDestinationId().toString() + ")\t" + relation.getDestinationName() + "\n");
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return odRelations.subList(0, 1000);
	}
	
	/*
	 * TODO: Check coordinate system of centroids!
	 *  We need a GIS-guy or girl here to make the export of the centroids of "Zonierung_2009_11_19_zone" from ArcGIS
	 *  What is the coordinate system used in "Zonierung_2009_11_19_zone"? How can this be converted to the facilities coordinate system?
	 *  Should be ok: see centroids_metadata.pdf
	 *  
	 *  But: Check the coordinates! There was a strange error for the ArcGIS export of centroids!
	 */
	private void createZones() {
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(this.zonesfilePath));
			String line = bufferedReader.readLine(); //skip header
			int cnt = 0;
			while ((line = bufferedReader.readLine()) != null) {
				String parts[] = line.split("\t");				
				double x = Double.parseDouble(parts[7]);
				double y = Double.parseDouble(parts[8]);
				Id id = new IdImpl(Integer.parseInt(parts[2]));
				String name = parts[3];
				Zone zone = new Zone(id, new CoordImpl(x,y), name);
				this.zones.put(id, zone);
				cnt++;
			}  
			log.info("reading " + cnt + " zones");
		} // end try
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	/*
	 * Simple rounding produces too few trips: 
	 * This probably means that we have many origin-destination pairs with a value << 1	
	 * 
	 * First approach to correct this: Do not use 0.5 as turning point but a lower value (say 0.4) (configurable)
	 */
	private void createPersonsFromODMatrices(double[][] odMatrix) {
		int counter = 0;
		int countRoundUps = 0;
		double roundUp = 0.0;
		
		int countRoundDowns = 0;
		double roundDown = 0.0;
		
		int countRoundDownsBelow1 = 0;
		
		for (int i = 0; i < numberOfZones; i++) {
			for (int j = 0; j < numberOfZones; j++) {
				int numberOfPersons = (int) Math.floor(odMatrix[i][j] + (1.0 - this.roundingLimit));
				
				if (odMatrix[i][j] - Math.floor(odMatrix[i][j]) >= this.roundingLimit) {
					countRoundUps++;
					roundUp +=  (Math.ceil(odMatrix[i][j]) - odMatrix[i][j]);
				}
				else {
					countRoundDowns++;
					roundDown += (odMatrix[i][j] - Math.floor(odMatrix[i][j]));
					
					if (odMatrix[i][j] < 1.0) countRoundDownsBelow1++;	
				}				
				for (int k = 0; k < numberOfPersons; k++) {
					Person p = this.createPerson(i, j, counter);
					this.scenario.getPopulation().addPerson(p);
					counter++;
				}
			}
			// for debugging
			//if (counter > 4000) break;
		} 
		log.info("Created " + counter + " freight agents");
		log.info("Round ups: " + countRoundUps + " || " + roundUp);
		log.info("Round downs: " + countRoundDowns + " || " + roundDown);
		log.info("Round downs < 1.0: " + countRoundDownsBelow1);
	}
		
	private Person createPerson(int originIndex, int destinationIndex, int index) {
		Person p = new PersonImpl(new IdImpl(this.freightOffset + index));
		((PersonImpl)p).setEmployed(true);
		((PersonImpl)p).setCarAvail("always");
		((PersonImpl)p).createDesires("freight");
		((PersonImpl)p).getDesires().putActivityDuration("freight", "12:00:00");
		Zone originZone = this.zones.get(this.zoneIds[originIndex]);
		Zone destinationZone = this.zones.get(this.zoneIds[destinationIndex]);
		p.addPlan(this.createSingleFreightPlan(originZone, destinationZone));
		return p;
	}
	
	private double[][] sumMatrices(double[][] od0, double[][] od1) {
		if (od0.length != numberOfZones || od0[0].length != numberOfZones ||
				od1.length != numberOfZones || od1[0].length != numberOfZones) {
			log.error("Matrices have not the correct sizes!");
			return null;
		}
				
		double[][] od = new double[numberOfZones][numberOfZones];
		double numberOfTrips = 0.0;
		for (int i = 0; i < numberOfZones; i++) {
			for (int j = 0; j < numberOfZones; j++) {
				od[i][j] = od0[i][j] + od1[i][j];
				numberOfTrips += od[i][j];
			}
		}
		log.info("Total number of trips: " + formatter.format(numberOfTrips));
		return od;
	}
		
	private double[][] readODMatrix(String odFile) {		
		double[][] od = new double[numberOfZones][numberOfZones];
		
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(odFile));
			String line = bufferedReader.readLine(); //skip header
			
			// fill the zones id index
			String header[] = line.split("\t");
			for (int i = 0; i < numberOfZones; i++) {
				this.zoneIds[i] = new IdImpl(header[i + 1]);
			}			
			double cnt = 0.0;
			for (int i = 0; i < numberOfZones; i++) {
				line = bufferedReader.readLine();
				String parts[] = line.split("\t");
				for (int j = 0; j < numberOfZones; j++) {
					od[i][j] = Double.parseDouble(parts[j + 1]);
					cnt += od[i][j];
				}
			}  
			log.info("reading " + odFile + "\n" + formatter.format(cnt) + " trips");
		} // end try
		catch (IOException e) {
			e.printStackTrace();
		}	
		return od;		
	}
	
	/*
	 * TODO: 
	 * 	- Use a nicer time distribution (-> look into Mohits report)
	 *  See the output -> departures.png
	 */
	private Plan createSingleFreightPlan(Zone origin, Zone destination) {
		double departureTime = 5.0 * 3600.0 + this.randomNumberGenerator.nextDouble() * 15.0 * 3600.0;	
		this.stats.addDeparture(departureTime);
		
		ActivityFacility homeFacility = this.getRandomFacilityFromZone(origin);
		ActivityFacility freightFacility = this.getRandomFacilityFromZone(destination);
						
		Plan plan = new PlanImpl();
		ActivityImpl actH = new ActivityImpl("freight", homeFacility.getLinkId());
		actH.setFacilityId(homeFacility.getId());
		actH.setCoord(homeFacility.getCoord());
		
		actH.setStartTime(0.0);
		actH.setMaximumDuration(departureTime);
		actH.setEndTime(departureTime);
		plan.addActivity(actH);		
		plan.addLeg(new LegImpl("car"));
				
		ActivityImpl actFreight = new ActivityImpl("freight", freightFacility.getLinkId());
		
		actFreight.setStartTime(departureTime);
		actFreight.setMaximumDuration(24.0 * 3600.0 - departureTime);

		actFreight.setFacilityId(freightFacility.getId());
		actFreight.setCoord(freightFacility.getCoord());
		plan.addActivity(actFreight);
		return plan;
	}
	
	/*
	 *  TODO: 
	 *  	- Take into account capacities. 
	 *  	- Take into account the number of activities that already have been assigned to a specific facility
	 */
	private ActivityFacility getRandomFacilityFromZone(Zone zone) {
		Collection<ActivityFacility> candidates = 
			this.facilityTree.get(zone.getCentroidCoord().getX(), zone.getCentroidCoord().getY(), this.radius);
		
		Collections.shuffle((List<?>) candidates);
		ActivityFacility facility = candidates.iterator().next();
		this.addFreightActivity2Facility(facility);
		return facility;
	}
	
	private void addFreightActivity2Facility(ActivityFacility facility) {
		if (facility.getActivityOptions().get("freight") == null) {
			((ActivityFacilityImpl)facility).createActivityOption("freight");
			OpeningTime ot = new OpeningTimeImpl(DayType.wk, 5.0 * 3600.0, 20.0 * 3600.0);
			((ActivityFacilityImpl)facility).getActivityOptions().get("freight").addOpeningTime(ot);
		}	
	}
	
	private void write() {	
		log.info("Writing population and facilities ...");
		new File(this.outpath).mkdirs();
		this.stats.writeDepartures(this.outpath);
		new FacilitiesWriter(this.scenario.getActivityFacilities()).write(this.outpath + "facilitiesWFreight.xml.gz");
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(this.outpath + "freightplans.xml.gz");
	}
}

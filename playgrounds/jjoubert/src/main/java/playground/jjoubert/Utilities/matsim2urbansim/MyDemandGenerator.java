/* *********************************************************************** *
 * project: org.matsim.*
 * MyDemandGenerator.java
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

package playground.jjoubert.Utilities.matsim2urbansim;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.utils.gis.matsim2esri.plans.SelectedPlans2ESRIShape;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Point;

public class MyDemandGenerator {
	private final static Logger log = Logger.getLogger(MyDemandGenerator.class);
	private File inputFile;
	private File shapefile;
	private File networkFile;
	private File outputFile;
	private Scenario scenario;
	
	public MyDemandGenerator(String input, String shapefile, String network, String output) {
		this(input, shapefile, network, output, false);
	}
	
	public MyDemandGenerator(String input, String shapefile, String network, String output, boolean overwrite) {
		File fIn = new File(input);
		if(!fIn.exists()){
			throw new RuntimeException("The input file " + input + " does not exist");
		} else{
			this.inputFile = fIn;
		}
		File fShapefile = new File(shapefile);
		if(!fShapefile.exists()){
			throw new RuntimeException("The shapefile " + input + " does not exist");			
		} else{
			this.shapefile = fShapefile;
		}
		File fNetwork = new File(network);
		if(!fNetwork.exists()){
			throw new RuntimeException("The network file " + network + " does not exist");
		} else{
			this.networkFile = fNetwork;
		}
		outputFile = new File(output);
		if(outputFile.exists()){
			if(overwrite){
				log.warn("The output file " + output + " exists and will be overwritten");
			} else{
				throw new RuntimeException("Output file exists and must not be overwritten");
			}
		}		
	}
	
	public void generateDemand(Map<Id, MyZone> zones){
		// TODO Complete.
		log.info("Generating a synthetic population:");
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population p = scenario.getPopulation();
		
		NetworkReaderMatsimV1 nr = new NetworkReaderMatsimV1(scenario);
		try {
			nr.parse(networkFile.getAbsolutePath());
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		NetworkImpl ni = (NetworkImpl) scenario.getNetwork(); 
		
		try {
			BufferedReader br = IOUtils.getBufferedReader(inputFile.getAbsolutePath());
			try{
				@SuppressWarnings("unused")
				String header = br.readLine();
				String line = null;
				while((line = br.readLine()) != null){
					String[] entry = line.split(",");
					String agentId = entry[0];
					Person agent = new PersonImpl(new IdImpl(agentId));
					Plan plan = new PlanImpl(agent);
					
					/*
					 * Generate the start-of-day home activity. Some criteria:
					 *    - must end between 05:55 and 06:05;
					 * TODO Check that "interiorpoint" is a random point
					 */
					String homeId = entry[1];
					MyZone homeZone = zones.get(new IdImpl(homeId));
					Point pHome = homeZone.getInteriorPoint();
					Coord cHome = new CoordImpl(pHome.getX(), pHome.getY());
					Link lHome = ni.getNearestRightEntryLink(cHome);
					Activity homeStart = new ActivityImpl("home", cHome, lHome.getId());
					homeStart.setStartTime(0);
					homeStart.setEndTime(21595 + Math.random()*10.0);
					
					/*
					 * Generate travel leg from home to work.
					 */
					Leg fromHome = new LegImpl(TransportMode.car);
					
					/*
					 * Generate work activity. some criteria:
					 * 	- must be 9-hours long;
					 *  - set to start at 07h00;
					 */
					String workId = entry[2];
					MyZone workZone = zones.get(new IdImpl(workId));
					Point pWork = workZone.getInteriorPoint();
					Coord cWork = new CoordImpl(pWork.getX(), pWork.getY());
					Link lWork = ni.getNearestRightEntryLink(cWork);
					Activity work = new ActivityImpl("work", cWork, lWork.getId());
					work.setStartTime(25200);
					work.setEndTime(work.getStartTime() + 32400);
					
					/*
					 * Generate travel leg from work back to home.
					 */
					Leg toHome = new LegImpl(TransportMode.car);
					
					/*
					 * Generate the end-of-day home activity.
					 */
					Activity homeEnd = new ActivityImpl("home", cHome, lHome.getId());
					homeEnd.setStartTime(work.getEndTime()+1800);
					homeEnd.setEndTime(86399);
					
					plan.addActivity(homeStart);
					plan.addLeg(fromHome);
					plan.addActivity(work);
					plan.addLeg(toHome);
					plan.addActivity(homeEnd);
					agent.addPlan(plan);
					p.addPerson(agent);		
				}
			} finally{
				br.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Total number of agents created: " + p.getPersons().size());
				
		PopulationWriter pw = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		pw.writeFileV4(outputFile.getAbsolutePath());
		
		SelectedPlans2ESRIShape sh = new SelectedPlans2ESRIShape(scenario.getPopulation(), scenario.getNetwork(), MGC.getCRS("WGS84_UTM36S"), outputFile.getParent());
		String outputFolder = outputFile.getParentFile().getAbsolutePath();
		log.info("Writing plans as shapefile to " + outputFolder);
		sh.setWriteActs(true);
		sh.setWriteLegs(false);
		sh.write();
	}
	
	
	/**
	 * Implements the <code>MyDemandGenerator</code> class. 
	 * @param args a String-array containing the following parameters, and in 
	 * 		  the following order:
	 * <ol>
	 * 	<li> the absolute path of the input filename;
	 * 	<li> the absolute pathname of the shapefile associated with the input zones;
	 * 	<li> the absolute pathname of the network file to use;
	 * 	<li> the absolute path of the output filename; and 
	 * 	<li> (optional) logical argument to control if the output file can be 
	 * 		 overwritten in the case of the file already existing. If not passed,
	 * 		 then assumed to be <code>false</code>.
	 * </ol>
	 */
	public static void main(String[] args) {
		log.info("===================================================================");
		log.info("  Generating a plans file from an UrbanSim population query. ");
		log.info("-------------------------------------------------------------------");

		MyDemandGenerator mdg = new MyDemandGenerator(args[0], args[1], args[2], args[3], Boolean.parseBoolean(args[4]));
		MyZoneReader mzr = new MyZoneReader(mdg.shapefile.getAbsolutePath());
		mzr.readZones(1);
		Map<Id,MyZone> zoneMap = mzr.getZoneMap();
		
		mdg.generateDemand(zoneMap);
		
		log.info("-------------------------------------------------------------------");
		log.info("  Process completed.");
		log.info("===================================================================");
	}
	
	

}

/* *********************************************************************** *
 * project: org.matsim.*
 * MyPlanJoiner.java
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

package playground.jjoubert.Utilities;

import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.population.algorithms.XY2Links;
import org.xml.sax.SAXException;

public class MyPlanMerger {
	private static final Logger log = Logger.getLogger(MyPlanMerger.class);
	private String baseFile;
	private String addFile;
	private String addNumber;
	private String outputFile;
	private String networkFile;
	private Scenario sc;
	
	/**
	 * Class to merge one {@link Population} file with another. The first is referred 
	 * to as the <i>base file</i>, and the second {@link Population} file is added 
	 * to the first.
	 * @param args {@link String}[] of arguments are all required, and in the 
	 * 		following order:
	 * <ol>
	 *   <li> the absolute path of the base {@link Population} file;
	 *   <li> the absolute path of the {@link Population} file to be added to the 
	 *   		base file;
	 *   <li> the first number to be used as {@link Id} for {@link Person}s when
	 *   		added to the base file;
	 *   <li> the absolute path of the merged {@link Population} file; and
	 *   <li> the {@link Network} file to use when assigning a {@link Link} 
	 *   		{@link Id} to each {@link Activity} of each {@link Person}
	 *   		from both {@link Population} files.
	 * </ol>
	 */
	public static void main(String[] args) {
		MyPlanMerger mpj = new MyPlanMerger();
		if(args.length == 5){
			mpj.baseFile = args[0];
			mpj.addFile = args[1];
			mpj.addNumber = args[2];
			mpj.outputFile = args[3];
			mpj.networkFile = args[4];
		} else{
			throw new IllegalArgumentException("Incorrect number of arguments");
		}
		log.info("");
		log.info("Joining the following plans:");
		log.info("         Base file: " + mpj.baseFile);
		log.info("          New file: " + mpj.addFile);
		log.info("  Starting with Id: " + mpj.addNumber);
		log.info("     Using network: " + mpj.networkFile);
		log.info("        Writing to: " + mpj.outputFile);
		log.info("");

		mpj.preparePlanMerger();
		mpj.mergePlans();
		
		log.info("------------------------------");
		log.info("          Completed");
		log.info("==============================");
	}
	
	
	public MyPlanMerger() {
		log.info("Successfully created the PlanJoiner.");
	}
	
	/**
	 * The <i>base</i> {@link Population} and the {@link Network} is read, and
	 * each {@link Activity} of each {@link Person} is assigned a {@link Link}{@link Id}
	 * using {@link XY2Links#run(Plan)}.
	 */
	public void preparePlanMerger(){
		sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimPopulationReader mpr = new MatsimPopulationReader(sc);
		NetworkReaderMatsimV1 nr = new NetworkReaderMatsimV1(sc);
		try {
			mpr.parse(baseFile);
			nr.parse(networkFile);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Successfully created the base scenario with plans and network.");
		XY2Links xy = new XY2Links((NetworkImpl) sc.getNetwork());
		for(Id id : sc.getPopulation().getPersons().keySet()){
			Person p = sc.getPopulation().getPersons().get(id);
			xy.run(p.getSelectedPlan());
		}
	}
	
	/**
	 * Add each person from the additional file to the <i>base</i> file, assigning
	 * each {@link Activity} to the {@link Network} using {@link XY2Links#run(Plan)}.
	 * <b><i>Note:</i></b> A new person is created with an {@link Id} based on the
	 * given initial number. Only the {@link Person}'s selected plan is used. In the 
	 * end, the merged {@link Population} is written to the given output file.
	 */
	public void mergePlans(){
		PopulationFactory pf = sc.getPopulation().getFactory();
		int nextId = Integer.parseInt(addNumber);
		XY2Links xy = new XY2Links((NetworkImpl) sc.getNetwork());

		Scenario scAdd = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		MatsimPopulationReader mprAdd = new MatsimPopulationReader(scAdd);
		
		try {
			mprAdd.parse(addFile);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Map<Id, ? extends Person> peopleToAdd = scAdd.getPopulation().getPersons();
		for (Id idToAdd : peopleToAdd.keySet()) {
			Person p = pf.createPerson(new IdImpl(nextId));
			Plan plan = peopleToAdd.get(idToAdd).getSelectedPlan();
			xy.run(plan);
			p.addPlan(plan);
			sc.getPopulation().addPerson(p);
			nextId++;
		}
		
		PopulationWriter pw = new PopulationWriter(sc.getPopulation(), sc.getNetwork());
		pw.write(outputFile);
	}

}

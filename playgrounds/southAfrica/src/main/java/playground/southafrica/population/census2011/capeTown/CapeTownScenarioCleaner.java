/* *********************************************************************** *
 * project: org.matsim.*
 * CapeTownScenarioCleaner.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package playground.southafrica.population.census2011.capeTown;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.southafrica.utilities.Header;

/**
 * Once all the population (person and commercial) construction is finished, 
 * this class cleans everything up by joining the persons and commercial 
 * vehicles, editing the network to ensure all routed modes are addressed, and
 * some (possible) other cleaning occurs.<br><br>
 * 
 * This class is basically just a repeatable script of area-specific edits that
 * must be done for the City of Cape Town scenario. It requires the following 
 * files to be present in the given input folder:
 * <ul>
 * 		<li><code>persons.xml.gz</code>;
 * 		<li><code>personAttributes.xml.gz</code>;
 * 		<li><code>commercial.xml.gz</code>;
 * 		<li><code>commercialAttributes.xml.gz</code>.
 * </ul>
 * The output is a single combined population made up of the following files:
 * <ul>
 * 		<li><code>population.xml.gz</code>;
 * 		<li><code>populationAttributes.xml.gz</code>; and
 * 		<li><code>facilities.xml.gz</code>.
 * </ul>
 * 
 * @author jwjoubert
 */
public class CapeTownScenarioCleaner {
	final private static Logger LOG = Logger.getLogger(CapeTownScenarioCleaner.class);
	private static List<String> modes = new ArrayList<>();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(CapeTownScenarioCleaner.class.toString(), args);
		
		/* Check the necessary population input files. */
		String folder = args[0];
		folder = folder + (folder.endsWith("/") ? "" : "/");
		checkFiles(folder);
		
		Scenario sc = joinPopulations(folder);
		
		String network = args[1];
		updateNetworkModes(sc, folder, network);
		
		
		/* Write the final population to file. */
		new PopulationWriter(sc.getPopulation()).write(folder + "population.xml.gz");
		new ObjectAttributesXmlWriter(sc.getPopulation().getPersonAttributes()).writeFile(folder + "populationAttributes.xml.gz");
		
		Header.printFooter();
	}
	
	private static void updateNetworkModes(Scenario sc, String folder, String network){
		LOG.info("Ensuring network and config has all routed modes.");
		new MatsimNetworkReader(sc.getNetwork()).parse(network);
		
		/* Update the network modes. */
		String[] networkModes = {"car", "commercial"};
		Set<String> modes = new HashSet<>(Arrays.asList(networkModes));
		for(Link link : sc.getNetwork().getLinks().values()){
			link.setAllowedModes(modes);
		}
		
		/* Update the config file accordingly. */
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, folder + "config.xml");
		config.qsim().setMainModes(modes);
		config.plansCalcRoute().setNetworkModes(modes);
		/* Add the teleported modes. */
		ModeRoutingParams ride = new ModeRoutingParams("ride");
		ride.setBeelineDistanceFactor(1.3);
		ride.setTeleportedModeFreespeedFactor(0.8); /* Free speed-based. */
		config.plansCalcRoute().addModeRoutingParams(ride);
		ModeRoutingParams taxi = new ModeRoutingParams("taxi");
		taxi.setBeelineDistanceFactor(1.3);
		taxi.setTeleportedModeSpeed(50.0 / 3.6);
		config.plansCalcRoute().addModeRoutingParams(taxi);
		ModeRoutingParams brt = new ModeRoutingParams("brt");
		brt.setBeelineDistanceFactor(1.3);
		brt.setTeleportedModeSpeed(50.0 / 3.6);
		config.plansCalcRoute().addModeRoutingParams(brt);
		ModeRoutingParams rail = new ModeRoutingParams("rail");
		rail.setBeelineDistanceFactor(1.3);
		rail.setTeleportedModeSpeed(20.0 / 3.6);
		config.plansCalcRoute().addModeRoutingParams(rail);
		ModeRoutingParams walk = new ModeRoutingParams("walk");
		walk.setBeelineDistanceFactor(1.3);
		walk.setTeleportedModeSpeed(2.0 / 3.6);
		config.plansCalcRoute().addModeRoutingParams(walk);
		ModeRoutingParams cycle = new ModeRoutingParams("cycle");
		cycle.setBeelineDistanceFactor(1.3);
		cycle.setTeleportedModeSpeed(2.0 / 3.6);
		config.plansCalcRoute().addModeRoutingParams(cycle);
		ModeRoutingParams other = new ModeRoutingParams("other");
		other.setBeelineDistanceFactor(1.3);
		other.setTeleportedModeSpeed(30.0 / 3.6);
		config.plansCalcRoute().addModeRoutingParams(other);
		
		new ConfigWriter(config).write(folder + "config.xml");

		new NetworkWriter(sc.getNetwork()).write(network);
		LOG.info("Done updating network modes.");
	}
	
	private static Scenario joinPopulations(String folder){
		LOG.info("joining the person and commercial vehicle populations...");
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationFactory pf = sc.getPopulation().getFactory();
		ActivityFacilitiesFactory aff = sc.getActivityFacilities().getFactory();
		
		/* Parse the persons. */
		Scenario scPersons = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(scPersons).parse(folder + "persons.xml.gz");
		new ObjectAttributesXmlReader(scPersons.getPopulation().getPersonAttributes()).parse(folder + "personAttributes.xml.gz");
		for(Id<Person> id : scPersons.getPopulation().getPersons().keySet()){
			Person person = pf.createPerson(Id.createPersonId("coct_p_" + id.toString()));
			PlanImpl plan = new PlanImpl();
			plan.copyFrom(scPersons.getPopulation().getPersons().get(id).getSelectedPlan());
			/* Check and add modes. */
			for(PlanElement pe : plan.getPlanElements()){
				if(pe instanceof Leg){
					Leg leg = (Leg)pe;
					if(!modes.contains(leg.getMode())){
						modes.add(leg.getMode());
					}
				}
			}
			person.addPlan(plan);
			sc.getPopulation().addPerson(person);
			
			/* Add all the person's attributes. */
			addAttribute(scPersons, id, sc, person.getId(), "age");
			addAttribute(scPersons, id, sc, person.getId(), "gender");
			addAttribute(scPersons, id, sc, person.getId(), "householdId");
			addAttribute(scPersons, id, sc, person.getId(), "population");
			addAttribute(scPersons, id, sc, person.getId(), "relationship");
			addAttribute(scPersons, id, sc, person.getId(), "school");
			sc.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), "subpopulation", "private");
		}
		scPersons = null;
		
		/* Parse the commercial vehicles. */
		Scenario scCom = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(scCom).parse(folder + "commercial.xml.gz");
		new ObjectAttributesXmlReader(scCom.getPopulation().getPersonAttributes()).parse(folder + "commercialAttributes.xml.gz");
		for(Id<Person> id : scCom.getPopulation().getPersons().keySet()){
			String[] sa = id.toString().split("_");
			Person person = pf.createPerson(Id.createPersonId("coct_c_" + sa[1]));
			PlanImpl plan = new PlanImpl();
			plan.copyFrom(scCom.getPopulation().getPersons().get(id).getSelectedPlan());
			for(PlanElement pe : plan.getPlanElements()){
				if(pe instanceof Leg){
					/* Check and add modes. */
					Leg leg = (Leg)pe;
					if(!modes.contains(leg.getMode())){
						modes.add(leg.getMode());
					}
				} else if(pe instanceof Activity){
					/* Check the facilities. */
					Activity act = (Activity)pe;
					Id<ActivityFacility> fid = act.getFacilityId();
					ActivityFacility facility;
					if(!sc.getActivityFacilities().getFacilities().containsKey(fid)){
						facility = aff.createActivityFacility(fid, act.getCoord());
						sc.getActivityFacilities().addActivityFacility(facility);
					} else{
						facility = sc.getActivityFacilities().getFacilities().get(fid);
					}
					if(!facility.getActivityOptions().containsKey(act.getType())){
						ActivityOption option = aff.createActivityOption(act.getType());
						facility.addActivityOption(option);
					}
				}
			}
			person.addPlan(plan);
			sc.getPopulation().addPerson(person);
			
			/* Add all the commercial vehicle's attributes. */
			addAttribute(scCom, id, sc, person.getId(), "subpopulation");
		}
		scCom = null;
		
		LOG.info("The following modes were observed:");
		for(String s : modes){
			LOG.info("  " + s);
		}
		LOG.info("Done joining the populations.");
		return sc;
	}
	
	
	private static void addAttribute(
			Scenario oldSc, Id<Person> oldId, 
			Scenario newSc, Id<Person> newId,
			String attribute){
		newSc.getPopulation().getPersonAttributes().putAttribute(newId.toString(), attribute, oldSc.getPopulation().getPersonAttributes().getAttribute(oldId.toString(), attribute));
	}
	
	
	private static void checkFiles(String folder){
		boolean ePerson = checkFile(new File(folder + "persons.xml.gz"));
		boolean ePersonAttributes = checkFile(new File(folder + "personAttributes.xml.gz"));
		boolean eCommercial = checkFile(new File(folder + "commercial.xml.gz"));
		boolean eCommercialAttributes = checkFile(new File(folder + "commercialAttributes.xml.gz"));
		boolean eConfig = checkFile(new File(folder + "config.xml"));
		if(ePerson || ePersonAttributes || eCommercial || eCommercialAttributes
				|| eConfig){
			try {
				throw new FileNotFoundException("Necessary input files are not in place.");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new RuntimeException("Check input files in " + folder);
			}
		}
	}
	
	private static boolean checkFile(File f){
		boolean error = false;
		if(!f.exists()){
			error = true;
			LOG.error(f.getAbsolutePath() + " is not available.");
		}
		return error;
	}

}

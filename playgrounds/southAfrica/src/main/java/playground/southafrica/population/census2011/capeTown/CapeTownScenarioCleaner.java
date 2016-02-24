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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
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
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.households.Household;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.southafrica.population.census2011.attributeConverters.CoordConverter;
import playground.southafrica.utilities.FileUtils;
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
		
		String folder = args[0];
		folder = folder + (folder.endsWith("/") ? "" : "/");

		/* Copy the person and commercial vehicle attribute files from the 
		 * work-in-progress folder. */
		try {
			FileUtils.copyFile(
					new File(folder + "wip/freightAttributes.xml.gz"), 
					new File(folder + "commercialAttributes.xml.gz"));
			FileUtils.copyFile(
					new File(folder + "wip/populationAttributes.xml.gz"), 
					new File(folder + "personAttributes.xml.gz"));
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not copy attribute files.");
		}
		
		/* Check the necessary population input files. */
		checkFiles(folder);
		
		String personsCRS = args[1];
		String freightCRS = args[2];
		String wantedCRS = args[3];
		Scenario sc = joinPopulations(folder, personsCRS, freightCRS, wantedCRS);
		
		String network = args[4];
		updateNetworkModes(sc, folder, network);
		
		sc = cleanupHouseholds(sc, folder, personsCRS, wantedCRS);
		
		/* Write the final population to file. */
		new PopulationWriter(sc.getPopulation()).write(folder + "population.xml.gz");
		new ObjectAttributesXmlWriter(sc.getPopulation().getPersonAttributes()).writeFile(folder + "populationAttributes.xml.gz");
		new FacilitiesWriter(sc.getActivityFacilities()).write(folder + "facilities.xml.gz");
		new HouseholdsWriterV10(sc.getHouseholds()).writeFile(folder + "households.xml.gz");
		ObjectAttributesXmlWriter oaw = new ObjectAttributesXmlWriter(sc.getHouseholds().getHouseholdAttributes());
		oaw.putAttributeConverter(Coord.class, new CoordConverter());
		oaw.writeFile(folder + "householdAttributes.xml.gz");
		
		Header.printFooter();
	}
	
		
	/**
	 * The households contain the member IDs. But in this class we've adapted
	 * the person IDs to distinguish between the persons and the commercial 
	 * vehicles. This class now just adapts the member IDs for all households.
	 * 
	 * @param folder
	 */
	private static Scenario cleanupHouseholds(Scenario sc, String folder, 
			String personCRS, String wantedCRS){
		LOG.info("Adjusting household member IDs and home coordinates...");
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(personCRS, wantedCRS);
		
		new HouseholdsReaderV10(sc.getHouseholds()).parse(folder + "wip/households.xml.gz");
		ObjectAttributesXmlReader oar = new ObjectAttributesXmlReader(sc.getHouseholds().getHouseholdAttributes());
		oar.putAttributeConverter(Coord.class, new CoordConverter());
		oar.parse(folder + "wip/householdAttributes.xml.gz");
		
		for(Household hh : sc.getHouseholds().getHouseholds().values()){
			int householdSize = hh.getMemberIds().size();
			for(int i = 0; i < householdSize; i++){
				String oldId = hh.getMemberIds().get(0).toString();
				Id<Person> newId = Id.createPersonId("coct_p_" + oldId);
				hh.getMemberIds().remove(0);
				hh.getMemberIds().add(newId);
			}
			
			/* Update the household's home coordinate. */
			Coord oldCoord = (Coord) sc.getHouseholds().getHouseholdAttributes().getAttribute(hh.getId().toString(), "homeCoord");
			sc.getHouseholds().getHouseholdAttributes().putAttribute(hh.getId().toString(), "homeCoord", ct.transform(oldCoord));
		}
		
		LOG.info("Done adjusting household member IDs and home coordinates.");
		return sc;
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
		
		ModeRoutingParams ride = config.plansCalcRoute().getOrCreateModeRoutingParams("ride");
		ride = config.plansCalcRoute().getModeRoutingParams().get("ride");
		ride.setBeelineDistanceFactor(1.3);
		ride.setTeleportedModeFreespeedFactor(0.8); /* Free speed-based. */
		ModeRoutingParams taxi = config.plansCalcRoute().getOrCreateModeRoutingParams("taxi");
		taxi.setBeelineDistanceFactor(1.3);
		taxi.setTeleportedModeSpeed(50.0 / 3.6);
		ModeRoutingParams brt = config.plansCalcRoute().getOrCreateModeRoutingParams("brt");
		brt.setBeelineDistanceFactor(1.3);
		brt.setTeleportedModeSpeed(50.0 / 3.6);
		ModeRoutingParams bus = config.plansCalcRoute().getOrCreateModeRoutingParams("bus");
		bus.setBeelineDistanceFactor(1.3);
		bus.setTeleportedModeSpeed(50.0 / 3.6);
		ModeRoutingParams rail = config.plansCalcRoute().getOrCreateModeRoutingParams("rail");
		rail.setBeelineDistanceFactor(1.3);
		rail.setTeleportedModeSpeed(20.0 / 3.6);
		ModeRoutingParams walk = config.plansCalcRoute().getOrCreateModeRoutingParams("walk");
		walk.setBeelineDistanceFactor(1.3);
		walk.setTeleportedModeSpeed(2.0 / 3.6);
		ModeRoutingParams cycle = config.plansCalcRoute().getOrCreateModeRoutingParams("cycle");
		cycle.setBeelineDistanceFactor(1.3);
		cycle.setTeleportedModeSpeed(2.0 / 3.6);
		ModeRoutingParams other = config.plansCalcRoute().getOrCreateModeRoutingParams("other");
		other.setBeelineDistanceFactor(1.3);
		other.setTeleportedModeSpeed(30.0 / 3.6);
		
		new ConfigWriter(config).write(folder + "config.xml");
		new NetworkWriter(sc.getNetwork()).write(network);
		LOG.info("Done updating network modes.");
	}
	
	private static Scenario joinPopulations(String folder,
			String personCRS, String freightCRS, String wantedCRS){
		LOG.info("Joining the person and commercial vehicle populations...");

		CoordinateTransformation ctPersons = TransformationFactory.getCoordinateTransformation(personCRS, wantedCRS);
		CoordinateTransformation ctFreight = TransformationFactory.getCoordinateTransformation(freightCRS, wantedCRS);
		
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
			for(PlanElement pe : plan.getPlanElements()){
				/* Check and add modes. */
				if(pe instanceof Leg){
					Leg leg = (Leg)pe;
					if(!modes.contains(leg.getMode())){
						modes.add(leg.getMode());
					}
				} else if (pe instanceof Activity){
					/* Transform the activity locations. */
					ActivityImpl act = (ActivityImpl)pe;
					Coord oldCoord = act.getCoord();
					act.setCoord(ctPersons.transform(oldCoord));
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
					
					/* Update the coordinate. */
					Coord oldCoord = act.getCoord();
					((ActivityImpl)act).setCoord(ctFreight.transform(oldCoord));
					
					/* Since chopStart and chopEnd activities will not have 
					 * facility IDs, they need to be ignored in the next 
					 * portion of the code. */
					if(fid != null){
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

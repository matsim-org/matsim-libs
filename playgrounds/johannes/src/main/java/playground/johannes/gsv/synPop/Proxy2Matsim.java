/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.synpop.processing.TaskRunner;
import playground.johannes.sna.util.ProgressLogger;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.PlainPerson;

import java.util.ArrayList;
import java.util.List;

/**
 * @author johannes
 * 
 */
public class Proxy2Matsim {

	private static final Logger logger = Logger.getLogger(Proxy2Matsim.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		Population pop = scenario.getPopulation();
		PopulationFactory factory = pop.getFactory();

		FacilitiesReaderMatsimV1 facReader = new FacilitiesReaderMatsimV1(scenario);
		facReader.readFile(args[1]);
		ActivityFacilities facilities = scenario.getActivityFacilities();

		XMLParser parser = new XMLParser();
		parser.setValidating(false);
		parser.parse(args[0]);

		TaskRunner.run(new Convert2MatsimModes(), parser.getPersons());

		ProgressLogger.init(parser.getPersons().size(), 1, 10);

		logger.info(String.format("Loaded %s persons.", parser.getPersons().size()));

		int legs = 0;
		int plans = 0;

		for (PlainPerson plainPerson : parser.getPersons()) {
			Person person = factory.createPerson(Id.create(plainPerson.getId(), Person.class));
			pop.addPerson(person);

			Episode proxyPlan = plainPerson.getPlan();
			Plan plan = factory.createPlan();
			person.addPlan(plan);
			plans++;

			for (int i = 0; i < proxyPlan.getActivities().size(); i++) {
				Attributable proxyAct = proxyPlan.getActivities().get(i);
				ActivityImpl act = null;

				String type = proxyAct.getAttribute(CommonKeys.ACTIVITY_TYPE);
				ActivityFacility facility = facilities.getFacilities().get(
						Id.create(proxyAct.getAttribute(CommonKeys.ACTIVITY_FACILITY), ActivityFacility.class));
				act = (ActivityImpl) factory.createActivityFromCoord(type, facility.getCoord());
				act.setFacilityId(facility.getId());
				String startTime = proxyAct.getAttribute(CommonKeys.ACTIVITY_START_TIME);
				if (startTime == null) {
					startTime = String.valueOf(i * 60 * 60 * 8); // TODO quick
																	// fix for
																	// mid
																	// journeys
				}
				act.setStartTime(Integer.parseInt(startTime));
				String endTime = proxyAct.getAttribute(CommonKeys.ACTIVITY_END_TIME);
				if (endTime == null) {
					endTime = String.valueOf((i + 1) + 60 * 60 * 7); // TODO
																		// quick
																		// fix
																		// for
																		// mid
																		// journeys
				}
				act.setEndTime(Integer.parseInt(endTime));
				plan.addActivity(act);

				if (i < proxyPlan.getLegs().size()) {
					Attributable proxyLeg = proxyPlan.getLegs().get(i);
					String mode = proxyLeg.getAttribute(CommonKeys.LEG_MODE);
					if (mode == null) {
						mode = "undefined";
					}
					Leg leg = factory.createLeg(mode);
					String val = proxyLeg.getAttribute(CommonKeys.LEG_GEO_DISTANCE);
					// if(val != null) {
					// leg.setTravelTime(Double.parseDouble(val)); //FIME:
					// temporary hack!
					List<Double> atts = (List<Double>) pop.getPersonAttributes().getAttribute(person.getId().toString(), CommonKeys.LEG_GEO_DISTANCE);
					if (atts == null) {
						atts = new ArrayList<>();
						pop.getPersonAttributes().putAttribute(person.getId().toString(), CommonKeys.LEG_GEO_DISTANCE, atts);
					}
					if (val != null)
						atts.add(Double.parseDouble(val));
					else {
						atts.add(null);
					}
					// }
					plan.addLeg(leg);
					legs++;
				}
			}

			ProgressLogger.step();
		}

		logger.info(String.format("Created %s plans and %s legs.", plans, legs));
		logger.info("Writing population...");
		PopulationWriter writer = new PopulationWriter(pop);
		String popOutFile = args[2];
		writer.write(popOutFile);

		logger.info("Writing person attributes...");
		int idx = popOutFile.lastIndexOf("/");
		String attFile = String.format("%s/attributes.xml.gz", popOutFile.substring(0, idx));
		ObjectAttributesXmlWriter oaWriter = new ObjectAttributesXmlWriter(pop.getPersonAttributes());
		oaWriter.putAttributeConverter(ArrayList.class, new Converter());
		oaWriter.writeFile(attFile);

	}

	public static class Converter implements AttributeConverter<List<Double>> {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.matsim.utils.objectattributes.AttributeConverter#convert(java
		 * .lang.String)
		 */
		@Override
		public List<Double> convert(String value) {
			ArrayList<Double> atts = new ArrayList<>();
			String[] tokens = value.split(" ");
			for (String str : tokens) {
				if (str.equalsIgnoreCase("NA")) {
					atts.add(null);
				} else {
					atts.add(Double.parseDouble(str));
				}
			}
			atts.trimToSize();
			return atts;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.matsim.utils.objectattributes.AttributeConverter#convertToString
		 * (java.lang.Object)
		 */
		@Override
		public String convertToString(Object o) {
			List<Double> atts = (List<Double>) o;
			StringBuilder builder = new StringBuilder();
			for (Double d : atts) {
				if (d == null) {
					builder.append("NA");
				} else {
					builder.append(d);
				}
				builder.append(" ");
			}
			String str = builder.toString();
			str = str.trim();
			return str;
		}

	}

}

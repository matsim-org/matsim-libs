/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.vsp.openberlinscenario.planmodification;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * @author dziemke
 * 
 * Reads in a plans file and copies persons with their plans into a new plans file according to
 * configurable parameters. Then writes new plans file to a given location.
 */
public class PlanFileModifier {
	private final static Logger LOG = Logger.getLogger(PlanFileModifier.class);
	
	private String inputPlansFile;
	private String outputPlansFile;
	private double selectionProbability;
	private boolean onlyTransferSelectedPlan;
	private boolean considerHomeStayingAgents;
	private boolean includeNonSelectedStayHomePlans;
	private boolean onlyConsiderPeopleAlwaysGoingByCar; // TODO A leftover from an early, quite specific case; should be generalized
	private int maxNumberOfAgentsConsidered;
	private boolean removeLinksAndRoutes;
	private CoordinateTransformation ct;
	
	Random random = MatsimRandom.getLocalInstance();
	
	
	public static void main(String[] args) {
		// Check if args has an interpretable length
		if (args.length != 0 && args.length != 9 && args.length != 11) {
			throw new IllegalArgumentException("Arguments array must have a length of 0, 9, or 11!");
		}
		
		// Local use
//		String inputPlansFile = "../../upretoria/data/capetown/scenario_2017/original/population.xml.gz";
//		String outputPlansFile = "../../upretoria/data/capetown/scenario_2017/population_32734.xml.gz";
//		String inputPlansFile = "../../capetown/data/scenario_2017/population_32734.xml.gz";
//		String outputPlansFile = "../../capetown/data/scenario_2017/population_32734_1pct.xml.gz";
		
		
		String inputPlansFile = "../../runs-svn/open_berlin_scenario/b5_22/b5_22.output_plans.xml.gz";
		String outputPlansFile = "../../runs-svn/open_berlin_scenario/b5_22/b5_22.output_plans_no_links.xml.gz";
		
		
//		String inputPlansFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/be_5/cemdap_input/502/plans1.xml.gz";
//		String outputPlansFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/be_6/population/plans_10000.xml.gz";
		double selectionProbability = 0.1;
		boolean onlyTransferSelectedPlan = true;
//		boolean onlyTransferSelectedPlan = false;
		boolean considerHomeStayingAgents = true;
		boolean includeNonSelectedStayHomePlans = true;
		boolean onlyConsiderPeopleAlwaysGoingByCar = false;
//		int maxNumberOfAgentsConsidered = 10000;
		int maxNumberOfAgentsConsidered = 1000000;

//		boolean removeLinksAndRoutes = false;
		boolean removeLinksAndRoutes = true;
//		String inputCRS = TransformationFactory.HARTEBEESTHOEK94_LO19;
//		String outputCRS = "EPSG:32734";
		String inputCRS = null;
		String outputCRS = null;

		
		CoordinateTransformation ct;
		if (inputCRS == null && outputCRS == null) {
			ct = new IdentityTransformation();
		} else {
			ct = TransformationFactory.getCoordinateTransformation(inputCRS, outputCRS);
		}
		
		// Server use, version without CRS transformation
		if (args.length == 9) {
			inputPlansFile = args[0];
			outputPlansFile = args[1];
			selectionProbability = Double.parseDouble(args[2]);
			onlyTransferSelectedPlan = Boolean.parseBoolean(args[3]);
			considerHomeStayingAgents = Boolean.parseBoolean(args[4]);
			includeNonSelectedStayHomePlans = Boolean.parseBoolean(args[5]);
			onlyConsiderPeopleAlwaysGoingByCar = Boolean.parseBoolean(args[6]);
			maxNumberOfAgentsConsidered = Integer.parseInt(args[7]);
			removeLinksAndRoutes = Boolean.parseBoolean(args[8]);
			inputCRS = null;
			outputCRS = null;
		}
		
		// Server use, version with CRS transformation
		if (args.length == 11) {
			inputPlansFile = args[0];
			outputPlansFile = args[1];
			selectionProbability = Double.parseDouble(args[2]);
			onlyTransferSelectedPlan = Boolean.parseBoolean(args[3]);
			considerHomeStayingAgents = Boolean.parseBoolean(args[4]);
			includeNonSelectedStayHomePlans = Boolean.parseBoolean(args[5]);
			onlyConsiderPeopleAlwaysGoingByCar = Boolean.parseBoolean(args[6]);
			maxNumberOfAgentsConsidered = Integer.parseInt(args[7]);
			removeLinksAndRoutes = Boolean.parseBoolean(args[8]);
			inputCRS = args[9];
			outputCRS = args[10];
		}
		
		PlanFileModifier planFileModifier = new PlanFileModifier(inputPlansFile, outputPlansFile, selectionProbability, onlyTransferSelectedPlan,
				considerHomeStayingAgents, includeNonSelectedStayHomePlans, onlyConsiderPeopleAlwaysGoingByCar,
				maxNumberOfAgentsConsidered, removeLinksAndRoutes, ct);
		
		planFileModifier.modifyPlans();
	}
	
	
	public PlanFileModifier(String inputPlansFile, String outputPlansFile, double selectionProbability, boolean onlyTransferSelectedPlan,
			boolean considerHomeStayingAgents, boolean includeNonSelectedStayHomePlans, boolean onlyConsiderPeopleAlwaysGoingByCar,
			int maxNumberOfAgentsConsidered, boolean removeLinksAndRoutes, CoordinateTransformation ct) {
		this.inputPlansFile = inputPlansFile;
		this.outputPlansFile = outputPlansFile;
		this.selectionProbability = selectionProbability;
		this.onlyTransferSelectedPlan = onlyTransferSelectedPlan;
		this.considerHomeStayingAgents = considerHomeStayingAgents;
		this.includeNonSelectedStayHomePlans = includeNonSelectedStayHomePlans;
		this.onlyConsiderPeopleAlwaysGoingByCar = onlyConsiderPeopleAlwaysGoingByCar;
		this.maxNumberOfAgentsConsidered = maxNumberOfAgentsConsidered;
		this.removeLinksAndRoutes = removeLinksAndRoutes;
		this.onlyTransferSelectedPlan = onlyTransferSelectedPlan;
		this.ct = ct;
	}

	public void modifyPlans() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(inputPlansFile);
		Population population = scenario.getPopulation();

		Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population population2 = scenario2.getPopulation();
		
		int agentCounter = 0;
		
		
		for (Person person : population.getPersons().values()) {
			if (agentCounter < maxNumberOfAgentsConsidered) {
				
				Plan selectedPlan = person.getSelectedPlan();
				boolean copyPerson = decideIfPersonIsCopied(person, selectedPlan);
				
				// If selected according to all criteria, create a copy of the person and add it to new population
				if (copyPerson) {
					createPersonAndAddToPopulation(population2, person, selectedPlan);
					agentCounter ++;
				}
			}
		}
						
		// Write population file
		new PopulationWriter(scenario2.getPopulation(), null).write(outputPlansFile);
		LOG.info("Modified plans file contains " + agentCounter + " agents.");
		LOG.info("Modified plans file has been written to " + outputPlansFile);
	}

	private boolean decideIfPersonIsCopied(Person person, Plan selectedPlan) {
		boolean copyPerson = true;
		if (!considerHomeStayingAgents) {
			if (selectedPlan.getPlanElements().size() <= 1) {
				copyPerson = false;
			}
		}
		int numberOfPlans = person.getPlans().size();
		if (onlyConsiderPeopleAlwaysGoingByCar) {
			for (int i=0; i < numberOfPlans; i++) {
				Plan plan = person.getPlans().get(i);						
				int numberOfPlanElements = plan.getPlanElements().size();
				for (int j=0; j < numberOfPlanElements; j++) {
					if (plan.getPlanElements().get(j) instanceof Leg) {
						Leg leg = (Leg) plan.getPlanElements().get(j);
						if (!leg.getMode().equals(TransportMode.car)) {
							copyPerson = false;
						}
					}
				}
			}
		}
		if (random.nextDouble() > selectionProbability) {
			copyPerson = false;
		}
		return copyPerson;
	}

	private void createPersonAndAddToPopulation(Population population, Person person, Plan selectedPlan) {
		Id<Person> id = person.getId();
		Person person2 = population.getFactory().createPerson(id);
		
		// Keeping the attributes of a person
		for (String attributeKey : person.getAttributes().getAsMap().keySet()) {
			person2.getAttributes().putAttribute(attributeKey, person.getAttributes().getAttribute(attributeKey));
		}
		
		if (onlyTransferSelectedPlan) {
			transformCoordinates(selectedPlan);
			if (removeLinksAndRoutes) {
				removeLinksAndRoutes(selectedPlan);
			}
			person2.addPlan(selectedPlan);
			person2.setSelectedPlan(selectedPlan);
			population.addPerson(person2);
		} else {
			for (int i=0; i < person.getPlans().size(); i++) {
				boolean considerPlan = true;
				Plan plan = person.getPlans().get(i);
				int numberOfPlanElements = plan.getPlanElements().size();

				if (!plan.equals(selectedPlan) && !includeNonSelectedStayHomePlans) {
					if (numberOfPlanElements <= 1) {
						considerPlan = false;
					}
				}
				if (considerPlan) {
					transformCoordinates(plan);
					if (removeLinksAndRoutes) {
						removeLinksAndRoutes(plan);
					}
					person2.addPlan(plan);
				}
			}
			person2.setSelectedPlan(selectedPlan);
			population.addPerson(person2);
		}
	}
	
	private static void removeLinksAndRoutes(Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				((Activity) pe).setLinkId(null); // Remove link
			}
			if (pe instanceof Leg) {
				((Leg) pe).setRoute(null); // Remove route
			}
		}
	}

	private static void removePtWalksAndInteractions(Plan plan) {
		for(int i = 0; i < plan.getPlanElements().size(); i++) {
			PlanElement pe = plan.getPlanElements().get(i);
			if (pe instanceof Activity) {
				if (((Activity) pe).getType().equals("pt interaction")) {
					plan.getPlanElements().remove(pe);
					i--;
				}
			}
			if (pe instanceof Leg) {
				if (((Leg) pe).getMode().equals(TransportMode.transit_walk) ||
						((Leg) pe).getMode().equals(TransportMode.egress_walk) ||
						((Leg) pe).getMode().equals(TransportMode.access_walk)) {
					plan.getPlanElements().remove(pe);
					i--;
				}
			}
		}
	}

	private static void removeLegFollowingLegs(Plan plan) {
		for(int i = 0; i < plan.getPlanElements().size(); i++) {
			PlanElement pe = plan.getPlanElements().get(i);
			if (pe instanceof Leg) {
				PlanElement nextPe = plan.getPlanElements().get(i+1);
				if (nextPe != null && nextPe instanceof Leg) {
					plan.getPlanElements().remove(nextPe);
					i--;
				}
			}
		}
	}
	
	private void transformCoordinates(Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				((Activity) pe).setCoord(ct.transform(((Activity) pe).getCoord()));
			}
		}
	}
}
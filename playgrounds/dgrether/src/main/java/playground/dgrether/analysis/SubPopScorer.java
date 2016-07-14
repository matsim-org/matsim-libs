/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.dgrether.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.analysis.filters.population.RouteLinkFilter;
import org.matsim.contrib.analysis.filters.population.SelectedPlanFilter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.PlanCollectFromAlgorithm;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;

import playground.dgrether.events.EventsFilterManagerImpl;
import playground.dgrether.events.filters.PersonEventFilter;



/**
 * @author dgrether
 *
 */
public class SubPopScorer {

	private static final Logger log = Logger.getLogger(SubPopScorer.class);

	private List<String> linkIds;

	private Scenario scenario;


	public SubPopScorer(final String configFilename, final List<String> linkIds) {
		Config config = ConfigUtils.loadConfig(configFilename);
		MatsimRandom.reset(config.global().getRandomSeed());
		this.scenario = ScenarioUtils.createScenario(config);
		ScenarioUtils.loadScenario(scenario);
		
		this.linkIds = linkIds;
		Set<Id<Person>> idSet = filterPlans(this.scenario.getPopulation());
		calculateScore(idSet, this.scenario.getConfig());

	}

  private void calculateScore(Set<Id<Person>> idSet, Config config) {
  	String eventsFilePath = null /*filename not specified*/;
  	EventsFilterManagerImpl events = new EventsFilterManagerImpl();
  	MatsimEventsReader reader = new MatsimEventsReader(events);
  	//set the filter
  	PersonEventFilter filter = new PersonEventFilter(idSet);
  	events.addFilter(filter);
  	//add the handler to score
  	EventsToScore scorer = EventsToScore.createWithScoreUpdating(this.scenario, new CharyparNagelScoringFunctionFactory(scenario), events);

  	reader.readFile(eventsFilePath);
  	scorer.finish();
  	log.info("Size of subpopulation: " + idSet.size());
//  	log.info("Score of subpopulation: " + scorer.getAveragePlanPerformance());
	}

	private Set<Id<Person>> filterPlans(Population plans) {
		PlanCollectFromAlgorithm collector = new PlanCollectFromAlgorithm();
		RouteLinkFilter linkFilter = new RouteLinkFilter(collector);
		for (String id : this.linkIds) {
			linkFilter.addLink(Id.create(id, Link.class));
		}
		SelectedPlanFilter selectedPlanFilter = new SelectedPlanFilter(linkFilter);
		selectedPlanFilter.run(plans);
		Set<Plan> planSet = collector.getPlans();
		Set<Id<Person>> idSet = new HashSet<>(planSet.size());
		for (Plan p : planSet) {
			idSet.add(p.getPerson().getId());
		}
  	return idSet;
  }


	public static void main(String[] args) {
//		String config = "/Volumes/data/work/cvsRep/vsp-cvs/studies/arvidDaniel/input/testBasic/config.xml";
		String config = "./input/schwedenSubPopScoring/config.xml";
		String id = "20";
		List<String> linkids = new ArrayList<String>();
		linkids.add(id);
		SubPopScorer scorer = new SubPopScorer(config, linkids);
	}


}

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
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
import org.matsim.population.algorithms.PlanCollectFromAlgorithm;
import org.matsim.population.filters.RouteLinkFilter;
import org.matsim.population.filters.SelectedPlanFilter;

import playground.dgrether.events.FilteredEvents;
import playground.dgrether.events.filters.PersonEventFilter;



/**
 * @author dgrether
 *
 */
public class SubPopScorer {

	private static final Logger log = Logger.getLogger(SubPopScorer.class);

	private List<String> linkIds;

	private ScenarioLoaderImpl scenario;


	public SubPopScorer(final String config, final List<String> linkIds) {
		this.scenario = new ScenarioLoaderImpl(config);
		this.scenario.loadScenario();
		this.linkIds = linkIds;
		Set<Id> idSet = filterPlans(this.scenario.getScenario().getPopulation());
		calculateScore(idSet);

	}

  private void calculateScore(Set<Id> idSet) {
  	Config config = Gbl.getConfig();
  	String eventsFilePath = config.events().getInputFile();
  	FilteredEvents events = new FilteredEvents();
  	MatsimEventsReader reader = new MatsimEventsReader(events);
  	//set the filter
  	PersonEventFilter filter = new PersonEventFilter(idSet);
  	events.addFilter(filter);
  	//add the handler to score
  	EventsToScore scorer = new EventsToScore(this.scenario.getScenario().getPopulation(), new CharyparNagelScoringFunctionFactory(config.charyparNagelScoring()));
  	events.addHandler(scorer);

  	reader.readFile(eventsFilePath);
  	scorer.finish();
  	log.info("Size of subpopulation: " + idSet.size());
  	log.info("Score of subpopulation: " + scorer.getAveragePlanPerformance());
	}

	private Set<Id> filterPlans(PopulationImpl plans) {
		PlanCollectFromAlgorithm collector = new PlanCollectFromAlgorithm();
		RouteLinkFilter linkFilter = new RouteLinkFilter(collector);
		for (String id : this.linkIds) {
			linkFilter.addLink(new IdImpl(id));
		}
		SelectedPlanFilter selectedPlanFilter = new SelectedPlanFilter(linkFilter);
		selectedPlanFilter.run(plans);
		Set<Plan> planSet = collector.getPlans();
		Set<Id> idSet = new HashSet<Id>(planSet.size());
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

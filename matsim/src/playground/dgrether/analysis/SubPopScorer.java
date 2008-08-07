/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.events.MatsimEventsReader;
import org.matsim.gbl.Gbl;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.algorithms.PlanCollectFromAlgorithm;
import org.matsim.population.filters.RouteLinkFilter;
import org.matsim.population.filters.SelectedPlanFilter;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.scoring.EventsToScore;

import playground.dgrether.events.FilteredEvents;
import playground.dgrether.events.filters.PersonEventFilter;



/**
 * @author dgrether
 *
 */
public class SubPopScorer {

	private static final Logger log = Logger.getLogger(SubPopScorer.class);

	private List<String> linkIds;

	private ScenarioLoader scenario;


	public SubPopScorer(final String config, final List<String> linkIds) {
		this.scenario = new ScenarioLoader(config);
		this.linkIds = linkIds;
		Set<Id> idSet = filterPlans(this.scenario.getPlans());
		calculateScore(idSet);

	}

  private void calculateScore(Set<Id> idSet) {
  	String eventsFilePath = Gbl.getConfig().events().getInputFile();
  	FilteredEvents events = new FilteredEvents();
  	MatsimEventsReader reader = new MatsimEventsReader(events);
  	//set the filter
  	PersonEventFilter filter = new PersonEventFilter(idSet);
  	events.addFilter(filter);
  	//add the handler to score
  	EventsToScore scorer = new EventsToScore(this.scenario.getPlans(), new CharyparNagelScoringFunctionFactory());
  	events.addHandler(scorer);

  	reader.readFile(eventsFilePath);
  	scorer.finish();
  	log.info("Size of subpopulation: " + idSet.size());
  	log.info("Score of subpopulation: " + scorer.getAveragePlanPerformance());
	}

	private Set<Id> filterPlans(Population plans) {
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

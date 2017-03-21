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
package playground.agarwalamit.analysis.activity.scoring;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;
import org.matsim.core.scoring.functions.ScoringParameters;

import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * To get desired part of utility for each person.
 * @author amit
 */

public class UtilityByPartsAnalyzer extends AbstractAnalysisModule {
	private final boolean includeActivitScoring;
	private final boolean includeLegScoring;
	private final boolean includeMoneyScongin;
	private final boolean includeStuckAgentScoring;

	private EventsToScore events2Score;
	private Scenario sc;
	private String eventsFile;
	private final Map<Id<Person>, Double> person2Score = new HashMap<>();
	private final EventsManager events = EventsUtils.createEventsManager();

	public UtilityByPartsAnalyzer(final boolean includeActivitScoring, final boolean includeLegScoring, 
			final boolean includeMoneyScongin, final boolean includeStuckAgentScoring) {
		super(UtilityByPartsAnalyzer.class.getSimpleName());
		this.includeActivitScoring = includeActivitScoring;
		this.includeLegScoring = includeLegScoring;
		this.includeMoneyScongin = includeMoneyScongin;
		this.includeStuckAgentScoring = includeStuckAgentScoring;
	}

	public void run(final Scenario sc, final String outputDir){
		this.sc = sc;
		int lastIt = sc.getConfig().controler().getLastIteration();
		this.eventsFile = outputDir+"/ITERS/it."+lastIt+"/"+lastIt+".events.xml.gz";
		ScoringFunctionFactory sfFactory = getScoringFunctionFactory(sc);
		this.events2Score = EventsToScore.createWithScoreUpdating(sc, sfFactory, events);
		preProcessData();
		postProcessData();
	}

	@Override
	public List<EventHandler> getEventHandler() {
		return null;
	}

	@Override
	public void preProcessData() {
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
	}

	@Override
	public void postProcessData() {
		this.events2Score.finish();
		for (Id<Person> personId : this.sc.getPopulation().getPersons().keySet()){
			person2Score.put(personId, events2Score.getAgentScore(personId));
		}
	}

	@Override
	public void writeResults(String outputFolder) {
	}

	private ScoringFunctionFactory getScoringFunctionFactory(final Scenario sc){
		ScoringFunctionFactory sfFactory = new ScoringFunctionFactory() {
			final ScoringParametersForPerson parametersForPerson = new SubpopulationScoringParameters( sc );
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final ScoringParameters params = parametersForPerson.getScoringParameters( person );
				SumScoringFunction sumScoringFunction = new SumScoringFunction();
				if(includeActivitScoring)
					sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params));
				if(includeLegScoring)
					sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params,sc.getNetwork()));
				if(includeMoneyScongin)
					sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(params));
				if(includeStuckAgentScoring)
					sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
				return sumScoringFunction;
			}
		}; 
		return sfFactory;
	}
	public Map<Id<Person>, Double> getPerson2Score() {
		return person2Score;
	}
}
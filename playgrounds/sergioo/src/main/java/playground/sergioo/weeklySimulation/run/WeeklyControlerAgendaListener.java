/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
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

package playground.sergioo.weeklySimulation.run;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.*;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.PassivePlanningAgendaFactory;
import playground.sergioo.passivePlanning2012.core.population.AgendaBasePersonImpl;
import playground.sergioo.passivePlanning2012.core.population.socialNetwork.SocialNetworkReader;
import playground.sergioo.passivePlanning2012.core.replanning.ReRoutePlanStrategyFactory;
import playground.sergioo.passivePlanning2012.core.replanning.TimeAllocationMutatorPlanStrategyFactory;
import playground.sergioo.passivePlanning2012.core.replanning.TripSubtourModeChoiceStrategyFactory;
import playground.sergioo.passivePlanning2012.core.router.PRTripRouterModule;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager;
import playground.sergioo.weeklySimulation.analysis.LegHistogramListener;
import playground.sergioo.weeklySimulation.scenario.ScenarioUtils;
import playground.sergioo.weeklySimulation.scoring.CharyparNagelWeekScoringFunctionFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


/**
 * This is currently only a substitute to the full Controler. 
 *
 * @author sergioo
 */
public class WeeklyControlerAgendaListener implements StartupListener, IterationStartsListener {

	//Attributes
	private boolean createPersons;
	
	public WeeklyControlerAgendaListener(Boolean createPersons) {
		this.createPersons = createPersons;
	}
	//Methods
	//@Override
	public void notifyStartup(StartupEvent event) {
		final Controler controler = event.getControler();
        TransportModeNetworkFilter filter = new TransportModeNetworkFilter(controler.getScenario().getNetwork());
        Network net = NetworkUtils.createNetwork();
		HashSet<String> carMode = new HashSet<String>();
		carMode.add(TransportMode.car);
		filter.filter(net, carMode);
		for(ActivityFacility facility:((ScenarioImpl)controler.getScenario()).getActivityFacilities().getFacilities().values())
			((ActivityFacilityImpl)facility).setLinkId(((NetworkImpl)net).getNearestLinkExactly(facility.getCoord()).getId());
		Collection<Person> toBeAdded = new ArrayList<Person>();
		Set<String> modes = new HashSet<String>();
		modes.addAll(controler.getConfig().plansCalcRoute().getNetworkModes());
		if(controler.getConfig().transit().isUseTransit())
			modes.add("pt");
        if(createPersons) {
			boolean fixedTypes = controler.getConfig().findParam("locationchoice", "flexible_types")==null ||controler.getConfig().findParam("locationchoice", "flexible_types").equals("");
			String[] types = fixedTypes?new String[]{"home", "work"}:controler.getConfig().findParam("locationchoice", "flexible_types").split(", ");
			TripRouter tripRouter = controler.getTripRouterProvider().get();
			for(Person person: controler.getScenario().getPopulation().getPersons().values())
				toBeAdded.add(AgendaBasePersonImpl.createAgendaBasePerson(fixedTypes, types, person, tripRouter, controler.getScenario().getActivityFacilities(), new HashSet<String>(controler.getConfig().qsim().getMainModes()), modes, controler.getConfig().qsim().getEndTime()));
		}
		else
			for(Person person: controler.getScenario().getPopulation().getPersons().values())
				toBeAdded.add(AgendaBasePersonImpl.convertToAgendaBasePerson(person, controler.getScenario().getActivityFacilities(), new HashSet<String>(controler.getConfig().qsim().getMainModes()), modes, controler.getConfig().qsim().getEndTime()));
		for(Person person:toBeAdded) {
            controler.getScenario().getPopulation().getPersons().remove(person.getId());
            controler.getScenario().getPopulation().addPerson(person);
		}
	}
	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {
		
	}
	//Main
	public static void main(String[] args) {
		final Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[0]));
		new SocialNetworkReader(scenario).parse(args.length>1 ? args[1] : null);
		final Controler controler = new Controler(scenario);
		controler.getConfig().plansCalcRoute().getTeleportedModeFreespeedFactors().put("empty", 0.0);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.setScoringFunctionFactory(new CharyparNagelWeekScoringFunctionFactory(controler.getConfig().planCalcScore(), controler.getScenario()));
		controler.addControlerListener(new LegHistogramListener(controler.getEvents()));
		controler.addControlerListener(new WeeklyControlerAgendaListener(new Boolean(args[2])));
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding("ReRouteBase").toProvider(new ReRoutePlanStrategyFactory(scenario));
			}
		});
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding("TimeAllocationBase").toProvider(new TimeAllocationMutatorPlanStrategyFactory(scenario));
			}
		});
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding("TripSubtourModeChoiceBase").toProvider(new TripSubtourModeChoiceStrategyFactory(scenario));
			}
		});
	/*WaitTimeCalculator waitTimeCalculator = new WaitTimeCalculator(controler.getScenario().getTransitSchedule(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().qsim().getEndTime()-controler.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(waitTimeCalculator);
		StopStopTimeCalculator stopStopTimeCalculator = new StopStopTimeCalculator(controler.getScenario().getTransitSchedule(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().qsim().getEndTime()-controler.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(stopStopTimeCalculator);
		TransitRouterFactory transitRouterFactory = new TransitRouterWSImplFactory(controler.getScenario(), waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes());
		controler.setTransitRouterFactory(transitRouterFactory);*/
		controler.addOverridingModule(new PRTripRouterModule());
		final PassivePlannerManager passivePlannerManager = new PassivePlannerManager(controler.getConfig().global().getNumberOfThreads()-controler.getConfig().qsim().getNumberOfThreads());
		controler.addControlerListener(passivePlannerManager);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return new PassivePlanningAgendaFactory(passivePlannerManager, controler.getTripRouterProvider().get()).createMobsim(controler.getScenario(), controler.getEvents());
					}
				});
			}
		});
		controler.run();
	}

}
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.*;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;

import com.google.inject.Provider;

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
import playground.sergioo.weeklySimulation.util.misc.Time;
import playground.sergioo.weeklySimulation.util.misc.Time.Week;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


/**
 * This is currently only a substitute to the full Controler. 
 *
 * @author sergioo
 */
public class WeeklyControlerAgendaListenerTests implements StartupListener, IterationStartsListener {

	private class Closing {
		private Time.Week startDay;
		private int startTimeSeconds;
		private Time.Week endDay;
		private int endTimeSeconds;
		private String type;
		private double minLat;
		private double maxLat;
		private double minLon;
		private double maxLon;
		public Closing(Week startDay, int startTimeSeconds, Week endDay,
				int endTimeSeconds, String type, double minLat, double maxLat,
				double minLon, double maxLon) {
			super();
			this.startDay = startDay;
			this.startTimeSeconds = startTimeSeconds;
			this.endDay = endDay;
			this.endTimeSeconds = endTimeSeconds;
			this.type = type;
			this.minLat = minLat;
			this.maxLat = maxLat;
			this.minLon = minLon;
			this.maxLon = maxLon;
		}
		
	}
	private class ScenarioWeeklyPR {
		Collection<Closing> closings = new ArrayList<>();
		Collection<String> mainTypes;
		public ScenarioWeeklyPR(Collection<String> mainTypes) {
			super();
			this.mainTypes = mainTypes;
		}
		private void addClosing(Closing closing) {
			closings.add(closing);
		}
	}
	
	//Attributes
	private boolean createPersons;
	
	public WeeklyControlerAgendaListenerTests(Boolean createPersons) {
		this.createPersons = createPersons;
	}
	//Methods
	//@Override
	public void notifyStartup(StartupEvent event) {
		final MatsimServices controler = event.getServices();
        TransportModeNetworkFilter filter = new TransportModeNetworkFilter(controler.getScenario().getNetwork());
        Network net = NetworkUtils.createNetwork();
		HashSet<String> carMode = new HashSet<String>();
		carMode.add(TransportMode.car);
		filter.filter(net, carMode);
		for(ActivityFacility facility:((MutableScenario)controler.getScenario()).getActivityFacilities().getFacilities().values())
			((ActivityFacilityImpl)facility).setLinkId(NetworkUtils.getNearestLinkExactly(((Network)net),facility.getCoord()).getId());
		ScenarioWeeklyPR scenario = new ScenarioWeeklyPR(Arrays.asList(new String[]{"shop"}));
		preparePopulation(controler, scenario);
		prepareFacilities(controler, scenario);
	}
	private void preparePopulation(MatsimServices controler,	ScenarioWeeklyPR scenario) {
		
	}
	private void prepareFacilities(MatsimServices controler, ScenarioWeeklyPR scenario) {
		for(ActivityFacility facility:((MutableScenario)controler.getScenario()).getActivityFacilities().getFacilities().values()) {
			for(ActivityOption option:facility.getActivityOptions().values())
				if(scenario.mainTypes.contains(option.getType()))
					System.out.println();
		}
	}
	private void preparePopulation(MatsimServices controler) {
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
		if(event.getIteration() == 0) {
			final PassivePlannerManager passivePlannerManager = new PassivePlannerManager(event.getServices().getConfig().global().getNumberOfThreads()-event.getServices().getConfig().qsim().getNumberOfThreads());
			event.getServices().addControlerListener(passivePlannerManager);
			throw new RuntimeException();
//			event.getServices().addOverridingModule(new AbstractModule() {
//				@Override
//				public void install() {
//					bindMobsim().toProvider(new Provider<Mobsim>() {
//						@Override
//						public Mobsim get() {
//							return new PassivePlanningAgendaFactory(passivePlannerManager, event.getServices().getTripRouterProvider().get()).createMobsim(event.getServices().getScenario(), event.getServices().getEvents());
//						}
//					});
//				}
//			});
		}
	}
	//Main
	public static void main(String[] args) {
		final Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[0]));
		new SocialNetworkReader(scenario).readFile(args.length>1 ? args[1] : null);
		Controler controler = new Controler(scenario);
		controler.getConfig().plansCalcRoute().getTeleportedModeFreespeedFactors().put("empty", 0.0);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.setScoringFunctionFactory(new CharyparNagelWeekScoringFunctionFactory(controler.getConfig().planCalcScore(), controler.getScenario()));
		controler.addControlerListener(new LegHistogramListener(controler.getEvents()));
		controler.addControlerListener(new WeeklyControlerAgendaListenerTests(new Boolean(args[2])));
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
				addPlanStrategyBinding("ReRouteBase").toProvider(new ReRoutePlanStrategyFactory(scenario, tripRouterProvider));
			}
		});
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
				addPlanStrategyBinding("TimeAllocationBase").toProvider(new TimeAllocationMutatorPlanStrategyFactory(scenario, tripRouterProvider));
			}
		});
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
				addPlanStrategyBinding("TripSubtourModeChoiceBase").toProvider(new TripSubtourModeChoiceStrategyFactory(scenario, tripRouterProvider));
			}
		});
	/*WaitTimeCalculator waitTimeCalculator = new WaitTimeCalculator(services.getScenario().getTransitSchedule(), services.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (services.getConfig().qsim().getEndTime()-services.getConfig().qsim().getStartTime()));
		services.getEvents().addHandler(waitTimeCalculator);
		StopStopTimeCalculator stopStopTimeCalculator = new StopStopTimeCalculator(services.getScenario().getTransitSchedule(), services.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (services.getConfig().qsim().getEndTime()-services.getConfig().qsim().getStartTime()));
		services.getEvents().addHandler(stopStopTimeCalculator);
		TransitRouterFactory transitRouterFactory = new TransitRouterWSImplFactory(services.getScenario(), waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes());
		services.setTransitRouterFactory(transitRouterFactory);*/
		controler.addOverridingModule(new PRTripRouterModule());
		controler.run();
	}

}
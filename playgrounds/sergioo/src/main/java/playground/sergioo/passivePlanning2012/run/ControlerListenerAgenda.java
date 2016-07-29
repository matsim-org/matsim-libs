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

package playground.sergioo.passivePlanning2012.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;

import playground.sergioo.passivePlanning2012.core.population.AgendaBasePersonImpl;
import playground.sergioo.passivePlanning2012.core.population.socialNetwork.SocialNetworkReader;
import playground.sergioo.passivePlanning2012.core.scenario.ScenarioSocialNetwork;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager;

import java.util.*;


/**
 * This is currently only a substitute to the full Controler. 
 *
 * @author sergioo
 */
public class ControlerListenerAgenda implements StartupListener, IterationStartsListener {

	//Methods
	//@Override
	@Override
	public void notifyStartup(StartupEvent event) {
		MatsimServices controler = event.getServices();
        TransportModeNetworkFilter filter = new TransportModeNetworkFilter(controler.getScenario().getNetwork());
		Network net = NetworkUtils.createNetwork();
		HashSet<String> carMode = new HashSet<String>();
		carMode.add(TransportMode.car);
		filter.filter(net, carMode);
		for(ActivityFacility facility:((MutableScenario)controler.getScenario()).getActivityFacilities().getFacilities().values())
			((ActivityFacilityImpl)facility).setLinkId(NetworkUtils.getNearestLinkExactly(net,facility.getCoord()).getId());
        Map<Id<Person>, ? extends Person> persons = event.getServices().getScenario().getPopulation().getPersons();
		Collection<Person> toBeAdded = new ArrayList<Person>();
		Set<String> modes = new HashSet<String>();
		modes.addAll(controler.getConfig().plansCalcRoute().getNetworkModes());
		if(controler.getConfig().transit().isUseTransit())
			modes.add("pt");
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
		Scenario scenario = new ScenarioSocialNetwork(ConfigUtils.loadConfig(args.length>0 ? args[0] : null));
		ScenarioUtils.loadScenario(scenario);
		new SocialNetworkReader(scenario).readFile(args.length>1 ? args[1] : null);
		org.matsim.core.controler.Controler controler = new org.matsim.core.controler.Controler(scenario);
		controler.getConfig().plansCalcRoute().getTeleportedModeFreespeedFactors().put("empty", 0.0);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
	/*WaitTimeCalculator waitTimeCalculator = new WaitTimeCalculator(services.getScenario().getTransitSchedule(), services.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (services.getConfig().qsim().getEndTime()-services.getConfig().qsim().getStartTime()));
		services.getEvents().addHandler(waitTimeCalculator);
		StopStopTimeCalculator stopStopTimeCalculator = new StopStopTimeCalculator(services.getScenario().getTransitSchedule(), services.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (services.getConfig().qsim().getEndTime()-services.getConfig().qsim().getStartTime()));
		services.getEvents().addHandler(stopStopTimeCalculator);
		services.setTransitRouterFactory(new TransitRouterWSImplFactory(services.getScenario(), waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes()));*/
		//services.setScoringFunctionFactory(new CharyparNagelOpenTimesScoringFunctionFactory(services.getConfig().planCalcScore(), services.getScenario()));
		controler.addControlerListener(new ControlerListenerAgenda());
		controler.run();
	}

}
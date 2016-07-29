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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
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

import playground.sergioo.passivePlanning2012.core.population.BasePersonImpl;
import playground.sergioo.passivePlanning2012.core.scenario.ScenarioSimplerNetwork;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager;
import playground.sergioo.singapore2012.scoringFunction.CharyparNagelOpenTimesScoringFunctionFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;


/**
 * This is currently only a substitute to the full Controler. 
 *
 * @author sergioo
 */
public class ControlerListenerSocial implements StartupListener, IterationStartsListener {

	//Constants
	private final static Logger log = Logger.getLogger(ControlerListenerSocial.class);

	//Methods
	//@Override
	@Override
	public void notifyStartup(StartupEvent event) {
        TransportModeNetworkFilter filter = new TransportModeNetworkFilter(event.getServices().getScenario().getNetwork());
		Network net = NetworkUtils.createNetwork();
		HashSet<String> modes = new HashSet<String>();
		modes.add(TransportMode.car);
		filter.filter(net, modes);
		for(ActivityFacility facility:((MutableScenario)event.getServices().getScenario()).getActivityFacilities().getFacilities().values())
			((ActivityFacilityImpl)facility).setLinkId(NetworkUtils.getNearestLinkExactly(net,facility.getCoord()).getId());
        Map<Id<Person>, ? extends Person> persons = event.getServices().getScenario().getPopulation().getPersons();
		Collection<Person> toBeAdded = new ArrayList<Person>();
		/*boolean fixedTypes = event.getServices().getConfig().findParam("locationchoice", "flexible_types")==null ||event.getServices().getConfig().findParam("locationchoice", "flexible_types").equals("");
		String[] types = fixedTypes?new String[]{"home", "work"}:event.getServices().getConfig().findParam("locationchoice", "flexible_types").split(", ");
		TransitRouterFactory transitRouterFactory = new TransitRouterImplFactory(
				event.getServices().getScenario().getTransitSchedule(),
				new TransitRouterConfig(
						event.getServices().getConfig().planCalcScore(),
						event.getServices().getConfig().plansCalcRoute(),
						event.getServices().getConfig().transitRouter(),
						event.getServices().getConfig().vspExperimental()));
		TripRouterFactory tripRouterFactory = new TripRouterFactoryImpl(
				event.getServices().getScenario(),
				event.getServices().getTravelDisutilityFactory(),
				event.getServices().getTravelTimeCalculatorFactory().createTravelTimeCalculator(event.getServices().getNetwork(), event.getServices().getConfig().travelTimeCalculator()).getLinkTravelTimes(),
				new FastDijkstraFactory(),
				event.getServices().getScenario().getConfig().transit().isUseTransit() ? transitRouterFactory : null);*/
		for(Person person: persons.values())
			toBeAdded.add(BasePersonImpl.convertToBasePerson(person));
			//toBeAdded.add(BasePersonImpl.getBasePerson(fixedTypes, types, (PersonImpl)person, tripRouterFactory.createTripRouter(), ((ScenarioImpl)event.getServices().getScenario()).getActivityFacilities()));
		for(Person person:toBeAdded) {
            event.getServices().getScenario().getPopulation().getPersons().remove(person.getId());
            event.getServices().getScenario().getPopulation().addPerson(person);
		}
	}
	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {
		if(event.getIteration() == 0)
			if(event.getServices().getConfig().households().getInputFile()!=null) {
				final PassivePlannerManager passivePlannerManager = new PassivePlannerManager(1);
				event.getServices().addControlerListener(passivePlannerManager);
				throw new RuntimeException();
//				event.getServices().addOverridingModule(new AbstractModule() {
//					@Override
//					public void install() {
//						bindMobsim().toProvider(new Provider<Mobsim>() {
//							@Override
//							public Mobsim get() {
//								return new PassivePlanningSocialFactory(passivePlannerManager, new PersonHouseholdMapping(((MutableScenario) event.getServices().getScenario()).getHouseholds()), event.getServices().getTripRouterProvider().get()).createMobsim(event.getServices().getScenario(), event.getServices().getEvents());
//							}
//						});
//					}
//				});
			}
			else
				log.error("Households information is neccesary for passive planning with social");
	}

	//Main
	public  static void main(String[] args) {
		Scenario scenario = new ScenarioSimplerNetwork(ConfigUtils.loadConfig(args.length > 0 ? args[0] : null));
		ScenarioUtils.loadScenario(scenario);
		org.matsim.core.controler.Controler controler = new org.matsim.core.controler.Controler(scenario);
		controler.getConfig().plansCalcRoute().getTeleportedModeFreespeedFactors().put("empty", 0.0);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.setScoringFunctionFactory(new CharyparNagelOpenTimesScoringFunctionFactory(controler.getConfig().planCalcScore(), controler.getScenario()));
		controler.addControlerListener(new ControlerListenerSocial());
		controler.run(); 
	}

}
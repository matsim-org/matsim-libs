package ch.sbb.matsim;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;
import ch.sbb.matsim.routing.pt.raptor.RaptorStopFinder;
import ch.sbb.matsim.routing.pt.raptor.RaptorParametersForPerson;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.InitialStop;
import com.google.inject.Injector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorModule;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.timing.TimeInterpretationModule;
import org.matsim.examples.ExamplesUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;


import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class RaptorDeterminismTest {

	public static boolean comparePlan(List<? extends PlanElement> left, List<? extends PlanElement> right) {
		if (left.size() != right.size()) {
			return false;
		}
		for (int i = 0; i < left.size(); i++) {
			PlanElement leftElement = left.get(i);
			PlanElement rightElement = right.get(i);
			if (!leftElement.getClass().equals(rightElement.getClass())) {
				return false;
			}
			if (leftElement instanceof Activity leftActivity && rightElement instanceof Activity rightActivity) {
				if (!leftActivity.getEndTime().equals(rightActivity.getEndTime())) {
					return false;
				}
				if (!leftActivity.getType().equals(rightActivity.getType())) {
					return false;
				}
				if (!Optional.ofNullable(leftActivity.getLinkId()).equals(Optional.ofNullable(rightActivity.getLinkId()))) {
					return false;
				}
				if (!leftActivity.getStartTime().equals(rightActivity.getStartTime())) {
					return false;
				}
				if (!leftActivity.getMaximumDuration().equals(rightActivity.getMaximumDuration())) {
					return false;
				}
			} else if (leftElement instanceof Leg leftLeg && rightElement instanceof Leg rightLeg) {
				if (!leftLeg.getMode().equals(rightLeg.getMode())) {
					return false;
				}
				if (!leftLeg.getTravelTime().equals(rightLeg.getTravelTime())) {
					return false;
				}
				if (!leftLeg.getMode().equals("pt")) {
					continue;
				}
				Route leftRoute = leftLeg.getRoute();
				Route rightRoute = leftLeg.getRoute();
				if (leftRoute instanceof DefaultTransitPassengerRoute leftTransitPassengerRoute && rightRoute instanceof DefaultTransitPassengerRoute rightTransitPassengerRoute) {
					if (!leftTransitPassengerRoute.toString().equals(rightTransitPassengerRoute.toString())) {
						return false;
					}
					if (!leftTransitPassengerRoute.getRouteId().equals(rightTransitPassengerRoute.getRouteId())) {
						return false;
					}
					if (!leftTransitPassengerRoute.getAccessStopId().equals(rightTransitPassengerRoute.getAccessStopId())) {
						return false;
					}
					if (!leftTransitPassengerRoute.getEgressStopId().equals(rightTransitPassengerRoute.getEgressStopId())) {
						return false;
					}
				} else {
					throw new IllegalStateException();
				}
			}
		}
		return true;
	}


	static private class ComparisonInstance {

		private final List<? extends Person> personList;
		private final RaptorStopFinder raptorStopFinder;
		private final ActivityFacilities activityFacilities;
		private final RaptorParametersForPerson raptorParametersForPerson;
		private final List<TransitStopFacility> transitStopFacilities;
		private final SwissRailRaptorData swissRailRaptorData;
		private final TripRouter tripRouter;
		private final Logger logger;

		ComparisonInstance(Config config, Logger logger) {
			Scenario scenario = ScenarioUtils.createScenario(config);
			ScenarioUtils.loadScenario(scenario);
			// SwissRailRaptorModule is installed by TransitRouterModule which installed by TripRouterModule
			AbstractModule mainModule = AbstractModule.override(List.of(new TripRouterModule(), new EventsManagerModule(), new TimeInterpretationModule(), new TravelDisutilityModule(), new TravelTimeCalculatorModule()), new ScenarioByInstanceModule(scenario));
			Injector injector = org.matsim.core.controler.Injector.createInjector(config, mainModule);
			raptorStopFinder = injector.getInstance(RaptorStopFinder.class);
			activityFacilities = injector.getInstance(ActivityFacilities.class);
			raptorParametersForPerson = injector.getInstance(RaptorParametersForPerson.class);
			SwissRailRaptor swissRailRaptor = injector.getInstance(SwissRailRaptor.class);
			transitStopFacilities = new ArrayList<>(scenario.getTransitSchedule().getFacilities().values());
			swissRailRaptorData = swissRailRaptor.getUnderlyingData();
			personList = scenario.getPopulation().getPersons().values().stream().toList();
			tripRouter = injector.getInstance(TripRouter.class);
			this.logger = logger;
		}

		@Override
		public boolean equals(Object other) {
			if (! (other instanceof ComparisonInstance comparisonInstance)) {
				return false;
			}


			logger.info("Comparing order of stop facilities");
			// Comparing stop facilities order
			if(transitStopFacilities.size() != comparisonInstance.transitStopFacilities.size()) {
				return false;
			}
			for (int i = 0; i < transitStopFacilities.size(); i++) {
				if(!transitStopFacilities.get(i).getId().equals(comparisonInstance.transitStopFacilities.get(i).getId())) {
					return false;
				}
			}

			logger.info("Comparing persons and generated PT routes");
			if(this.personList.size() != comparisonInstance.personList.size()) {
				return false;
			}
			for (int personIndex = 0; personIndex < personList.size(); personIndex++) {
				if (personIndex % 1000 == 0) {
					logger.info(String.format("Person %d", personIndex));
				}
				Person referencePerson = personList.get(personIndex);
				Person otherPerson = comparisonInstance.personList.get(personIndex);

				if(!referencePerson.getId().equals(otherPerson.getId())) {
					return false;
				}

				RaptorParameters referenceRaptorParameters = raptorParametersForPerson.getRaptorParameters(referencePerson);
				RaptorParameters otherRaptorParameters = comparisonInstance.raptorParametersForPerson.getRaptorParameters(referencePerson);

				for (TripStructureUtils.Trip referenceTrip : TripStructureUtils.getTrips(referencePerson.getSelectedPlan())) {
					Facility fromFacility = FacilitiesUtils.toFacility(referenceTrip.getOriginActivity(), activityFacilities);
					Facility toFacility = FacilitiesUtils.toFacility(referenceTrip.getDestinationActivity(), activityFacilities);

					Facility otherFromFacility = FacilitiesUtils.toFacility(referenceTrip.getOriginActivity(), comparisonInstance.activityFacilities);
					Facility otherToFacility = FacilitiesUtils.toFacility(referenceTrip.getDestinationActivity(), comparisonInstance.activityFacilities);

					if(!otherFromFacility.getCoord().equals(fromFacility.getCoord()) || !otherToFacility.getCoord().equals(toFacility.getCoord())) {
						return false;
					}


					// We specifically test the RaptorStopFinder
					for (RaptorStopFinder.Direction direction : RaptorStopFinder.Direction.values()) {
						List<InitialStop> referenceInitialStops = raptorStopFinder.findStops(fromFacility, toFacility, referencePerson, referenceTrip.getOriginActivity().getEndTime().seconds(), referenceTrip.getTripAttributes(), referenceRaptorParameters, swissRailRaptorData, direction);
						List<InitialStop> sortedReferenceInitialStops = new ArrayList<>(referenceInitialStops);
						sortedReferenceInitialStops.sort(Comparator.comparing(InitialStop::toString));

						List<InitialStop> comparedInitialStops = comparisonInstance.raptorStopFinder.findStops(otherFromFacility, otherToFacility, referencePerson, referenceTrip.getOriginActivity().getEndTime().seconds(), referenceTrip.getTripAttributes(), otherRaptorParameters, comparisonInstance.swissRailRaptorData, direction);

						if(referenceInitialStops.size() != comparedInitialStops.size()) {
							return false;
						}

						List<InitialStop> sortedComparedInitialStops = new ArrayList<>(comparedInitialStops);
						sortedComparedInitialStops.sort(Comparator.comparing(InitialStop::toString));
						for (int j = 0; j < referenceInitialStops.size(); j++) {
							if(!sortedReferenceInitialStops.get(j).toString().equals(sortedComparedInitialStops.get(j).toString())) {
								return false;
							}
						}
						for (int j = 0; j < referenceInitialStops.size(); j++) {
							if(!referenceInitialStops.get(j).toString().equals(comparedInitialStops.get(j).toString())) {
								return false;
							}
						}
					}


					// Then we test the elements return by raptor
					List<? extends PlanElement> referenceElements = tripRouter.calcRoute("pt", fromFacility, toFacility, referenceTrip.getOriginActivity().getEndTime().seconds(), referencePerson, referenceTrip.getTripAttributes());


					List<? extends PlanElement> comparedElements = comparisonInstance.tripRouter.calcRoute("pt", otherFromFacility, otherToFacility, referenceTrip.getOriginActivity().getEndTime().seconds(), otherPerson, referenceTrip.getTripAttributes());
					if(!comparePlan(referenceElements, comparedElements)) {
						return false;
					}

				}
			}
			return true;
		}
	}

	@Test
	public void testRaptorDeterminism() {
		Logger logger = LogManager.getLogger(RaptorDeterminismTest.class);
		logger.info("Testing raptor determinism");
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("siouxfalls-2014"), "config_default.xml");
		Config config = ConfigUtils.loadConfig(configUrl);
		int scenarioSamples = 10;


		logger.info(String.format("Loading sample 1/%d", scenarioSamples));
		ComparisonInstance referenceSample = new ComparisonInstance(config, logger);

		for(int i=1; i<scenarioSamples; i++) {
			logger.info(String.format("Loading sample %d/%d", i+1, scenarioSamples));
			ComparisonInstance otherSample = new ComparisonInstance(config, logger);
			logger.info(String.format("Comparing sample 1 with sample %d/%d", i+1, scenarioSamples));
			assert referenceSample.equals(otherSample);
		}
	}

}

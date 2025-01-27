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


	@Test
	public void testRaptorDeterminism() {
		Logger logger = LogManager.getLogger(RaptorDeterminismTest.class);
		logger.info("Testing raptor determinism");
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("siouxfalls-2014"), "config_default.xml");
		Config config = ConfigUtils.loadConfig(configUrl);
		int scenarioSamples = 5;
		Scenario[] scenarios = new Scenario[scenarioSamples];
		RaptorStopFinder[] raptorStopFinders = new RaptorStopFinder[scenarioSamples];
		ActivityFacilities[] activityFacilities = new ActivityFacilities[scenarioSamples];
		RaptorParametersForPerson[] raptorParametersForPerson = new RaptorParametersForPerson[scenarioSamples];
		SwissRailRaptor[] swissRailRaptors = new SwissRailRaptor[scenarioSamples];
		SwissRailRaptorData[] swissRailRaptorData = new SwissRailRaptorData[scenarioSamples];
		List<TransitStopFacility>[] transitStopFacilitiesByScenario = new List[scenarioSamples];
		List<? extends Person>[] personLists = new List[scenarioSamples];
		TripRouter[] tripRouters = new TripRouter[scenarioSamples];

		logger.info(String.format("Loading scenario %d times", scenarioSamples));
		for (int scenarioIndex = 0; scenarioIndex < scenarioSamples; scenarioIndex++) {
			Scenario scenario = ScenarioUtils.createScenario(config);
			ScenarioUtils.loadScenario(scenario);

			// SwissRailRaptorModule is installed by TransitRouterModule which installed by TripRouterModule
			AbstractModule mainModule = AbstractModule.override(List.of(new TripRouterModule(), new EventsManagerModule(), new TimeInterpretationModule(), new TravelDisutilityModule(), new TravelTimeCalculatorModule()), new ScenarioByInstanceModule(scenario));

			Injector injector = org.matsim.core.controler.Injector.createInjector(config, mainModule);

			raptorStopFinders[scenarioIndex] = injector.getInstance(RaptorStopFinder.class);
			activityFacilities[scenarioIndex] = injector.getInstance(ActivityFacilities.class);
			raptorParametersForPerson[scenarioIndex] = injector.getInstance(RaptorParametersForPerson.class);
			swissRailRaptors[scenarioIndex] = injector.getInstance(SwissRailRaptor.class);
			scenarios[scenarioIndex] = scenario;
			transitStopFacilitiesByScenario[scenarioIndex] = new ArrayList<>(scenario.getTransitSchedule().getFacilities().values());
			swissRailRaptorData[scenarioIndex] = swissRailRaptors[scenarioIndex].getUnderlyingData();
			personLists[scenarioIndex] = scenario.getPopulation().getPersons().values().stream().toList();

			tripRouters[scenarioIndex] = injector.getInstance(TripRouter.class);
		}

		logger.info(String.format("Comparing stop facilities order %d", scenarioSamples));

		for (int scenarioIndex = 1; scenarioIndex < scenarioSamples; scenarioIndex++) {
			for (int i = 0; i < transitStopFacilitiesByScenario[0].size(); i++) {
				assert transitStopFacilitiesByScenario[0].get(i).getId().equals(transitStopFacilitiesByScenario[scenarioIndex].get(i).getId());
			}
		}

		logger.info("Comparing stop RaptorStopFinder.findStops alongSide ptRouter.calcRoute(pt, ...)");

		for (int personIndex = 0; personIndex < Math.min(10000, personLists[0].size()); personIndex++) {
			if (personIndex % 1000 == 0) {
				logger.info(String.format("Person %d", personIndex));
			}
			Person referencePerson = personLists[0].get(personIndex);
			RaptorParameters referenceRaptorParameters = raptorParametersForPerson[0].getRaptorParameters(referencePerson);

			for (TripStructureUtils.Trip referenceTrip : TripStructureUtils.getTrips(referencePerson.getSelectedPlan())) {
				Facility fromFacility = FacilitiesUtils.toFacility(referenceTrip.getOriginActivity(), activityFacilities[0]);
				Facility toFacility = FacilitiesUtils.toFacility(referenceTrip.getDestinationActivity(), activityFacilities[0]);

				List<? extends PlanElement> referenceElements = tripRouters[0].calcRoute("pt", fromFacility, toFacility, referenceTrip.getOriginActivity().getEndTime().seconds(), referencePerson, referenceTrip.getTripAttributes());

				for (int scenarioIndex = 1; scenarioIndex < scenarioSamples; scenarioIndex++) {

					assert personLists[scenarioIndex].get(personIndex).getId().equals(referencePerson.getId());
					Person otherPerson = scenarios[scenarioIndex].getPopulation().getPersons().get(referencePerson.getId());

					RaptorParameters otherRaptorParameters = raptorParametersForPerson[scenarioIndex].getRaptorParameters(referencePerson);
					Facility otherFromFacility = FacilitiesUtils.toFacility(referenceTrip.getOriginActivity(), activityFacilities[scenarioIndex]);
					Facility otherToFacility = FacilitiesUtils.toFacility(referenceTrip.getDestinationActivity(), activityFacilities[scenarioIndex]);

					assert otherFromFacility.getCoord().equals(fromFacility.getCoord());
					assert otherToFacility.getCoord().equals(toFacility.getCoord());

					// We specifically test the RaptorStopFinder
					for (RaptorStopFinder.Direction direction : RaptorStopFinder.Direction.values()) {
						List<InitialStop> referenceInitialStops = raptorStopFinders[0].findStops(fromFacility, toFacility, referencePerson, referenceTrip.getOriginActivity().getEndTime().seconds(), referenceTrip.getTripAttributes(), referenceRaptorParameters, swissRailRaptorData[0], direction);
						List<InitialStop> sortedReferenceInitialStops = new ArrayList<>(referenceInitialStops);
						sortedReferenceInitialStops.sort(Comparator.comparing(InitialStop::toString));

						List<InitialStop> comparedInitialStops = raptorStopFinders[scenarioIndex].findStops(otherFromFacility, otherToFacility, referencePerson, referenceTrip.getOriginActivity().getEndTime().seconds(), referenceTrip.getTripAttributes(), otherRaptorParameters, swissRailRaptorData[scenarioIndex], direction);

						assert referenceInitialStops.size() == comparedInitialStops.size();

						List<InitialStop> sortedComparedInitialStops = new ArrayList<>(comparedInitialStops);
						sortedComparedInitialStops.sort(Comparator.comparing(InitialStop::toString));
						for (int j = 0; j < referenceInitialStops.size(); j++) {
							assert sortedReferenceInitialStops.get(j).toString().equals(sortedComparedInitialStops.get(j).toString());
						}
						for (int j = 0; j < referenceInitialStops.size(); j++) {
							assert referenceInitialStops.get(j).toString().equals(comparedInitialStops.get(j).toString());
						}
					}

					// Then we test the elements return by raptor

					List<? extends PlanElement> comparedElements = tripRouters[scenarioIndex].calcRoute("pt", otherFromFacility, otherToFacility, referenceTrip.getOriginActivity().getEndTime().seconds(), otherPerson, referenceTrip.getTripAttributes());
					assert comparePlan(referenceElements, comparedElements);
				}
			}
		}
	}

}

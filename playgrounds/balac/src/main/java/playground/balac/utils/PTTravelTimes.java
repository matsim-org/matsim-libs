package playground.balac.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.*;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.*;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.router.old.TeleportationLegRouter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterNetwork;
import playground.balac.carsharing.preprocess.membership.MyLinkUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public class PTTravelTimes {
	


	private static TransitRouterFactory transitRouterFactory;
	
	public static void main(String[] args) throws IOException {
		
		Config config = ConfigUtils.createConfig();
		config.global().setNumberOfThreads(16);	// for parallel population reading
	//	config.network().setInputFile("C:/Users/balacm/Desktop/InputPt/PTWithoutSimulation/network.xml.gz");
	//	config.plans().setInputFile("/data/matsim/cdobler/2030/60.plans_without_pt_routes.xml.gz");
	//	config.plans().setInputFile("/data/matsim/cdobler/2030/plans_test.xml");
	//	config.facilities().setInputFile("C:/Users/balacm/Desktop/InputPt/PTWithoutSimulation/facilities.xml.gz");
	//	config.transit().setTransitScheduleFile("C:/Users/balacm/Desktop/InputPt/PTWithoutSimulation/schedule.20120117.ch-edited.xml.gz");
	//	config.transit().setVehiclesFile("C:/Users/balacm/Desktop/InputPt/PTWithoutSimulation/transitVehicles.ch.xml.gz");
		
		config.network().setInputFile("./network_multimodal.xml.gz");

	    config.facilities().setInputFile("./facilities.xml.gz");
	    config.transit().setTransitScheduleFile("./schedule.20120117.ch-edited.xml.gz");
	    config.transit().setVehiclesFile("./transitVehicles.ch.xml.gz");
		
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		//config.scenario().setUseKnowledge(true);
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		
		TransitRouterNetwork routerNetwork = new TransitRouterNetwork();
	    new TransitRouterNetworkReaderMatsimV1(scenario, routerNetwork).parse("./transitRouterNetwork_thinned.xml.gz");

	//	new TransitRouterNetworkReaderMatsimV1(scenario, routerNetwork).parse("C:/Users/balacm/Desktop/InputPt/PTWithoutSimulation/transitRouterNetwork_thinned.xml.gz");
		//config.planCalcScore().setUtilityOfLineSwitch(0.0);
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(config.planCalcScore(),
				config.plansCalcRoute(), config.transitRouter(), config.vspExperimental());
		
	//	transitRouterFactory = new FastTransitRouterImplFactory(scenario.getTransitSchedule(), transitRouterConfig, routerNetwork);
		transitRouterFactory = new TransitRouterImplFactory(scenario.getTransitSchedule(), transitRouterConfig, routerNetwork);
		 BufferedReader readLink = IOUtils.getBufferedReader("./coord_"+args[0] +".txt");

		    BufferedWriter outLink = IOUtils.getBufferedWriter("./travelTimesPT_"+args[0] +".txt");
	//	final BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/InputPt/PTWithoutSimulation/coord_"+args[0]+".txt");
		
	//final BufferedWriter outLink = IOUtils.getBufferedWriter("C:/Users/balacm/Desktop/InputPt/PTWithoutSimulation/travelTimesPT_"+args[0]+".txt");

		
		
		String s = readLink.readLine();
		s = readLink.readLine();
		int i = 0;
		while(s != null) {
			String[] arr = s.split("\\s");
			CoordImpl coordStart = new CoordImpl(arr[3], arr[4]);
			Link lStart = MyLinkUtils.getClosestLink(scenario.getNetwork(), coordStart);
			CoordImpl coordEnd = new CoordImpl(arr[5], arr[6]);
			Link lEnd = MyLinkUtils.getClosestLink(scenario.getNetwork(), coordEnd);
			
			PersonImpl person = new PersonImpl(new IdImpl(arr[0]));
		//	i++;
			PlanImpl plan = (PlanImpl) scenario.getPopulation().getFactory().createPlan();
			ActivityImpl act = new ActivityImpl("home", lStart.getId());
			act.setCoord(coordStart);
			//String[] arr2 = arr[6].split(":");
			//double h = Double.parseDouble(arr2[0]);
			double m = Double.parseDouble(arr[2]);
			act.setEndTime(m * 60);
			plan.addActivity(act);
			
			LegImpl leg = new LegImpl("pt");
			plan.addLeg(leg);
			
			act = new ActivityImpl("leisure", lEnd.getId());
			act.setCoord(coordEnd);
			act.setEndTime(48800);
			plan.addActivity(act);
			leg = new LegImpl("pt");
			plan.addLeg(leg);
			act = new ActivityImpl("home", lStart.getId());
			act.setCoord(coordStart);
			plan.addActivity(act);
			person.addPlan(plan);
			
			scenario.getPopulation().addPerson(person);
			s = readLink.readLine();
			
			
		}
		
		
		// keep only one plan per person
		//for (Person person : scenario.getPopulation().getPersons().values()) {
	//		((PersonImpl) person).removeUnselectedPlans();
	//	}
		
		// create pt routes
		int numThreads = 24;
		ParallelPersonAlgorithmRunner.run(scenario.getPopulation(), numThreads, new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
			@Override
			public AbstractPersonAlgorithm getPersonAlgorithm() {
				return new PrepareForSimOnlyPT(createRoutingAlgorithm(scenario));
			}
		});
		
		for(Person per: scenario.getPopulation().getPersons().values()) {
			double time = 0.0;
			Plan p = per.getPlans().get(0);
			
			for(PlanElement pe: p.getPlanElements()) {
				
				if (pe instanceof Activity) {
					if (((Activity) pe).getType().equals("leisure")) {
						
						break;
					}
				}
				else if (pe instanceof Leg) {
					
					time += ((Leg) pe).getTravelTime();
					
				}
				
			}
			
			outLink.write(per.getId() + " ");
			outLink.write(Double.toString(time));
			outLink.newLine();
			
			
		}
		outLink.flush();
		outLink.close();
		
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()
//				((ScenarioImpl) scenario).getKnowledges()).writeFileV4("/data/matsim/cdobler/2030/60.plans_with_pt_routes.xml.gz");
		//		((ScenarioImpl) scenario).getKnowledges()).writeFileV4("C:/Users/balacm/Desktop/InputPt/PTWithoutSimulation/plans_with_pt_routes_single_plan_"+args[0]+".xml.gz");
        ).writeFileV4("./plans_with_pt_routes_"+args[0]+".xml.gz");

	}
		
private static final PersonAlgorithm createRoutingAlgorithm(Scenario scenario) {
		
		PlansCalcRouteConfigGroup routeConfigGroup = scenario.getConfig().plansCalcRoute();

		TripRouter tripRouter = new TripRouter();
        if ( scenario.getConfig().scenario().isUseTransit() ) {
            TransitRouterWrapper routingModule = new TransitRouterWrapper(
            		transitRouterFactory.createTransitRouter(),
                    scenario.getTransitSchedule(),
                    scenario.getNetwork(), // use a walk router in case no PT path is found
                    new LegRouterWrapper(
                            TransportMode.transit_walk,
                            scenario.getPopulation().getFactory(),
                            new TeleportationLegRouter(
                                    ((PopulationFactoryImpl) scenario.getPopulation().getFactory()).getModeRouteFactory(),
                                    routeConfigGroup.getTeleportedModeSpeeds().get(TransportMode.walk),
                                    routeConfigGroup.getBeelineDistanceFactor())));
            for (String mode : scenario.getConfig().transit().getTransitModes()) {
                // XXX one can't check for inconsistent setting here...
                // because the setting is inconsistent by default (defaults
                // set a teleportation setting for pt routing, which is overriden
                // here) (td, may 2013)
                tripRouter.setRoutingModule(mode, routingModule);
            }
        }
		return new OnlyPTPlanRouter(tripRouter, scenario.getActivityFacilities()); 
	}
	
	private static final class PrepareForSimOnlyPT extends AbstractPersonAlgorithm {

		private final PersonAlgorithm personAlgorithm;
		
		public PrepareForSimOnlyPT(PersonAlgorithm personAlgorithm) {
			this.personAlgorithm = personAlgorithm;
		}
		
		@Override
		public void run(Person person) {
			this.personAlgorithm.run(person);
		}
	}
	
	private static class OnlyPTPlanRouter implements PlanAlgorithm, PersonAlgorithm {

		private final TripRouter tripRouter;
		private final ActivityFacilities facilities;
		
		public OnlyPTPlanRouter(TripRouter tripRouter, ActivityFacilities facilities) {
			this.tripRouter = tripRouter;
			this.facilities = facilities;
		}

		@Override
		public void run(Person person) {
			for (Plan plan : person.getPlans()) {
				run(plan);
			}
		}	
		
		@Override
		public void run(final Plan plan) {
			final List<Trip> trips = TripStructureUtils.getTrips(plan, this.tripRouter.getStageActivityTypes());

			for (Trip trip : trips) {
				// handle only pt trips - other trips are up-to-date
				String mainMode = this.tripRouter.getMainModeIdentifier().identifyMainMode(trip.getTripElements());
				if (mainMode.equals(TransportMode.pt)) {
					final List<? extends PlanElement> newTrip =
							this.tripRouter.calcRoute(mainMode,
									toFacility(trip.getOriginActivity()),
									toFacility(trip.getDestinationActivity()),
									calcEndOfActivity(trip.getOriginActivity(), plan),
									plan.getPerson());
					
					TripRouter.insertTrip(plan, 
							trip.getOriginActivity(),
							newTrip,
							trip.getDestinationActivity());					
				}
			}
		}
		
		// /////////////////////////////////////////////////////////////////////////
		// helpers
		// /////////////////////////////////////////////////////////////////////////
		private Facility toFacility(final Activity act) {
			if ((act.getLinkId() == null || act.getCoord() == null)
					&& facilities != null
					&& !facilities.getFacilities().isEmpty()) {
				// use facilities only if the activity does not provides the required fields.
				return facilities.getFacilities().get( act.getFacilityId() );
			}
			return new ActivityWrapperFacility( act );
		}

		private static double calcEndOfActivity(
				final Activity activity,
				final Plan plan) {
			if (activity.getEndTime() != Time.UNDEFINED_TIME) return activity.getEndTime();

			// no sufficient information in the activity...
			// do it the long way.
			// XXX This is inefficient! Using a cache for each plan may be an option
			// (knowing that plan elements are iterated in proper sequence,
			// no need to re-examine the parts of the plan already known)
			double now = 0;

			for (PlanElement pe : plan.getPlanElements()) {
				now = updateNow( now , pe );
				if (pe == activity) return now;
			}

			throw new RuntimeException( "activity "+activity+" not found in "+plan.getPlanElements() );
		}
		
		private static double updateNow(
				final double now,
				final PlanElement pe) {
			if (pe instanceof Activity) {
				Activity act = (Activity) pe;
				double endTime = act.getEndTime();
				double startTime = act.getStartTime();
				double dur = (act instanceof ActivityImpl ? act.getMaximumDuration() : Time.UNDEFINED_TIME);
				if (endTime != Time.UNDEFINED_TIME) {
					// use fromAct.endTime as time for routing
					return endTime;
				}
				else if ((startTime != Time.UNDEFINED_TIME) && (dur != Time.UNDEFINED_TIME)) {
					// use fromAct.startTime + fromAct.duration as time for routing
					return startTime + dur;
				}
				else if (dur != Time.UNDEFINED_TIME) {
					// use last used time + fromAct.duration as time for routing
					return now + dur;
				}
				else {
					throw new RuntimeException("activity has neither end-time nor duration." + act);
				}
			}
			double tt = ((Leg) pe).getTravelTime();
			return now + (tt != Time.UNDEFINED_TIME ? tt : 0);
		}
	}
	

}

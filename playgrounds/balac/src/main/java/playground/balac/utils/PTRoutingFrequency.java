package playground.balac.utils;

import com.google.inject.Provider;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.router.TransitRouterWrapper;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.Departure;

import playground.balac.twowaycarsharingredisigned.scenario.TwoWayCSFacility;
import playground.balac.twowaycarsharingredisigned.scenario.TwoWayCSFacilityImpl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PTRoutingFrequency {
	


	private static Provider<TransitRouter> transitRouterFactory;
	public static void main(String[] args) throws IOException {
		
		PTRoutingFrequency pTRoutingFrequency = new PTRoutingFrequency();
		pTRoutingFrequency.run(args);
	}
	
	public double getDepartureTime(List<? extends PlanElement> route) {
		
		if (route.size() == 1)
			return ((Leg)route.get(0)).getDepartureTime();
		else {
			for (PlanElement pe : route) {
				
				if (pe instanceof Leg && ((Leg) pe).getMode().equals("pt")) {
					
					return ((Leg)pe).getDepartureTime();
					
				}
				
			}
			
		}
		return 0.0;
		
	}
	
	public double getTraveltime(List<? extends PlanElement> route) {
		
		double travelTime = 0.0;
		
		for (PlanElement pe : route) {
			
			
			if (pe instanceof Leg) {
				
				travelTime += ((Leg) pe).getTravelTime();
				
			}
			
		}
 		
		
		
		return travelTime;
	}
	
	public int getNumberOfTransfers(List<? extends PlanElement> route) {
		int count = 0;
		
		for (PlanElement pe : route)
			if (pe instanceof Leg && ((Leg) pe).getMode().equals("pt")) {
				count++;
			}
		
		
		return -1 + count;
	}
	//simple check without checking if it is exactly the same route
	public boolean isDominated(List<? extends PlanElement> route, ArrayList<List<? extends PlanElement>> allRoutes) {
		
		for (List<? extends PlanElement> r : allRoutes) {
			
			if (((Leg)r.get(0)).getDepartureTime() + getTraveltime(r) == 
					((Leg)route.get(0)).getDepartureTime() + getTraveltime(route)) {
				
				
				return true;
				
			}
			
		}
		
		
		return false;
	}
	
	
	public void run(String[] args) throws IOException {
		
		double timeStep = 300;
		
		Config config = ConfigUtils.createConfig();
		config.global().setNumberOfThreads(16);	// for parallel population reading
	//	config.network().setInputFile("C:/Users/balacm/Desktop/InputPt/PTWithoutSimulation/network_multimodal.xml.gz");
	//	config.plans().setInputFile("/data/matsim/cdobler/2030/60.plans_without_pt_routes.xml.gz");
	//	config.plans().setInputFile("/data/matsim/cdobler/2030/plans_test.xml");
	//	config.facilities().setInputFile("C:/Users/balacm/Desktop/InputPt/PTWithoutSimulation/facilities.xml.gz");
	//	config.transit().setTransitScheduleFile("C:/Users/balacm/Desktop/InputPt/PTWithoutSimulation/schedule.20120117.ch-edited.xml.gz");
	//	config.transit().setVehiclesFile("C:/Users/balacm/Desktop/InputPt/PTWithoutSimulation/transitVehicles.ch.xml.gz");
		
		config.network().setInputFile("./network_multimodal.xml.gz");

	    config.facilities().setInputFile("./facilities.xml.gz");
	    config.transit().setTransitScheduleFile("./schedule.20120117.ch-edited.xml.gz");
	    config.transit().setVehiclesFile("./transitVehicles.ch.xml.gz");
		
		config.transit().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		//config.scenario().setUseKnowledge(true);
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		
		TransitRouterNetwork routerNetwork = new TransitRouterNetwork();
	    new TransitRouterNetworkReaderMatsimV1(scenario, routerNetwork).readFile("./transitRouterNetwork_thinned.xml.gz");

	//	new TransitRouterNetworkReaderMatsimV1(scenario, routerNetwork).parse("C:/Users/balacm/Desktop/InputPt/PTWithoutSimulation/transitRouterNetwork_thinned.xml.gz");
		//config.planCalcScore().setUtilityOfLineSwitch(0.0);
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(config.planCalcScore(),
				config.plansCalcRoute(), config.transitRouter(), config.vspExperimental());
		
	//	transitRouterFactory = new FastTransitRouterImplFactory(scenario.getTransitSchedule(), transitRouterConfig, routerNetwork);
		transitRouterFactory = new TransitRouterImplFactory(scenario.getTransitSchedule(), transitRouterConfig, routerNetwork);
		 BufferedReader readLink = IOUtils.getBufferedReader("./coord_"+args[0] +".txt");

//		    BufferedWriter outLink = IOUtils.getBufferedWriter("C:/Users/balacm/Desktop/InputPt/PTWithoutSimulation/travelTimesPT_"+args[0] +".txt");
//	final BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/InputPt/PTWithoutSimulation/coord_"+args[0]+".txt");
			final BufferedWriter outLink = IOUtils.getBufferedWriter("./travelTimesPT_"+args[0]+".txt");

			final BufferedWriter outFrequency = IOUtils.getBufferedWriter("./frequency_"+args[0]+".txt");

		
		String s = readLink.readLine();
		s = readLink.readLine();
		
		NetworkLinkUtils lUtils = new NetworkLinkUtils(scenario.getNetwork());
		
		PlansCalcRouteConfigGroup routeConfigGroup = scenario.getConfig().plansCalcRoute();

		TransitRouterWrapper routingModule = new TransitRouterWrapper(
        		transitRouterFactory.get(),
                scenario.getTransitSchedule(),
                scenario.getNetwork(), // use a walk router in case no PT path is found
                DefaultRoutingModules.createTeleportationRouter( TransportMode.transit_walk, scenario.getPopulation().getFactory(), 
				        routeConfigGroup.getModeRoutingParams().get( TransportMode.walk )
				        ));
		
		int i =1;
		System.out.println("starting to aprse the input file");
		while(s != null) {
			
			String[] arr = s.split("\\s");
			if (arr[2].equals("1")) {
				Coord coordStart = new Coord(Double.parseDouble(arr[4]), Double.parseDouble(arr[5]));
			Link lStart = lUtils.getClosestLink(coordStart);
				Coord coordEnd = new Coord(Double.parseDouble(arr[6]), Double.parseDouble(arr[7]));
			Link lEnd = lUtils.getClosestLink(coordEnd);

			
		//	Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId(arr[0]));
			
			Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId(Integer.toString(i)));
		//	i++;
		/*	PlanImpl plan = (PlanImpl) scenario.getPopulation().getFactory().createPlan();
			ActivityImpl act = new ActivityImpl("home", lStart.getId());
			act.setCoord(coordStart);
			//String[] arr2 = arr[6].split(":");
			//double h = Double.parseDouble(arr2[0]);
			double m = Double.parseDouble(arr[2]);
			act.setEndTime(m * 60);
			plan.addActivity(act);
			
			LegImpl leg = new LegImpl("pt");	*/		
			
			double m = Double.parseDouble(arr[3]);
			TwoWayCSFacilityImpl startFacility = new TwoWayCSFacilityImpl(Id.create("100", TwoWayCSFacility.class), coordStart, lStart.getId());
			
			TwoWayCSFacilityImpl endFacility = new TwoWayCSFacilityImpl(Id.create("101", TwoWayCSFacility.class), coordEnd, lEnd.getId());
			
			ArrayList<List<? extends PlanElement>> allRoutes = new ArrayList<List<? extends PlanElement>>();
			
			List<? extends PlanElement> route =  routingModule.calcRoute(startFacility, endFacility, m * 60, person);
			double departureTime = m *60 + ((Leg)route.get(0)).getTravelTime();
			double travelTime = this.getTraveltime(route);			
			int numberOfTransfers = this.getNumberOfTransfers(route);
			
			
			if (route.size() != 1) {
				
				outFrequency.write(arr[0] + " "); // writing the ID of the person routed

			double firstTime = 0.0;
			double lastTime = 0.0;
				
			int count = 0;
			for (double time = departureTime + 7200; time >= departureTime - 7200; time -= timeStep) {
			
			//	if (time != departureTime) {
					
					List<? extends PlanElement> routeNew =  routingModule.calcRoute(startFacility, endFacility, time, person);
					
					double travelTimeNew = getTraveltime (routeNew);
					
					if (travelTimeNew < 1.30 * travelTime && numberOfTransfers + 2 > this.getNumberOfTransfers(routeNew)) {
						
						double lastArrival = time;

						
						int countTransfers = -1;
						
						double transferTime = 0.0;
						
						boolean writtenAccessTime = false;
						
						double egressTime = 0.0;
						
						double distance = 0.0;
						
						
						((Leg)routeNew.get(0)).setDepartureTime(time);
						
						if(!isDominated(routeNew, allRoutes)) {
							if (routeNew.size() != 1) {
								if (lastTime == 0.0)
									lastTime = time;
								firstTime = time;
								count++;
							}
							outLink.write(arr[0] + " "); // writing the ID of the person routed
						for(PlanElement pe1: routeNew) {
							
							if (pe1 instanceof Leg && ((Leg) pe1).getMode().equals("pt")) {
								countTransfers++;
								//plan.addLeg((Leg)pe1);
								ExperimentalTransitRoute tr1 = ((ExperimentalTransitRoute)(((Leg)pe1).getRoute()));
								double temp = Double.MAX_VALUE;
								//scenario.getTransitSchedule().getTransitLines().get(tr1.getLineId()).getRoutes().get(tr1.getRouteId()).getDepartures()
								for (Departure d: scenario.getTransitSchedule().getTransitLines().get(tr1.getLineId()).getRoutes().get(tr1.getRouteId()).getDepartures().values()) {
									
									double fromStopArrivalOffset = scenario.getTransitSchedule().getTransitLines().get(tr1.getLineId()).getRoutes().get(tr1.getRouteId()).getStop(scenario.getTransitSchedule().getFacilities().get(tr1.getAccessStopId())).getDepartureOffset();
																
									if (d.getDepartureTime() + fromStopArrivalOffset >= lastArrival && d.getDepartureTime() + fromStopArrivalOffset < temp) {
										
										temp = d.getDepartureTime() + fromStopArrivalOffset;
										
									}
								}
								
								distance += ((Leg) pe1).getRoute().getDistance();
								
								double transfertTimePart = temp - lastArrival;
								
								if (countTransfers == 0)
									outLink.write(Double.toString(transfertTimePart) + " "); //writing first waiting time
								
								else
									
								transferTime += transfertTimePart;
									
								lastArrival +=  ((Leg) pe1).getTravelTime();
							}
							else if (pe1 instanceof Leg) {
								//plan.addLeg((Leg)pe1);
								lastArrival += ((Leg) pe1).getTravelTime();
								
								if (!writtenAccessTime) {
									
									if (routeNew.size() == 1) 
										
										outLink.write(Double.toString(0.0) + " "); //writing access time
																
									else
										outLink.write(Double.toString(((Leg) pe1).getTravelTime()) + " "); //writing access time
									writtenAccessTime = true;
								}
								
								egressTime = ((Leg) pe1).getTravelTime();
								
							}
							
						}
						if (routeNew.size() == 1)
							outLink.write(Double.toString(0.0) + " "); //
						outLink.write(Double.toString(transferTime) + " "); //writing transfer times not including first wait time
						
						outLink.write(Integer.toString(countTransfers) + " "); //writing the number of transfers (-1 means pure walk trip)
						if (routeNew.size() == 1)
							outLink.write(Double.toString(0.0) + " "); //writing egress time

						else
							outLink.write(Double.toString(egressTime) + " ");  //writing egress time
						
						outLink.write(Double.toString(lastArrival - time) + " "); //writing the total travel time from door to door
						
						outLink.write(Double.toString(distance));
						
						outLink.newLine();
						
						allRoutes.add(routeNew);		
						}
					}
										
					
			//	}			
			}
			
			if (count == 1)
				outFrequency.write(Integer.toString(0));
			else
				outFrequency.write(Double.toString((lastTime - firstTime)/(count - 1)));
			outFrequency.newLine();
			
			}			
			}			
			s = readLink.readLine();	
			System.out.println(arr[0]);
			
		}
		
		outFrequency.flush();
		outFrequency.close();
		outLink.flush();
		outLink.close();
		
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()
//				((ScenarioImpl) scenario).getKnowledges()).writeFileV4("/data/matsim/cdobler/2030/60.plans_with_pt_routes.xml.gz");
		//		((ScenarioImpl) scenario).getKnowledges()).writeFileV4("C:/Users/balacm/Desktop/InputPt/PTWithoutSimulation/plans_with_pt_routes_single_plan_"+args[0]+".xml.gz");
        ).writeV4("./plans_pt_trips_"+args[0]+".xml.gz");


	}

}

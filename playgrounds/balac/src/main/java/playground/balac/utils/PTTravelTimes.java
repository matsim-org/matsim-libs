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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.TransitRouterWrapper;
import org.matsim.core.router.old.DefaultRoutingModules;
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
import java.util.List;

public class PTTravelTimes {
	


	private static Provider<TransitRouter> transitRouterFactory;
	
	public static void main(String[] args) throws IOException {
		
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
	    new TransitRouterNetworkReaderMatsimV1(scenario, routerNetwork).parse("./transitRouterNetwork_thinned.xml.gz");

	//new TransitRouterNetworkReaderMatsimV1(scenario, routerNetwork).parse("C:/Users/balacm/Desktop/InputPt/PTWithoutSimulation/transitRouterNetwork_thinned.xml.gz");
		//config.planCalcScore().setUtilityOfLineSwitch(0.0);
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(config.planCalcScore(),
				config.plansCalcRoute(), config.transitRouter(), config.vspExperimental());
		
	//	transitRouterFactory = new FastTransitRouterImplFactory(scenario.getTransitSchedule(), transitRouterConfig, routerNetwork);
		transitRouterFactory = new TransitRouterImplFactory(scenario.getTransitSchedule(), transitRouterConfig, routerNetwork);
		 BufferedReader readLink = IOUtils.getBufferedReader("./"+args[0] +".txt");

		//    BufferedWriter outLink = IOUtils.getBufferedWriter("C:/Users/balacm/Desktop/InputPt/PTWithoutSimulation/travelTimesPT_"+args[0] +".txt");
	//	final BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/InputPt/PTWithoutSimulation/coord_"+args[0]+".txt");
		
	final BufferedWriter outLink = IOUtils.getBufferedWriter("./travelTimesPT_"+args[0]+".txt");

		
		
		String s = readLink.readLine();

		//s = readLink.readLine();
		//int i = 0;

		s = readLink.readLine();
					
		NetworkLinkUtils lUtils = new NetworkLinkUtils(scenario.getNetwork());
		
		PlansCalcRouteConfigGroup routeConfigGroup = scenario.getConfig().plansCalcRoute();

		TransitRouterWrapper routingModule = new TransitRouterWrapper(
        		transitRouterFactory.get(),
                scenario.getTransitSchedule(),
                scenario.getNetwork(), // use a walk router in case no PT path is found
                DefaultRoutingModules.createTeleportationRouter(TransportMode.transit_walk, scenario.getPopulation().getFactory(), 
				        routeConfigGroup.getModeRoutingParams().get( TransportMode.walk ) ));
		
		//final BufferedWriter outLink = IOUtils.getBufferedWriter("C:/Users/balacm/Desktop/InputPt/StatisticsPt.txt");
		int i = 1;
		while(s != null) {
			String[] arr = s.split("\\s");
			if (arr[2].equals("1")) {
				Coord coordStart = new Coord(Double.parseDouble(arr[4]), Double.parseDouble(arr[5]));
				Link lStart = lUtils.getClosestLink(coordStart);
				Coord coordEnd = new Coord(Double.parseDouble(arr[6]), Double.parseDouble(arr[7]));
				Link lEnd = lUtils.getClosestLink(coordEnd);
				
				outLink.write(Integer.toString(i) + " "); // writing the ID of the person routed
				
				Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId(Integer.toString(i)));
				i++;
				//PersonImpl person = new PersonImpl(new IdImpl(arr[0]));
			//	i++;
				PlanImpl plan = (PlanImpl) scenario.getPopulation().getFactory().createPlan();
				ActivityImpl act = new ActivityImpl("home", lStart.getId());
				act.setCoord(coordStart);
				//String[] arr2 = arr[6].split(":");
				//double h = Double.parseDouble(arr2[0]);
				double m = Double.parseDouble(arr[3]);
				act.setEndTime(m * 60);
				plan.addActivity(act);
				
				LegImpl leg = new LegImpl("pt");
				
				TwoWayCSFacilityImpl startFacility = new TwoWayCSFacilityImpl(Id.create("100", TwoWayCSFacility.class), coordStart, lStart.getId());
						
				TwoWayCSFacilityImpl endFacility = new TwoWayCSFacilityImpl(Id.create("101", TwoWayCSFacility.class), coordEnd, lEnd.getId());
				List<? extends PlanElement> route =  routingModule.calcRoute(startFacility, endFacility, m * 60, person);

				//TransitScheduleImpl tr = ((TransitScheduleImpl)(scenario.getTransitSchedule()));
				double lastArrival = m * 60.0;
				
				int countTransfers = -1;
				
				double transferTime = 0.0;
				
				boolean writtenAccessTime = false;
				
				double egressTime = 0.0;
				
				double distance = 0.0;
				for(PlanElement pe1: route) {
			    	
					if (pe1 instanceof Leg && ((Leg) pe1).getMode().equals("pt")) {
						countTransfers++;
						plan.addLeg((Leg)pe1);
						distance += ((Leg) pe1).getRoute().getDistance();
						ExperimentalTransitRoute tr1 = ((ExperimentalTransitRoute)(((Leg)pe1).getRoute()));
						double temp = Double.MAX_VALUE;
						//scenario.getTransitSchedule().getTransitLines().get(tr1.getLineId()).getRoutes().get(tr1.getRouteId()).getDepartures()
						for (Departure d: scenario.getTransitSchedule().getTransitLines().get(tr1.getLineId()).getRoutes().get(tr1.getRouteId()).getDepartures().values()) {
							
							double fromStopArrivalOffset = scenario.getTransitSchedule().getTransitLines().get(tr1.getLineId()).getRoutes().get(tr1.getRouteId()).getStop(scenario.getTransitSchedule().getFacilities().get(tr1.getAccessStopId())).getDepartureOffset();
														
							if (d.getDepartureTime() + fromStopArrivalOffset >= lastArrival && d.getDepartureTime() + fromStopArrivalOffset < temp) {
								
								temp = d.getDepartureTime() + fromStopArrivalOffset;
								
							}
						}
						
						
						double transfertTimePart = temp - lastArrival;
						
						if (countTransfers == 0)
							outLink.write(Double.toString(transfertTimePart) + " "); //writing first waiting time
						else
						transferTime += transfertTimePart;
							
						lastArrival +=  ((Leg) pe1).getTravelTime();
					}
					else if (pe1 instanceof Leg) {
						plan.addLeg((Leg)pe1);
						lastArrival += ((Leg) pe1).getTravelTime();
						
						if (!writtenAccessTime) {
							
							if (route.size() == 1) 
								
								outLink.write(Double.toString(0.0) + " "); //writing access time
														
							else
								outLink.write(Double.toString(((Leg) pe1).getTravelTime()) + " "); //writing access time
							writtenAccessTime = true;
						}
						
						egressTime = ((Leg) pe1).getTravelTime();
						
					}
					
				}
				if (route.size() == 1)
					outLink.write(Double.toString(0.0) + " "); //
				outLink.write(Double.toString(transferTime) + " "); //writing transfer times not including first wait time
				
				outLink.write(Integer.toString(countTransfers) + " "); //writing the number of transfers (-1 means pure walk trip)
				if (route.size() == 1)
					outLink.write(Double.toString(0.0) + " "); //writing egress time

				else
					outLink.write(Double.toString(egressTime) + " ");  //writing egress time
				
				outLink.write(Double.toString(lastArrival - m * 60.0) + " "); //writing the total travel time from door to door
				
				outLink.write(Double.toString(distance));
				outLink.newLine();
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
					
				System.out.println(arr[0]); 
			}
			s = readLink.readLine();
			
		}
		
		
		outLink.flush();
		outLink.close();
		
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()
//				((ScenarioImpl) scenario).getKnowledges()).writeFileV4("/data/matsim/cdobler/2030/60.plans_with_pt_routes.xml.gz");
		//		((ScenarioImpl) scenario).getKnowledges()).writeFileV4("C:/Users/balacm/Desktop/InputPt/PTWithoutSimulation/plans_with_pt_routes_single_plan_"+args[0]+".xml.gz");
        ).writeFileV4("./plans_pt_trips_"+args[0]+".xml.gz");

	}
		

}

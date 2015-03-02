package playground.balac.pcw;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.TransitRouterWrapper;
import org.matsim.core.router.old.LegRouterWrapper;
import org.matsim.core.router.old.TeleportationLegRouter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.config.TransitRouterConfigGroup;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.Departure;

import playground.balac.twowaycarsharingredisigned.scenario.TwoWayCSFacilityImpl;
import playground.balac.utils.NetworkLinkUtils;
import playground.balac.utils.TimeConversion;
import playground.balac.utils.TransitRouterImplFactory;
import playground.balac.utils.TransitRouterNetworkReaderMatsimV1;

public class PTRouting {

	private static TransitRouterFactory transitRouterFactory;
	public static void main(String[] args) throws IOException {
		
		PTRouting pTRouting = new PTRouting();
		pTRouting.run(args);
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
	public boolean isDominatedWithTran(List<? extends PlanElement> route, ArrayList<List<? extends PlanElement>> allRoutes) {
		
		for (List<? extends PlanElement> r : allRoutes) {
			
			if (((Leg)r.get(0)).getDepartureTime() + getTraveltime(r) == 
					((Leg)route.get(0)).getDepartureTime() + getTraveltime(route) && getNumberOfTransfers(r) == getNumberOfTransfers(route)) {
				
				
				return true;
				
			}
			
		}
		
		
		return false;
	}
	public boolean isDominatedWithoutTran(List<? extends PlanElement> route, ArrayList<List<? extends PlanElement>> allRoutes) {
		
		for (List<? extends PlanElement> r : allRoutes) {
			
			if (((Leg)r.get(0)).getDepartureTime() + getTraveltime(r) == 
					((Leg)route.get(0)).getDepartureTime() + getTraveltime(route)) {
				
				
				return true;
				
			}
			
		}
		
		
		return false;
	}
	
	public void run(String[] args) throws IOException {
		
		double timeStep = 60;
		
		Config config = ConfigUtils.createConfig();
		config.global().setNumberOfThreads(16);	// for parallel population reading
	
		
		config.network().setInputFile("C:/Users/balacm/Desktop/InputPt/network_multimodal.xml.gz");

	    config.facilities().setInputFile("C:/Users/balacm/Desktop/InputPt/facilities.xml.gz");
	    config.transit().setTransitScheduleFile("C:/Users/balacm/Desktop/InputPt/schedule.20120117.ch-edited.xml.gz");
	    config.transit().setVehiclesFile("C:/Users/balacm/Desktop/InputPt/transitVehicles.ch.xml.gz");
		
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);
				
		TransitRouterNetwork routerNetwork = new TransitRouterNetwork();
	    new TransitRouterNetworkReaderMatsimV1(scenario, routerNetwork).parse("C:/Users/balacm/Desktop/InputPt/transitRouterNetwork_thinned.xml.gz");

	    ((PlansCalcRouteConfigGroup)config.getModule("planscalcroute")).getModeRoutingParams().get("walk").setTeleportedModeSpeed(1.34);
	    
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(config.planCalcScore(),
				config.plansCalcRoute(), config.transitRouter(), config.vspExperimental());
		
	//	transitRouterFactory = new FastTransitRouterImplFactory(scenario.getTransitSchedule(), transitRouterConfig, routerNetwork);
		transitRouterFactory = new TransitRouterImplFactory(scenario.getTransitSchedule(), transitRouterConfig, routerNetwork);
		 BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/InputPt/coord_"+args[0] +".txt");

//		    BufferedWriter outLink = IOUtils.getBufferedWriter("C:/Users/balacm/Desktop/InputPt/PTWithoutSimulation/travelTimesPT_"+args[0] +".txt");
//	final BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/InputPt/PTWithoutSimulation/coord_"+args[0]+".txt");
			final BufferedWriter outLink = IOUtils.getBufferedWriter("C:/Users/balacm/Desktop/InputPt/travelTimesPT_"+args[0]+".txt");
			final BufferedWriter outLinkF = IOUtils.getBufferedWriter("C:/Users/balacm/Desktop/InputPt/travelTimesPTFr_"+args[0]+".txt");

			final BufferedWriter outFrequency = IOUtils.getBufferedWriter("C:/Users/balacm/Desktop/InputPt/frequency_"+args[0]+".txt");
			
		((TransitRouterConfigGroup) config.getModule("transitRouter")).setSearchRadius(2000.0);
			
		String s = readLink.readLine();
		
		NetworkLinkUtils lUtils = new NetworkLinkUtils(scenario.getNetwork());
		
		PlansCalcRouteConfigGroup routeConfigGroup = scenario.getConfig().plansCalcRoute();

		TransitRouterWrapper routingModule = new TransitRouterWrapper(
        		transitRouterFactory.createTransitRouter(),
                scenario.getTransitSchedule(),
                scenario.getNetwork(), // use a walk router in case no PT path is found
                LegRouterWrapper.createLegRouterWrapper(TransportMode.transit_walk, scenario.getPopulation().getFactory(), new TeleportationLegRouter(
				        ((PopulationFactoryImpl) scenario.getPopulation().getFactory()).getModeRouteFactory(),
				        routeConfigGroup.getTeleportedModeSpeeds().get(TransportMode.walk),
				        routeConfigGroup.getModeRoutingParams().get( TransportMode.walk ).getBeelineDistanceFactor())));
		
		
		
		
		System.out.println("starting to parse the input file");
		
		WGS84toCH1903LV03 transformation = new WGS84toCH1903LV03();
		
		TimeConversion timeConv = new TimeConversion();
		
		while(s != null) {
			
			String[] arr = s.split(";");
			
			if (!(arr[1].startsWith("-") || arr[2].startsWith("-") || arr[3].startsWith("-") || arr[4].startsWith("-"))) {
			
			CoordImpl coordStart = new CoordImpl(arr[5], arr[6]);
			
			
			
			Link lStart = lUtils.getClosestLink(coordStart);
			
			
			CoordImpl coordEnd = new CoordImpl(arr[7], arr[8]);
			

			Link lEnd = lUtils.getClosestLink(coordEnd);

			
			Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId(arr[0]));
			
			double m = TimeConversion.convertTimeToDouble(arr[10]);
			
			
			TwoWayCSFacilityImpl startFacility = new TwoWayCSFacilityImpl(Id.create("100", Facility.class), coordStart, lStart.getId());
			
			TwoWayCSFacilityImpl endFacility = new TwoWayCSFacilityImpl(Id.create("101", Facility.class), coordEnd, lEnd.getId());
			
			ArrayList<List<? extends PlanElement>> allRoutes = new ArrayList<List<? extends PlanElement>>();
			
			List<? extends PlanElement> route =  routingModule.calcRoute(startFacility, endFacility, m * 60, person);
			((Leg)route.get(0)).setDepartureTime(m * 60);

			System.out.println("routed for the initial time");
			
			double departureTime = m *60 + ((Leg)route.get(0)).getTravelTime();
			
			System.out.println(departureTime);
			double travelTime = this.getTraveltime(route);			
			int numberOfTransfers = this.getNumberOfTransfers(route);
			
			double firstTime = 0.0;
			double lastTime = 0.0;
				
			int count = 0;
			
			ArrayList<List<? extends PlanElement>> allRoutesFirst = new ArrayList<List<? extends PlanElement>>();
			
			if(route.size() == 1) {
				System.out.println(((Leg)route.get(0)).getTravelTime());
				System.out.println(((Leg)route.get(0)).getMode());
			}

			
			if (route.size() != 1) {
				allRoutes.add(route);
			for (double time = departureTime + 7200; time >= departureTime - 7200; time -= timeStep) {
				
					List<? extends PlanElement> routeNew =  routingModule.calcRoute(startFacility, endFacility, time, person);
					
					double travelTimeNew = getTraveltime (routeNew);
						
					((Leg)routeNew.get(0)).setDepartureTime(time);
						
					if(!isDominatedWithTran(routeNew, allRoutes)) {
						if (routeNew.size() != 1) {
								
							allRoutesFirst.add(routeNew);	
							if (travelTimeNew < 1.30 * travelTime && numberOfTransfers + 2 > this.getNumberOfTransfers(routeNew) && 
									!isDominatedWithoutTran(routeNew, allRoutes)	
									) {
							
								allRoutes.add(routeNew);
								
							}
								
						}
					}
			}
			
			System.out.println("found all the routes");

			
			double lastArrival = ((Leg)route.get(0)).getDepartureTime();

			int countTransfers = -1;
			
			double transferTime = 0.0;
			
			boolean writtenAccessTime = false;
			
			double egressTime = 0.0;
			
			double distance = 0.0;
			
			outLinkF.write(arr[0] + ";"); // writing the ID of the person routed

			for(PlanElement pe1: route) {
				
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
						outLinkF.write(Double.toString(transfertTimePart) + ";"); //writing first waiting time
					
					else
						
						transferTime += transfertTimePart;
						
					lastArrival +=  ((Leg) pe1).getTravelTime();
				}
				else if (pe1 instanceof Leg) {
					lastArrival += ((Leg) pe1).getTravelTime();
					
					if (!writtenAccessTime) {
						
						if (route.size() == 1) {
							outLinkF.write(Double.toString(((Leg) pe1).getDepartureTime()) + ";");  //writing departure time

						
							
							outLinkF.write(Double.toString(0.0) + ";"); //writing access time
						}						
						else{			
							outLinkF.write(Double.toString(((Leg) pe1).getDepartureTime()) + ";");  //writing departure time
						

							outLinkF.write(Double.toString(((Leg) pe1).getTravelTime()) + ";"); //writing access time
						}
						writtenAccessTime = true;
					}
					
					egressTime = ((Leg) pe1).getTravelTime();
					
				}
				
			}
			System.out.println("written the rout to output");

			outLinkF.write(Double.toString(transferTime) + ";"); //writing transfer times not including first wait time
			
			outLinkF.write(Integer.toString(countTransfers) + ";"); //writing the number of transfers (-1 means pure walk trip)
			
			if (route.size() == 1)
				outLinkF.write(Double.toString(0.0) + ";"); //writing egress time

			else
				outLinkF.write(Double.toString(egressTime) + ";");  //writing egress time
			
			outLinkF.write(Double.toString(lastArrival - ((Leg)route.get(0)).getDepartureTime()) + ";"); //writing the total travel time from door to door
			
			outLinkF.write(Double.toString(distance));
			
			outLinkF.newLine();
			
			
			for (List<? extends PlanElement> routeIter : allRoutes) {
			
				lastArrival = ((Leg)routeIter.get(0)).getDepartureTime();
				
					if (routeIter.size() != 1) {
						if (lastTime == 0.0)
							lastTime = lastArrival;
						else if (lastTime < lastArrival)
							lastTime = lastArrival;
												
						if (firstTime == 0.0)
							firstTime = lastArrival;
						else if (firstTime > lastArrival)
							firstTime = lastArrival;
												
						count++;
					}
				
			
			
			
			}
			System.out.println("found frequency");

			
			outFrequency.write(arr[0] + ";");
			
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
		outLinkF.flush();
		outLinkF.close();
		
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()
//				((ScenarioImpl) scenario).getKnowledges()).writeFileV4("/data/matsim/cdobler/2030/60.plans_with_pt_routes.xml.gz");
		//		((ScenarioImpl) scenario).getKnowledges()).writeFileV4("C:/Users/balacm/Desktop/InputPt/PTWithoutSimulation/plans_with_pt_routes_single_plan_"+args[0]+".xml.gz");
        ).writeFileV4("./plans_pt_trips_"+args[0]+".xml.gz");


}

}

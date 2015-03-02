package playground.balac.pcw;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.contrib.multimodal.router.DefaultDelegateFactory;
import org.matsim.contrib.multimodal.router.MultimodalTripRouterFactory;
import org.matsim.contrib.multimodal.router.util.LinkSlopesReader;
import org.matsim.contrib.multimodal.router.util.MultiModalTravelTimeFactory;
import org.matsim.contrib.multimodal.tools.PrepareMultiModalScenario;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TransitRouterWrapper;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.old.LegRouterWrapper;
import org.matsim.core.router.old.TeleportationLegRouter;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

import playground.balac.onewaycarsharingredisgned.config.OneWayCarsharingRDConfigGroup;
import playground.balac.twowaycarsharingredisigned.scenario.TwoWayCSFacilityImpl;
import playground.balac.utils.NetworkLinkUtils;
import playground.balac.utils.TimeConversion;

public class WalkBikeRouting {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		

		//Config config = ConfigUtils.createConfig();
		
    	final Config config = ConfigUtils.loadConfig("C:/Users/balacm/Desktop/InputPt/config_multimodal.xml");
    	MultiModalConfigGroup multiModalConfigGroup = new MultiModalConfigGroup();
    	config.addModule(multiModalConfigGroup);
    	
    	
    	Scenario scenario = ScenarioUtils.loadScenario(config);
		PrepareMultiModalScenario.run(scenario);
		Controler controler = new Controler(scenario);
		Map<Id<Link>, Double> linkSlopes = new LinkSlopesReader().getLinkSlopes(multiModalConfigGroup, scenario.getNetwork());

		MultiModalTravelTimeFactory multiModalTravelTimeFactory = new MultiModalTravelTimeFactory(scenario.getConfig(), linkSlopes);
		Map<String, TravelTime> multiModalTravelTimes = multiModalTravelTimeFactory.createTravelTimes();	
	
		TripRouterFactory defaultDelegateFactory = new DefaultDelegateFactory(controler.getScenario(), new FastDijkstraFactory());
		TripRouterFactory multiModalTripRouterFactory = new MultimodalTripRouterFactory(controler.getScenario(), multiModalTravelTimes,
                ControlerDefaults.createDefaultTravelDisutilityFactory(scenario), defaultDelegateFactory, new FastDijkstraFactory());
		
		controler.setTripRouterFactory(multiModalTripRouterFactory);	
		TripRouter tripRouter = multiModalTripRouterFactory.instantiateAndConfigureTripRouter(new RoutingContextImpl(
										new TravelDisutility() {

											@Override
											public double getLinkTravelDisutility(
													Link link, double time,
													Person person,
													Vehicle vehicle) {
												// TODO Auto-generated method stub
												return 0;
											}

											@Override
											public double getLinkMinimumTravelDisutility(
													Link link) {
												// TODO Auto-generated method stub
												return 0;
											}
										},
										new TravelTime() {

											@Override
											public double getLinkTravelTime(
													Link link, double time,
													Person person,
													Vehicle vehicle) {
												// TODO Auto-generated method stub
												return 0;
											}
										}));
		RoutingModule routingModuleWalk = tripRouter.getRoutingModule("walk");
		RoutingModule routingModuleBike = tripRouter.getRoutingModule("bike");

		BufferedReader readLink = IOUtils.getBufferedReader("P:/Projekte/SNF/SNF Post-Car World/STATEDCHOICE/geo_coded_fucker.txt");
		
		final BufferedWriter outLink = IOUtils.getBufferedWriter("P:/Projekte/SNF/SNF Post-Car World/STATEDCHOICE/walkbike_fucker.txt");

		NetworkImpl subNetworkWalk = NetworkImpl.createNetwork();
		Set<String> restrictions = new HashSet<>();
		restrictions.add("walk");
		TransportModeNetworkFilter networkFilter = new TransportModeNetworkFilter(scenario.getNetwork());
		networkFilter.filter(subNetworkWalk, restrictions);
		
		NetworkImpl subNetworkBike = NetworkImpl.createNetwork();
		restrictions = new HashSet<>();
		restrictions.add("bike");
		 networkFilter = new TransportModeNetworkFilter(scenario.getNetwork());
		networkFilter.filter(subNetworkBike, restrictions);
		
		NetworkLinkUtils lUtilsWalk = new NetworkLinkUtils(subNetworkWalk);
		NetworkLinkUtils lUtilsBike = new NetworkLinkUtils(subNetworkBike);

		System.out.println("starting to parse the input file");
				
		String s = readLink.readLine();
		
		while(s != null) {
			
			String[] arr = s.split(";");
			
			
			if (arr[0].equals("45"))
				System.out.println();
				CoordImpl coordStart = new CoordImpl(arr[5], arr[6]);
				
				
				
				Link lStartWalk = lUtilsWalk.getClosestLink(coordStart);
				Link lStartBike = lUtilsBike.getClosestLink(coordStart);
				
				CoordImpl coordEnd = new CoordImpl(arr[7], arr[8]);
				
	
				Link lEndWalk = lUtilsWalk.getClosestLink(coordEnd);
				Link lEndBike = lUtilsBike.getClosestLink(coordEnd);
				
				Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId(arr[0]));
				
				((PersonImpl) person).setAge(Integer.parseInt(arr[12]));
				((PersonImpl) person).setSex(arr[11]);
				
				
				double m = TimeConversion.convertTimeToDouble(arr[10]);
				
				
				TwoWayCSFacilityImpl startFacilityWalk = new TwoWayCSFacilityImpl(Id.create("100", Facility.class), coordStart, lStartWalk.getId());
				
				TwoWayCSFacilityImpl endFacilityWalk = new TwoWayCSFacilityImpl(Id.create("101", Facility.class), coordEnd, lEndWalk.getId());
			
				TwoWayCSFacilityImpl startFacilityBike = new TwoWayCSFacilityImpl(Id.create("100", Facility.class), coordStart, lStartBike.getId());
				
				TwoWayCSFacilityImpl endFacilityBike = new TwoWayCSFacilityImpl(Id.create("101", Facility.class), coordEnd, lEndBike.getId());
				
				List<? extends PlanElement> routeWalk =  routingModuleWalk.calcRoute(startFacilityWalk, endFacilityWalk, m * 60, person);

				List<? extends PlanElement> routeBike =  routingModuleBike.calcRoute(startFacilityBike, endFacilityBike, m * 60, person);

				outLink.write(arr[0] + ";" + ((Leg)routeWalk.get(0)).getTravelTime() + ";" + ((Leg)routeWalk.get(0)).getRoute().getDistance()
						+ ";" + ((Leg)routeBike.get(0)).getTravelTime() + ";" + ((Leg)routeBike.get(0)).getRoute().getDistance());
				
				outLink.newLine();
			
			s = readLink.readLine();
		}
		
		outLink.flush();
		outLink.close();
		

	}

}

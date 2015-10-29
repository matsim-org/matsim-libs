/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.visum;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.visum.VisumNetwork;
import org.matsim.visum.VisumNetworkReader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DataPrepare {

	private static final Logger log = Logger.getLogger(DataPrepare.class);

	// INPUT FILES
	private final static String VISUM_FILE = "/home/johannes/gsv/matsim/studies/netz2030/data/raw/network.net";
	private final static String NETWORK_FILE = "/home/johannes/gsv/matsim/studies/netz2030/data/roadnetwork.gk3.xml";
	private final static String INPUT_PLANS_FILE = "/home/johannes/gsv/matsim/studies/netz2030/data/raw/population.tmp.xml";

	// INTERMEDIARY FILES
	private final static String TRANSIT_NETWORK_FILE = "/home/johannes/gsv/matsim/studies/netz2030/data/transitNetwork.tmp.xml";
	private final static String TRANSIT_SCHEDULE_WITHOUT_NETWORK_FILE = "/home/johannes/gsv/citytunnel/data/transitSchedule.tmp.xml";

	// OUTPUT FILES
	private final static String TRANSIT_SCHEDULE_WITH_NETWORK_FILE = "/home/johannes/gsv/citytunnel/data/transitSchedule.gk3.50.xml";
	private final static String VEHICLE_FILE = "/home/johannes/gsv/citytunnel/data/vehicles.xml";
	private final static String MULTIMODAL_NETWORK_FILE = "/home/johannes/gsv/matsim/studies/netz2030/data/network.multimodal.gk3.xml";
	private final static String ROUTED_PLANS_FILE = "/home/johannes/gsv/matsim/studies/netz2030/data/raw/population.xml";


	private final MutableScenario scenario;
	private final Config config;

	public DataPrepare() {
		this.scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.config = this.scenario.getConfig();
	}

	protected void prepareConfig() {
		this.config.transit().setUseTransit(true);
		this.config.scenario().setUseVehicles(true);
	}

	protected void convertSchedule() {
		final VisumNetwork vNetwork = new VisumNetwork();
		log.info("reading visum network.");
		new VisumNetworkReader(vNetwork).read(VISUM_FILE);
		log.info("converting visum data to TransitSchedule.");
		Visum2TransitSchedule converter = new Visum2TransitSchedule(vNetwork, this.scenario.getTransitSchedule(), this.scenario.getTransitVehicles());

		/*
		 * 1;temp;OV;1.000
2;Railion EW Fern;OV;1.000
3;Railion EW Nah;OV;1.000
4;Railion GZ;OV;1.000
5;Railion KV/RoLa;OV;1.000
9;Leer/Schad;OV;1.000
A;Bus Fernverkehr;OV;1.000
B;RB;OV;1.000
C;IC/EC/D-Tag;OV;1.000
D;NZ/CNL/D-Nacht/UEx/AZ;OV;1.000
E;ICE/ICE-T;OV;1.000
F;Fuss;OVFuss;1.000
H;Ausland FV A;OV;1.000
I;Ausland FV B;OV;1.000
J;Ausland NV;OV;1.000
K;Ausland GV;OV;1.000
N;Netz;OV;1.000
O;Sonstiger OEV;OV;1.000
R;RE/IRE;OV;1.000
S;S-Bahn;OV;1.000
T;Thalys;OV;1.000
V;Dritte FV;OV;1.000
W;Dritte NV;OV;1.000
X;Dritte GV;OV;1.000
		 */
		Map<String, String> systems = new HashMap<String, String>();
		systems.put("1", "temp");
		systems.put("2", "Railion EW Fern");
		systems.put("3", "Railion EW Nah");
		systems.put("4", "Railion GZ");
		systems.put("5", "Railion KV/RoLa");
		systems.put("9", "Leer/Schad");
		systems.put("A", "Bus Fernverkehr");
		systems.put("B", "RB");
		systems.put("C", "IC/EC/D-Tag");
		systems.put("D", "NZ/CNL/D-Nacht/UEx/AZ");
		systems.put("E", "ICE/ICE-T");
		systems.put("F", "Fuss");
		systems.put("H", "Ausland FV A");
		systems.put("I", "Ausland FV B");
		systems.put("J", "Ausland NV");
		systems.put("K", "Ausland GV");
		systems.put("N", "Netz");
		systems.put("O", "Sonstiger OEV");
		systems.put("R", "RE/IRE");
		systems.put("S", "S-Bahn");
		systems.put("T", "Thalys");
		systems.put("V", "Dritte FV");
		systems.put("W", "Dritte NV");
		systems.put("X", "Dritte GV");
		
		converter.setTransportSystems(systems);
		
		
		// configure how transport modes must be converted
		// the ones for Berlin
		converter.registerTransportMode("1", TransportMode.other);
		converter.registerTransportMode("2", TransportMode.other);
		converter.registerTransportMode("3", TransportMode.other);
		converter.registerTransportMode("4", TransportMode.other);
		converter.registerTransportMode("5", TransportMode.other);
		converter.registerTransportMode("9", TransportMode.other);
		converter.registerTransportMode("A", TransportMode.pt);
		converter.registerTransportMode("B", TransportMode.pt);
		converter.registerTransportMode("C", TransportMode.pt);
		converter.registerTransportMode("D", TransportMode.pt);
		converter.registerTransportMode("E", TransportMode.pt);
		converter.registerTransportMode("F", TransportMode.walk);
		converter.registerTransportMode("H", TransportMode.pt);
		converter.registerTransportMode("I", TransportMode.pt);
		converter.registerTransportMode("J", TransportMode.pt);
		converter.registerTransportMode("K", TransportMode.pt);
		converter.registerTransportMode("N", TransportMode.other);
		converter.registerTransportMode("O", TransportMode.pt);
		converter.registerTransportMode("R", TransportMode.pt);
		converter.registerTransportMode("S", TransportMode.pt);
		converter.registerTransportMode("T", TransportMode.pt);
		converter.registerTransportMode("V", TransportMode.pt);
		converter.registerTransportMode("W", TransportMode.pt);
		converter.registerTransportMode("X", TransportMode.pt);
		
			// the ones for Zurich
//			converter.registerTransportMode("B", TransportMode.bus); // BUS
//			converter.registerTransportMode("F", TransportMode.walk); // FUSS
//			converter.registerTransportMode("I", TransportMode.car); // IV-PW
//			converter.registerTransportMode("R", TransportMode.train); // REGIONALVERKEHR
//			converter.registerTransportMode("S", TransportMode.other); // SCHIFF
//			converter.registerTransportMode("T", TransportMode.tram); // TRAM
//			converter.registerTransportMode("Y", TransportMode.train); // BERGBAHN
//			converter.registerTransportMode("Z", TransportMode.train); // FERNVERKEHR

		converter.convert();
//		log.info("writing TransitSchedule to file.");
//		new TransitScheduleWriterV1(this.scenario.getTransitSchedule()).write(TRANSIT_SCHEDULE_WITHOUT_NETWORK_FILE);
//		log.info("writing vehicles to file.");
//		new VehicleWriterV1(this.scenario.getVehicles()).writeFile(VEHICLE_FILE);
	}

	protected void createNetworkFromSchedule() {
		NetworkImpl network = NetworkImpl.createNetwork();
		new CreatePseudoNetwork(this.scenario.getTransitSchedule(), network, "tr_").createNetwork();
		new NetworkWriter(network).write(TRANSIT_NETWORK_FILE);
		new TransitScheduleWriter(this.scenario.getTransitSchedule()).writeFile(TRANSIT_SCHEDULE_WITH_NETWORK_FILE);
	}

	protected void mergeNetworks() {
		MutableScenario transitScenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network transitNetwork = transitScenario.getNetwork();
		MutableScenario streetScenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network streetNetwork = streetScenario.getNetwork();
		new MatsimNetworkReader(transitScenario).parse("/home/johannes/gsv/matsim/studies/netz2030/data/network.rail.wgs84.xml");
		new MatsimNetworkReader(streetScenario).parse("/home/johannes/gsv/matsim/studies/netz2030/data/network.road.wgs84.xml");
		MergeNetworks.merge(streetNetwork, "", transitNetwork, "", (NetworkImpl) this.scenario.getNetwork());
		new NetworkWriter(this.scenario.getNetwork()).write("/home/johannes/gsv/matsim/studies/netz2030/data/network.wgs84.xml");
	}

	protected void routePopulation() {
		Population pop = this.scenario.getPopulation();
		new MatsimPopulationReader(this.scenario).parse(INPUT_PLANS_FILE);

		FreespeedTravelTimeAndDisutility timeCostCalculator = new FreespeedTravelTimeAndDisutility(this.scenario.getConfig().planCalcScore());
//		if ( scenario.getConfig().transit().isUseTransit() ) {
//			throw new IllegalStateException( "Routing will not behave as desired" );
//		}
		PlanRouter router =
			new PlanRouter(
					new TripRouterFactoryBuilderWithDefaults().build(
						scenario ).get(
					) );
		log.info("start pt-router");
		for ( Person p : pop.getPersons().values() ) {
			router.run( p );
		}
		log.info("write routed plans out.");
		new PopulationWriter(pop, this.scenario.getNetwork()).write(ROUTED_PLANS_FILE);
	}

//	protected void visualizeRouterNetwork() {
//		TransitRouterConfig tRConfig = new TransitRouterConfig(this.scenario.getConfig().planCalcScore(), 
//				this.scenario.getConfig().plansCalcRoute(), this.scenario.getConfig().transitRouter(),
//				this.scenario.getConfig().vspExperimental());
//
//		TransitRouterImpl router = new TransitRouterImpl(tRConfig, this.scenario.getTransitSchedule() );
//		Network routerNet = router.getTransitRouterNetwork();
//
//		log.info("create vis network");
//		ScenarioImpl visScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		Network visNet = visScenario.getNetwork();
//
//		for (Node node : routerNet.getNodes().values()) {
//			visNet.getFactory().createNode(node.getId(), node.getCoord());
//			visNet.addNode(node);
//		}
//		for (Link link : routerNet.getLinks().values()) {
//			Link l = visNet.getFactory().createLink(link.getId(), link.getFromNode(), link.getToNode());
//			l.setLength(link.getLength());
//			l.setFreespeed(link.getFreespeed());
//			l.setCapacity(link.getCapacity());
//			l.setNumberOfLanes(link.getNumberOfLanes());
//		}
//
//		log.info("write routerNet.xml");
//		new NetworkWriter(visNet).write("visNet.xml");
//
//		log.info("start visualizer");
//		EventsManager events = EventsUtils.createEventsManager();
//		QSim otfVisQSim = (QSim) new QSimFactory().createMobsim(visScenario, events);
//		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, otfVisQSim);
//		OTFClientLive.run(scenario.getConfig(), server);
//		otfVisQSim.run();
//	}

	public static void main(final String[] args) {
		DataPrepare app = new DataPrepare();
		app.prepareConfig();
		
		TransitConfigGroup transitConfig = (TransitConfigGroup) app.config.getModule(TransitConfigGroup.GROUP_NAME);
//		transitConfig.setTransitScheduleFile("/home/johannes/gsv/matsim/studies/netz2030/data/transitSchedule.xml");
		transitConfig.setVehiclesFile("/home/johannes/gsv/matsim/studies/netz2030/data/vehicles.xml");
		Set<String> modes = new HashSet<String>();
		modes.add("pt");
		transitConfig.setTransitModes(modes);
		
//		NetworkConfigGroup netConfig = (NetworkConfigGroup) app.config.getModule(NetworkConfigGroup.GROUP_NAME);
//		netConfig.setInputFile("/home/johannes/gsv/netz2030/data/network.multimodal.xml");
		
		MatsimNetworkReader netreader = new MatsimNetworkReader(app.scenario);
		netreader.readFile("/home/johannes/gsv/matsim/studies/netz2030/data/network.gk3.xml");
		
//		VehicleReaderV1 vehReader = new VehicleReaderV1(app.scenario.getVehicles());
//		vehReader.readFile("/home/johannes/gsv/matsim/studies/netz2030/data/vehicles.xml");
		
		TransitScheduleReader schedReader = new TransitScheduleReader(app.scenario);
		schedReader.readFile("/home/johannes/gsv/matsim/studies/netz2030/data/transitSchedule.longdist.linked.xml");
//		
//		TransitRouterConfigGroup trConfig = (TransitRouterConfigGroup) app.config.getModule(TransitRouterConfigGroup.GROUP_NAME);
//		trConfig.setMaxBeelineWalkConnectionDistance(1);
//		app.convertSchedule();
//		app.createNetworkFromSchedule();
//		app.mergeNetworks();
		app.routePopulation();
//		app.visualizeRouterNetwork();

		log.info("done.");
	}

}

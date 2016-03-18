package playground.dhosse.gap.scenario.network;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.dhosse.gap.Global;

import com.vividsolutions.jts.geom.Geometry;

public class NetworkCreator {

	public static void createAndAddNetwork(Scenario scenario, String osmFile){
		
		Network network = scenario.getNetwork();
		
		OsmNetworkReader onr = new OsmNetworkReader(network, Global.ct);
		
		onr.setScaleMaxSpeed(true);
		
		onr.setHighwayDefaults(1, "motorway", 2, 100./3.6, 1.2, 2000, true);
		onr.setHighwayDefaults(1, "motorway_link", 1, 60/3.6, 1.2, 1500, true);
		
		onr.setHighwayDefaults(2, "trunk", 1, 80/3.6, 0.5, 1000);
		onr.setHighwayDefaults(2, "trunk_link", 1, 60/3.6, 0.5, 1000);
		
		onr.setHighwayDefaults(3, "primary", 1, 50/3.6, 0.5, 1000);
		onr.setHighwayDefaults(3, "primary_link", 1, 50/3.6, 0.5, 1000);
		
		onr.setHighwayDefaults(4, "secondary", 1, 50/3.6, 0.5, 1000);
		
		onr.setHighwayDefaults(5, "tertiary", 1, 30/3.6, 0.8, 600);
		
		onr.setHighwayDefaults(6, "minor", 1, 30/3.6, 0.8, 600);
		onr.setHighwayDefaults(6, "unclassified", 1, 30/3.6, 0.8, 600);
		onr.setHighwayDefaults(6, "residential", 1, 30/3.6, 0.6, 600);
		onr.setHighwayDefaults(6, "living_street", 1, 15/3.6, 1., 600);
		
		onr.setHierarchyLayer(48.0928, 9.6268, 46.6645, 12.4365, 4); //secondary network of survey area
		onr.setHierarchyLayer(47.7389, 10.8662, 47.3793, 11.4251, 6); //complete ways of lk garmisch-partenkirchen
		onr.setHierarchyLayer(47.4330, 11.1034, 47.2871, 11.2788, 6); //complete ways of seefeld & leutasch
		onr.setHierarchyLayer(47.5851, 10.6597, 47.5638, 10.7142, 6); //complete ways of füssen
//		onr.setKeepPaths(true);
		
		onr.parse(osmFile);
		
		new NetworkCleaner().run(network);
		
	}
	
	public static void createMultimodalNetwork(Network network, String outputNetworkFile){
		
		Set<String> allowedModes = new HashSet<>();
		allowedModes.add(TransportMode.car);
		allowedModes.add(TransportMode.bike);
		allowedModes.add(TransportMode.walk);
		
		for(Link link : network.getLinks().values()){
			
			if(link.getFreespeed() <= 50 / 3.6){
				link.setAllowedModes(allowedModes);
			}
			
		}
		
		new NetworkWriter(network).write(outputNetworkFile);
		
	}
	
	public static void createAdditionalLinks2025(Network network, String outputNetworkFile){
		
		NetworkFactory netFactory = network.getFactory();
		
		Set<String> modes = new HashSet<>();
		modes.add(TransportMode.car);
		double laneCapacity = 1000;
		double nLanes = 2;
		double freespeed = 50/3.6;
		double freespeedFactor = 0.5;
		
		Link tunnelGP01 = netFactory.createLink(Id.createLinkId("tGP01"), network.getNodes().get(Id.createNodeId("389886914")), network.getNodes().get(Id.createNodeId("2835402")));
		tunnelGP01.setAllowedModes(modes);
		tunnelGP01.setCapacity(laneCapacity * nLanes);
		tunnelGP01.setFreespeed(freespeed * freespeedFactor);
		tunnelGP01.setLength(4780);
		tunnelGP01.setNumberOfLanes(nLanes);
		network.addLink(tunnelGP01);
		
		Link tunnelGP02 = netFactory.createLink(Id.createLinkId("tGP02"), network.getNodes().get(Id.createNodeId("2835402")), network.getNodes().get(Id.createNodeId("389886914")));
		tunnelGP02.setAllowedModes(modes);
		tunnelGP02.setCapacity(laneCapacity * nLanes);
		tunnelGP02.setFreespeed(80/3.6*0.5);
		tunnelGP02.setLength(4780);
		tunnelGP02.setNumberOfLanes(1);
		network.addLink(tunnelGP02);
		
		Link tunnelGP03 = netFactory.createLink(Id.createLinkId("tGP03"), network.getNodes().get(Id.createNodeId("274282")), network.getNodes().get(Id.createNodeId("349932512")));
		tunnelGP03.setAllowedModes(modes);
		tunnelGP03.setCapacity(laneCapacity * nLanes);
		tunnelGP03.setFreespeed(80/3.6*0.5);
		tunnelGP03.setLength(4840);
		tunnelGP03.setNumberOfLanes(1);
		network.addLink(tunnelGP03);
		
		Link tunnelGP04 = netFactory.createLink(Id.createLinkId("tGP04"), network.getNodes().get(Id.createNodeId("349932512")), network.getNodes().get(Id.createNodeId("274312")));
		tunnelGP04.setAllowedModes(modes);
		tunnelGP04.setCapacity(laneCapacity * nLanes);
		tunnelGP04.setFreespeed(80/3.6*0.5);
		tunnelGP04.setLength(4840);
		tunnelGP04.setNumberOfLanes(1);
		network.addLink(tunnelGP04);
		
		Link tunnelGP05 = netFactory.createLink(Id.createLinkId("tGP05"), network.getNodes().get(Id.createNodeId("1628744409")), network.getNodes().get(Id.createNodeId("503680756")));
		tunnelGP05.setAllowedModes(modes);
		tunnelGP05.setCapacity(laneCapacity * nLanes);
		tunnelGP05.setFreespeed(80/3.6*0.5);
		tunnelGP05.setLength(6880);
		tunnelGP05.setNumberOfLanes(1);
		network.addLink(tunnelGP05);
		
		Link tunnelGP06 = netFactory.createLink(Id.createLinkId("tGP06"), network.getNodes().get(Id.createNodeId("503680756")), network.getNodes().get(Id.createNodeId("1628744409")));
		tunnelGP06.setAllowedModes(modes);
		tunnelGP06.setCapacity(laneCapacity * nLanes);
		tunnelGP06.setFreespeed(80/3.6*0.5);
		tunnelGP06.setLength(6880);
		tunnelGP06.setNumberOfLanes(1);
		network.addLink(tunnelGP06);
		
		new NetworkWriter(network).write(outputNetworkFile);
		
	}
	
	public static void createZone30InAllOfGarmisch(Network network, String outputNetworkFile){
		
		double minSpeedToIgnore = 60 / 3.6 * 0.5;
		double maxSpeed = 30/3.6 * 0.6;
		
		//read in built area shapefile
		Collection<SimpleFeature> builtAreas = new ShapeFileReader().readFileAndInitialize(Global.adminBordersDir + "Gebietsstand_2007/gemeinden_2007_bebaut.shp");
		
		Geometry geometry = null;
		for(SimpleFeature feature : builtAreas){
			
			//search for the Garmisch-Partenkirchen geometry and make it selected
			Long identifier = (Long) feature.getAttribute("GEM_KENNZ");
			if(identifier == Long.parseLong(Global.idGarmischPartenkirchen)){
				geometry = (Geometry) feature.getDefaultGeometry();
			}
			
		}
		
		//parse all links and lower their max speed
		for(Link link : network.getLinks().values()){
			
			com.vividsolutions.jts.geom.Point point = MGC.coord2Point(Global.UTM32NtoGK4.transform(link.getCoord()));
			
			if(geometry.contains(point)){
				
				//make sure, the max speed of links is not accidentially increased!!!
				if(link.getFreespeed() <= minSpeedToIgnore){
					
					double speed = Math.min(link.getFreespeed(), maxSpeed);

					if(link.getFreespeed() != speed){
						link.setCapacity(600 * link.getNumberOfLanes());
					}
					link.setFreespeed(speed);
					
				}
				
			}
			
		}
		
		new NetworkWriter(network).write(outputNetworkFile);
		
	}
	
}

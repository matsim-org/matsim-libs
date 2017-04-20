/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.santiago.network;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;

import playground.benjamin.utils.MergeNetworks;
import playground.santiago.SantiagoScenarioConstants;

/**
 * @author dhosse, kturner, benjamin
 *
 */
public class SantiagoNetworkBuilder {
	private static final Logger log = Logger.getLogger(SantiagoNetworkBuilder.class);
	
	final boolean prepareForModeChoice = false;
//	final boolean prepareForModeChoice = true;
	
//	private final String svnWorkingDir = "/Users/michaelzilske/svn/santiago/scenario/";
	private final String svnWorkingDir = "../../../shared-svn/projects/santiago/scenario/";
	private final String workingDirInputFiles = svnWorkingDir + "inputFromElsewhere/";
//	private final String outputDir = svnWorkingDir + "inputFromElsewhere/network/";
	private final String outputDir = svnWorkingDir + "inputFromElsewhere/TMP_networkOSM/";
	
	private final String transitNetworkFile = svnWorkingDir + "inputForMATSim/TMP_Transit/transitnetwork.xml.gz";
	
	public static void main(String[] args) {
		SantiagoNetworkBuilder snb = new SantiagoNetworkBuilder();
		snb.run();
	}
	
	private void run() {
		createDir(new File(outputDir));

		String crs = SantiagoScenarioConstants.toCRS;
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, crs);
		
		double[] boundingBox1 = new double[]{-71.3607, -33.8875, -70.4169, -33.0144};
		double[] boundingBox2 = new double[]{-70.9, -33.67, -70.47, -33.27};
		double[] boundingBox3 = new double[]{-71.0108, -33.5274, -70.9181, -33.4615};
		
		Network network = (Network) ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		OsmNetworkReader onr = new OsmNetworkReader(network, ct);
		onr.setHierarchyLayer(boundingBox1[3], boundingBox1[0], boundingBox1[1], boundingBox1[2], 4);
		onr.setHierarchyLayer(boundingBox2[3], boundingBox2[0], boundingBox2[1], boundingBox2[2], 5);
		onr.setHierarchyLayer(boundingBox3[3], boundingBox3[0], boundingBox3[1], boundingBox3[2], 5);
		onr.parse(workingDirInputFiles + "networkOSM/santiago_tertiary.osm");
		new NetworkCleaner().run(network);
		
		addSomeLinks(network);
		removeSomeLinks(network);
		changeNumberOfLanes(network);
		
		createRoadTypeMappingForHBEFA(network);
		changeFreespeedInSecondaryNetwork(network);
		addNetworkModes(network);
		
		if(prepareForModeChoice) mergeWithTransitNetwork(network);
		
		new NetworkWriter(network).write(outputDir + "network_tertiary.xml");
		
		convertNet2Shape(network, crs, outputDir + "networkShp/"); 
		printBoundingBox(network);
		log.info("Finished network creation.");
	}

	private void mergeWithTransitNetwork(Network network) {
		Scenario ptScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		ptScenario.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspDefaultsCheckingLevel.ignore);
		new MatsimNetworkReader(ptScenario.getNetwork()).readFile(transitNetworkFile);
//		new NetworkCleaner().run(ptScenario.getNetwork());
		// Hack in order to avoid pt jamming on the routing network
		// TODO: should not be necessary if pt is non-congested mode in qsim?
		for(Link ptLink : ptScenario.getNetwork().getLinks().values()){
			ptLink.setCapacity(200 * ptLink.getCapacity());
			// TODO: how to adjust storage capacity??
		}
		new MergeNetworks().merge(network, "", ptScenario.getNetwork());
	}

	private void changeNumberOfLanes(Network network) {
		//change number of lanes according to kt's e-mail
		int newNLanes = 2;
		network.getLinks().get(Id.createLinkId("10308")).setCapacity(network.getLinks().get(Id.createLinkId("10308")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10308")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10308")).setNumberOfLanes(newNLanes);

		network.getLinks().get(Id.createLinkId("10309")).setCapacity(network.getLinks().get(Id.createLinkId("10309")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10309")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10309")).setNumberOfLanes(newNLanes);

		network.getLinks().get(Id.createLinkId("10310")).setCapacity(network.getLinks().get(Id.createLinkId("10310")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10310")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10310")).setNumberOfLanes(newNLanes);

		network.getLinks().get(Id.createLinkId("10311")).setCapacity(network.getLinks().get(Id.createLinkId("10311")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10311")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10311")).setNumberOfLanes(newNLanes);

		network.getLinks().get(Id.createLinkId("10326")).setCapacity(network.getLinks().get(Id.createLinkId("10326")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10326")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10326")).setNumberOfLanes(newNLanes);

		network.getLinks().get(Id.createLinkId("10327")).setCapacity(network.getLinks().get(Id.createLinkId("10327")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10327")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10327")).setNumberOfLanes(newNLanes);

		network.getLinks().get(Id.createLinkId("10328")).setCapacity(network.getLinks().get(Id.createLinkId("10328")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10328")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10328")).setNumberOfLanes(newNLanes);

		network.getLinks().get(Id.createLinkId("10329")).setCapacity(network.getLinks().get(Id.createLinkId("10329")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10329")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10329")).setNumberOfLanes(newNLanes);

		network.getLinks().get(Id.createLinkId("10330")).setCapacity(network.getLinks().get(Id.createLinkId("10330")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10330")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10330")).setNumberOfLanes(newNLanes);

		network.getLinks().get(Id.createLinkId("10331")).setCapacity(network.getLinks().get(Id.createLinkId("10331")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10331")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10331")).setNumberOfLanes(newNLanes);

		network.getLinks().get(Id.createLinkId("10332")).setCapacity(network.getLinks().get(Id.createLinkId("10332")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10332")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10332")).setNumberOfLanes(newNLanes);

		network.getLinks().get(Id.createLinkId("10333")).setCapacity(network.getLinks().get(Id.createLinkId("10333")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10333")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10333")).setNumberOfLanes(newNLanes);

		network.getLinks().get(Id.createLinkId("10334")).setCapacity(network.getLinks().get(Id.createLinkId("10334")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10334")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10334")).setNumberOfLanes(newNLanes);

		network.getLinks().get(Id.createLinkId("10335")).setCapacity(network.getLinks().get(Id.createLinkId("10335")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10335")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10335")).setNumberOfLanes(newNLanes);

		network.getLinks().get(Id.createLinkId("10336")).setCapacity(network.getLinks().get(Id.createLinkId("10336")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10336")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10336")).setNumberOfLanes(newNLanes);

		network.getLinks().get(Id.createLinkId("10337")).setCapacity(network.getLinks().get(Id.createLinkId("10337")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10337")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10337")).setNumberOfLanes(newNLanes);

		network.getLinks().get(Id.createLinkId("21383")).setCapacity(network.getLinks().get(Id.createLinkId("21383")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("21383")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("21383")).setNumberOfLanes(newNLanes);

		network.getLinks().get(Id.createLinkId("21384")).setCapacity(network.getLinks().get(Id.createLinkId("21384")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("21384")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("21384")).setNumberOfLanes(newNLanes);

		network.getLinks().get(Id.createLinkId("21381")).setCapacity(network.getLinks().get(Id.createLinkId("21381")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("21381")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("21381")).setNumberOfLanes(newNLanes);

		network.getLinks().get(Id.createLinkId("21382")).setCapacity(network.getLinks().get(Id.createLinkId("21382")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("21382")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("21382")).setNumberOfLanes(newNLanes);

		network.getLinks().get(Id.createLinkId("16272")).setCapacity(network.getLinks().get(Id.createLinkId("16272")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("16272")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("16272")).setNumberOfLanes(newNLanes);

		network.getLinks().get(Id.createLinkId("16273")).setCapacity(network.getLinks().get(Id.createLinkId("16273")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("16273")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("16273")).setNumberOfLanes(newNLanes);

		network.getLinks().get(Id.createLinkId("16270")).setCapacity(network.getLinks().get(Id.createLinkId("16270")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("16270")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("16270")).setNumberOfLanes(newNLanes);

		network.getLinks().get(Id.createLinkId("16271")).setCapacity(network.getLinks().get(Id.createLinkId("16271")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("16271")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("16271")).setNumberOfLanes(newNLanes);

		network.getLinks().get(Id.createLinkId("16268")).setCapacity(network.getLinks().get(Id.createLinkId("16268")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("16268")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("16268")).setNumberOfLanes(newNLanes);

		network.getLinks().get(Id.createLinkId("16269")).setCapacity(network.getLinks().get(Id.createLinkId("16269")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("16269")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("16269")).setNumberOfLanes(newNLanes);

		network.getLinks().get(Id.createLinkId("19484")).setCapacity(network.getLinks().get(Id.createLinkId("19484")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("19484")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("19484")).setNumberOfLanes(newNLanes);

		network.getLinks().get(Id.createLinkId("19485")).setCapacity(network.getLinks().get(Id.createLinkId("19485")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("19485")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("19485")).setNumberOfLanes(newNLanes);
	}

	private void removeSomeLinks(Network network) {
		//remove small streets in the south-west of the network
		network.removeLink(Id.createLinkId("4978"));
		network.removeLink(Id.createLinkId("9402"));
	}

	private void addSomeLinks(Network network) {
		//create connection links (according to e-mail from kt 2015-07-27)
		NetworkFactory netFactory = (NetworkFactory) network.getFactory();
		Node node = netFactory.createNode(Id.createNodeId("n_add_01"), new Coord((double) 345165, (double) 6304696));
		network.addNode(node);
		final Network network1 = network;

		Link link01 = NetworkUtils.createLink(Id.createLinkId("l_add_01"), network.getNodes().get(Id.createNodeId("n_add_01")), network.getNodes().get(Id.createNodeId("267315588")), network1, 50.2, 40/3.6, (double) 600, (double) 1);
		network.addLink(link01);
		final Network network2 = network;
		Link link02 = NetworkUtils.createLink(Id.createLinkId("l_add_02"), network.getNodes().get(Id.createNodeId("267315588")), network.getNodes().get(Id.createNodeId("n_add_01")), network2, 50.2, 40/3.6, (double) 600, (double) 1);
		network.addLink(link02);
		final Network network3 = network;
		Link link03 = NetworkUtils.createLink(Id.createLinkId("l_add_03"), network.getNodes().get(Id.createNodeId("267315579")), network.getNodes().get(Id.createNodeId("n_add_01")), network3, 58.23, 40/3.6, (double) 600, (double) 1);
		network.addLink(link03);
		final Network network4 = network;
		Link link04 = NetworkUtils.createLink(Id.createLinkId("l_add_04"), network.getNodes().get(Id.createNodeId("n_add_01")), network.getNodes().get(Id.createNodeId("267315579")), network4, 58.23, 40/3.6, (double) 600, (double) 1);
		network.addLink(link04);
		final Network network5 = network;
		Link link05 = NetworkUtils.createLink(Id.createLinkId("l_add_05"), network.getNodes().get(Id.createNodeId("n_add_01")), network.getNodes().get(Id.createNodeId("267315716")), network5, 233.03, 40/3.6, (double) 600, (double) 1);
		network.addLink(link05);
		final Network network6 = network;
		Link link06 = NetworkUtils.createLink(Id.createLinkId("l_add_06"), network.getNodes().get(Id.createNodeId("267315716")), network.getNodes().get(Id.createNodeId("n_add_01")), network6, 233.03, 40/3.6, (double) 600, (double) 1);
		network.addLink(link06);
	}

	private void createRoadTypeMappingForHBEFA(Network network) {
		for(Link ll : network.getLinks().values()){
			double fs = ll.getFreespeed();
			// TODO: rural areas might not be not considered; count the cases and decide...
			if(fs <= 8.333333334){ //30kmh
				NetworkUtils.setType( ((Link) ll), (String) "URB/Access/30");
			} else if(fs <= 11.111111112){ //40kmh
				NetworkUtils.setType( ((Link) ll), (String) "URB/Access/40");
			} else if(fs <= 13.888888889){ //50kmh
				double lanes = ll.getNumberOfLanes();
				if(lanes <= 1.0){
					NetworkUtils.setType( ((Link) ll), (String) "URB/Local/50");
				} else if(lanes <= 2.0){
					NetworkUtils.setType( ((Link) ll), (String) "URB/Distr/50");
				} else if(lanes > 2.0){
					NetworkUtils.setType( ((Link) ll), (String) "URB/Trunk-City/50");
				} else{
					throw new RuntimeException("NoOfLanes not properly defined");
				}
			} else if(fs <= 16.666666667){ //60kmh
				double lanes = ll.getNumberOfLanes();
				if(lanes <= 1.0){
					NetworkUtils.setType( ((Link) ll), (String) "URB/Local/60");
				} else if(lanes <= 2.0){
					NetworkUtils.setType( ((Link) ll), (String) "URB/Trunk-City/60");
				} else if(lanes > 2.0){
					NetworkUtils.setType( ((Link) ll), (String) "URB/MW-City/60");
				} else{
					throw new RuntimeException("NoOfLanes not properly defined");
				}
			} else if(fs <= 19.444444445){ //70kmh
				double lanes = ll.getNumberOfLanes();
				if(lanes <= 1.0){
					NetworkUtils.setType( ((Link) ll), (String) "RUR/Distr/70");
				} else if(lanes <= 2.0){
					NetworkUtils.setType( ((Link) ll), (String) "RUR/Trunk/70");
				} else if(lanes > 2.0){
					NetworkUtils.setType( ((Link) ll), (String) "URB/MW-City/70");
				} else{
					throw new RuntimeException("NoOfLanes not properly defined");
				}
			} else if(fs <= 22.222222223){ //80kmh
				double lanes = ll.getNumberOfLanes();
				if(lanes <= 1.0){
					NetworkUtils.setType( ((Link) ll), (String) "RUR/Distr/80");
				} else if(lanes <= 2.0){
					NetworkUtils.setType( ((Link) ll), (String) "RUR/Trunk/80");
				} else if(lanes > 2.0){
					NetworkUtils.setType( ((Link) ll), (String) "URB/MW-Nat./80");
				} else{
					throw new RuntimeException("NoOfLanes not properly defined");
				}
			} else if(fs <= 27.777777778){ //100kmh
				double lanes = ll.getNumberOfLanes();
				if(lanes <= 1.0){
					NetworkUtils.setType( ((Link) ll), (String) "RUR/Distr/100");
				} else if(lanes > 1.0){
					NetworkUtils.setType( ((Link) ll), (String) "RUR/MW/100");
				} else{
					throw new RuntimeException("NoOfLanes not properly defined");
				}
			} else if(fs <= 33.333333334){ //120kmh
				NetworkUtils.setType( ((Link) ll), (String) "RUR/MW/120");
			} else if(fs > 33.333333334){ //faster
				NetworkUtils.setType( ((Link) ll), (String) "RUR/MW/>130");
			} else{
				throw new RuntimeException("Link not considered...");
			}
		}
	}

	private void changeFreespeedInSecondaryNetwork(Network network) {
		for(Link ll : network.getLinks().values()){
			double fs = ll.getFreespeed();
			if(fs <= 8.333333334){ //30kmh
				((Link) ll).setFreespeed(0.5 * ll.getFreespeed());
			} else if(fs <= 11.111111112){ //40kmh
				((Link) ll).setFreespeed(0.5 * ll.getFreespeed());
			} else if(fs <= 13.888888889){ //50kmh
				double lanes = ll.getNumberOfLanes();
				if(lanes <= 1.0){
					((Link) ll).setFreespeed(0.5 * ll.getFreespeed());
				} else if(lanes <= 2.0){
					((Link) ll).setFreespeed(0.75 * ll.getFreespeed());
				} else if(lanes > 2.0){
					// link assumed to not have second-row parking, traffic lights, bikers/pedestrians crossing etc.
				} else{
					throw new RuntimeException("NoOfLanes not properly defined");
				}
			} else if(fs <= 16.666666667){ //60kmh
				double lanes = ll.getNumberOfLanes();
				if(lanes <= 1.0){
					((Link) ll).setFreespeed(0.5 * ll.getFreespeed());
				} else if(lanes <= 2.0){
					((Link) ll).setFreespeed(0.75 * ll.getFreespeed());
				} else if(lanes > 2.0){
					// link assumed to not have second-row parking, traffic lights, bikers/pedestrians crossing etc.
				} else{
					throw new RuntimeException("NoOfLanes not properly defined");
				}
			} else if(fs > 16.666666667){
				// link assumed to not have second-row parking, traffic lights, bikers/pedestrians crossing etc.
			} else{
				throw new RuntimeException("Link not considered...");
			}
		}
	}

	private void addNetworkModes(Network network) {
		Set<String> allowedModes = new HashSet<>();
		allowedModes.add(TransportMode.car);
		allowedModes.add(TransportMode.ride);
		allowedModes.add(SantiagoScenarioConstants.Modes.taxi.toString());
		allowedModes.add(SantiagoScenarioConstants.Modes.colectivo.toString());
		allowedModes.add(SantiagoScenarioConstants.Modes.other.toString());
//		allowedModes.add(SantiagoScenarioConstants.Modes.motorcycle.toString());
//		allowedModes.add(SantiagoScenarioConstants.Modes.school_bus.toString());
		for(Link ll : network.getLinks().values()){
			ll.setAllowedModes(allowedModes);
		}
	}

	private void convertNet2Shape(Network network, String crs, String outputDir){
		createDir(new File(outputDir));
		
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		PolylineFeatureFactory linkFactory = new PolylineFeatureFactory.Builder().
				setCrs(MGC.getCRS(crs)).
				setName("link").
				addAttribute("ID", String.class).
				addAttribute("fromID", String.class).
				addAttribute("toID", String.class).
				addAttribute("length", Double.class).
				addAttribute("type", String.class).
				addAttribute("capacity", Double.class).
				addAttribute("freespeed", Double.class).
				create();

		for (Link link : network.getLinks().values()) {
			Coordinate fromNodeCoordinate = new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
			Coordinate toNodeCoordinate = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
			Coordinate linkCoordinate = new Coordinate(link.getCoord().getX(), link.getCoord().getY());
			SimpleFeature ft = linkFactory.createPolyline(new Coordinate [] {fromNodeCoordinate, linkCoordinate, toNodeCoordinate},
					new Object [] {link.getId().toString(), link.getFromNode().getId().toString(),link.getToNode().getId().toString(), link.getLength(), NetworkUtils.getType(((Link)link)), link.getCapacity(), link.getFreespeed()}, null);
			features.add(ft);
		}   
		ShapeFileWriter.writeGeometries(features, outputDir + "network_merged_cl_Links.shp");

		features.clear();

		PointFeatureFactory nodeFactory = new PointFeatureFactory.Builder().
				setCrs(MGC.getCRS(crs)).
				setName("nodes").
				addAttribute("ID", String.class).
				create();

		for (Node node : network.getNodes().values()) {
			SimpleFeature ft = nodeFactory.createPoint(node.getCoord(), new Object[] {node.getId().toString()}, null);
			features.add(ft);
		}
		ShapeFileWriter.writeGeometries(features, outputDir + "network_merged_cl_Nodes.shp");
	}
	
	private void printBoundingBox(Network network) {
		double[] box = NetworkUtils.getBoundingBox(network.getNodes().values());
		log.info("Network bounding box:");
		log.info("minX "+ box[0]);
		log.info("minY "+ box[1]);
		log.info("maxX "+ box[2]);
		log.info("maxY "+ box[3]);
	}
	
	private void createDir(File file) {
		log.info("Directory " + file + " created: "+ file.mkdirs());	
	}
}

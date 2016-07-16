/* *********************************************************************** *
 * project: org.matsim.*
 * SetLinkAttributesControlerListener.java
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
package playground.benjamin.scenarios.munich;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.opengis.feature.simple.SimpleFeature;

import java.util.Collection;

/**
 * @author benjamin
 *
 */
public class SetLinkAttributesControlerListener implements StartupListener {
	private static final Logger logger = Logger.getLogger(SetLinkAttributesControlerListener.class);

	private final Collection<SimpleFeature> featuresInZone30;
	private final String outputFile;

	public SetLinkAttributesControlerListener(Collection<SimpleFeature> featuresInZone30, String outputFile) {
		this.featuresInZone30 = featuresInZone30;
		this.outputFile = outputFile;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
        Network network = event.getServices().getScenario().getNetwork();
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network zone30Links = (Network) sc.getNetwork();

		for(Link link : network.getLinks().values()){
			Id linkId = link.getId();
			Link ll = (Link) network.getLinks().get(linkId);
			if(isLinkInShape(ll)){
				logger.info("Changing freespeed of link " + ll.getId() + " from " + ll.getFreespeed() + " to 8.3333333334.");
				ll.setFreespeed(30 / 3.6);
				if(ll.getNumberOfLanes() == 1){
					logger.info("Changing type of link " + ll.getId() + " from " + NetworkUtils.getType(ll) + " to 75.");
					NetworkUtils.setType( ll, (String) "75");
					logger.info("Changing capacity of link " + ll.getId() + " from " + ll.getCapacity() + " to 11200.");
					ll.setCapacity(11200);
				}
				else{
					logger.info("Changing type of link " + ll.getId() + " from " + NetworkUtils.getType(ll) + " to 83.");
					NetworkUtils.setType( ll, (String) "83");
					logger.info("Changing capacity of link " + ll.getId() + " from " + ll.getCapacity() + " to 20000.");
					ll.setCapacity(20000);
				}

				Id fromId = ll.getFromNode().getId();
				Id toId = ll.getToNode().getId();
				Node from = null;
				Node to = null;
				Node nn;
				//check if from node already exists
				if (! zone30Links.getNodes().containsKey(fromId)) {
					nn = network.getNodes().get(fromId);
					from = addNode(zone30Links, nn);
				}
				else {
					from = zone30Links.getNodes().get(fromId);
				}
				//check if to node already exists
				if (! zone30Links.getNodes().containsKey(toId)){
					nn = network.getNodes().get(toId);
					to = addNode(zone30Links, nn);
				}
				else {
					to = zone30Links.getNodes().get(toId);
				}
				Link lll = zone30Links.getFactory().createLink(ll.getId(), from, to);
				lll.setAllowedModes(ll.getAllowedModes());
				lll.setCapacity(ll.getCapacity());
				lll.setFreespeed(ll.getFreespeed());
				lll.setLength(ll.getLength());
				lll.setNumberOfLanes(ll.getNumberOfLanes());
				zone30Links.addLink(lll);
			}
		}
		new Links2ESRIShape(zone30Links, outputFile, "DHDN_GK4").write();
	}

	private Node addNode(Network net, Node n){
		Node newNode = net.getFactory().createNode(n.getId(), n.getCoord());
		net.addNode(newNode);
		return newNode;
	}
	
	private boolean isLinkInShape(Link link) {
		boolean isInShape = false;
		Coord coord = link.getCoord();
		GeometryFactory factory = new GeometryFactory();
		Geometry geo = factory.createPoint(new Coordinate(coord.getX(), coord.getY()));
		for(SimpleFeature feature : this.featuresInZone30){
			if(((Geometry) feature.getDefaultGeometry()).contains(geo)){
				isInShape = true;
				break;
			}
		}
		return isInShape;
	}
}

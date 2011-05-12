/* *********************************************************************** *
 * project: org.matsim.*
 * DgSignals2Shape
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.DgPaths;
import playground.dgrether.utils.DgNet2Shape;


/**
 * @author dgrether
 *
 */
public class DgSignalizedLinks2Shape {

	
	private static final Logger log = Logger.getLogger(DgSignalizedLinks2Shape.class);
	
	private CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84_UTM33N);
	
	public Network getSignalizedLinks(SignalSystemsData ssd, Network net) {
		Config c2 = ConfigUtils.createConfig();
		Scenario sc2 = ScenarioUtils.createScenario(c2);
		Network net2 = sc2.getNetwork();
		
		for (SignalSystemData signalSystem : ssd.getSignalSystemData().values()){
			for (SignalData signal : signalSystem.getSignalData().values()){
				Id linkId = signal.getLinkId();
				Link link = net.getLinks().get(linkId);
				if (link == null){
					log.warn("link " + linkId + " not in the network!");
					continue;
				}
			  Node node = link.getFromNode();
			  node.getOutLinks().clear();
			  node.getInLinks().clear();
				net2.addNode(node);
				node = link.getToNode();
			  node.getOutLinks().clear();
			  node.getInLinks().clear();
				net2.addNode(node);
				net2.addLink(link);
			}
		}
		return net2;
	}
	
	
	public void getSignalizedLinksAndWrite2Shape(Scenario sc, String shapeFilename){
		SignalsData sd = sc.getScenarioElement(SignalsData.class);
		if (sd == null)	return;
		SignalSystemsData ssd = sd.getSignalSystemsData();
		Network net = sc.getNetwork();
		Network net2 = this.getSignalizedLinks(ssd, net);
		
		new DgNet2Shape().write(net2, shapeFilename, crs);
	}
	
	
	public static void main(String[] args) {
		String configFilename = DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/config.xml";
		String shapeFilename = DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/network_small/signalized_links.shp";
		Config config = ConfigUtils.loadConfig(configFilename);
		Scenario sc = ScenarioUtils.loadScenario(config);
		new DgSignalizedLinks2Shape().getSignalizedLinksAndWrite2Shape(sc, shapeFilename);
	}
	
	
}

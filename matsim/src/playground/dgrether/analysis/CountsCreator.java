/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.dgrether.analysis;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Route;
import org.matsim.population.RouteImpl;

/**
 * @author dgrether
 *
 */
public class CountsCreator {

	private static final Logger log = Logger.getLogger(CountsCreator.class);

	private NetworkLayer network;

	/*
	 * main NODES: 9233, 1563, 1558, 1562, 1564, 1000597, 1565, 1525, 1519, 1518,
	 * 1776, 1780, 1000498, 1000497, 1786, 1000470, 1794, 1795, 1797, 1806, 1809,
	 * 1811, 1758, 1666, 1757, 1754(last common node)
	 *
	 *
	 * alt NODES: 9231, 9234, 9243, 9218, 9189, 9210, 9193, 9206, 9283, 9295,
	 * 9299, 9297, 8356, 9288, 9305,
	 */

	public CountsCreator(String networkPath) {
		Gbl.createConfig(null);
		Gbl.getConfig().simulation().setFlowCapFactor(0.13);
		Gbl.getConfig().simulation().setStorageCapFactor(0.13);
		this.network = loadNetwork(networkPath);
		log.info("  creating routes...");
		Route r1 = new RouteImpl();
		//the nodes of carls master thesis main route
//		r1.setRoute("9232  1563  1558  1562  1564  1000597  1565  1525  1519  1518 1776  1780  1000498  1000497  1786  1000470  1794  1795  1797  1806  1809  1811  1758  1757  1754");
		//the nodes of davids otf scenario
//		r1.setRoute("9296 9298 9294 9282 9281");
		//the new route
		r1.setRoute("1341 5268 1342 1348 1349 1375 1585 1581 1583");
		Route r2 = new RouteImpl();
		//the nodes of carls master thesis alternative route
//		r2.setRoute("9231 9234 9243 9218 9189 9210 9193 9206 9283 9295 9299 9297 9285 9288 9305");
		//the nodes of davids otf scenario
//		r2.setRoute("9286 1725 1727 1722 1718 9284 9281");
		//the new routes
		r2.setRoute("1583 1588 1597 1596 1591 1593 1600 2216 1603 1601 9250 9254");
		Route r3 = new RouteImpl();
		r3.setRoute("1583 1587 1641 1644 1645 1648 5210 2420 9275 9272");
		Route r4 = new RouteImpl();
		r4.setRoute("1370 9180 9183 9181 9261 9265 9269 9408 9409 9404 9400 9403 9402 9398 9394 9414 9276");

		log.info("  creating routes done");
		Set<Route> routes = new HashSet<Route>();
		routes.add(r1);
		routes.add(r2);
		routes.add(r3);
		routes.add(r4);

		Set<Link> links = new HashSet<Link>(r2.getRoute().size() + r1.getRoute().size());
		for (Route r : routes) {
			for (Link l : r.getLinkRoute()) {
				if (!links.contains(l))
					links.add(l);
			}
		}
		createCounts(links);
	}

	public void createCounts(Set<Link> links) {
		Counts counts = new Counts();
		Count c;
		counts.setLayer("superLayer");
		counts.setDescription("");
		counts.setName("noname china counts");
		counts.setYear(2005);
		QueueLink ql;
		for (Link l : links) {
			ql = (QueueLink) l;
			c = counts.createCount(l.getId(), l.getId().toString());
			for (int i = 1; i < 25; i++) {
				c.createVolume(i, ql.getSimulatedFlowCapacity()*3600);
			}
		}
		CountsWriter cw = new CountsWriter(counts, "./output/counts.xml");
		cw.write();
		log.info("counts written successfully to: ./output/counts.xml");
	}

	/**
	 * load the network
	 *
	 * @return the network layer
	 */
	protected NetworkLayer loadNetwork(final String networkFile) {
		// - read network: which buildertype??
		log.info("  creating network layer... ");
		NetworkLayer network = (NetworkLayer) Gbl.getWorld().createLayer(
				NetworkLayer.LAYER_TYPE, null);
		log.info("   done");

		log.info("    reading network xml file... ");
		new MatsimNetworkReader(network).readFile(networkFile);
		log.info("   done");
		return network;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new CountsCreator("../../cvsRep/vsp-cvs/studies/berlin-wip/network/wip_net.xml");
	}

}

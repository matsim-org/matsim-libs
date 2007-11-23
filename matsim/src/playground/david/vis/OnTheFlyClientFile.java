/* *********************************************************************** *
 * project: org.matsim.*
 * OnTheFlyClientFile.java
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

package playground.david.vis;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.List;

import org.matsim.basic.v01.BasicLink;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Route;

import playground.david.vis.interfaces.OTFServerRemote;


public class OnTheFlyClientFile extends Thread {
	OTFVisNet visnet = null;
	OTFGUI netVis = null;

	public void startVis(OTFServerRemote host) {
		netVis = new OTFGUI(visnet, host);
	}

	@Override
	public void run() {
		try {
					OTFServerRemote host = new OTFNetFileHandler(10,null,"OTFNetfile.vis");
					host.pause();
					Gbl.startMeasurement();

					// set new RouteBuilder
					Route.setNodeBuilder(new Route.NodeBuilder() {
						@Override
						@SuppressWarnings("unchecked")
						public void addNode(List route, String nodeId) {
							route.add(nodeId);
						}

					});
					Act.setLinkBuilder(new Act.LinkBuilder() {
						@Override
						public void addLink(Act act, String linkId) {
							act.setLink(new BasicLink(linkId));
						}
					});
					visnet = host.getNet(null);
					visnet.connect();
					System.out.println("get net time");
					Gbl.printElapsedTime();
					//DisplayNet network = prepareNet();
					for (int i=0; i<1;i++){
						host.step();
						//Plan plan = host.getAgentPlan("66128");
						//System.out.println("Plan:" + plan.toString());
						Gbl.startMeasurement();
						byte [] bbyte = host.getStateBuffer();
						System.out.println("get state time");
						Gbl.printElapsedTime();

						Gbl.startMeasurement();
						visnet.readMyself(new DataInputStream(new ByteArrayInputStream(bbyte,0,bbyte.length)));
						System.out.println("SET state time");
						Gbl.printElapsedTime();
						startVis(host);
						Thread.sleep(10000);
					}
		}catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		OnTheFlyClientFile client = new OnTheFlyClientFile();
		client.start();
	}

}

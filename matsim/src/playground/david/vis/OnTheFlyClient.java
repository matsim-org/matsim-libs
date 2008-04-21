/* *********************************************************************** *
 * project: org.matsim.*
 * OnTheFlyClient.java
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
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

import javax.rmi.ssl.SslRMIClientSocketFactory;

import org.matsim.basic.v01.BasicLinkImpl;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Route;

import playground.david.vis.interfaces.OTFServerRemote;


public class OnTheFlyClient extends Thread {
	OTFVisNet visnet = null;
	OTFGUI netVis = null;

	public void startVis(OTFServerRemote host) {
		netVis = new OTFGUI(visnet, host);
	}

	@Override
	public void run() {
		System.setProperty("javax.net.ssl.keyStore", "input/keystore");
		System.setProperty("javax.net.ssl.keyStorePassword", "vspVSP");
		System.setProperty("javax.net.ssl.trustStore", "input/truststore");
		System.setProperty("javax.net.ssl.trustStorePassword", "vspVSP");
		try {
			Thread.sleep(1000);
		    Registry registry = LocateRegistry.getRegistry(
		    		"127.0.0.1", 4019,
		    		new SslRMIClientSocketFactory());

		    	    // "obj" is the identifier that we'll use to refer
		    	    // to the remote object that implements the "Hello"
		    	    // interface
			String[] liste = registry.list();
			for (String name : liste) {
				if (name.indexOf("DSOTFServer_") != -1){
					OTFServerRemote host = (OTFServerRemote)registry.lookup(name);
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
							act.setLink(new BasicLinkImpl(linkId));
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
					//Thread.sleep(5000);
					//host.play();
				}
			}
		}catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		OnTheFlyClient client = new OnTheFlyClient();
		client.start();
	}

}

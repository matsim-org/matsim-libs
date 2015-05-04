/* *********************************************************************** *
 * project: org.matsim.*
 * DummyJuPedSim.java
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

package playground.gregor.grpc.dummyjupedsim;

import org.matsim.hybrid.MATSimInterface;
import org.matsim.hybrid.MATSimInterface.ExternalConnect;
import org.matsim.hybrid.MATSimInterface.ExternalConnectConfirmed;
import org.matsim.hybrid.MATSimInterfaceServiceGrpc.MATSimInterfaceServiceBlockingStub;

public final class DummyJuPedSim {

	public static void main(String [] args) {
		ExternalInterfaceServiceImpl ext = new ExternalInterfaceServiceImpl();
		JuPedSimServer server = new JuPedSimServer(ext);
		Thread t1 = new Thread(server);
		t1.start();
		
		JuPedSimClient c = new JuPedSimClient("localhost", 9999);
		MATSimInterfaceServiceBlockingStub bs = c.getBlockingStub();
		ExternalConnect req = MATSimInterface.ExternalConnect.newBuilder().setHost("localhost").setPort(9998).build();
		ExternalConnectConfirmed resp = bs.reqExternalConnect(req);
		
	}
	
}

/* *********************************************************************** *
 * project: org.matsim.*
 * OTFHostConnectionBuilder
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
package org.matsim.vis.otfvis.gui;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.rmi.ssl.SslRMIClientSocketFactory;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.vis.otfvis.data.fileio.OTFFileReader;
import org.matsim.vis.otfvis.interfaces.OTFLiveServerRemote;
import org.matsim.vis.otfvis.interfaces.OTFServerRemote;
import org.matsim.vis.otfvis.server.OTFTVehServer;


/**
 * @author dgrether
 *
 */
public class OTFHostConnectionBuilder {

	private static final Logger log = Logger.getLogger(OTFHostConnectionBuilder.class);
	
	private String address;
	
	private OTFServerRemote host = null;

	public OTFServerRemote createRemoteServerConnection(final String address) throws RemoteException, InterruptedException, NotBoundException{
		log.info("trying to open connection to " + address);
		// try to open/connect to host if given a string of form
		// connection type (rmi or file or tveh)
		// rmi:ip  [: port]
		// file:mvi-filename
		// tveh:T.veh-filename @ netfilename
		// e.g. "file:../MatsimJ/otfvis.mvi" or "rmi:127.0.0.1:4019" or "tveh:../MatsimJ/output/T.veh@../../studies/wip/network.xml"
		if (address == null) {
			this.address = "rmi:127.0.0.1:4019";
		} else {
			this.address = address;
		}

		String type = this.address.substring(0,this.address.indexOf(':'));
		String connection = this.address.substring(this.address.indexOf(':')+1, this.address.length());
		if (type.equals("rmi")) {
			int port = 4019;
			String name = null;
			String [] connparse = connection.split(":");
			if (connparse.length > 1 ) port = Integer.parseInt(connparse[1]);
			if (connparse.length > 2 ) name = connparse[2];
			this.host = openRMI(connparse[0], port, name);

		} else if (type.equals("ssl")) {
			int port = 4019;
			String name = null;
			String [] connparse = connection.split(":");
			if (connparse.length > 1 ) port = Integer.parseInt(connparse[1]);
			if (connparse.length > 2 ) name = connparse[2];
			this.host = openSSL(connparse[0], port, name);

		} else if (type.equals("file")) {
			this.host = openFile(connection);

		} else if (type.equals("tveh")) {
			String [] connparse = connection.split("@");
			this.host = openTVehFile(connparse[1], connparse[0]);

		} else {
			throw new UnsupportedOperationException("Connctiontype " + type + " not known");
		}
		return host;
	}
	
	private OTFServerRemote openSSL(String hostname, int port, String servername) throws InterruptedException, RemoteException, NotBoundException {
		System.setProperty("javax.net.ssl.keyStore", "input/keystore");
		System.setProperty("javax.net.ssl.keyStorePassword", "vspVSP");
		System.setProperty("javax.net.ssl.trustStore", "input/truststore");
		System.setProperty("javax.net.ssl.trustStorePassword", "vspVSP");

		Thread.sleep(1000);
		Registry registry = LocateRegistry.getRegistry(hostname, port, new SslRMIClientSocketFactory());

		if(servername == null) servername = "OTFServer_"; // take any

		String[] liste = registry.list();
		for (String name : liste) {
			if (name.indexOf(servername) != -1){
				this.host = (OTFServerRemote)registry.lookup(name);
				((OTFLiveServerRemote)host).pause();
			}
		}
		return host;
	}

	private OTFServerRemote openRMI(String hostname, int port, String servername) throws InterruptedException, RemoteException, NotBoundException {
		Thread.sleep(1000);
		Registry registry = LocateRegistry.getRegistry(hostname, port);

		if(servername == null) servername = "OTFServer_"; // take any

		String[] liste = registry.list();
		for (String name : liste) {
			if (name.indexOf(servername) != -1){
				this.host = (OTFServerRemote)registry.lookup(name);
				((OTFLiveServerRemote)host).pause();
			}
		}
		return host;
	}

	private OTFServerRemote openFile( String fileName) {
		OTFServerRemote host = new OTFFileReader(fileName);
		Gbl.printMemoryUsage();
		return host;
	}

	private OTFServerRemote openTVehFile(String netname, String vehname) {
		return new OTFTVehServer(netname,vehname);
	}
	
}

/* *********************************************************************** *
 * project: org.matsim.*
 * OTFNetFileHandler.java
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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.plans.Plan;
import org.matsim.utils.vis.netvis.streaming.SimStateWriterI;

import playground.david.vis.interfaces.OTFNetHandler;
import playground.david.vis.interfaces.OTFServerRemote;

public class OTFNetFileHandler implements SimStateWriterI, OTFServerRemote{

	private FileOutputStream outStream = null;
	private DataOutputStream outFile;
	private String fileName;

	private OTFVisNet net = null;
	public ByteArrayOutputStream out = null;
	double nextTime = 0;
	double intervall_s = 1;
	Map<Double,Long> timeSteps = new HashMap<Double,Long>();

	public OTFNetFileHandler(double intervall_s, QueueNetworkLayer network, String fileName) {
		if (network != null) net = new OTFVisNet(network);
		out = new ByteArrayOutputStream(500000);
		this.intervall_s = intervall_s;
		this.fileName = fileName;
	}

	public void close() throws IOException {
		try {
			outFile.close();
			outStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean dump(int time_s) throws IOException {
		if (time_s >= nextTime) {
			// dump time
			outFile.writeDouble(time_s);
			// get State
			//Gbl.startMeasurement();
			out.reset();
			net.writeMyself(null, new DataOutputStream(out));
			outFile.writeInt(out.size());
			out.writeTo(outFile);
			// dump State
			//Gbl.printElapsedTime();

			nextTime = time_s + intervall_s;
			return true;
		}
		return false;
	}

	public void open() {
		// open file
		try {
			outStream = new FileOutputStream(fileName);
			outFile = new DataOutputStream(outStream);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// dump some infos
		try {
			outFile.writeDouble(intervall_s);
			new ObjectOutputStream(outStream).writeObject(net);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// dump the network out
	}



	// IN
	private FileInputStream inStream = null;
	private DataInputStream inFile;

	public void readNet() {
		// open file
		try {
			inStream = new FileInputStream(fileName);
			inFile = new DataInputStream(inStream);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// dump some infos
		try {
			intervall_s = inFile.readDouble();
			net = (OTFVisNet) new ObjectInputStream(inStream).readObject();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// dump the network out
 catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void hasNextTimeStep() {

	}

	public void getNextTimeStep() {

	}

	public Plan getAgentPlan(String id) throws RemoteException {
		return null;
	}

	public int getLocalTime() throws RemoteException {
		return (int)nextTime;
	}

	public OTFVisNet getNet(OTFNetHandler handler) throws RemoteException {
		if (net == null) readNet();

		return net;
	}

	long filepos = 0;
	public byte[] getStateBuffer() throws RemoteException {
		int size =  0 ;
		byte [] result = null;

		try {
			nextTime = inFile.readDouble();
			size = inFile.readInt();

			result = new byte[size];
			int read = inFile.read(result);

			if (read != size) throw new IOException("READ SIZE did not fit! File corrupted!");
			timeSteps.put(nextTime, filepos);
			filepos += read;

		} catch (IOException e) {
			System.out.println(e.toString());
		}

		return result;
	}

	public void pause() throws RemoteException {
	}

	public void play() throws RemoteException {
	}

	public void setStatus(int status) throws RemoteException {
	}

	public void step() throws RemoteException {
	}

}

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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.plans.Plan;
import org.matsim.utils.collections.QuadTree.Rect;
import org.matsim.utils.vis.netvis.streaming.SimStateWriterI;

import playground.david.vis.data.OTFNetWriterFactory;
import playground.david.vis.data.OTFServerQuad;
import playground.david.vis.interfaces.OTFNetHandler;
import playground.david.vis.interfaces.OTFServerRemote;

public class OTFNetFileHandler implements SimStateWriterI, OTFServerRemote{

	private static final int BUFFERSIZE = 100000000;
	private OutputStream outStream = null;
	private DataOutputStream outFile;
	private final String fileName;

	private OTFVisNet net = null;
	public ByteArrayOutputStream out = null;
	double nextTime = 0;
	double intervall_s = 1;
	Map<Double,Long> timeSteps = new HashMap<Double,Long>();

	public OTFNetFileHandler(double intervall_s, QueueNetworkLayer network, String fileName) {
		if (network != null) this.net = new OTFVisNet(network);
		this.out = new ByteArrayOutputStream(500000);
		this.intervall_s = intervall_s;
		this.fileName = fileName;
	}

	public void close() throws IOException {
		try {
			this.outFile.close();
			this.outStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean dump(int time_s) throws IOException {
		if (time_s >= this.nextTime) {
			// dump time
			this.outFile.writeDouble(time_s);
			// get State
			//Gbl.startMeasurement();
			this.out.reset();
			this.net.writeMyself(null, new DataOutputStream(this.out));
			this.outFile.writeInt(this.out.size());
			this.out.writeTo(this.outFile);
			// dump State
			//Gbl.printElapsedTime();

			this.nextTime = time_s + this.intervall_s;
			return true;
		}
		return false;
	}

	public void open() {
		// open file
		try {
			if (this.fileName.endsWith(".gz")) {
				this.outStream = new GZIPOutputStream (new FileOutputStream(this.fileName),BUFFERSIZE);
			}else {
				this.outStream = new BufferedOutputStream(new FileOutputStream(this.fileName),BUFFERSIZE);
			}
			this.outFile = new DataOutputStream(this.outStream);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// dump some infos
		try {
			this.outFile.writeDouble(this.intervall_s);
			new ObjectOutputStream(this.outStream).writeObject(this.net);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// dump the network out
	}



	// IN
	private InputStream inStream = null;
	private DataInputStream inFile;

	public void readNet() {
		// open file
		try {
			if (this.fileName.endsWith(".gz")) {
				GZIPInputStream gzInStream =  new GZIPInputStream(new BufferedInputStream(new FileInputStream(this.fileName), BUFFERSIZE));
				this.inStream = gzInStream;
			} else {
				this.inStream = new FileInputStream(this.fileName);
			}
			this.inFile = new DataInputStream(this.inStream);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// dump some infos
		try {
			this.intervall_s = this.inFile.readDouble();
			this.net = (OTFVisNet) new ObjectInputStream(this.inStream).readObject();
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
		return (int)this.nextTime;
	}

	public OTFVisNet getNet(OTFNetHandler handler) throws RemoteException {
		if (this.net == null) readNet();

		return this.net;
	}

	long filepos = 0;
	public byte[] getStateBuffer() throws RemoteException {
		int size =  0 ;
		byte [] result = null;

		try {
			this.nextTime = this.inFile.readDouble();
			size = this.inFile.readInt();

			result = new byte[size];
			int offset = 0;
			int remain = size;
			int read = 0;
			while ((remain > 0) && (read != -1)){
				read = this.inFile.read(result,offset,remain);
				remain -= read;
				offset +=read;
			}

			if (offset != size) throw new IOException("READ SIZE did not fit! File corrupted!");
			this.timeSteps.put(this.nextTime, this.filepos);
			this.filepos += read;

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

	public boolean isLive() {
		return false;
	}

	public OTFServerQuad getQuad(String id, OTFNetWriterFactory writers)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	public byte[] getQuadConstStateBuffer(String id) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	public byte[] getQuadDynStateBuffer(String id, Rect bounds)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean requestNewTime(int time, TimePreference searchDirection) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}



}

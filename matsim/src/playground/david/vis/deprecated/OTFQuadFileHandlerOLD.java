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

package playground.david.vis.deprecated;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Plan;
import org.matsim.utils.collections.QuadTree.Rect;
import org.matsim.utils.vis.netvis.streaming.SimStateWriterI;

import playground.david.vis.OTFVisNet;
import playground.david.vis.data.OTFDefaultNetWriterFactoryImpl;
import playground.david.vis.data.OTFNetWriterFactory;
import playground.david.vis.data.OTFServerQuad;
import playground.david.vis.interfaces.OTFNetHandler;
import playground.david.vis.interfaces.OTFServerRemote;

public class OTFQuadFileHandlerOLD implements SimStateWriterI, OTFServerRemote{

	private static final int BUFFERSIZE = 100000000;
	private OutputStream outStream = null;
	private DataOutputStream outFile;
	private final String fileName;

	private NetworkLayer net = null;
	private OTFServerQuad quad = null;
	private final String id = null;
	private byte[] actBuffer = null;

	//public ByteArrayOutputStream out = null;
	double nextTime = -1;
	double intervall_s = 1;
	SortedMap<Double,Long> timeSteps = new TreeMap<Double,Long>();

	public OTFQuadFileHandlerOLD(double intervall_s, NetworkLayer network, String fileName) {
		if (network != null) this.net = network;
		//out = new ByteArrayOutputStream(500000);
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

	ByteBuffer buf = ByteBuffer.allocate(BUFFERSIZE);

	public void dumpConstData() throws IOException {
		this.buf.position(0);
		this.outFile.writeDouble(-1.);
		this.quad.writeConstData(this.buf);

		this.outFile.writeInt(this.buf.position());
		this.outFile.write(this.buf.array(), 0, this.buf.position());
	}

	public boolean dump(int time_s) throws IOException {
		if (time_s >= this.nextTime) {
			// dump time
			this.buf.position(0);
			this.outFile.writeDouble(time_s);
			// get State
			//Gbl.startMeasurement();
			this.quad.writeDynData(null, this.buf);
			this.outFile.writeInt(this.buf.position());
			this.outFile.write(this.buf.array(), 0, this.buf.position());
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
				this.outStream = new BufferedOutputStream(new GZIPOutputStream (new FileOutputStream(this.fileName),BUFFERSIZE),BUFFERSIZE);
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
			//outFile.writeUTF("fromFile");
			Gbl.startMeasurement();
			this.quad = new OTFServerQuad(this.net);
			System.out.print("build Quad on Server: "); Gbl.printElapsedTime();

			Gbl.startMeasurement();
			this.quad.fillQuadTree(new OTFDefaultNetWriterFactoryImpl());
			System.out.print("fill writer Quad on Server: "); Gbl.printElapsedTime();
			Gbl.startMeasurement();
			new ObjectOutputStream(this.outStream).writeObject(this.quad);
			dumpConstData();
			System.out.print("write to file  Quad on Server: "); Gbl.printElapsedTime();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// dump the network out
	}



	// IN
	private InputStream inStream = null;
	private DataInputStream inFile;

	public void readQuad() {
		// open file

		try {
			if (this.fileName.endsWith(".gz")) {
				InputStream gzInStream =  new BufferedInputStream(new GZIPInputStream(new FileInputStream(this.fileName), BUFFERSIZE), BUFFERSIZE);
				this.inStream = gzInStream;
			} else {
				this.inStream = new BufferedInputStream(new FileInputStream(this.fileName), BUFFERSIZE);
			}
			this.inFile = new DataInputStream(this.inStream);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// read some infos
		try {
			this.intervall_s = this.inFile.readDouble();
			//this.id = inFile.readUTF();
			this.quad = (OTFServerQuad) new ObjectInputStream(this.inStream).readObject();
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
//		if (nextTime < timeSteps.lastKey()) return true
//		return false;
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
		throw new RemoteException("getNet not implemented for QuadFileHandler");
	}

	long filepos = 0;
	byte [] result = this.result = new byte[BUFFERSIZE];

	public byte[] getStateBuffer() throws RemoteException {
		int size =  0 ;
		Gbl.startMeasurement();

		try {
			this.nextTime = this.inFile.readDouble();
			size = this.inFile.readInt();


			int offset = 0;
			int remain = size;
			int read = 0;
			while ((remain > 0) && (read != -1)){
				read = this.inFile.read(this.result,offset,remain);
				remain -= read;
				offset +=read;
				System.out.print(" " + read);
			}

			if (offset != size) throw new IOException("READ SIZE did not fit! File corrupted!");
			this.timeSteps.put(this.nextTime, this.filepos);
			this.filepos += read;

		} catch (IOException e) {
			System.out.println(e.toString());
		}
		System.out.print("getStateBuffer: "); Gbl.printElapsedTime();

		return this.result;
	}

	public void pause() throws RemoteException {
	}

	public void play() throws RemoteException {
	}

	public void setStatus(int status) throws RemoteException {
	}

	public void step() throws RemoteException {
		// retrieve latest buffer and set appropriate time
		this.actBuffer = getStateBuffer();
	}

	public boolean isLive() {
		return false;
	}


	private final List<Double> timeStepIndex = new ArrayList<Double>();

	private boolean readNextTimeStep() {
		double time = 0;
		try {
			time = this.inFile.readDouble();
			int size = this.inFile.readInt();
			this.timeStepIndex.add(time);
			this.inFile.skip(size);
			this.timeSteps.put(time, this.filepos);
			this.filepos += size;

		} catch (IOException e) {
			System.out.println(e.toString());
			return false;
		}
		return true;
	}

	private void buildIndex(){
		this.inFile.mark(-1);
		while (readNextTimeStep());
		try {
			this.inFile.reset();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public OTFServerQuad getQuad(String id, OTFNetWriterFactory writers) throws RemoteException {
		if (writers != null) throw new RemoteException("writers need to be NULL, when reading from file");
		if (this.id == null) readQuad();
		if ((id != null) && !id.equals(this.id)) throw new RemoteException("id does not match, set id to NULL will match ALL!");

		return this.quad;
	}

	public byte[] getQuadConstStateBuffer(String id) throws RemoteException {
		byte [] buffer = getStateBuffer();
		if( this.nextTime != -1)  throw new RemoteException("CONST data needs to be read FIRST");
		// Now we have read the QUAD and the CONST data, we can build the time index for the rest of
		// the file and set a mark to this place
		//buildIndex();

		return buffer;
	}

	public byte[] getQuadDynStateBuffer(String id, Rect bounds)	throws RemoteException {
		// DS TODO bounds is ignored, maybe throw exception if bounds != null??
		if (this.actBuffer == null) step();
		return this.actBuffer;
	}

	public boolean requestNewTime(int time, TimePreference searchDirection) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}



}

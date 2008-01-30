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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.plans.Plan;
import org.matsim.utils.StringUtils;
import org.matsim.utils.collections.QuadTree.Rect;
import org.matsim.utils.vis.netvis.streaming.SimStateWriterI;

import playground.david.vis.data.OTFDefaultNetWriterFactoryImpl;
import playground.david.vis.data.OTFNetWriterFactory;
import playground.david.vis.data.OTFServerQuad;
import playground.david.vis.interfaces.OTFNetHandler;
import playground.david.vis.interfaces.OTFServerRemote;

public class OTFQuadFileHandlerZIP implements SimStateWriterI, OTFServerRemote{

	private static final int BUFFERSIZE = 100000000;
	private ZipOutputStream zos = null;
	private DataOutputStream outFile;
	private final String fileName;

	protected QueueNetworkLayer net = null;
	protected OTFServerQuad quad = null;
	private final String id = null;
	private byte[] actBuffer = null;

	//public ByteArrayOutputStream out = null;
	protected double nextTime = -1;
	protected double intervall_s = 1;

	public OTFQuadFileHandlerZIP(double intervall_s, QueueNetworkLayer network, String fileName) {
		if (network != null) net = network;
		//out = new ByteArrayOutputStream(500000);
		this.intervall_s = intervall_s;
		this.fileName = fileName;
	}

	public void close() throws IOException {
		try {
			zos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	ByteBuffer buf = ByteBuffer.allocate(BUFFERSIZE);

	public boolean dump(int time_s) throws IOException {
		if (time_s >= nextTime) {
			// dump time
			writeDynData(time_s);
			nextTime = time_s + intervall_s;
			return true;
		}
		return false;
	}

	private void writeInfos() throws IOException {
		// Add ZIP entry to output stream.
		zos.putNextEntry(new ZipEntry("info.bin"));	
		outFile = new DataOutputStream(zos);
		outFile.writeDouble(intervall_s);
		//outFile.writeUTF("fromFile");
		zos.closeEntry();
	}

	protected void onAdditionalQuadData() {
		
	}
	
	private void writeQuad() throws IOException {
		zos.putNextEntry(new ZipEntry("quad.bin"));	
		Gbl.startMeasurement();
		quad = new OTFServerQuad(net);
		System.out.print("build Quad on Server: "); Gbl.printElapsedTime();
		
		onAdditionalQuadData();
		
		Gbl.startMeasurement();
		quad.fillQuadTree(new OTFDefaultNetWriterFactoryImpl());
		System.out.print("fill writer Quad on Server: "); Gbl.printElapsedTime();
		Gbl.startMeasurement();
		new ObjectOutputStream(zos).writeObject(quad);
		zos.closeEntry();
	}

	private void writeConstData() throws IOException {
		zos.putNextEntry(new ZipEntry("const.bin"));	
		outFile = new DataOutputStream(zos);
		buf.position(0);
		outFile.writeDouble(-1.);
		
		quad.writeConstData(buf);

		outFile.writeInt(buf.position());
		outFile.write(buf.array(), 0, buf.position());
		zos.closeEntry();
	}

	private void writeDynData(int time_s) throws IOException {
		zos.putNextEntry(new ZipEntry("step." + time_s + ".bin"));	
		outFile = new DataOutputStream(zos);
		buf.position(0);
		outFile.writeDouble(time_s);
		// get State
		//Gbl.startMeasurement();
		quad.writeDynData(null, buf);
		outFile.writeInt(buf.position());
		outFile.write(buf.array(), 0, buf.position());
		// dump State
		//Gbl.printElapsedTime();
		zos.closeEntry();
	}

	public void open() {
		//open zip file
		//

		try {
			zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(fileName + ".zip"),BUFFERSIZE));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			// dump some infos
			writeInfos();
			writeQuad();
			writeConstData();
			System.out.print("write to file  Quad on Server: "); Gbl.printElapsedTime();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// dump the network out
	}



	// IN
	private ZipFile zipFile = null;

	private final ZipInputStream inStream = null;
	private DataInputStream inFile;

	TreeMap<Integer, byte[]> timesteps = new TreeMap<Integer, byte[]>();

	public void scanZIPFile() throws IOException {
		nextTime = -1;
		// Create an enumeration of the entries in the zip file
		Enumeration zipFileEntries = zipFile.entries();

		Gbl.startMeasurement();

		// Process each entry
		while (zipFileEntries.hasMoreElements())
		{
			// grab a zip file entry
			ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();

			String currentEntry = entry.getName();
			System.out.println("Found: " + entry);
			if(currentEntry.contains("step")) {
				String regex = "";
				String [] spliti = StringUtils.explode(currentEntry, '.', 10);

				int time_s = Integer.parseInt(spliti[1]);
				if (nextTime == -1) nextTime = time_s;
				timesteps.put(time_s, new byte[(int) entry.getSize()]); //DS TODO unsafe, entry could get bigger than int 
			}
		}
		Gbl.printElapsedTime();
		Gbl.printMemoryUsage();
	}

	public void readZIPFile() throws IOException {
		// Create an enumeration of the entries in the zip file
		Enumeration zipFileEntries = zipFile.entries();

		// Process each entry
		while (zipFileEntries.hasMoreElements())
		{
			// grab a zip file entry
			ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();

			String currentEntry = entry.getName();
			System.out.println("Extracting: " + entry);
			if(currentEntry.contains("step")) {
				String regex = "";
				String [] spliti = StringUtils.explode(currentEntry, '.', 10);

				int time_s = Integer.parseInt(spliti[1]);
				byte [] buffer = timesteps.get(time_s);
				inFile = new DataInputStream(new BufferedInputStream(zipFile.getInputStream(entry)));
				readStateBuffer(buffer);
			}
		}
	}

	public void readQuad() {
		// open file
		try {
			File sourceZipFile = new File(fileName + ".zip");
			// Open Zip file for reading
			zipFile = new ZipFile(sourceZipFile, ZipFile.OPEN_READ);
			scanZIPFile();
			readZIPFile();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//open zip file
		//

		try {
			ZipEntry infoEntry = zipFile.getEntry("info.bin");
			inFile = new DataInputStream(zipFile.getInputStream(infoEntry));
			intervall_s = inFile.readDouble();

			ZipEntry quadEntry = zipFile.getEntry("quad.bin");
			BufferedInputStream is =  new BufferedInputStream(zipFile.getInputStream(quadEntry));
			try {
				this.quad = (OTFServerQuad) new ObjectInputStream(is).readObject();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean hasNextTimeStep() {
		return timesteps.get((int)nextTime) != null;
//		if (nextTime < timeSteps.lastKey()) return true
//		return false;
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
		throw new RemoteException("getNet not implemented for QuadFileHandler");
	}

	public void readStateBuffer(byte[] result) throws RemoteException {
		int size =  0 ;
		Gbl.startMeasurement();

		try {
			double timenextTime = inFile.readDouble();
			size = inFile.readInt();


			int offset = 0;
			int remain = size;
			int read = 0;
			while ((remain > 0) && (read != -1)){
				read = inFile.read(result,offset,remain);
				remain -= read;
				offset +=read;
				System.out.print(" " + read);
			}

			if (offset != size) {
				throw new IOException("READ SIZE did not fit! File corrupted!");
			}

		} catch (IOException e) {
			System.out.println(e.toString());
		}
		System.out.print("getStateBuffer: "); Gbl.printElapsedTime();
	}

	public void pause() throws RemoteException {
	}

	public void play() throws RemoteException {
	}

	public void setStatus(int status) throws RemoteException {
	}

	public void step() throws RemoteException {
		// retrieve latest buffer and set appropriate time
		actBuffer = getStateBuffer();
	}

	public boolean isLive() {
		return false;
	}



	public OTFServerQuad getQuad(String id, OTFNetWriterFactory writers) throws RemoteException {
		if (writers != null) throw new RemoteException("writers need to be NULL, when reading from file");
		if (this.id == null) readQuad();
		if (id != null && !id.equals(this.id)) throw new RemoteException("id does not match, set id to NULL will match ALL!");

		return quad;
	}

	public byte[] getQuadConstStateBuffer(String id) throws RemoteException {
		ZipEntry entry = zipFile.getEntry("const.bin");
		byte [] buffer = new byte[(int) entry.getSize()];

		try {
			inFile = new DataInputStream(zipFile.getInputStream(entry));
			readStateBuffer(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return buffer;
	}

	public byte[] getQuadDynStateBuffer(String id, Rect bounds)	throws RemoteException {
		// DS TODO bounds is ignored, maybe throw exception if bounds != null??
		if (actBuffer == null) step();
		return actBuffer;
	}

	public byte[] getStateBuffer() throws RemoteException {
		byte [] buffer = timesteps.get((int)nextTime);
		int time = 0;
		Iterator<Integer> it =  timesteps.keySet().iterator();
		while(it.hasNext() && time <= nextTime) time = it.next();
		if (time == nextTime) {
			time = timesteps.firstKey();
		}
		nextTime = time;
		return buffer;
	}



}

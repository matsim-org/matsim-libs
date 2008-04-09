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
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Plan;
import org.matsim.utils.StringUtils;
import org.matsim.utils.collections.QuadTree.Rect;
import org.matsim.utils.vis.netvis.streaming.SimStateWriterI;

import playground.david.vis.OTFVisNet;
import playground.david.vis.data.OTFDefaultNetWriterFactoryImpl;
import playground.david.vis.data.OTFNetWriterFactory;
import playground.david.vis.data.OTFServerQuad;
import playground.david.vis.gui.OTFVisConfig;
import playground.david.vis.interfaces.OTFNetHandler;
import playground.david.vis.interfaces.OTFServerRemote;

public class OTFQuadFileHandlerZIP implements SimStateWriterI, OTFServerRemote{

	private static final int BUFFERSIZE = 100000000;

	// the version number should be increased to imply a compatibility break
	public static final int VERSION = 1;
	// minor version increase does not break compatibility
	public static final int MINORVERSION = 2;

	private ZipOutputStream zos = null;
	private DataOutputStream outFile;
	private final String fileName;

	protected NetworkLayer net = null;
	protected OTFServerQuad quad = null;
	private final String id = null;
	private byte[] actBuffer = null;

	//public ByteArrayOutputStream out = null;
	protected double nextTime = -1;
	protected double intervall_s = 1;

	public OTFQuadFileHandlerZIP(double intervall_s, NetworkLayer network, String fileName) {
		if (network != null) this.net = network;
		//out = new ByteArrayOutputStream(500000);
		this.intervall_s = intervall_s;
		this.fileName = fileName;
	}

	public void close() throws IOException {
		try {
			this.zos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	ByteBuffer buf = ByteBuffer.allocate(BUFFERSIZE);

	public boolean dump(int time_s) throws IOException {
		if (time_s >= this.nextTime) {
			// dump time
			writeDynData(time_s);
			this.nextTime = time_s + this.intervall_s;
			return true;
		}
		return false;
	}

	private void writeInfos() throws IOException {
		// Add ZIP entry to output stream.
		this.zos.putNextEntry(new ZipEntry("info.bin"));
		this.outFile = new DataOutputStream(this.zos);
		this.outFile.writeInt(VERSION);
		this.outFile.writeInt(MINORVERSION);

		this.outFile.writeDouble(this.intervall_s);
		//outFile.writeUTF("fromFile");
		this.zos.closeEntry();
	}

	protected void onAdditionalQuadData() {

	}

	private void writeQuad() throws IOException {
		this.zos.putNextEntry(new ZipEntry("quad.bin"));
		Gbl.startMeasurement();
		this.quad = new OTFServerQuad(this.net);
		System.out.print("build Quad on Server: "); Gbl.printElapsedTime();

		onAdditionalQuadData();

		Gbl.startMeasurement();
		this.quad.fillQuadTree(new OTFDefaultNetWriterFactoryImpl());
		System.out.print("fill writer Quad on Server: "); Gbl.printElapsedTime();
		Gbl.startMeasurement();
		new ObjectOutputStream(this.zos).writeObject(this.quad);
		this.zos.closeEntry();
	}

	private void writeConstData() throws IOException {
		this.zos.putNextEntry(new ZipEntry("const.bin"));
		this.outFile = new DataOutputStream(this.zos);
		this.buf.position(0);
		this.outFile.writeDouble(-1.);

		this.quad.writeConstData(this.buf);

		this.outFile.writeInt(this.buf.position());
		this.outFile.write(this.buf.array(), 0, this.buf.position());
		this.zos.closeEntry();
	}

	private void writeDynData(int time_s) throws IOException {
		this.zos.putNextEntry(new ZipEntry("step." + time_s + ".bin"));
		this.outFile = new DataOutputStream(this.zos);
		this.buf.position(0);
		this.outFile.writeDouble(time_s);
		// get State
		//Gbl.startMeasurement();
		this.quad.writeDynData(null, this.buf);
		this.outFile.writeInt(this.buf.position());
		this.outFile.write(this.buf.array(), 0, this.buf.position());
		// dump State
		//Gbl.printElapsedTime();
		this.zos.closeEntry();
	}

	public void open() {
		//open zip file
		//

		try {
			this.zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(this.fileName),BUFFERSIZE));
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
		this.nextTime = -1;
		// Create an enumeration of the entries in the zip file
		Enumeration zipFileEntries = this.zipFile.entries();

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
				if (this.nextTime == -1) this.nextTime = time_s;
				this.timesteps.put(time_s, new byte[(int) entry.getSize()]); //DS TODO unsafe, entry could get bigger than int
			}
		}
		Gbl.printElapsedTime();
		Gbl.printMemoryUsage();
	}

	public void readZIPFile() throws IOException {
		// Create an enumeration of the entries in the zip file
		Enumeration zipFileEntries = this.zipFile.entries();

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
				byte [] buffer = this.timesteps.get(time_s);
				this.inFile = new DataInputStream(new BufferedInputStream(this.zipFile.getInputStream(entry)));
				readStateBuffer(buffer);
			}
		}
	}

	public void readQuad() {
		// open file
		try {
			File sourceZipFile = new File(this.fileName);
			// Open Zip file for reading
			this.zipFile = new ZipFile(sourceZipFile, ZipFile.OPEN_READ);
			scanZIPFile();
			readZIPFile();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//open zip file
		//

		try {
			ZipEntry infoEntry = this.zipFile.getEntry("info.bin");
			this.inFile = new DataInputStream(this.zipFile.getInputStream(infoEntry));
			int version = this.inFile.readInt();
			int minorversion = this.inFile.readInt();
			this.intervall_s = this.inFile.readDouble();
			OTFVisConfig config = (OTFVisConfig)Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME);

			config.setFileVersion(version);
			config.setFileMinorVersion(minorversion);

			ZipEntry quadEntry = this.zipFile.getEntry("quad.bin");
			BufferedInputStream is =  new BufferedInputStream(this.zipFile.getInputStream(quadEntry));
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
		return this.timesteps.get((int)this.nextTime) != null;
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

	public void readStateBuffer(byte[] result) throws RemoteException {
		int size =  0 ;
		Gbl.startMeasurement();

		try {
			double timenextTime = this.inFile.readDouble();
			size = this.inFile.readInt();


			int offset = 0;
			int remain = size;
			int read = 0;
			while ((remain > 0) && (read != -1)){
				read = this.inFile.read(result,offset,remain);
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
		this.actBuffer = getStateBuffer();
	}

	public boolean isLive() {
		return false;
	}



	public OTFServerQuad getQuad(String id, OTFNetWriterFactory writers) throws RemoteException {
		if (writers != null) throw new RemoteException("writers need to be NULL, when reading from file");
		if (this.id == null) readQuad();
		if ((id != null) && !id.equals(this.id)) throw new RemoteException("id does not match, set id to NULL will match ALL!");

		return this.quad;
	}

	public byte[] getQuadConstStateBuffer(String id) throws RemoteException {
		ZipEntry entry = this.zipFile.getEntry("const.bin");
		byte [] buffer = new byte[(int) entry.getSize()];

		try {
			this.inFile = new DataInputStream(this.zipFile.getInputStream(entry));
			readStateBuffer(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return buffer;
	}

	public byte[] getQuadDynStateBuffer(String id, Rect bounds)	throws RemoteException {
		// DS TODO bounds is ignored, maybe throw exception if bounds != null??
		if (this.actBuffer == null) step();
		return this.actBuffer;
	}

	public byte[] getStateBuffer() throws RemoteException {
		byte [] buffer = this.timesteps.get((int)this.nextTime);
		int time = 0;
		Iterator<Integer> it =  this.timesteps.keySet().iterator();
		while(it.hasNext() && (time <= this.nextTime)) time = it.next();
		if (time == this.nextTime) {
			time = this.timesteps.firstKey();
		}
		this.nextTime = time;
		return buffer;
	}

	public boolean requestNewTime(int time, TimePreference searchDirection) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}





}

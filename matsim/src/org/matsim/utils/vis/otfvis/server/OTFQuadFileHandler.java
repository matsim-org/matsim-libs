/* *********************************************************************** *
 * project: org.matsim.*
 * OTFQuadFileHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.utils.vis.otfvis.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.matsim.gbl.Gbl;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.utils.StringUtils;
import org.matsim.utils.collections.QuadTree.Rect;
import org.matsim.utils.vis.netvis.streaming.SimStateWriterI;
import org.matsim.utils.vis.otfvis.data.OTFDefaultNetWriterFactoryImpl;
import org.matsim.utils.vis.otfvis.data.OTFNetWriterFactory;
import org.matsim.utils.vis.otfvis.data.OTFServerQuad;
import org.matsim.utils.vis.otfvis.gui.OTFVisConfig;
import org.matsim.utils.vis.otfvis.interfaces.OTFServerRemote;
import org.matsim.utils.vis.snapshots.writers.PositionInfo;
import org.matsim.utils.vis.snapshots.writers.SnapshotWriterI;


public class OTFQuadFileHandler {

	private static final int BUFFERSIZE = 100000000;

	// the version number should be increased to imply a compatibility break
	public static final int VERSION = 1;
	// minor version increase does not break compatibility
	public static final int MINORVERSION = 3;

	public static class Writer implements SimStateWriterI, SnapshotWriterI {
		protected final QueueNetwork net;
		protected OTFServerQuad quad = null;
		private final String fileName;
		protected final double interval_s;
		protected double nextTime = -1;

		private ZipOutputStream zos = null;
		private DataOutputStream outFile;
		private boolean isOpen = false;

		private final ByteBuffer buf = ByteBuffer.allocate(BUFFERSIZE);

		public Writer(double intervall_s, QueueNetwork network, String fileName) {
			this.net = network;
			this.interval_s = intervall_s;
			this.fileName = fileName;
		}

		public boolean dump(int time_s) throws IOException {
			if (time_s >= this.nextTime) {
				// dump time
				writeDynData(time_s);
				this.nextTime = time_s + this.interval_s;
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

			this.outFile.writeDouble(this.interval_s);
			//outFile.writeUTF("fromFile");
			this.zos.closeEntry();
		}

		protected void onAdditionalQuadData() {
			// intentionally left blank for inheriting classes to overwrite
		}

		private void writeQuad() throws IOException {
			this.zos.putNextEntry(new ZipEntry("quad.bin"));
			Gbl.startMeasurement();
			this.quad = new OTFServerQuad(this.net);
			System.out.print("build Quad on Server: ");
			Gbl.printElapsedTime();

			onAdditionalQuadData();

			Gbl.startMeasurement();
			this.quad.fillQuadTree(new OTFDefaultNetWriterFactoryImpl());
			System.out.print("fill writer Quad on Server: ");
			Gbl.printElapsedTime();
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
			this.quad.writeDynData(null, this.buf);
			this.outFile.writeInt(this.buf.position());
			this.outFile.write(this.buf.array(), 0, this.buf.position());
			// dump State
			this.zos.closeEntry();
		}

		public void open() {
			//open zip file
			isOpen = true;

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

		public void close() {
			try {
				this.zos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void addAgent(PositionInfo position) {
			// Do nothing
		}

		public void beginSnapshot(double time) {
			if (!isOpen) open();
			try {
				dump((int)time);
			} catch (IOException e) {

				e.printStackTrace();
			}
		}

		public void endSnapshot() {
			// TODO Auto-generated method stub
		}

		public void finish() {
			close();
		}
	}

	public static class Reader implements OTFServerRemote {

		private final String fileName;

		public Reader(String fname) {
			this.fileName = fname;
			openAndReadInfo();
		}

		private ZipFile zipFile = null;

		private DataInputStream inFile;

		protected OTFServerQuad quad = null;
		private final String id = null;
		private byte[] actBuffer = null;

		//public ByteArrayOutputStream out = null;
		protected double intervall_s = -1, nextTime = -1;

		TreeMap<Double, Long> timesteps = new TreeMap<Double, Long>();

		public void scanZIPFile() {
			this.nextTime = -1;
			// Create an enumeration of the entries in the zip file
			Enumeration<? extends ZipEntry> zipFileEntries = this.zipFile.entries();
			System.out.println("Scanning timesteps:");

			Gbl.startMeasurement();

			// Process each entry
			while (zipFileEntries.hasMoreElements())
			{
				// grab a zip file entry
				ZipEntry entry = zipFileEntries.nextElement();

				String currentEntry = entry.getName();
				if(currentEntry.contains("step")) {
					String [] spliti = StringUtils.explode(currentEntry, '.', 10);

					double time_s = Double.parseDouble(spliti[1]);
					if (this.nextTime == -1) this.nextTime = time_s;
					this.timesteps.put(time_s,  entry.getSize());
					System.out.print(time_s);
					System.out.print(", ");
				}
			}
			System.out.println("");
			System.out.println("Nr of timesteps: " + timesteps.size());

			Gbl.printElapsedTime();
			Gbl.printMemoryUsage();
		}

		public byte [] readTimeStep(double time_s) throws IOException {
			int time_string = (int)time_s;
			ZipEntry entry = this.zipFile.getEntry("step." + time_string + ".bin");
			byte [] buffer = new byte [(int)this.timesteps.get(time_s).longValue()]; //DS TODO Might be bigger than int??

			this.inFile = new DataInputStream(new BufferedInputStream(this.zipFile.getInputStream(entry)));
			readStateBuffer(buffer);

			return buffer;
		}

		private void openAndReadInfo() {
			// open file
			try {
				File sourceZipFile = new File(this.fileName);
				// Open Zip file for reading
				this.zipFile = new ZipFile(sourceZipFile, ZipFile.OPEN_READ);
				ZipEntry infoEntry = this.zipFile.getEntry("info.bin");
				this.inFile = new DataInputStream(this.zipFile.getInputStream(infoEntry));
				int version = this.inFile.readInt();
				int minorversion = this.inFile.readInt();
				this.intervall_s = this.inFile.readDouble();
				OTFVisConfig config = (OTFVisConfig)Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME);

				config.setFileVersion(version);
				config.setFileMinorVersion(minorversion);

				scanZIPFile();

			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		public static class OTFObjectInputStream extends ObjectInputStream {
			public OTFObjectInputStream(InputStream in) throws IOException {
				super(in);
				// TODO Auto-generated constructor stub
			}

			@Override
			protected Class resolveClass(ObjectStreamClass desc)
			throws IOException, ClassNotFoundException {
				String name = desc.getName();
				System.out.println("try to resolve "+ name);
				//
				if(name.equals("playground.david.vis.data.OTFServerQuad")) return OTFServerQuad.class;
				else if (name.startsWith("playground.david.vis")){
					name = name.replaceFirst("playground.david.vis", "org.matsim.utils.vis.otfvis");
				    return Class.forName(name);
				}
				else if (name.startsWith("org.matsim.utils.vis.otfivs")){
					name = name.replaceFirst("org.matsim.utils.vis.otfivs", "org.matsim.utils.vis.otfvis");
				    return Class.forName(name);
				}
				return super.resolveClass(desc);
			}
		}

		public void readQuad() {
			try {
				// we do not chache anymore ...readZIPFile();
				ZipEntry quadEntry = this.zipFile.getEntry("quad.bin");
				BufferedInputStream is =  new BufferedInputStream(this.zipFile.getInputStream(quadEntry));
				try {
					this.quad = (OTFServerQuad) new OTFObjectInputStream(is).readObject();
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
			return this.timesteps.get(Double.valueOf(this.nextTime)) != null;
//			if (nextTime < timeSteps.lastKey()) return true
//			return false;
		}


		public int getLocalTime() throws RemoteException {
			return (int)this.nextTime;
		}


		public void readStateBuffer(byte[] result) {
			int size =  0;

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
				}

				if (offset != size) {
					throw new IOException("READ SIZE did not fit! File corrupted! in second " + timenextTime);
				}

			} catch (IOException e) {
				System.out.println(e.toString());
			}
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
				this.inFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return buffer;
		}

		public byte[] getQuadDynStateBuffer(String id, Rect bounds)	throws RemoteException {
			// DS TODO bounds is ignored, maybe throw exception if bounds != null??
			if (this.actBuffer == null)
					this.actBuffer = getStateBuffer();
			return this.actBuffer;
		}

		public byte[] getStateBuffer() {
			byte[] buffer = null;
			try {
				buffer = readTimeStep(this.nextTime);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			double time = 0;
//			Iterator<Double> it =  this.timesteps.keySet().iterator();
//			while(it.hasNext() && (time <= this.nextTime)) time = it.next();
//			if (time == this.nextTime) {
//				time = this.timesteps.firstKey();
//			}
//			this.nextTime = time;
			return buffer;
		}

		public boolean requestNewTime(int time, TimePreference searchDirection) throws RemoteException {
			double lastTime = -1;
			double foundTime = -1;
			for(Double timestep : this.timesteps.keySet()) {
				if (searchDirection == TimePreference.EARLIER){
					if(timestep >= time) {
						// take next lesser time than requested, if not exacty the same
						foundTime = lastTime;
						break;
					}
				} else {
					if(timestep >= time) {
						foundTime = timestep; //the exact time or one biggers
						break;
					}
				}
				lastTime = timestep;
			}
			if (foundTime == -1) return false;

			this.nextTime = foundTime;
			this.actBuffer = null;
			return true;
		}

		public Collection<Double> getTimeSteps() {
			return this.timesteps.keySet();
		}
	}

}

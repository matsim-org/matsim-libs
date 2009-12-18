/* *********************************************************************** *
 * project: org.matsim.*
 * Reader
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
package org.matsim.vis.otfvis.data.fileio;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.data.OTFServerQuadI;
import org.matsim.vis.otfvis.gui.OTFVisConfig;
import org.matsim.vis.otfvis.interfaces.OTFServerRemote;
/**
 * The OTF has a file Reader and a file Writer part.
 * The Reader is the the mvi playing OTFServer.

 * @author dstrippgen 
 * @author dgrether
 */
public class OTFFileReader implements OTFServerRemote {
	
	private static final Logger log = Logger.getLogger(OTFFileReader.class);
	
	private final String fileName;

	public OTFFileReader(final String fname) {
		this.fileName = fname;
		openAndReadInfo();
	}

	// private ZipFile zipFile = null;
	private File sourceZipFile = null;

	private DataInputStream inFile;

	private byte[] actBuffer = null;

	// public ByteArrayOutputStream out = null;
	protected double intervall_s = -1, nextTime = -1;

	TreeMap<Double, Long> timesteps = new TreeMap<Double, Long>();

	// TODO [DS] This is not safe when opening more than one file concurrently 
	public static int version = 0;
	public static int minorversion = 0;

	private void scanZIPFile(ZipFile zipFile) {
		this.nextTime = -1;
		// Create an enumeration of the entries in the zip file
		Enumeration<? extends ZipEntry> zipFileEntries = zipFile.entries();
		System.out.println("Scanning timesteps:");

		Gbl.startMeasurement();

		// Process each entry
		while (zipFileEntries.hasMoreElements()) {
			// grab a zip file entry
			ZipEntry entry = zipFileEntries.nextElement();

			String currentEntry = entry.getName();
			if (currentEntry.contains("step")) {
				String[] spliti = StringUtils
				.explode(currentEntry, '.', 10);

				double time_s = Double.parseDouble(spliti[1]);
				if (this.nextTime == -1)
					this.nextTime = time_s;
				this.timesteps.put(time_s, entry.getSize());
				System.out.print(time_s);
				System.out.print(", ");
			}
		}
		System.out.println("");
		System.out.println("Nr of timesteps: " + this.timesteps.size());

	}

	private byte[] readTimeStep(final double time_s) throws IOException {
		int time_string = (int) time_s;
		ZipFile zipFile = new ZipFile(this.sourceZipFile, ZipFile.OPEN_READ);
		ZipEntry entry = zipFile.getEntry("step." + time_string + ".bin");
		byte[] buffer = new byte[(int) this.timesteps.get(time_s)
		                         .longValue()]; // DS TODO Might be bigger than int??

		this.inFile = new DataInputStream(new BufferedInputStream(zipFile
				.getInputStream(entry), 1000000));
		readStateBuffer(buffer);
		zipFile.close();

		return buffer;
	}

	private void openAndReadInfo() {
		this.sourceZipFile = new File(this.fileName);
		if (!this.sourceZipFile.exists()){
			String message = "The file: " + this.fileName + " cannot be found!";
			log.error(message);
			throw new RuntimeException(message);
		}
		// open file
		try {
			// Open Zip file for reading
			ZipFile zipFile = new ZipFile(this.sourceZipFile, ZipFile.OPEN_READ);
			ZipEntry infoEntry = zipFile.getEntry("info.bin");
			this.inFile = new DataInputStream(zipFile.getInputStream(infoEntry));
			version = this.inFile.readInt();
			minorversion = this.inFile.readInt();
			this.intervall_s = this.inFile.readDouble();
			OTFVisConfig config = (OTFVisConfig) Gbl.getConfig().getModule(
					OTFVisConfig.GROUP_NAME);

			config.setFileVersion(version);
			config.setFileMinorVersion(minorversion);

			scanZIPFile(zipFile);
			zipFile.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	//		public void closeFile() {
	//			try {
	//				this.zipFile.close();
	//			} catch (IOException e) {
	//				e.printStackTrace();
	//			}
	//
	//		}

	public static class OTFObjectInputStream extends
	ObjectInputStream {
		public OTFObjectInputStream(final InputStream in)
		throws IOException {
			super(in);
		}

		@Override
		protected Class resolveClass(final ObjectStreamClass desc)
		throws IOException, ClassNotFoundException {
			String name = desc.getName();
			log.info("try to resolve " + name);
			if((version >= 1) && (minorversion >=6)) {
				return super.resolveClass(desc);
			}
			// these remappings only happen with older file versions
			if (name.equals("playground.david.vis.data.OTFServerQuad")) {
				return OTFServerQuad2.class;
			} else if (name.startsWith("org.matsim.utils.vis.otfvis")) {
				name = name.replaceFirst("org.matsim.utils.vis.otfvis",
				"org.matsim.vis.otfvis");
				return Class.forName(name);
			}else if (name.startsWith("playground.david.vis")) {
				name = name.replaceFirst("playgrounidd.david.vis",
				"org.matsim.utils.vis.otfvis");
				return Class.forName(name);
			} else if (name.startsWith("org.matsim.utils.vis.otfivs")) {
				name = name.replaceFirst("org.matsim.utils.vis.otfivs",
				"org.matsim.vis.otfvis");
				return Class.forName(name);
			} else if (name.startsWith("org.matsim.mobsim")) {
				name = name.replaceFirst("org.matsim.mobsim",
				"org.matsim.core.mobsim");
				return Class.forName(name);
			} else if (name.startsWith("org.matsim.utils.collections")) {
				name = name.replaceFirst("org.matsim.utils.collections",
				"org.matsim.core.utils.collections");
				return Class.forName(name);
			}else if (name.startsWith("playground.gregor.otf.readerwriter")) {
				name = name.replaceFirst("playground.gregor.otf.readerwriter",
				"org.matsim.evacuation.otfvis.readerwriter");
				return Class.forName(name);
			}else if (name.startsWith("playground.gregor.otf.drawer")) {
				name = name.replaceFirst("playground.gregor.otf.drawer",
				"org.matsim.evacuation.otfvis.drawer");
				return Class.forName(name);
			}else if (name.startsWith("playground.gregor.collections")) {
				name = name.replaceFirst("playground.gregor.collections",
				"org.matsim.evacuation.collections");
				return Class.forName(name);
			}
			return super.resolveClass(desc);
		}
	}


	private OTFServerQuadI readQuad() {
		OTFServerQuadI quad = null;
		try {
			// we do not cache anymore ...readZIPFile();
			ZipFile zipFile = new ZipFile(this.sourceZipFile, ZipFile.OPEN_READ);
			ZipEntry quadEntry = zipFile.getEntry("quad.bin");
			BufferedInputStream is = new BufferedInputStream(zipFile
					.getInputStream(quadEntry));
			try {
				quad = (OTFServerQuadI) new OTFObjectInputStream(is)
				.readObject();
				log.debug("Read OTFServerQuadI from file, type: " + quad.getClass().getName());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			zipFile.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return quad;
	}

	private void readConnect(OTFConnectionManager connect) {
		try {
			ZipFile zipFile = new ZipFile(this.sourceZipFile, ZipFile.OPEN_READ);
			ZipEntry connectEntry = zipFile.getEntry("connect.bin");
			// maybe no connect given.. no Problem
			if (connectEntry != null) {
				BufferedInputStream is = new BufferedInputStream(zipFile
						.getInputStream(connectEntry));
				try {
					OTFConnectionManager connect2 = (OTFConnectionManager) new OTFObjectInputStream(
							is).readObject();
					connect.updateEntries(connect2);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
			zipFile.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int getLocalTime() throws RemoteException {
		return (int) this.nextTime;
	}

	public void readStateBuffer(final byte[] result) {
		int size = 0;

		try {
			double timenextTime = this.inFile.readDouble();
			size = this.inFile.readInt();

			int offset = 0;
			int remain = size;
			int read = 0;
			while ((remain > 0) && (read != -1)) {
				read = this.inFile.read(result, offset, remain);
				remain -= read;
				offset += read;
			}

			if (offset != size) {
				throw new IOException(
						"READ SIZE did not fit! File corrupted! in second "
						+ timenextTime);
			}

		} catch (IOException e) {
			System.out.println(e.toString());
		}
	}

	public boolean isLive() {
		return false;
	}

	public OTFServerQuadI getQuad(final String id,
			final OTFConnectionManager connect) throws RemoteException {
		OTFServerQuadI quad = null;
		// if (connect != null) throw new
		// RemoteException("writers need to be NULL, when reading from file"
		// );
		log.info("reading quad from file...");
		quad  = readQuad();
		readConnect(connect);
		return quad;
	}

	public byte[] getQuadConstStateBuffer(final String id)
	throws RemoteException {
		byte[] buffer = null;
		try {
			ZipFile zipFile = new ZipFile(this.sourceZipFile, ZipFile.OPEN_READ);
			ZipEntry entry = zipFile.getEntry("const.bin");
			buffer = new byte[(int) entry.getSize()];

			this.inFile = new DataInputStream(zipFile
					.getInputStream(entry));
			readStateBuffer(buffer);
			this.inFile.close();
			zipFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return buffer;
	}

	public byte[] getQuadDynStateBuffer(final String id, final Rect bounds)
	throws RemoteException {
		// DS TODO bounds is ignored, maybe throw exception if bounds !=
		// null??
		if (this.actBuffer == null)
			this.actBuffer = getStateBuffer();
		return this.actBuffer;
	}

	public byte[] getStateBuffer() {
		byte[] buffer = null;
		try {
			buffer = readTimeStep(this.nextTime);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// double time = 0;
		// Iterator<Double> it = this.timesteps.keySet().iterator();
		// while(it.hasNext() && (time <= this.nextTime)) time = it.next();
		// if (time == this.nextTime) {
		// time = this.timesteps.firstKey();
		// }
		// this.nextTime = time;
		return buffer;
	}

	public boolean requestNewTime(final int time,
			final TimePreference searchDirection) throws RemoteException {
		double lastTime = -1;
		double foundTime = -1;
		for (Double timestep : this.timesteps.keySet()) {
			if(timestep == time) {
				foundTime = time;
				break;
			}else 
				if (searchDirection == TimePreference.EARLIER) {
					if (timestep >= time) {
						// take next lesser time than requested, if not exacty
						// the same
						foundTime = lastTime;
						break;
					}
				} else if (timestep >= time) {
					foundTime = timestep; // the exact time or one biggers
					break;
				}
			lastTime = timestep;
		}
		if (foundTime == -1)
			return false;

		this.nextTime = foundTime;
		this.actBuffer = null;
		return true;
	}

	public Collection<Double> getTimeSteps() {
		return this.timesteps.keySet();
	}
}
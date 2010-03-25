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
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuadI;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.interfaces.OTFServerRemote;
/**
 * The OTF has a file Reader and a file Writer part.
 * The Reader is the the mvi playing OTFServer.

 * @author dstrippgen 
 * @author dgrether
 * @author michaz
 */
public final class OTFFileReader implements OTFServerRemote {
	
	private static final Logger log = Logger.getLogger(OTFFileReader.class);
	
	private final String fileName;

	private File sourceZipFile = null;

	private byte[] actBuffer = null;
	
	private double nextTime = -1;

	private TreeMap<Double, Long> timesteps = new TreeMap<Double, Long>();

	public OTFFileReader(final String fname) {
		this.fileName = fname;
		openAndReadInfo();
	}

	private void openAndReadInfo() {
		this.sourceZipFile = new File(this.fileName);
		if (!this.sourceZipFile.exists()){
			String message = "The file: " + this.fileName + " cannot be found!";
			log.error(message);
			throw new RuntimeException(message);
		}
		try {
			ZipFile zipFile = new ZipFile(this.sourceZipFile, ZipFile.OPEN_READ);
			ZipEntry infoEntry = zipFile.getEntry("info.bin");
			DataInputStream inFile = new DataInputStream(zipFile.getInputStream(infoEntry));
			int version = inFile.readInt();
			int minorversion = inFile.readInt();
			inFile.readDouble(); // unused value 'intervall_s'
			OTFClientControl.getInstance().getOTFVisConfig().setFileVersion(version);
			OTFClientControl.getInstance().getOTFVisConfig().setFileMinorVersion(minorversion);
			scanZIPFile(zipFile);
			zipFile.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private void scanZIPFile(ZipFile zipFile) {
		this.nextTime = -1;
		Enumeration<? extends ZipEntry> zipFileEntries = zipFile.entries();
		System.out.println("Scanning timesteps:");
		Gbl.startMeasurement();
		while (zipFileEntries.hasMoreElements()) {
			ZipEntry entry = zipFileEntries.nextElement();
			String currentEntry = entry.getName();
			if (currentEntry.contains("step")) {
				String[] spliti = StringUtils.explode(currentEntry, '.', 10);
				double time_s = Double.parseDouble(spliti[1]);
				if (this.nextTime == -1) {
					this.nextTime = time_s;
				}
				this.timesteps.put(time_s, entry.getSize());
				System.out.print(time_s);
				System.out.print(", ");
			}
		}
		System.out.println("");
		System.out.println("Nr of timesteps: " + this.timesteps.size());
	}

	public int getLocalTime() throws RemoteException {
		return (int) this.nextTime;
	}

	public boolean isLive() {
		return false;
	}

	public OTFServerQuadI getQuad(final String id, final OTFConnectionManager connect) throws RemoteException {
		log.info("reading quad from file...");
		OTFServerQuadI quad = readQuad();
		readConnectionManager(connect);
		return quad;
	}

	private OTFServerQuadI readQuad() {
		OTFServerQuadI quad = null;
		try {
			ZipFile zipFile = new ZipFile(this.sourceZipFile, ZipFile.OPEN_READ);
			ZipEntry quadEntry = zipFile.getEntry("quad.bin");
			BufferedInputStream is = new BufferedInputStream(zipFile.getInputStream(quadEntry));
			try {
				quad = (OTFServerQuadI) new OTFObjectInputStream(is).readObject();
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

	private void readConnectionManager(OTFConnectionManager connect) {
		try {
			ZipFile zipFile = new ZipFile(this.sourceZipFile, ZipFile.OPEN_READ);
			ZipEntry connectEntry = zipFile.getEntry("connect.bin");
			if (connectEntry != null) {
				BufferedInputStream is = new BufferedInputStream(zipFile.getInputStream(connectEntry));
				try {
					OTFConnectionManager connect2 = (OTFConnectionManager) new OTFObjectInputStream(is).readObject();
					connect.addEntriesFrom(connect2);
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

	public byte[] getQuadConstStateBuffer(final String id)
	throws RemoteException {
		byte[] buffer = null;
		try {
			ZipFile zipFile = new ZipFile(this.sourceZipFile, ZipFile.OPEN_READ);
			ZipEntry entry = zipFile.getEntry("const.bin");
			buffer = new byte[(int) entry.getSize()];
			DataInputStream inFile = new DataInputStream(zipFile.getInputStream(entry));
			readStateBuffer(inFile, buffer);
			inFile.close();
			zipFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return buffer;
	}

	public byte[] getQuadDynStateBuffer(final String id, final Rect bounds)
			throws RemoteException {
		if (bounds == null) {
			log.warn("Bounds are ignored but set!");
		}
		if (this.actBuffer == null) {
			byte[] buffer = null;
			try {
				buffer = readTimeStep(this.nextTime);
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.actBuffer = buffer;
		}
		return this.actBuffer;
	}

	private byte[] readTimeStep(final double time_s) throws IOException {
		int time_string = (int) time_s;
		ZipFile zipFile = new ZipFile(this.sourceZipFile, ZipFile.OPEN_READ);
		ZipEntry entry = zipFile.getEntry("step." + time_string + ".bin");
		byte[] buffer = new byte[(int) this.timesteps.get(time_s) .longValue()]; 
		DataInputStream inFile = new DataInputStream(new BufferedInputStream(zipFile.getInputStream(entry), 1000000));
		readStateBuffer(inFile, buffer);
		zipFile.close();
		return buffer;
	}

	private void readStateBuffer(DataInputStream inputFile, final byte[] result) {
		int size = 0;
	
		try {
			double timenextTime = inputFile.readDouble();
			size = inputFile.readInt();
			int offset = 0;
			int remain = size;
			int read = 0;
			while ((remain > 0) && (read != -1)) {
				read = inputFile.read(result, offset, remain);
				remain -= read;
				offset += read;
			}
	
			if (offset != size) {
				throw new IOException("READ SIZE did not fit! File corrupted! in second " + timenextTime);
			}
	
		} catch (IOException e) {
			System.out.println(e.toString());
		}
	}

	public boolean requestNewTime(final int time, final TimePreference searchDirection) throws RemoteException {
		double lastTime = -1;
		double foundTime = -1;
		for (Double timestep : this.timesteps.keySet()) {
			if (timestep == time) {
				foundTime = time;
				break;
			} else if (searchDirection == TimePreference.EARLIER) {
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

	@Override
	public void toggleShowParking() throws RemoteException {
		OTFLinkAgentsHandler.showParked = !OTFLinkAgentsHandler.showParked;
	}

}
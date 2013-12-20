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
import java.io.IOException;
import java.io.ObjectInputStream;
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
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.interfaces.OTFServer;
/**
 * The OTF has a file Reader and a file Writer part.
 * The Reader is the the mvi playing OTFServer.

 * @author dstrippgen
 * @author dgrether
 * @author michaz
 */
public final class OTFFileReader implements OTFServer {

	private static final Logger log = Logger.getLogger(OTFFileReader.class);

	private final String fileName;

	private File sourceZipFile = null;

	private byte[] actBuffer = null;

	private double nextTime = -1;

	private TreeMap<Double, Long> timesteps = new TreeMap<Double, Long>();

	private OTFVisConfigGroup otfVisConfig;

	public OTFFileReader(final String fname) {
		this.fileName = fname;
		this.sourceZipFile = new File(this.fileName);
		otfVisConfig = readConfigOrUseDefaults();
		OTFClientControl.getInstance().setOTFVisConfig(otfVisConfig);
		openAndScanZipFile();
	}

	private void openAndScanZipFile() {
		try {
			ZipFile zipFile = new ZipFile(this.sourceZipFile, ZipFile.OPEN_READ);
			scanZIPFile(zipFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void setDelayParameterIfZero(OTFVisConfigGroup cfg) {
		cfg.setDelay_ms(cfg.getDelay_ms() == 0 ? 30 : cfg.getDelay_ms());
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

	@Override
	public int getLocalTime() {
		return (int) this.nextTime;
	}

	@Override
	public boolean isLive() {
		return false;
	}

	@Override
	public OTFServerQuadTree getQuad(final OTFConnectionManager connect) {
		log.info("reading quad from file...");
		OTFServerQuadTree quad = readQuad();
		return quad;
	}

	private OTFServerQuadTree readQuad() {
		OTFServerQuadTree quad = null;
		try {
			ZipFile zipFile = new ZipFile(this.sourceZipFile, ZipFile.OPEN_READ);
			ZipEntry quadEntry = zipFile.getEntry("quad.bin");
			BufferedInputStream is = new BufferedInputStream(zipFile.getInputStream(quadEntry));
			quad = (OTFServerQuadTree) new ObjectInputStream(is).readObject();
			log.debug("Read OTFServerQuadI from file, type: " + quad.getClass().getName());
			zipFile.close();
			return quad;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public byte[] getQuadConstStateBuffer() {
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
			throw new RuntimeException(e);
		}
		return buffer;
	}

	@Override
	public byte[] getQuadDynStateBuffer(final Rect bounds) {
		if (bounds == null) {
			log.warn("Bounds are ignored but set! [[I don't understand what this means here.  kai, feb'11]]");
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

	@Override
	public boolean requestNewTime(final int time, final TimePreference searchDirection) {
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

	@Override
	public Collection<Double> getTimeSteps() {
		return this.timesteps.keySet();
	}

	@Override
	public void setShowNonMovingItems(boolean showNonMovingItems) {
		OTFLinkAgentsHandler.showParked = showNonMovingItems;
	}

	@Override
	public OTFVisConfigGroup getOTFVisConfig() {
		return otfVisConfig;
	}

	private OTFVisConfigGroup readConfigOrUseDefaults() {
		OTFVisConfigGroup otfVisConfig2 = tryToReadSettingsFromFileNextToMovie();
		if (otfVisConfig2 == null) {
			otfVisConfig2 = new OTFVisConfigGroup();
		}
		this.setEffectiveLaneWidthIfNull(otfVisConfig2);
		return otfVisConfig2;
	}

	private void setEffectiveLaneWidthIfNull(OTFVisConfigGroup c) {
		if (c.getEffectiveLaneWidth() == null){
			c.setEffectiveLaneWidth(3.75); //default value
		}
	}
	
	private OTFVisConfigGroup tryToReadSettingsFromFileNextToMovie() {
		log.debug("Looking for settings in: " + this.fileName);
		SettingsSaver saver = new SettingsSaver(this.fileName);
		OTFVisConfigGroup settingsFromFile = saver.tryToReadSettingsFile();
		return settingsFromFile;
	}

}
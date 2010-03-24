/* *********************************************************************** *
 * project: org.matsim.*
 * OTFFileSettingsSaver
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
package org.matsim.vis.otfvis.opengl.gui;

import java.io.File;
import java.io.ObjectInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.log4j.Logger;
import org.matsim.vis.otfvis.data.fileio.OTFFileReader;
import org.matsim.vis.otfvis.gui.OTFVisConfig;

/**
 * @author dgrether
 * @author michaz
 * 
 * Tries to read OTFVis settings from the (old, deprecated) binary format zipped into an MVI.
 * Returns null if anything at all goes wrong, in which case the caller must use default settings.
 * 
 * This is only so that old settings aren't lost.
 * 
 */
public class ReadOTFSettingsFromMovie {

	private static final Logger log = Logger
			.getLogger(ReadOTFSettingsFromMovie.class);

	String fileName;

	public ReadOTFSettingsFromMovie(String filename) {
		if (filename.startsWith("file:")) {
			this.fileName = filename.substring(5);
		} else {
			this.fileName = filename;
		}
	}

	public OTFVisConfig openAndReadConfig() {
		try {
			File sourceZipFile = new File(fileName);
			ZipFile zipFile = new ZipFile(sourceZipFile, ZipFile.OPEN_READ);
			ZipEntry infoEntry = zipFile.getEntry("config.bin");
			if (infoEntry != null) {
				ObjectInputStream inFile = new OTFFileReader.OTFObjectInputStream(zipFile.getInputStream(infoEntry));
				OTFVisConfig cfg = (OTFVisConfig) inFile.readObject();
				setDelayParameterIfZero(cfg);
				return cfg;
			} else {
				return null;
			}
		} catch (Exception e) {
			log.error("Not able to load config from file. This is not fatal. file=" + this.fileName, e);
			return null;
		} 
	}

	private void setDelayParameterIfZero(OTFVisConfig cfg) {
		cfg.setDelay_ms(cfg.getDelay_ms() == 0 ? 30 : cfg.getDelay_ms());
	}

}

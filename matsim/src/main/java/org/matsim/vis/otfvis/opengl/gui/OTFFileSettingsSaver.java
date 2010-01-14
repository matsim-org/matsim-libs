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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.log4j.Logger;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.data.fileio.OTFFileReader;
import org.matsim.vis.otfvis.gui.OTFVisConfig;


/**
 * @author dgrether
 *
 */
public class OTFFileSettingsSaver extends OTFAbstractSettingsSaver {

  private static final Logger log = Logger.getLogger(OTFFileSettingsSaver.class);
	/**
	 * @param visconf
	 * @param filename
	 */
	public OTFFileSettingsSaver(OTFVisConfig visconf, String filename) {
		super(visconf, filename);
	}

	public OTFVisConfig openAndReadConfig() {
		OTFVisConfig conf;
			ZipFile zipFile;
			ObjectInputStream inFile;
			// open file
			try {
				File sourceZipFile = new File(fileName);
				// Open Zip file for reading
				zipFile = new ZipFile(sourceZipFile, ZipFile.OPEN_READ);
				ZipEntry infoEntry = zipFile.getEntry("config.bin");
				if(infoEntry != null) {
					//load config settings
					inFile = new OTFFileReader.OTFObjectInputStream(zipFile.getInputStream(infoEntry));
					OTFVisConfig cfg = (OTFVisConfig)inFile.readObject();
					// force this to 30 if zero!
					cfg.setDelay_ms(cfg.getDelay_ms() == 0 ? 30: cfg.getDelay_ms());
					OTFClientControl.getInstance().setOTFVisConfig(cfg);
				} 
			} catch (IOException e1) {
				log.error("Not able to load config from file. This is not fatal.");
				e1.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} 
		// Test if loading worked, otherwise create default
		if(OTFClientControl.getInstance().getOTFVisConfig() == null) {
			log.info("No otfvis config loaded creating default otfvisconfig.");
			OTFClientControl.getInstance().setOTFVisConfig(new OTFVisConfig());
		}
		conf = OTFClientControl.getInstance().getOTFVisConfig();
		conf.clearModified();
		return conf;
	}		
	
}

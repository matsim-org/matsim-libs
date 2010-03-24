/* *********************************************************************** *
 * project: org.matsim.*
 * OTFLiveFileSettingsSaver
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.vis.otfvis.gui.OTFVisConfig;

/**
 * @author dgrether
 * @author michaz
 * 
 */
public class SettingsSaver {

	private static final Logger log = Logger.getLogger(SettingsSaver.class);

	private final String fileName;

	private OTFVisConfig visConfig;
	
	public SettingsSaver(OTFVisConfig visconf, String filename) {
		if (filename.startsWith("file:")) {
			this.fileName = filename.substring(5);
		} else {
			this.fileName = filename;
		}
		this.visConfig = visconf;
	}

	private File chooseFile(boolean saveIt) {
		File erg = null;
		File selFile = new File(fileName + ".vcfg");
		JFileChooser fc;

		String path = selFile.getParent();

		fc = new JFileChooser(path);
		fc.setSelectedFile(selFile);
		if (saveIt)
			fc.setDialogType(JFileChooser.SAVE_DIALOG);

		fc.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory()
						|| f.getName().toLowerCase().endsWith(".vcfg");
			}

			@Override
			public String getDescription() {
				return "OTFVis Config File (*.vcfg)";
			}
		});

		int state = saveIt ? fc.showSaveDialog(null) : fc.showOpenDialog(null);

		if (state == JFileChooser.APPROVE_OPTION) {
			erg = fc.getSelectedFile();
		} else {
			log.info("Auswahl abgebrochen");
		}

		return erg;
	}

	private OTFVisConfig readConfigFromFile(File file) {
		ObjectInputStream inFile;
		if (file == null) {
			throw new NullPointerException("Not able to read config from file.");
		}
		try {
			inFile = new ObjectInputStream(new FileInputStream(file));
			this.visConfig = (OTFVisConfig) inFile.readObject();
			log.info("Config read from file : " + file.getAbsolutePath());
			log.info("Config has " + this.visConfig.getZooms().size()
					+ " zoom entries...");
			this.dumpConfig();
			return this.visConfig;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		throw new IllegalArgumentException(
				"Not able to read config from file: " + file.getPath());
	}

	public void saveSettingsAs() {
		File file = chooseFile(true);
		if (file != null) {
			OutputStream out;
			try {
				out = new FileOutputStream(file);
				ObjectOutputStream outFile = new ObjectOutputStream(out);
				this.visConfig.clearModified();
				outFile.writeObject(this.visConfig);
				outFile.close();
				log.info("Config has " + this.visConfig.getZooms().size()
						+ " zoom entries");
				log.info("Config written to file...");
				this.dumpConfig();
				out.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public OTFVisConfig chooseAndReadSettingsFile() {
		File file = chooseFile(false);
		return readConfigFromFile(file);
	}

	private void dumpConfig() {
		log.info("OTFVis config dump:");
		StringWriter writer = new StringWriter();
		Config tmpConfig = new Config();
		tmpConfig.addModule(OTFVisConfig.GROUP_NAME, this.visConfig);
		PrintWriter pw = new PrintWriter(writer);
		new ConfigWriter(tmpConfig).writeStream(pw);
		log.info("\n\n" + writer.getBuffer().toString());
		log.info("Complete config dump done.");
	}

	public void tryToReadSettingsFile() {
		File file = new File(fileName + ".vcfg");
		if (file.exists()) {
			readConfigFromFile(file);
		}
	}
	
}

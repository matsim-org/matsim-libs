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
package org.matsim.vis.otfvis.data.fileio;

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
import java.util.Locale;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author dgrether
 * @author michaz
 *
 */
public class SettingsSaver {

	private static final Logger log = Logger.getLogger(SettingsSaver.class);

	private final String fileName;

	public SettingsSaver(String filename) {
		if (filename.startsWith("file:")) {
			this.fileName = filename.substring(5);
		} else {
			this.fileName = filename;
		}
	}

	private File chooseFile(boolean saveIt) {
		File selFile = new File(fileName + ".vcfg");
		File currentDirectory = selFile.getAbsoluteFile().getParentFile();
		JFileChooser fc = new JFileChooser(currentDirectory);
		fc.setSelectedFile(selFile);
		if (saveIt) {
			fc.setDialogType(JFileChooser.SAVE_DIALOG);
		}
		fc.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith(".vcfg");
			}

			@Override
			public String getDescription() {
				return "OTFVis Config File (*.vcfg)";
			}
		});
		int state = saveIt ? fc.showSaveDialog(null) : fc.showOpenDialog(null);
		if (state == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fc.getSelectedFile();
			return selectedFile;
		} else {
			log.info("Auswahl abgebrochen");
			return null;
		}

	}

	private OTFVisConfigGroup readConfigFromFile(File file) {
		ObjectInputStream inFile;
		if (file == null) {
			throw new NullPointerException("Not able to read config from file.");
		}
		try {
			inFile = new ObjectInputStream(new FileInputStream(file));
			OTFVisConfigGroup visConfig = (OTFVisConfigGroup) inFile.readObject();
			log.info("Config read from file : " + file.getAbsolutePath());
			log.info("Config has " + visConfig.getZooms().size()
					+ " zoom entries...");
			dumpConfig(visConfig);
			return visConfig;
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

	public void saveSettingsAs(OTFVisConfigGroup visConfig) {
		File file = chooseFile(true);
		if (file != null) {
			OutputStream out;
			try {
				out = new FileOutputStream(file);
				ObjectOutputStream outFile = new ObjectOutputStream(out);
				visConfig.clearModified();
				outFile.writeObject(visConfig);
				outFile.close();
				log.info("Config has " + visConfig.getZooms().size()
						+ " zoom entries");
				log.info("Config written to file...");
				dumpConfig(visConfig);
				out.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public OTFVisConfigGroup chooseAndReadSettingsFile() {
		File file = chooseFile(false);
		return readConfigFromFile(file);
	}

	private void dumpConfig(OTFVisConfigGroup visConfig) {
		log.info("OTFVis config dump:");
		StringWriter writer = new StringWriter();
		Config tmpConfig = new Config();
		tmpConfig.addModule(visConfig);
		PrintWriter pw = new PrintWriter(writer);
		new ConfigWriter(tmpConfig).writeStream(pw);
		log.info("\n\n" + writer.getBuffer().toString());
		log.info("Complete config dump done.");
	}

	public OTFVisConfigGroup tryToReadSettingsFile() {
		File file = new File(fileName + ".vcfg");
		System.out.println(file.getAbsolutePath());
		if (file.exists()) {
			return readConfigFromFile(file);
		} else {
			return null;
		}
	}

}

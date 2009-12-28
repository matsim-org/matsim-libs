/* *********************************************************************** *
 * project: org.matsim.*
 * SaveRevisionInfo.java
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

package playground.meisterk.kti.controler.listeners;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

public class SaveRevisionInfo implements StartupListener {

	final private String filename;

	private static final Logger logger = Logger.getLogger(SaveRevisionInfo.class);

	public SaveRevisionInfo(String filename) {
		super();
		this.filename = filename;
	}

	public void notifyStartup(StartupEvent event) {

			logger.info("Running revision info...");
			Process pr = null;
			try {
				pr = Runtime.getRuntime().exec("svn info");
			} catch (IOException e) {
				e.printStackTrace();
			}
			logger.info("Running revision info...done.");

			logger.info("Saving revision info output...");
			try {
				BufferedReader procout = new BufferedReader(new InputStreamReader(pr.getInputStream()));
				BufferedWriter fileout = new BufferedWriter(new FileWriter(Controler.getOutputFilename(this.filename)));
				String line;
				while ((line = procout.readLine()) != null) {
					fileout.write(line);
					fileout.write(System.getProperty("line.separator"));
				}
				fileout.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			logger.info("Saving revision info output...done.");

	}

}

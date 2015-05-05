/* *********************************************************************** *
 * project: org.matsim.*
 * RunJupedSim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.gregor.hybridsim.grpc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

public class RunJupedSim implements Runnable, ExternalSim{
	
	private static final Logger log = Logger.getLogger(RunJupedSim.class);
	private Process p1;
	
	@Override
	public void run() {
		try {
			this.p1 = new ProcessBuilder("/Users/laemmel/svn/jpscore/Debug/jupedsim","/Users/laemmel/svn/jpscore/inputfiles/hybrid/hybrid_hall_ini.xml").start();
			logToLog(this.p1);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void logToLog(Process p1) throws IOException {
		{
			InputStream is = p1.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String l = br.readLine();
			while (l != null) {
//				log.info(l);
				l = br.readLine();
			}
		}
		{
			InputStream is = p1.getErrorStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String l = br.readLine();
			while (l != null) {
				log.error(l);
				l = br.readLine();
			}
		}
	}

	@Override
	public void shutdown() {
		this.p1.destroy();
	}

}

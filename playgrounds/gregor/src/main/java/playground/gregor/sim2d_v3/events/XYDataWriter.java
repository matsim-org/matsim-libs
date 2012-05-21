/* *********************************************************************** *
 * project: matsim
 * XYDataWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v3.events;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.utils.io.IOUtils;

/**
 * Writes data from XYVxVyEvents to a text file which can be imported in
 * senozon's via.
 * 
 * @author cdobler
 */
public class XYDataWriter implements XYVxVyEventsHandler, MobsimInitializedListener, BeforeMobsimListener, AfterMobsimListener {
	
	static final Logger log = Logger.getLogger(XYDataWriter.class);
	
	private static final String newLine = "\n";
	private static final String separator = "\t";
	private static final String fileName = "events.xy.gz";

	private BufferedWriter bw;
	private String iterationFileName;
	
	@Override
	public void handleEvent(XYVxVyEvent event) {

		try {
			bw.write(String.valueOf(event.getTime()));
			bw.write(separator);
			bw.write(String.valueOf(event.getPersonId().toString()));
			bw.write(separator);
			bw.write(String.valueOf(event.getX()));
			bw.write(separator);
			bw.write(String.valueOf(event.getY()));
			bw.write(separator);
			bw.write(String.valueOf(event.getVX()));
			bw.write(separator);
			bw.write(String.valueOf(event.getVY()));
			bw.write(separator);
			bw.write(String.valueOf(Math.sqrt(Math.pow(event.getVX(), 2) + Math.pow(event.getVY(), 2))));
			bw.write(newLine);
			
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
	
	@Override
	public void reset(int iteration) {
		// nothing to do here
	}

	private void writeHeader() {
		try {
			bw.write("time");
			bw.write(separator);
			bw.write("personId");
			bw.write(separator);
			bw.write("x");
			bw.write(separator);
			bw.write("y");
			bw.write(separator);
			bw.write("vx");
			bw.write(separator);
			bw.write("vy");
			bw.write(separator);
			bw.write("vxy");
			bw.write(newLine);
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		bw = IOUtils.getBufferedWriter(iterationFileName);
		writeHeader();
	}
	
	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		iterationFileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), fileName);
	}
	
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		try {
			bw.flush();
			bw.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}	
}
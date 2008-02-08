/* *********************************************************************** *
 * project: org.matsim.*
 * JamTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.yu.test;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.controler.Controler;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.ShutdownEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.ShutdownListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.io.IOUtils;

public class JamTest extends Controler {

	public JamTest(String[] configFileName) {
		super(configFileName);
	}

	public static class JamListener implements IterationEndsListener,
			ShutdownListener, StartupListener {
		private Controler c;
		private BufferedWriter out;
		private VolumesAnalyzer va = null;

		public void notifyIterationEnds(IterationEndsEvent event) {
			c = event.getControler();
			NetworkLayer n = c.getNetwork();
			try {
				for (String linkId : va.getLinkIds()) {
					int[] v = va.getVolumesForLink(linkId);
					StringBuffer sb = new StringBuffer("");
					for (int i = 6; i < 10; i++) {
						sb.append("\t" + v[i]);
					}
					out.write(linkId + "\t" + n.getLink(linkId).getCapacity()
							/ 100.0 + sb + "\n");
					out.flush();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void notifyStartup(StartupEvent event) {
			va = new VolumesAnalyzer(3600, 3600 * 24 - 1, c.getNetwork());
			c.getEvents().addHandler(va);
			try {
				out = IOUtils
						.getBufferedWriter(getOutputFilename("travol.txt.gz"));
				out.write("LinkId\tCapacity/100[Fz/h]\tVolume[Fz/h]\n");
				out.flush();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void notifyShutdown(ShutdownEvent event) {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		JamTest jt = new JamTest(args);
		jt.addControlerListener(new JamListener());
		jt.run();
		System.exit(0);
	}

}

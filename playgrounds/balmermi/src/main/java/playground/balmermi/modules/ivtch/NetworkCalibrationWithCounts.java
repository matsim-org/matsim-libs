/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkAdaptCHNavtec.java
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

package playground.balmermi.modules.ivtch;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.misc.Time;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;

public class NetworkCalibrationWithCounts {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final Counts counts;
	private final String gtf_outfile;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NetworkCalibrationWithCounts(final String gtf_outfile, final Counts counts) {
		super();
		this.gtf_outfile = gtf_outfile;
		this.counts = counts;
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final void writeGTFfile(final NetworkLayer network) {
		try {
//			double capperiod = network.getCapacityPeriod();
			FileWriter fw = new FileWriter(this.gtf_outfile);
			BufferedWriter out = new BufferedWriter(fw);
			out.write("<greentimefractions desc=\"calibration gtfs file\">\n");
			Iterator<Count> c_it = this.counts.getCounts().values().iterator();
			double maxval = 0.0;
			while (c_it.hasNext()) {
				Count c = c_it.next();
				Link l = network.getLinks().get(c.getLocId());
				if (l == null) { System.out.println("csid="+c.getCsId()+";locid="+c.getLocId()+": link not found"); }
				else {
					l.setFreespeed(360.0/3.6);
					out.write("\t<linkgtfs id=\""+l.getId()+"\" time_period=\"24:00:00\" desc=\"from csid "+c.getCsId()+"\">\n");
					for (int h=1; h<25; h++) {
						int time_start = (h-1)*3600;
//						int time = h*3600-1800;
						int time_end = h*3600-1;
//						System.out.println("lcap="+l.getCapacity()+";vol("+h+")="+c.getVolume(h));
						double val = c.getVolume(h).getValue()/(l.getCapacity()/network.getCapacityPeriod()*3600);
						if (val >= 1.0) { System.out.println("csid="+c.getCsId()+";locid="+c.getLocId()+": val="+val); }
						if (val < 0.01) { val = 0.01; }
						out.write("\t\t<gtf time=\""+Time.writeTime(time_start)+"\" val=\""+val+"\"/>\n");
//						out.write("\t\t<gtf time=\""+Time.writeTime(time)+"\" val=\""+val+"\"/>\n");
						out.write("\t\t<gtf time=\""+Time.writeTime(time_end)+"\" val=\""+val+"\"/>\n");
						if (maxval < val) { maxval = val; }
					}
					out.write("\t</linkgtfs>\n");
				}
			}
			out.write("</greentimefractions>\n");
			out.flush();
			out.close();
			fw.close();
			System.out.println(maxval);
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(NetworkLayer network) {
		System.out.println("    running " + this.getClass().getName() + " module...");
//		for (Link l : network.getLinks().values()) { l.setCapacity(l.getCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME)*1.5); }
		for (Link l : network.getLinks().values()) { l.setCapacity(10000.0); l.setNumberOfLanes(5.0); l.setFreespeed(36.0/3.6); }
		this.writeGTFfile(network);
		System.out.println("    done.");
	}
}

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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;

public class NetworkParseETNet {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final String nodefile;
	private final String linkfile;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NetworkParseETNet(final String nodefile, final String linkfile) {
		super();
		this.nodefile = nodefile;
		this.linkfile = linkfile;
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final void parseNodes(final NetworkImpl network) {
		try {
			FileReader file_reader = new FileReader(this.nodefile);
			BufferedReader buffered_reader = new BufferedReader(file_reader);

			// read header
			String curr_line = buffered_reader.readLine();
			while ((curr_line = buffered_reader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				// ID  X  Y
				// 0   1  2
				network.createAndAddNode(new IdImpl(entries[0]), new CoordImpl(entries[1],entries[2]));
			}
			buffered_reader.close();
			file_reader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	private final void parseLinksET(final NetworkImpl network) {
		try {
			FileReader file_reader = new FileReader(this.linkfile);
			BufferedReader buffered_reader = new BufferedReader(file_reader);

			// read header
			String curr_line = buffered_reader.readLine();
			while ((curr_line = buffered_reader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				// ET_FID  ID  FROMID  TOID  LENGTH  SPEED  CAP  LANES  ORIGID  TYPE  X1  Y1  X2  Y2
				// 0       1   2       3     4       5      6    7      8       9     10  11  12  13
				if (entries.length == 14) {
					double length = Double.parseDouble(entries[4]);
					double freespeed = Double.parseDouble(entries[5])/3.6;
					double capacity = Double.parseDouble(entries[6]);
					double nofLanes = Double.parseDouble(entries[7]);
					network.createAndAddLink(new IdImpl(entries[1]), network.getNodes().get(new IdImpl(entries[2])), network.getNodes().get(new IdImpl(entries[3])),
					                   length, freespeed, capacity, nofLanes, entries[8], entries[9]);
				}
			}
			buffered_reader.close();
			file_reader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(final NetworkImpl network) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		if (!network.getNodes().isEmpty()) { Gbl.errorMsg("links already exist."); }
		if (!network.getLinks().isEmpty()) { Gbl.errorMsg("links already exist."); }

		network.setName("created by '"+this.getClass().getName()+"'");
		network.setCapacityPeriod(Time.parseTime("01:00:00"));

		this.parseNodes(network);
		this.parseLinksET(network);

		System.out.println("    done.");
	}
}

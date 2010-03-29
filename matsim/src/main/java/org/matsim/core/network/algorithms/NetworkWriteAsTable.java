/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkWriteAsTable.java
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

package org.matsim.core.network.algorithms;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.NetworkRunnable;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.Time;

public class NetworkWriteAsTable implements NetworkRunnable {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final String outdir;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NetworkWriteAsTable(final String outdir) {
		this.outdir = outdir;
	}

	public NetworkWriteAsTable() {
		this("output");
	}

	//////////////////////////////////////////////////////////////////////
	// final method
	//////////////////////////////////////////////////////////////////////

	// no functionality anymore: can be removed
	public final void close() {
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(NetworkLayer network) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		double capperiod = network.getCapacityPeriod();
		capperiod = capperiod / 3600;
		System.out.println("      capperiod = " + capperiod);

		try {
			BufferedWriter out_n = IOUtils.getBufferedWriter(this.outdir + "/nodes.txt");
			out_n.write("ID\tX\tY\n");
			out_n.flush();
			for (Node n : network.getNodes().values()) {
				out_n.write(n.getId() + "\t" + n.getCoord().getX() + "\t" + n.getCoord().getY() + "\n");
				out_n.flush();
			}
			out_n.flush();
			out_n.close();

			BufferedWriter out_l = IOUtils.getBufferedWriter(this.outdir + "/links.txt");
			out_l.write("ID\tX1\tY1\tX2\tY2\tLENGHT\tFREESPEED\tCAPACITY\tPERMLANES\tMODES\n");
			out_l.flush();
			BufferedWriter out_et = IOUtils.getBufferedWriter(this.outdir + "/linksET.txt");
			out_et.write("ID\tFROMID\tTOID\tLENGTH\tSPEED\tCAP\tLANES\tORIGID\tTYPE\tMODES\n");
			out_et.flush();
			for (LinkImpl l : network.getLinks().values()) {
				Node f = l.getFromNode();
				Node t = l.getToNode();
				out_l.write(l.getId() + "\t" + f.getCoord().getX() + "\t" + f.getCoord().getY() + "\t");
				out_l.write(t.getCoord().getX() + "\t" + t.getCoord().getY() + "\t" + l.getLength() + "\t");
				out_l.write(l.getFreespeed()+"\t"
						+(l.getCapacity()/capperiod)+"\t"
						+ NetworkUtils.getNumberOfLanesAsInt(Time.UNDEFINED_TIME, l)+"\t"
						+l.getAllowedModes().toString()+"\n");
				out_l.flush();

				out_et.write(l.getId() + "\t" + l.getFromNode().getId() + "\t" + l.getToNode().getId() + "\t");
				out_et.write(Math.round(l.getLength()) + "\t" + Math.round(l.getFreespeed()*3.6) + "\t");
				out_et.write(Math.round(l.getCapacity()/capperiod) + "\t" + NetworkUtils.getNumberOfLanesAsInt(Time.UNDEFINED_TIME, l) + "\t");
				out_et.write(l.getOrigId() + "\t" + l.getType() + "\t"+l.getAllowedModes().toString()+"\n");
				out_et.write(f.getCoord().getX() + "\t" + f.getCoord().getY() + "\n");
				out_et.write(t.getCoord().getX() + "\t" + t.getCoord().getY() + "\n");
				out_et.write("END\n");
			}
			out_l.flush();
			out_l.close();
			out_et.write("END\n");
			out_et.flush();
			out_et.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}

		System.out.println("    done.");
	}
}

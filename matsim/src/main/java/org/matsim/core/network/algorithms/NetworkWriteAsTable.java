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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.NetworkRunnable;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

public final class NetworkWriteAsTable implements NetworkRunnable {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = LogManager.getLogger(NetworkWriteAsTable.class);

	private final String outdir;
	private final double offset;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NetworkWriteAsTable(final String outdir, double offset) {
		this.offset = offset;
		this.outdir = outdir;
	}

	public NetworkWriteAsTable(final String outdir) {
		this(outdir,0.0);
	}

	public NetworkWriteAsTable() {
		this("output");
	}

	//////////////////////////////////////////////////////////////////////
	// final method
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Network network) {
		double capperiod = network.getCapacityPeriod();
		capperiod = capperiod / 3600;
		log.info("capperiod = " + capperiod);

		try {
			BufferedWriter out_n = IOUtils.getBufferedWriter(this.outdir + "/nodes.txt.gz");
			out_n.write("ID\tX\tY\n");
			out_n.flush();
			for (Node n : network.getNodes().values()) {
				out_n.write(n.getId() + "\t" + n.getCoord().getX() + "\t" + n.getCoord().getY() + "\n");
				out_n.flush();
			}
			out_n.flush();
			out_n.close();

			BufferedWriter out_l = IOUtils.getBufferedWriter(this.outdir + "/links.txt.gz");
			out_l.write("ID\tX1\tY1\tX2\tY2\tLENGHT\tFREESPEED\tCAPACITY\tPERMLANES\tMODES\n");
			out_l.flush();
			BufferedWriter out_et = IOUtils.getBufferedWriter(this.outdir + "/linksET.txt.gz");
			out_et.write("ID\tFROMID\tTOID\tLENGTH\tSPEED\tCAP\tLANES\tORIGID\tTYPE\tMODES\n");
			out_et.flush();
			for (Link l : network.getLinks().values()) {
				Node f = l.getFromNode();
				Node t = l.getToNode();

				final double y = -t.getCoord().getX() + f.getCoord().getX();
				Coord offsetVector = new Coord(t.getCoord().getY() - f.getCoord().getY(), y);
				offsetVector = CoordUtils.scalarMult(offset/CoordUtils.length(offsetVector),offsetVector);
				Coord fc = CoordUtils.plus(f.getCoord(),offsetVector);
				Coord tc = CoordUtils.plus(t.getCoord(),offsetVector);

				out_l.write(l.getId() + "\t" + fc.getX() + "\t" + fc.getY() + "\t");
				out_l.write(tc.getX() + "\t" + tc.getY() + "\t" + l.getLength() + "\t");
				out_l.write(l.getFreespeed()+"\t"
						+(l.getCapacity()/capperiod)+"\t"
						+ NetworkUtils.getNumberOfLanesAsInt(l)+"\t"
						+l.getAllowedModes().toString()+"\n");
				out_l.flush();

				out_et.write(l.getId() + "\t" + l.getFromNode().getId() + "\t" + l.getToNode().getId() + "\t");
				out_et.write(Math.round(l.getLength()) + "\t" + Math.round(l.getFreespeed()*3.6) + "\t");
				out_et.write(Math.round(l.getCapacity()/capperiod) + "\t" + NetworkUtils.getNumberOfLanesAsInt(l) + "\t");
				out_et.write(NetworkUtils.getOrigId( ((Link) l) ) + "\t" + NetworkUtils.getType(((Link) l)) + "\t"+l.getAllowedModes().toString()+"\n");
				out_et.write(fc.getX() + "\t" + fc.getY() + "\n");
				out_et.write(tc.getX() + "\t" + tc.getY() + "\n");
				out_et.write("END\n");
			}
			out_l.flush();
			out_l.close();
			out_et.write("END\n");
			out_et.flush();
			out_et.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}

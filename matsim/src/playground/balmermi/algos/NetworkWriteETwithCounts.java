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

package playground.balmermi.algos;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.network.algorithms.NetworkAlgorithm;
import org.matsim.utils.misc.Time;

public class NetworkWriteETwithCounts extends NetworkAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final String outdir;
	private FileWriter fw_n = null;
	private BufferedWriter out_n = null;
	private FileWriter fw_l = null;
	private BufferedWriter out_l = null;
	private FileWriter fw_et = null;
	private BufferedWriter out_et = null;

	private final Counts counts;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NetworkWriteETwithCounts(final String outdir, final Counts counts) {
		super();
		this.counts = counts;
		this.outdir = outdir;
		try {
			this.fw_n = new FileWriter(this.outdir + "/nodes.txt");
			this.out_n = new BufferedWriter(this.fw_n);
			this.out_n.write("ID\tX\tY\n");
			this.out_n.flush();

			this.fw_l = new FileWriter(this.outdir + "/links.txt");
			this.out_l = new BufferedWriter(this.fw_l);
			this.out_l.write("ID\tX1\tY1\tX2\tY2\tLENGHT\tFREESPEED\tCAPACITY\tPERMLANES\tONEWAY\n");
			this.out_l.flush();

			this.fw_et = new FileWriter(this.outdir + "/linksET.txt");
			this.out_et = new BufferedWriter(this.fw_et);
			this.out_et.write("ID\tFROMID\tTOID\tLENGTH\tSPEED\tCAP\tLANES\tORIGID\tTYPE\tMAXVOL\tX1\tY1\tX2\tY2\n");
			this.out_et.flush();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	public NetworkWriteETwithCounts(final Counts counts) {
		this("output",counts);
	}

	//////////////////////////////////////////////////////////////////////
	// final method
	//////////////////////////////////////////////////////////////////////

	public final void close() {
		try {
			this.out_n.flush();
			this.out_n.close();
			this.fw_n.close();

			this.out_l.flush();
			this.out_l.close();
			this.fw_l.close();

			this.out_et.write("END\n");
			this.out_et.flush();
			this.out_et.close();
			this.fw_et.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final double getMaxCount(Link l) {
		Count count = this.counts.getCounts().get(l.getId());
		if (count != null) {
			Volume v = count.getMaxVolume();
			return v.getValue();
		}
		return -1.0;
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(NetworkLayer network) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		int capperiod = network.getCapacityPeriod();
		capperiod = capperiod / 3600;
		System.out.println("      capperiod = " + capperiod);

		try {
			for (Node n : network.getNodes().values()) {
				this.out_n.write(n.getId() + "\t" + n.getCoord().getX() + "\t" + n.getCoord().getY() + "\n");
				this.out_n.flush();
			}
			for (Link l : network.getLinks().values()) {
				double maxvol = this.getMaxCount(l);
				Node f = l.getFromNode();
				Node t = l.getToNode();
				this.out_l.write(l.getId() + "\t" + f.getCoord().getX() + "\t" + f.getCoord().getY() + "\t");
				this.out_l.write(t.getCoord().getX() + "\t" + t.getCoord().getY() + "\t" + l.getLength() + "\t");
				this.out_l.write(l.getFreespeed(Time.UNDEFINED_TIME) + "\t" + (l.getCapacity()/capperiod) + "\t" + l.getLanes() + "\t1\n");
				this.out_l.flush();

				this.out_et.write(l.getId() + "\t" + l.getFromNode().getId() + "\t" + l.getToNode().getId() + "\t");
				this.out_et.write(Math.round(l.getLength()) + "\t" + Math.round(l.getFreespeed(Time.UNDEFINED_TIME)*3.6) + "\t");
				this.out_et.write(Math.round(l.getCapacity()/capperiod) + "\t" + l.getLanes() + "\t");
				this.out_et.write(l.getOrigId() + "\t" + l.getType() + "\t" + maxvol + "\t");
				this.out_et.write(f.getCoord().getX() + "\t" + f.getCoord().getY() + "\t");
				this.out_et.write(t.getCoord().getX() + "\t" + t.getCoord().getY() + "\n");

				this.out_et.write(f.getCoord().getX() + "\t" + f.getCoord().getY() + "\n");
				this.out_et.write(t.getCoord().getX() + "\t" + t.getCoord().getY() + "\n");
				this.out_et.write("END\n");
			}
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}

		System.out.println("    done.");
	}
}

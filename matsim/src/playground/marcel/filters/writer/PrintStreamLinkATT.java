/* *********************************************************************** *
 * project: org.matsim.*
 * PrintStreamLinkATT.java
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

package playground.marcel.filters.writer;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.List;

import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;

import playground.marcel.filters.filter.finalFilters.FinalEventFilterA;

/**
 * @author ychen
 * 
 */
public class PrintStreamLinkATT extends PrintStreamATTA {
	/*------------------------MEMBER VARIABLE-----------------*/
	private final static String tableName = "Strecken";

	/*------------------------CONSTRUCTOR------------------*/
	public PrintStreamLinkATT(String fileName, NetworkLayer network) {
		super(fileName, network);
		try {
			out.writeBytes(tableName + "\n$LINK:NO;FROMNODENO;TONODENO");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.demandmodeling.filters.writer.PrintStreamATTA#printRow(java.lang.String)
	 */
	@Override
	public void printRow(String linkID) throws IOException {
		try {
			Link link = (Link) network.getLocation(linkID);
			if (link == null)
				return;
			out.writeBytes(link.getOrigId() + SPRT + link.getFromNode().getId()
					+ SPRT + link.getToNode().getId());
			int i = 0;
			List<Double> udawList = udaws.get(linkID);
			for (Double udaw : udawList) {
				DoubleDF.applyPattern(udas.get(i).getPattern());
				if (udaw == null)
					udaw = 0.0;
				out.writeBytes(SPRT + DoubleDF.format(udaw));
				i++;
			}
			out.writeBytes("\n");
		} catch (ConcurrentModificationException cme) {
			System.err.println(cme);
			System.err.println("in printRow(int)");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.demandmodeling.filters.writer.PrintStreamVisum9_3I#output(org.matsim.demandmodeling.filters.filter.finalFilters.FinalEventFilterA)
	 */
	@Override
	public void output(FinalEventFilterA fef) {
		udaws = fef.UDAWexport();
		udas = fef.UDAexport();
		try {
			printTable();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.yu.visum.writer;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.LinkImpl;

import playground.yu.visum.filter.finalFilters.FinalEventFilterA;

/**
 * @author ychen
 * 
 */
public class PrintStreamLinkATT extends PrintStreamATTA {
	/*------------------------MEMBER VARIABLE-----------------*/
	private final static String tableName = "Strecken";

	/*------------------------CONSTRUCTOR------------------*/
	public PrintStreamLinkATT(String fileName, Network network) {
		super(fileName, network);
		try {
			out.writeBytes(tableName + "\n$LINK:NO;FROMNODENO;TONODENO");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void printRow(String linkID) throws IOException {
		try {
			Link link = network.getLinks().get(Id.create(linkID, Link.class));
			if (link == null) {
				return;
			}
			out.writeBytes(((LinkImpl) link).getOrigId() + SPRT
					+ link.getFromNode().getId() + SPRT
					+ link.getToNode().getId());
			int i = 0;
			List<Double> udawList = udaws.get(linkID);
			for (Double udaw : udawList) {
				DoubleDF.applyPattern(udas.get(i).getPattern());
				if (udaw == null) {
					udaw = 0.0;
				}
				out.writeBytes(SPRT + DoubleDF.format(udaw));
				i++;
			}
			out.writeBytes("\n");
		} catch (ConcurrentModificationException cme) {
			System.err.println(cme);
			System.err.println("in printRow(int)");
		}
	}

	@Override
	public void output(FinalEventFilterA fef) {
		udaws = fef.UDAWexport();
		udas = fef.UDAexport();
		try {
			printTable();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkTeleatlasAddSpeedRestrictions.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.network.algorithms;

import java.io.FileInputStream;
import java.nio.channels.FileChannel;

import org.apache.log4j.Logger;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.matsim.basic.v01.IdImpl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.misc.Time;

public class NetworkTeleatlasAddSpeedRestrictions {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(NetworkTeleatlasAddSpeedRestrictions.class);
	
	private final String srDbfFileName; // teleatlas speed restriction dbf file name
	
	public final String srIdName = "ID";
	public final String srSpeedName = "SPEED";
	public final String srValDirName = "VALDIR";
	public final String srVerifiedName = "VERIFIED";
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NetworkTeleatlasAddSpeedRestrictions(final String srDbfFileName) {
		log.info("init " + this.getClass().getName() + " module...");
		this.srDbfFileName = srDbfFileName;
		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(final NetworkLayer network) throws Exception {
		log.info("running " + this.getClass().getName() + " module...");
		FileChannel in = new FileInputStream(this.srDbfFileName).getChannel();
		DbaseFileReader r = new DbaseFileReader(in,true);
		// getting header indices
		int srIdNameIndex = -1;
		int srSpeedNameIndex = -1;
		int srValDirNameIndex = -1;
		int srVerifiedNameIndex = -1;
		for (int i=0; i<r.getHeader().getNumFields(); i++) {
			if (r.getHeader().getFieldName(i).equals(srIdName)) { srIdNameIndex = i; }
			if (r.getHeader().getFieldName(i).equals(srSpeedName)) { srSpeedNameIndex = i; }
			if (r.getHeader().getFieldName(i).equals(srValDirName)) { srValDirNameIndex = i; }
			if (r.getHeader().getFieldName(i).equals(srVerifiedName)) { srVerifiedNameIndex = i; }
		}
		if (srIdNameIndex < 0) { throw new NoSuchFieldException("Field name '"+srIdName+"' not found."); }
		if (srSpeedNameIndex < 0) { throw new NoSuchFieldException("Field name '"+srSpeedName+"' not found."); }
		if (srValDirNameIndex < 0) { throw new NoSuchFieldException("Field name '"+srValDirName+"' not found."); }
		if (srVerifiedNameIndex < 0) { throw new NoSuchFieldException("Field name '"+srVerifiedName+"' not found."); }
		log.debug("  FieldName-->Index:");
		log.debug("    "+srIdName+"-->"+srIdNameIndex);
		log.debug("    "+srSpeedName+"-->"+srSpeedNameIndex);
		log.debug("    "+srValDirName+"-->"+srValDirNameIndex);
		log.debug("    "+srVerifiedName+"-->"+srVerifiedNameIndex);
	
		int srCnt = 0;
		int srIgnoreCnt = 0;
		while (r.hasNext()) {
			Object[] entries = r.readEntry();
			int verified = Integer.parseInt(entries[srVerifiedNameIndex].toString());
			// assign only verfied speed restricitons
			if (verified == 1) {
				int valdir = Integer.parseInt(entries[srValDirNameIndex].toString());
				String id = entries[srIdNameIndex].toString();
				if (valdir == 1) {
					// Valid in Both Directions
					Link ftLink = network.getLinks().get(new IdImpl(id+"FT"));
					Link tfLink = network.getLinks().get(new IdImpl(id+"TF"));
					if ((ftLink == null) || (tfLink == null)) { log.debug("  linkid="+id+", valdir="+valdir+": at least one link not found. Ignoring and proceeding anyway..."); srIgnoreCnt++; }
					else {
						double speed = Double.parseDouble(entries[srSpeedNameIndex].toString())/3.6;
						// assigning speed restriction only if given freespeed is higher
						if (speed < ftLink.getFreespeed(Time.UNDEFINED_TIME)) { ftLink.setFreespeed(speed); srCnt++; } else { srIgnoreCnt++; }
						if (speed < tfLink.getFreespeed(Time.UNDEFINED_TIME)) { tfLink.setFreespeed(speed); srCnt++; } else { srIgnoreCnt++; }
					}
				}
				else if (valdir == 2) {
					// Valid Only in Positive Direction
					Link ftLink = network.getLinks().get(new IdImpl(id+"FT"));
					if (ftLink == null) { log.debug("  linkid="+id+", valdir="+valdir+": link not found. Ignoring and proceeding anyway..."); srIgnoreCnt++; }
					else {
						double speed = Double.parseDouble(entries[srSpeedNameIndex].toString())/3.6;
						// assigning speed restriction only if given freespeed is higher
						if (speed < ftLink.getFreespeed(Time.UNDEFINED_TIME)) { ftLink.setFreespeed(speed); srCnt++; } else { srIgnoreCnt++; }
					}
				}
				else if (valdir == 3) {
					// Valid Only in Negative Direction
					Link tfLink = network.getLinks().get(new IdImpl(id+"TF"));
					if (tfLink == null) { log.debug("  linkid="+id+", valdir="+valdir+": link not found. Ignoring and proceeding anyway..."); srIgnoreCnt++; }
					else {
						double speed = Double.parseDouble(entries[srSpeedNameIndex].toString())/3.6;
						// assigning speed restriction only if given freespeed is higher
						if (speed < tfLink.getFreespeed(Time.UNDEFINED_TIME)) { tfLink.setFreespeed(speed); srCnt++; } else { srIgnoreCnt++; }
					}
				}
				else { throw new IllegalArgumentException("linkid="+id+": valdir="+valdir+" not known."); }
			}
		}
		log.info("  "+srCnt+" links with restricted speed assigned.");
		log.info("  "+srIgnoreCnt+" speed restrictions ignored (while verified = 1).");
		log.info("done.");
	}
}

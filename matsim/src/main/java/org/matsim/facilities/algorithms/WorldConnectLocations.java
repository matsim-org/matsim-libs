/* *********************************************************************** *
 * project: org.matsim.*
 * WorldBottom2TopCompletion.java
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

package org.matsim.facilities.algorithms;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

// this class seems to be unmaintained, uses deprecated code and was never updated.
// we added some hard-coded RuntimeExceptions in the code below in march 2025
// while the code might have worked before, because the deprecated methods always return null,
// it will never have done anything in that case.
@Deprecated(since="march 2025")
public class WorldConnectLocations {

	private final static Logger log = LogManager.getLogger(WorldConnectLocations.class);

	private final Config config;

	public final static String CONFIG_F2L = "f2l";

	public final static String CONFIG_F2L_INPUTF2LFile = "inputF2LFile";

	public final static String CONFIG_F2L_OUTPUTF2LFile = "outputF2LFile";

	public WorldConnectLocations(final Config config) {
		this.config = config;
	}

	private final void connectByFile(final ActivityFacilities facilities, final Network network, final String file, final Set<Id<ActivityFacility>> remainingFacilities) {
		log.info("    connecting facilities with links via "+CONFIG_F2L_INPUTF2LFile+"="+file);
		try (BufferedReader br = IOUtils.getBufferedReader(file)) {
			int lineCnt = 0;
			String currLine;
			br.readLine(); lineCnt++; // Skip header
			while ((currLine = br.readLine()) != null) {
				String[] entries = currLine.split("\t", -1);
				// fid  lid
				// 0    1
				Id<ActivityFacility> fid = Id.create(entries[0].trim(), ActivityFacility.class);
				Id<Link> lid = Id.create(entries[1].trim(), Link.class);
				ActivityFacility f = facilities.getFacilities().get(fid);
				Link l = network.getLinks().get(lid);
				if ((f != null) && (l != null)) {
					l = network.getLinks().get(l.getId());
					mapFacilityToLink(f,l);
					remainingFacilities.remove(f.getId());
				}
				else { log.warn(lineCnt+": at least one of the two locations not found."); }
				lineCnt++;
			}
		} catch (IOException e) {
			throw new RuntimeException("Error while reading given inputF2LFile='"+file+"'.", e);
		}
		log.info("      number of facilities that are still not connected to a link = "+remainingFacilities.size());
		log.info("    done. (connecting facilities with links via "+CONFIG_F2L_INPUTF2LFile+"="+file+")");
	}

	private void mapFacilityToLink(ActivityFacility f, Link l) {
		((ActivityFacilityImpl) f).setLinkId(l.getId());
	}

	private final void writeF2LFile(final ActivityFacilities facilities, final String file) {
		log.info("    writing f<-->l connections to  "+CONFIG_F2L_OUTPUTF2LFile+"="+file);
		try (BufferedWriter bw = IOUtils.getBufferedWriter(file)) {
			bw.write("fid\tlid\n");
			for (ActivityFacility f : facilities.getFacilities().values()) {
				bw.write(f.getId().toString()+"\t"+f.getLinkId().toString()+"\n");
			}
		} catch (IOException e) {
			throw new RuntimeException("Error while writing given outputF2LFile='"+file+"'.", e);
		}
		log.info("    done. (writing f<-->l connections to  "+CONFIG_F2L_OUTPUTF2LFile+"="+file+")");
	}

	public final void connectFacilitiesWithLinks(final ActivityFacilities facilities, final Network network) {
		log.info("  connecting facilities with links...");

		Set<Id<ActivityFacility>> remainingFacilities = new HashSet<>(facilities.getFacilities().keySet());
		if (this.config != null) {
			throw new RuntimeException("outdated code, should use materialized config group since 2016. / march 2025.");
//			String inputF2LFile = this.config.findParam(CONFIG_F2L,CONFIG_F2L_INPUTF2LFile);
//			if (inputF2LFile != null) {
//				connectByFile(facilities,network,inputF2LFile,remainingFacilities);
//			}
		}

		log.info("    connecting remaining facilities with links ("+remainingFacilities.size()+" remaining)...");
		for (Id<ActivityFacility> fid : remainingFacilities) {
			ActivityFacility f = facilities.getFacilities().get(fid);
			Link l = NetworkUtils.getNearestRightEntryLink(network, f.getCoord());
			l = network.getLinks().get(l.getId());
			mapFacilityToLink(f,l);
		}
		log.info("    done.");

		if (this.config != null) {
			throw new RuntimeException("outdated code, should use materialized config group since 2016. / march 2025.");
//			String outputF2LFile = this.config.findParam(CONFIG_F2L,CONFIG_F2L_OUTPUTF2LFile);
//			if (outputF2LFile != null) {
//				writeF2LFile(facilities,outputF2LFile);
//			}
		}
		log.info("  done. (connecting facilities with links)");
	}

}

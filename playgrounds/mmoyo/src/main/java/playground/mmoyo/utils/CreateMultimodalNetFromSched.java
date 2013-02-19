/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mmoyo.utils;

import java.io.File;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mrieser.pt.utils.MergeNetworks;

/**
 * reads a mivNet File and a schedule file to merge them into a multimodal network
 * 
 * */
class CreateMultimodalNetFromSched {

	public void run(final Network mivNet, final TransitSchedule schedule, final double beelineWalkConnectionDistance, final String outDir) {
		NetworkImpl mergedNet = NetworkImpl.createNetwork();
		final TransitRouterNetwork trRouterNetwork = TransitRouterNetwork.createFromSchedule(schedule, beelineWalkConnectionDistance);
		MergeNetworks.merge(mivNet, "", trRouterNetwork, "", mergedNet);
		
		//write ptnetwork
		NetworkWriter nwriter= 	new NetworkWriter(mergedNet);
		String outPtNet = outDir + "/ptNet.xml.gz"; 
		nwriter.write(outPtNet);
		System.out.println("done writting " + outPtNet);
		
		//write multimodalnet
		nwriter= new NetworkWriter(trRouterNetwork);
		String outMultimNet = outDir + "/newSchedOldMivMultimNetwork.xml.gz";
		nwriter.write(outMultimNet);
		System.out.println("done writting " + outMultimNet);
	}

	public static void main(String[] args) {
		String mivNetFile;
		String transitScheduleFile;
		
		if (args.length>0){
			mivNetFile=args[0];
			transitScheduleFile = args[1];
		}else{
			mivNetFile="../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/onlyMIVnet.xml.gz";
			transitScheduleFile = "../../input/NewTransitSchedule/transitSchedule.xml.gz";
		}
		
		DataLoader dloader = new DataLoader();
		TransitSchedule schedule = dloader.readTransitSchedule(transitScheduleFile) ;
		Network mivNet = dloader.readNetwork(mivNetFile); 
		String outDir = new File(transitScheduleFile).getParent();
		
		new CreateMultimodalNetFromSched().run(mivNet, schedule, 100.0, outDir );
	}

}
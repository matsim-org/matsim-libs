/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.dziemke.utils;

//import playground.andreas.fcd.Fcd;

// based on "package playground.andreas.osmBB.PTCountsOsm2Matsim"

public class MyPTCountsOsm2Matsim {
	

	public static void main(String[] args) {
		// String osmRepository = "e:/_shared-svn/osm_berlinbrandenburg/workingset/";
		String osmRepository = "D:/Workspace/shared-svn/studies/countries/de/osm_berlinbrandenburg/workingset/";
		String osmFile = "berlinbrandenburg_filtered.osm";
		// String countsFile = "f:/bln_counts/Di-Do_counts.xml";
		String countsFile = "D:/Workspace/shared-svn/studies/countries/de/berlin/counts/iv_counts/vmz_di-do.xml";
		//String countsOutFile = "f:/bln_counts/Di-Do_counts_out.xml";
		String countsOutFile = "D:/new/Di-Do_counts_out.xml";
		
//		String fcdNetInFile = "D:\\Berlin\\DLR_FCD\\20110207_Analysis\\berlin_2010_anonymized.ext";
//		String fcdEventsInFile = "D:\\Berlin\\DLR_FCD\\20110207_Analysis\\fcd-20101028_10min.ano";
		
		//String outDir = "f:/bln_counts/";
		String outDir = "D:/new/";
		String outName = "counts";
		
		String filteredOsmFile = outDir + outName + ".osm";
		
//		String[] streetFilter = new String[]{"motorway","motorway_link","trunk","trunk_link","primary","primary_link","secondary","tertiary","minor","unclassified","residential","living_street"};
		String[] streetFilter = new String[]{"motorway","motorway_link","trunk","trunk_link","primary","primary_link","secondary","tertiary","residential","living_street"};
		
//		String[] transitFilter = new String[]{"ferry", "subway", "light_rail", "tram", "train", "bus", "trolleybus"};
		String[] transitFilter = new String[]{"fsfsfgsg"};
		
//		OsmPrepare osmPrepare = new OsmPrepare(osmRepository + osmFile, filteredOsmFile, streetFilter, transitFilter);
//		osmPrepare.prepareOsm();
//		osmPrepare = null;
//		
//		Osm2Counts osm2Counts = new Osm2Counts(filteredOsmFile);
//		osm2Counts.prepareOsm();
//		HashMap<String, String> shortNameMap = osm2Counts.getShortNameMap();
//		osm2Counts = null;
//		
//		Counts counts = new Counts();
//		CountsReaderMatsimV1 countsReader = new CountsReaderMatsimV1(counts);
//		countsReader.parse(countsFile);
//				
//		OsmTransitMain osmTransitMain = new OsmTransitMain(filteredOsmFile, TransformationFactory.WGS84, TransformationFactory.DHDN_GK4, outDir + outName + "_network.xml", outDir + outName + "_schedule.xml", outDir + outName + "_vehicles.xml");
//		osmTransitMain.convertOsm2Matsim(transitFilter);
//		osmTransitMain = null;
//
////		ResizeLinksByCount4 r = new ResizeLinksByCount4(outDir + outName + "_network.xml", counts, shortNameMap, 1.0);
////		r.run(outDir + outName + "_network_resized.xml");
////		r = null;
//		counts = null;
//		
////		Set<String> linksBlocked = Fcd.readFcdReturningLinkIdsUsed(fcdNetInFile, fcdEventsInFile, outDir, outDir + outName + "_network_resized.xml", 500.0);
//		Set<String> linksBlocked = new TreeSet<String>();
//		
//		//PTCountsNetworkSimplifier ptCountNetSimplifier = new PTCountsNetworkSimplifier(outDir + outName + "_network_resized.xml", outDir + outName + "_schedule.xml", outDir + outName + "_network_merged.xml", outDir + outName + "_schedule_merged.xml", shortNameMap, countsFile, countsOutFile, outDir + "transitVehicles.xml.gz", linksBlocked);
//		PTCountsNetworkSimplifier ptCountNetSimplifier = new PTCountsNetworkSimplifier(outDir + outName + "_network.xml", outDir + outName + "_schedule.xml", outDir + outName + "_network_merged.xml", outDir + outName + "_schedule_merged.xml", shortNameMap, countsFile, countsOutFile, outDir + "transitVehicles.xml.gz", linksBlocked);
//		Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();
//		nodeTypesToMerge.add(new Integer(4));
//		nodeTypesToMerge.add(new Integer(5));
//		ptCountNetSimplifier.setNodesToMerge(nodeTypesToMerge);
//		ptCountNetSimplifier.setMergeLinkStats(false);
//		ptCountNetSimplifier.simplifyPTNetwork();		
	}

}

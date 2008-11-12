/* *********************************************************************** *
 * project: org.matsim.*
 * TeleatlasParser.java
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

package org.matsim.run;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkReaderTeleatlas;
import org.matsim.network.NetworkWriter;
import org.matsim.network.algorithms.NetworkTeleatlasAddManeuverRestrictions;
import org.matsim.network.algorithms.NetworkTeleatlasAddSpeedRestrictions;
import org.matsim.network.algorithms.NetworkWriteAsTable;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilder;
import org.matsim.utils.gis.matsim2esri.network.LanesBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.LineStringBasedFeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.Network2ESRIShape;
import org.matsim.utils.misc.ArgumentParser;

public class TeleAtlas2Network {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(TeleAtlas2Network.class);
	

	private static NetworkReaderTeleatlas reader = null;
	private static NetworkLayer network = null;
	private static String jcShpFileName = null; // teleatlas junction shape file name
	private static String nwShpFileName = null; // teleatlas network shape file name

	private static NetworkTeleatlasAddSpeedRestrictions srModule = null;
	private static String srDbfFileName = null;

	private static NetworkTeleatlasAddManeuverRestrictions mrModule = null;
	private static String mnShpFileName; // teleatlas maneuvers shape file name
	private static String mpDbfFileName; // teleatlas maneuver paths dbf file name
	
	private static String outputDir = "output";
	private static boolean writeNetworkXmlFile = false;
	private static boolean writeNetworkTxtFile = false;
	private static boolean writeNetworkShapeFile = false;

	//////////////////////////////////////////////////////////////////////
	// convert method
	//////////////////////////////////////////////////////////////////////
	
	public static final NetworkLayer convert() throws Exception {
		log.info("conversion settings...");
		printSetting();
		log.info("done.");
		
		reader.read();
		if (srModule != null) { srModule.run(network); }
		if (mrModule != null) { mrModule.run(network); }
		
		if (writeNetworkXmlFile) {
			log.info("writing xml file...");
			new NetworkWriter(network,outputDir+"/output_network.xml.gz").write();
			log.info("done.");
		}
		if (writeNetworkTxtFile) {
			log.info("writing txt files...");
			NetworkWriteAsTable nwat = new NetworkWriteAsTable(outputDir);
			nwat.run(network);
			nwat.close();
			log.info("done.");
		}
		if (writeNetworkShapeFile) {
			log.info("writing shape file...");
			if (Gbl.getConfig() == null) { Gbl.createConfig(null); }
			Gbl.getConfig().global().setCoordinateSystem("WGS84");
			FeatureGeneratorBuilder builder = new FeatureGeneratorBuilder(network);
			builder.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
			builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);		
			new Network2ESRIShape(network,outputDir+"/output_links.shp",builder).write();
			log.info("done.");
		}
		return network;
	}
	
	//////////////////////////////////////////////////////////////////////
	// print method
	//////////////////////////////////////////////////////////////////////

	private static final void printSetting() {
		log.info("  input / output:");
		log.info("    jcShpFileName: "+jcShpFileName);
		log.info("    nwShpFileName: "+nwShpFileName);
		log.info("    srDbfFileName: "+srDbfFileName);
		log.info("    mnShpFileName: "+mnShpFileName);
		log.info("    mpDbfFileName: "+mpDbfFileName);
		log.info("    outputDir:     "+outputDir);
		log.info("  options:");
		log.info("    ignoreFrcType8:              "+reader.ignoreFrcType8);
		log.info("    ignoreFrcType7onewayN:       "+reader.ignoreFrcType7onewayN);
		log.info("    maxFrcTypeForDoubleLaneLink: "+reader.maxFrcTypeForDoubleLaneLink);
		log.info("    minSpeedForNormalCapacity:   "+reader.minSpeedForNormalCapacity);
		log.info("    removeUTurns:                "+mrModule.removeUTurns);
		log.info("    expansionRadius:             "+mrModule.expansionRadius);
		log.info("    linkSeparation:              "+mrModule.linkSeparation);
		log.info("    writeNetworkXmlFile:         "+writeNetworkXmlFile);
		log.info("    writeNetworkTxtFile:         "+writeNetworkTxtFile);
		log.info("    writeNetworkShapeFile:       "+writeNetworkShapeFile);
		log.info("  junction shape file attributes:");
		log.info("    nodeIdName:      "+reader.nodeIdName);
		log.info("    nodeFeattypName: "+reader.nodeFeattypName);
		log.info("    nodeJncttypName: "+reader.nodeJncttypName);
		log.info("  network shape file attributes:");
		log.info("    linkIdName:             "+reader.linkIdName);
		log.info("    linkFeatTypName:        "+reader.linkFeatTypName);
		log.info("    linkFerryTypeName:      "+reader.linkFerryTypeName);
		log.info("    linkFromJunctionIdName: "+reader.linkFromJunctionIdName);
		log.info("    linkToJunctionIdName:   "+reader.linkToJunctionIdName);
		log.info("    linkLengthName:         "+reader.linkLengthName);
		log.info("    linkFrcTypeName:        "+reader.linkFrcTypeName);
		log.info("    linkOnewayName:         "+reader.linkOnewayName);
		log.info("    linkSpeedName:          "+reader.linkSpeedName);
		log.info("    linkLanesName:          "+reader.linkLanesName);
		if (srDbfFileName != null) {
			log.info("  speed restriction dbf file attributes:");
			log.info("    srIdName:       "+srModule.srIdName);
			log.info("    srSpeedName:    "+srModule.srSpeedName);
			log.info("    srValDirName:   "+srModule.srValDirName);
			log.info("    srVerifiedName: "+srModule.srVerifiedName);
		}
		if (mrModule != null) {
			log.info("  maneuver shape file attributes:");
			log.info("    mnIdName:       "+mrModule.mnIdName);
			log.info("    mnFeatTypeName: "+mrModule.mnFeatTypeName);
			log.info("    mnJnctIdName:   "+mrModule.mnJnctIdName);
		}
		if (mrModule != null) {
			log.info("  maneuver paths dbf file attributes:");
			log.info("    mpIdName: "+mrModule.mpIdName);
			log.info("    mpSeqNrName: "+mrModule.mpSeqNrName);
			log.info("    mpTrpelIDName: "+mrModule.mpTrpelIDName);
		}
	}
	
	//////////////////////////////////////////////////////////////////////
	// helper methods
	//////////////////////////////////////////////////////////////////////

	private static final void parseArguments(final String[] args) {
		// reader params
		boolean ignoreFrcType8 = false;
		boolean ignoreFrcType7onewayN = false;
		int maxFrcTypeForDoubleLaneLink = Integer.MIN_VALUE;
		int minSpeedForNormalCapacity = Integer.MAX_VALUE;
		// mrModule params
		boolean removeUTurns = false;
		double expansionRadius = Double.NaN;
		double linkSeparation = Double.NaN;
		
		if (args.length == 0) { System.out.println("Too few arguments."); printUsage(); System.exit(1); }
		Iterator<String> argIter = new ArgumentParser(args).iterator();
		while (argIter.hasNext()) {
			String arg = argIter.next();
			if (arg.equals("--xml")) { writeNetworkXmlFile = true; }
			else if (arg.equals("--txt")) { writeNetworkTxtFile = true; }
			else if (arg.equals("--shp")) { writeNetworkShapeFile = true; }
			else if (arg.equals("--frc8")) { ignoreFrcType8 = true; }
			else if (arg.equals("--frc7N")) { ignoreFrcType7onewayN = true; }
			else if (arg.equals("--maxfrc2l")) {
				ensureNextElement(argIter);
				try { maxFrcTypeForDoubleLaneLink = Integer.parseInt(argIter.next()); }
				catch (Exception e) { System.out.println("Cannot understand argument: --maxfrc2l " + arg); printUsage(); System.exit(1); }
			}
			else if (arg.equals("--minsnc")) {
				ensureNextElement(argIter);
				try { minSpeedForNormalCapacity = Integer.parseInt(argIter.next()); }
				catch (Exception e) { System.out.println("Cannot understand argument: --minsnc " + arg); printUsage(); System.exit(1); }
			}
			else if (arg.equals("--radius")) {
				ensureNextElement(argIter);
				try { expansionRadius = Double.parseDouble(argIter.next()); }
				catch (Exception e) { System.out.println("Cannot understand argument: --radius " + arg); printUsage(); System.exit(1); }
			}
			else if (arg.equals("--offset")) {
				ensureNextElement(argIter);
				try { linkSeparation =Double.parseDouble(argIter.next()); }
				catch (Exception e) { System.out.println("Cannot understand argument: --offset " + arg); printUsage(); System.exit(1); }
			}
			else if (arg.equals("--uturn")) { removeUTurns = true; }
			else if (arg.equals("-h") || arg.equals("--help")) { printUsage(); System.exit(0); }
			else if (arg.startsWith("-")) { System.out.println("Unrecognized option " + arg); System.exit(1); }
			else {
				jcShpFileName = arg;
				ensureNextElement(argIter);
				nwShpFileName = argIter.next();
				if (argIter.hasNext()) {
					arg = argIter.next(); // dbf, shp&dbf, dir
					if (arg.endsWith(".dbf")) {
						srDbfFileName = arg;
						if (argIter.hasNext()) {
							arg = argIter.next(); // shp&dbf, dir
							if (arg.endsWith(".shp")) {
								ensureNextElement(argIter);
								mnShpFileName = arg;
								mpDbfFileName = argIter.next();
								if (argIter.hasNext()) { // dir
									outputDir = argIter.next();
								}
							}
							else {
								outputDir = arg;
							}
						}
					}
					else if (arg.endsWith(".shp")) {
						ensureNextElement(argIter);
						mnShpFileName = arg;
						mpDbfFileName = argIter.next();
						if (argIter.hasNext()) { // dir
							outputDir = argIter.next();
						}
					}
					else {
						outputDir = arg;
					}
				}
				if (argIter.hasNext()) {
					System.out.println("Too many arguments.");
					printUsage();
					System.exit(1);
				}
			}
		}
		// init the reader
		reader = new NetworkReaderTeleatlas(network,jcShpFileName,nwShpFileName);
		reader.ignoreFrcType8 = ignoreFrcType8;
		reader.ignoreFrcType7onewayN = ignoreFrcType7onewayN;
		if (maxFrcTypeForDoubleLaneLink != Integer.MIN_VALUE) { reader.maxFrcTypeForDoubleLaneLink = maxFrcTypeForDoubleLaneLink; }
		if (minSpeedForNormalCapacity != Integer.MAX_VALUE) { reader.minSpeedForNormalCapacity = minSpeedForNormalCapacity; }
		// init the srModule
		if (srDbfFileName != null) { srModule = new NetworkTeleatlasAddSpeedRestrictions(srDbfFileName); }
		// init mrModule
		if (mnShpFileName != null) {
			mrModule = new NetworkTeleatlasAddManeuverRestrictions(mnShpFileName,mpDbfFileName);
			mrModule.removeUTurns = removeUTurns;
			if (expansionRadius != Double.NaN) { mrModule.expansionRadius = expansionRadius; }
			if (linkSeparation != Double.NaN) { mrModule.linkSeparation = linkSeparation; }
		}
	}

	private static final void ensureNextElement(final Iterator<String> iter) {
		if (!iter.hasNext()) {
			System.out.println("Too few arguments.");
			printUsage();
			System.exit(1);
		}
	}

	private static final void printUsage() {
		System.out.println();
		System.out.println("TeleatlasParser");
		System.out.println("Parsers Teleatlas databases into MATSim network data structure.");
		System.out.println("Optional: It also writes a MATSim XML network file and/or a shape file of the data.");
		System.out.println();
		System.out.println("usage: TeleatlasParser [OPTIONS] jcShpFile nwShpFile [srDbfFile] [mnShpFile mpShpFile] [outputDirectory]");
		System.out.println();
		System.out.println("jcShpFile:       Teleatlas Junction Shape File (typically called 'xyz________jc.shp')");
		System.out.println("nwShpFile:       Teleatlas network Shape File (typically called 'xyz________nw.shp')");
		System.out.println("srDbfFile:       Teleatlas speed restriction DBF File (typically called 'xyz________sr.dbf')");
		System.out.println("mnShpFile:       Teleatlas maneuver Shape File (typically called 'xyz________mn.shp')");
		System.out.println("mpShpFile:       Teleatlas maneuver paths DBF File (typically called 'xyz________mp.dbf')");
		System.out.println("outputDirectory: Directory where output files (MATSim XML network file and shape files) are stored.");
		System.out.println("                 default: ./output");
		System.out.println("                 If writing option is set (see below) the files will be stored in:");
		System.out.println("                 <outputDirectory>/output_network.xml.gz");
		System.out.println("                 <outputDirectory>/output_links.shp");
		System.out.println();
		System.out.println("Options:");
		System.out.println("--xml:           If set, a MATSim XML network file will be written to <outputDirectory>/output_network.xml.gz.");
		System.out.println("--txt:           If set, three ASCII network files will be written to <outputDirectory>/nodes.txt, links.txt, resp. linksET.txt).");
		System.out.println("                 These are useful for manual conversion with ESRI ArcGIS (ET GeoWizards plugin).");
		System.out.println("--shp:           If set, a network Shape file will be written to <outputDirectory>/output_links.shp");
		System.out.println("--frc8:          If set, links with FRC type = '8' will be ignored from the nwShpFile.");
		System.out.println("--frc7N:         If set, links with FRC type = '7' and ONEWAY = 'N' will be ignored from the nwShpFile.");
		System.out.println("--maxfrc2l FRCtype:");
		System.out.println("                 Defines, which links of the nwShpFile get 2 lanes per direction ('MAX FRC for 2 LANES').");
		System.out.println("                 Teleatlas defines the number of lanes (LANES attribute of the nwShpFile) only for");
		System.out.println("                 a few links. This option defines for links with LANES<'1' how many lanes will be set");
		System.out.println("                 based on the FRC type. E.g. '--maxfrc2l 4' sets 2 lanes for FRC=[0-4] and 1 lane for FRC>4.");
		System.out.println("                 default: '--maxfrc2l 3'");
		System.out.println("--minsnc freespeed:");
		System.out.println("                 Defines, which links of the nwShpFile get capacity[veh/h]=2000*#lanes ('MIN SPEED for NORMAL CAPACITY').");
		System.out.println("                 Teleatlas does not define link capacities. This option sets capactities based on freespeed and");
		System.out.println("                 derived number of lanes. E.g. '--minsnc 20'[km/h] sets capacity[veh/h]=2000*#lanes for freespeed>=20[km/h] and");
		System.out.println("                 capacity[veh/h]=1000*#lanes for freespeed<20[km/h].");
		System.out.println("                 default: '--minsnc 40'");
		System.out.println("--radius NodeExpansionRadius:");
		System.out.println("                 If [mnShpFile mpShpFile] are given, turn maneuvers will be created via expanding the corresponing node");
		System.out.println("                 with virtual nodes. The option defines the radius on which the virtual nodes will be places around");
		System.out.println("                 the expanded node. The unit of 'NodeExpansionRadius' depends on the projection of the input network.");
		System.out.println("                 E.g. for WGS84, '--radius 0.00003'[degrees] suits well. '--radius 0' will place all virtual nodes at the same place,");
		System.out.println("                 causing zero distance virtual links (for turn maneuvers).");
		System.out.println("                 default: '--radius 0.00003'");
		System.out.println("--offset NodeExpansionOffset:");
		System.out.println("                 If [mnShpFile mpShpFile] are given, turn maneuvers will be created via expanding the corresponing node");
		System.out.println("                 with virtual nodes. The option defines the offset against the position of the incident links of the");
		System.out.println("                 expanded node. The unit of 'NodeExpansionOffset' depends on the projection of the input network.");
		System.out.println("                 E.g. for WGS84, '--offset 0.0.000005'[degrees] suits well. '--offset 0' will place virtual nodes of an in- and");
		System.out.println("                 out-link pair at the same place, causing zero distance virtual u-turn links (for turn maneuvers).");
		System.out.println("                 default: '--offset 0.000005'");
		System.out.println("--uturn:         If set and if [mnShpFile mpShpFile] are given, no virtual u-turn links for an in- and out-link pair will be created.");
		System.out.println("-h, --help:      Displays this message.");
		System.out.println();
		System.out.println("----------------");
		System.out.println("2009, matsim.org");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main method
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) throws Exception {
//		// example arguments:
//		// usage: TeleatlasParser [OPTIONS] jcShpFile nwShpFile [srDbfFile] [mnShpFile mpShpFile] [outputDirectory]
//		String options = "--xml --txt --shp --frc8 --frc7N --maxfrc2l 3 --minsnc 40 --radius 0.000030 --offset 0.000005 --uturn ";
//		String jcShpFile = "../../input/teleatlas/jc_zurich.shp ";
//		String nwShpFile = "../../input/teleatlas/nw_zurich.shp ";
//		String srDbfFile = "../../input/teleatlas/cheche________sr.dbf ";
//		String mnShpFile = "../../input/teleatlas/cheche________mn.shp ";
//		String mpShpFile = "../../input/teleatlas/cheche________mp.dbf ";
//		String outDir = "../../output ";
//		String str = options+jcShpFile+nwShpFile+srDbfFile+mnShpFile+mpShpFile+outDir;
//		str = str.trim();
//		args = str.split(" ");
		
		network = new NetworkLayer();
		parseArguments(args);
		printSetting();
		convert();
		// TODO balmermi: more options
		// transform // -t WGS84toCH1903LV03
		// clean network // -c
		// network thinning // -t
	}
}

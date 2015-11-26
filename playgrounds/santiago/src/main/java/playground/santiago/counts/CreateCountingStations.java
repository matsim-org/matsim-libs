/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.santiago.counts;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class CreateCountingStations {

	private static final Logger log = Logger.getLogger(CreateCountingStations.class) ;

	private static final String svnWorkingDir = "../../../shared-svn/projects/santiago/scenario/";	//Path: KT (SVN-checkout)
	private static final String workingDirInputFiles = svnWorkingDir + "inputFromElsewhere/exportedFilesFromDatabase/" ;
	private static final String outputDir = svnWorkingDir + "inputForMATSim/counts/" ; //outputDir of this class -> input for Matsim (KT)

	//A: Position (linkId) of counting stations
	private static final String CSIdFILE_NAME = "CSId-LinkId_merged";		
	private static final String CSIdFILE = workingDirInputFiles + "../counts/" + CSIdFILE_NAME + ".csv" ;

	//B: recent network-File
	private static final String NETFILE = svnWorkingDir + "inputForMATSim/network/network_merged_cl.xml.gz";	


	//TODO: Integrate this class to the network creation? (kt 2015-08-15)
	//TODO: clean up the both variants A and B  - maybe allow user to choose by a switch (kt 2015-08-15)
	public static void main(String args[]){

		File file = new File(outputDir);
		System.out.println("Verzeichnis " + file + " erstellt: "+ file.mkdirs());	

		String csDataFile_Name;
		String countfile_Name;
		String csDataFile;

//		//Variante A: Use Id-relation from file
//		//#Counts in persons
//		csDataFile_Name = "T_VIAJESTEMP"; //Number of Persons = counts per Vehicle * Factor (#Persons / vehicle)
//		csDataFile = workingDirInputFiles + csDataFile_Name + ".csv" ;
//		countfile_Name = "counts_merged_PERS" ;	//Output-Countfile_name
//		createCsFilesWithIdFile(countfile_Name, csDataFile);
//		
//		//#Counts in vehicles
//		csDataFile_Name = "T_FLUJO_TASA"; //Number of Persons = counts per Vehicle * Factor (#Persons / vehicle)
//		csDataFile = workingDirInputFiles + csDataFile_Name + ".csv" ;
//		countfile_Name = "counts_merged_VEH" ;	//Output-Countfile_name
//		createCsFilesWithIdFile(countfile_Name, csDataFile);

		//Variante B: Use Id-relation from map
		//Network-Stuff
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(NETFILE);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		//#Counts in persons
		csDataFile_Name = "T_VIAJESTEMP"; //Number of Persons = counts per Vehicle * Factor (#Persons / vehicle)
		csDataFile = workingDirInputFiles + csDataFile_Name + ".csv" ;
		countfile_Name = "counts_merged_PERS" ;	//Output-Countfile_name
		createCsFilesWithoutIdFile(countfile_Name, csDataFile, scenario.getNetwork());

		//#Counts in vehicles
		csDataFile_Name = "T_FLUJO_TASA"; //Number of Persons = counts per Vehicle * Factor (#Persons / vehicle)
		csDataFile = workingDirInputFiles + csDataFile_Name + ".csv" ;
		countfile_Name = "counts_merged_VEH" ;	//Output-Countfile_name
		createCsFilesWithoutIdFile(countfile_Name, csDataFile, scenario.getNetwork());

		log.info("### Done. ###");
	}

	private static void createCsFilesWithIdFile(String countfile_Name, String csDataFile) {
		for (int i=1; i<=13; i++){
			String vehCat = "C"+ String.format("%2s", new DecimalFormat("00").format(i));
			System.out.println(vehCat);

			//Use Id-relation from file
			String ouputCSFile = outputDir + countfile_Name + "_" + vehCat + ".xml" ;	//Output-Countfile
			CSVToCountingStations converter = new CSVToCountingStations(ouputCSFile, CSIdFILE, csDataFile, vehCat);
			converter.createCsIdString2LinkId();
			converter.readCountsFromCSV();
			converter.writeCountsToFile();
		}

	}

	private static void createCsFilesWithoutIdFile(String countfile_Name, String csDataFile, Network network) {
		Map<String,Id<Link>> csIdString2LinkId = new TreeMap<String,Id<Link>>();	
		csIdString2LinkId = getLinkIdsforCS(network);

		for (int i=1; i<=13; i++){
			String vehCat = "C"+ String.format("%2s", new DecimalFormat("00").format(i));
			System.out.println(vehCat);		

			//Use Id-relation from map
			String ouputCSFile = outputDir + countfile_Name + "_" + vehCat + ".xml" ;	//Output-Countfile
			CSVToCountingStations converter = new CSVToCountingStations(ouputCSFile, csDataFile, vehCat, csIdString2LinkId);
			converter.readCountsFromCSV();
			converter.writeCountsToFile();

		}
	}

	private static Map<String, Id<Link>> getLinkIdsforCS(Network network) {
		log.info("Start collecting linkIds of counting stations.");
		Map<String,Id<Link>> csIdString2LinkId = new TreeMap<String,Id<Link>>();

		Map<String, ArrayList<Double>> csIdString2LinkCoordinates = new TreeMap<String, ArrayList<Double>>();
		
		
		//Fill Map with coordinate information: CS-Id; Link is defined by coordinates: fromX, fromY, toX, toY (gained by class ExportCsLinkIdInfo)
		//Comment unused CS, so they were finally not written to counts file.(kt 2015-08-15); Done 2015-09-07 kt: reason is low traffic in data
		csIdString2LinkCoordinates.put("P4001", new ArrayList<Double>(Arrays.asList(348741.6403540815, 6298712.226616853, 348777.7343999935, 6298721.354068928)));
		csIdString2LinkCoordinates.put("P4002", new ArrayList<Double>(Arrays.asList(345988.9050458382, 6301186.871172849, 346047.83562092524, 6300866.269743414)));
		csIdString2LinkCoordinates.put("P4003", new ArrayList<Double>(Arrays.asList(347530.86573883635, 6300114.48511683, 347516.6457841758, 6300629.253318562)));
		csIdString2LinkCoordinates.put("P4004", new ArrayList<Double>(Arrays.asList(345560.8341227104, 6288354.275038192, 345505.223207086, 6288127.834027756)));
		csIdString2LinkCoordinates.put("P4005", new ArrayList<Double>(Arrays.asList(352501.6580244927, 6290983.209605534, 352705.43569131615, 6290657.528322749)));
//		csIdString2LinkCoordinates.put("P4006", new ArrayList<Double>(Arrays.asList(341590.25799234456, 6299911.947005313, 342039.8714344363, 6299813.4459771775)));
		csIdString2LinkCoordinates.put("P4007", new ArrayList<Double>(Arrays.asList(336762.3570879779, 6288778.437179374, 336918.86172700214, 6288879.520922063)));
		csIdString2LinkCoordinates.put("P4008", new ArrayList<Double>(Arrays.asList(355244.2917613711, 6297268.712461952, 355607.32536916086, 6296665.622070781)));
		csIdString2LinkCoordinates.put("P4009", new ArrayList<Double>(Arrays.asList(353227.51817795576, 6301913.9402803425, 353467.13888071815, 6302000.112240496)));
		csIdString2LinkCoordinates.put("P4010", new ArrayList<Double>(Arrays.asList(353823.4735664143, 6303746.755542972, 353802.3076836841, 6303858.309778554)));
		csIdString2LinkCoordinates.put("P4011", new ArrayList<Double>(Arrays.asList(357564.65325167694, 6281572.011181919, 357852.65839787526, 6281382.794156178)));
		csIdString2LinkCoordinates.put("P4012", new ArrayList<Double>(Arrays.asList(352414.4784928054, 6295984.641221459, 351855.5068405433, 6296063.055565931)));
		csIdString2LinkCoordinates.put("P4013", new ArrayList<Double>(Arrays.asList(350801.1835430289, 6296878.435791828, 350822.5487373812, 6297046.2539615985)));
		csIdString2LinkCoordinates.put("P4014", new ArrayList<Double>(Arrays.asList(346002.12674172677, 6296442.538960482, 345726.4929528664, 6296681.4637730485)));
		csIdString2LinkCoordinates.put("P4015", new ArrayList<Double>(Arrays.asList(341304.21720147226, 6298458.864394044, 341749.2612742036, 6298611.099713646)));
		csIdString2LinkCoordinates.put("P4016", new ArrayList<Double>(Arrays.asList(347582.90565141797, 6293392.39588717, 347482.9061639274, 6294192.382412657)));
//		csIdString2LinkCoordinates.put("P4017", new ArrayList<Double>(Arrays.asList(338203.3747150472, 6300480.966189107, 338605.58603058197, 6300452.771646736)));
		csIdString2LinkCoordinates.put("P4018", new ArrayList<Double>(Arrays.asList(350786.56134206324, 6296756.206699034, 350740.41997394816, 6296335.403150608)));
		csIdString2LinkCoordinates.put("P4019", new ArrayList<Double>(Arrays.asList(343397.41305059474, 6297262.083308905, 343813.5372866099, 6297387.968160456)));
		csIdString2LinkCoordinates.put("P4020", new ArrayList<Double>(Arrays.asList(346576.05777126097, 6291589.676693663, 346300.3192060798, 6291670.056336965)));
		csIdString2LinkCoordinates.put("P4021", new ArrayList<Double>(Arrays.asList(345708.2752227861, 6298718.966129151, 345633.24957030313, 6298194.7604689365)));
		csIdString2LinkCoordinates.put("P4021C", new ArrayList<Double>(Arrays.asList(345686.90933659626, 6298745.566086771, 345669.1453348678, 6298556.561050668)));
		csIdString2LinkCoordinates.put("P4022", new ArrayList<Double>(Arrays.asList(340973.8269947665, 6283015.683044365, 340985.9314704983, 6283191.436747448)));
		csIdString2LinkCoordinates.put("P4022C", new ArrayList<Double>(Arrays.asList(341003.8935479203, 6282998.00068631, 341040.6969382686, 6283395.914414318)));
		csIdString2LinkCoordinates.put("P4023", new ArrayList<Double>(Arrays.asList(337881.12703960657, 6297183.733686175, 339096.8897968944, 6296924.306625824)));
		csIdString2LinkCoordinates.put("P4024", new ArrayList<Double>(Arrays.asList(342451.71697426995, 6291537.252628419, 343055.03717953654, 6293184.290823318)));
//		csIdString2LinkCoordinates.put("P4024C", new ArrayList<Double>(Arrays.asList(342591.9260217162, 6291747.824772469, 343169.7640801817, 6293376.0029335795)));
//		csIdString2LinkCoordinates.put("P4025", new ArrayList<Double>(Arrays.asList(347145.3092119654, 6285013.597937774, 346214.65486027254, 6285142.730628112)));
//		csIdString2LinkCoordinates.put("P4026", new ArrayList<Double>(Arrays.asList(345070.0294750921, 6285946.467347169, 345160.67445241223, 6286349.912576522)));
		csIdString2LinkCoordinates.put("P4027", new ArrayList<Double>(Arrays.asList(339231.5150003758, 6291369.841542346, 339694.09648319, 6291122.608940721)));
		csIdString2LinkCoordinates.put("P4027C", new ArrayList<Double>(Arrays.asList(339345.1267850042, 6291311.573078447, 339733.72535055847, 6291061.230705491)));
//		csIdString2LinkCoordinates.put("P4028", new ArrayList<Double>(Arrays.asList(333520.0063940933, 6279131.889945801, 333221.6555269258, 6278378.337523911)));
		csIdString2LinkCoordinates.put("P4029", new ArrayList<Double>(Arrays.asList(341831.23058163736, 6320625.080959927, 341818.3254941361, 6319987.141910888)));
//		csIdString2LinkCoordinates.put("P4030", new ArrayList<Double>(Arrays.asList(343297.3744950009, 6306095.353475468, 343129.76678558637, 6306019.2037957795)));
//		csIdString2LinkCoordinates.put("P4031", new ArrayList<Double>(Arrays.asList(347719.1947680814, 6305959.436104504, 347728.80980782607, 6305995.400810116)));
		csIdString2LinkCoordinates.put("P4032", new ArrayList<Double>(Arrays.asList(350189.42825046944, 6287607.810276749, 349643.29767575883, 6287503.276325313)));
//		csIdString2LinkCoordinates.put("P4032C", new ArrayList<Double>(Arrays.asList(350284.3161210218, 6287660.772843761, 349574.9150556776, 6287514.929716816)));
		csIdString2LinkCoordinates.put("P4033", new ArrayList<Double>(Arrays.asList(338867.4415136289, 6314316.19716293, 338772.91898856126, 6314552.53297171)));
//		csIdString2LinkCoordinates.put("P4033C", new ArrayList<Double>(Arrays.asList(338903.8764100816, 6314305.121995285, 338780.41571915476, 6314637.375162274)));
		csIdString2LinkCoordinates.put("P4034", new ArrayList<Double>(Arrays.asList(358967.4255705662, 6307673.516610031, 359093.49065522826, 6307303.739488844)));
		csIdString2LinkCoordinates.put("P4035", new ArrayList<Double>(Arrays.asList(342225.32174008526, 6289475.895323425, 341172.0853133272, 6290035.463314236)));
		csIdString2LinkCoordinates.put("P4035C", new ArrayList<Double>(Arrays.asList(341751.6363102448, 6289808.112087689, 341580.0138988708, 6289879.717577882)));
		csIdString2LinkCoordinates.put("P4036", new ArrayList<Double>(Arrays.asList(351214.4404882479, 6295026.506569565, 350745.23560061917, 6294999.09685377)));
		csIdString2LinkCoordinates.put("P4037", new ArrayList<Double>(Arrays.asList(354230.35456606967, 6295418.575978987, 355497.1581532542, 6295136.08593913)));
		csIdString2LinkCoordinates.put("P4038", new ArrayList<Double>(Arrays.asList(338710.18781639554, 6306115.225801121, 340582.6749540648, 6305935.614416813)));
		csIdString2LinkCoordinates.put("P4038C", new ArrayList<Double>(Arrays.asList(338476.60251406895, 6306129.335618628, 339069.81668374164, 6306059.747783846)));
//		csIdString2LinkCoordinates.put("P4039", new ArrayList<Double>(Arrays.asList(339322.32774276554, 6302316.155125948, 338915.8920216869, 6302219.918385169)));
//		csIdString2LinkCoordinates.put("P4040", new ArrayList<Double>(Arrays.asList(348583.74412872986, 6285647.484919871, 348490.70092987316, 6285637.2353572585)));


		//Search link (id) in current network
		for (String csIdString : csIdString2LinkCoordinates.keySet()){
			Id<Link> linkId = null;
			for (Link link : network.getLinks().values()){
				if (link.getFromNode().getCoord().getX() == csIdString2LinkCoordinates.get(csIdString).get(0)){
					if (link.getFromNode().getCoord().getY() == csIdString2LinkCoordinates.get(csIdString).get(1)){
						if (link.getToNode().getCoord().getX() == csIdString2LinkCoordinates.get(csIdString).get(2)){
							if (link.getToNode().getCoord().getY() == csIdString2LinkCoordinates.get(csIdString).get(3)){
								linkId = link.getId();
								System.out.println("Link for CS:  " + csIdString + " is : " + linkId);
							} 
						} 
					} 
				} 
			}

			if (linkId != null) {
				csIdString2LinkId.put(csIdString, linkId);
			} else {
				log.warn("Can't find link for CS: " + csIdString );
			}
		}

		log.info("collected linkIds of counting stations");
		return csIdString2LinkId;
	}



}

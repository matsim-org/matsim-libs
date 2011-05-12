/* *********************************************************************** *
 * project: org.matsim.*
 * MyLinkstatComparator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.roadpricing.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class MyLinkstatComparator {
	private final static Logger log = Logger.getLogger(MyLinkstatComparator.class);
	private File baselineFile;
	private File compareFile;
	private String compareField;
	private int baselineIndex;
	private int compareIndex;
	private Map<Id, Double> differenceMap;

	/**
	 * <P><i>Note:</i> there seems to be 154 columns in the 
	 * <code>linkstats.txt</code> file (1 Dec '10).</P>
	 * @param args containing the following elements, and in this order:
	 * <ol>
	 * 	<li> <b>baselineFile</b> absolute pathname of the <code>linkstats.txt</code>
	 * 		file that will be used as the baseline for comparison.
	 * 	<li> <b>compareFile</b> absolute pathname of the <code>linkstats.txt</code> 
	 * 		file that will be compared against the baseline.
	 *  <li> <b>compareField</b> the <i>correct</i> column description that 
	 *  	should be used for comparison. 
	 *  <li> <b>linkComparison</b> absolute pathname of the <code>csv</code> file
	 *  	to which comparisons are written.
	 *  <li> <b>threshold</b> the absolute fraction difference (increase/decrease)
	 *  	above which differences are recorded and written to file. 
	 *  <li> <b>minimumLanes</b> indicating what the minimum number of lanes are
	 *  	that should be considered in the comparison.
	 *  <li> <b>network</b> absolute pathname of the <code>network.xml.gz</code> 
	 *  	file used in link comparisons. 
	 * </ol>
	 */
	public static void main(String[] args) {
		String outputFile = null;
		Double threshold = null;
		Integer minimumLanes = null;
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		MyLinkstatComparator mlc = null;
		if(args.length != 7){
			throw new IllegalArgumentException("Wrong number of arguments");
		} else{
			mlc = new MyLinkstatComparator(
					args[0],
					args[1],
					args[2]
			);
			outputFile = args[3];
			threshold = Double.parseDouble(args[4]);
			minimumLanes = Integer.parseInt(args[5]);
			
			// Read the network file if the number of lanes are specified.
			NetworkReaderMatsimV1 nr = new NetworkReaderMatsimV1(sc);
			nr.parse(args[6]);
		}
		
		mlc.compare(sc.getNetwork(), minimumLanes, threshold);
		mlc.writeDifferenceToFile(outputFile);

		log.info("-----------------------");
		log.info("       Completed");
		log.info("=======================");
	}
	
	/**
	 * Constructor to instantiate a {@link MyLinkstatComparator}. The constructor
	 * also checks that both the baseline and the comparison files exist, and that
	 * the comparison field exists in both files.
	 * @param baselineFile absolute path of the <code>linkstats.txt.gz</code> file 
	 * 		used as baseline file.
	 * @param compareFile absolute path of the <code>linkstats.txt.gz</code> file
	 * 		compared to the baseline file.
	 * @param compareField the <i>correct</i> description of the field that will be
	 * 		used as the basis for the comparison, for example <code>"HRS5-6avg"</code>
	 * 		to indicate the average number of vehicles on each link between 05:00
	 * 		and 06:00 in the morning. 
	 */
	public MyLinkstatComparator(String baselineFile, String compareFile, String compareField) {

		// Check that baseline file exists.
		File f1 = new File(baselineFile);
		if(!f1.exists()){
			throw new RuntimeException("Could not find " + baselineFile);
		} else{
			this.baselineFile = f1;
		}
		// Check that field used for comparison is found.
		try {
			BufferedReader br1 = IOUtils.getBufferedReader(baselineFile);
			try{
				String[] header = br1.readLine().split("\t");
				boolean found = false;
				int index = 0;
				while(!found && index < header.length){
					if(header[index].equalsIgnoreCase(compareField)){
						found = true;
						baselineIndex = index;
					} else{
						index++;
					}
				}
				if(!found){
					log.warn("Could not find " + compareField + " in baseline file " + baselineFile);
				} else{
					log.info("Found " + compareField + " in column " + baselineIndex + " in baseline file " + baselineFile);
				}
			} finally{
				br1.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Check that comparison file exist.
		File f2 = new File(compareFile);
		if(!f2.exists()){
			throw new RuntimeException("Could not find " + compareFile);
		} else{
			this.compareFile = f2;
		}	
		// Check that field used for comparison is found.
		try {
			BufferedReader br2 = IOUtils.getBufferedReader(compareFile);
			try{
				String[] header = br2.readLine().split("\t");
				boolean found = false;
				int index = 0;
				while(!found && index < header.length){
					if(header[index].equalsIgnoreCase(compareField)){
						found = true;
						compareIndex = index;
					} else{
						index++;
					}
				}
				if(!found){
					log.warn("Could not find " + compareField + " in comparison file " + compareFile);
				} else{
					log.info("Found " + compareField + " in column " + compareIndex + " in comparison file " + compareFile);
				}
			} finally{
				br2.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		differenceMap = new TreeMap<Id, Double>();
		log.info("The MyLinkstatComparator successfully instantiated.");
	}
	
	/**
	 * Compare only links with a specified number of lanes in the given network.
	 * @param network from which the number of lanes are identified.
	 * @param lanes the minimum number of lanes that a link must have to be
	 * 		considered in the comparison. 
	 * @param threshold the absolute fraction difference (increase/decrease)
	 *  	above which differences are recorded.
	 */
	public void compare(Network network, int lanes, double threshold){
		log.info("Comparing the " + compareField + " field of the two files.");
		int comparisonCounter = 0;
		int counter = 0;
		int multiplier = 1;
				
		try {
			BufferedReader br1 = IOUtils.getBufferedReader(baselineFile.getAbsolutePath());
			BufferedReader br2 = IOUtils.getBufferedReader(compareFile.getAbsolutePath());
			try{
				String line1 = br1.readLine();
				String line2 = br2.readLine();
				while((line1 = br1.readLine()) != null && 
					  (line2 = br2.readLine()) != null){
					String[] sa1 = line1.split("\t");
					String[] sa2 = line2.split("\t");
					if(!sa1[0].equalsIgnoreCase(sa2[0])){
						log.error("Two line entries do not have the same link Id.");
					}
					// Now just do the comparison.
					if(lanes > 1){
						if(network.getLinks().get(new IdImpl(sa1[0])).getNumberOfLanes() >= lanes){
							comparisonCounter++;
							double baseValue = Double.parseDouble(sa1[baselineIndex]);
							double compareValue = Double.parseDouble(sa2[compareIndex]);
							double difference = (compareValue - baseValue) / baseValue;
							if(Math.abs(difference) > threshold){
								differenceMap.put(new IdImpl(sa1[0]), difference);
							}
						}						
					} else{
						comparisonCounter++;
						double baseValue = Double.parseDouble(sa1[baselineIndex]);
						double compareValue = Double.parseDouble(sa2[compareIndex]);
						double difference = (compareValue - baseValue) / baseValue;
						if(Math.abs(difference) > threshold){
							differenceMap.put(new IdImpl(sa1[0]), difference);
						}						
					}

					// Report progress.
					if(++counter == multiplier){
						log.info("   lines: " + counter);
						multiplier *= 2;
					}
				}
				log.info("   lines: " + counter + " (Done)");
			} finally{
				br1.close();
				br2.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Of the " + network.getLinks().size() + " links, " 
				+ comparisonCounter + " were compared (had " 
				+ lanes + " lanes or more).");
		log.info(differenceMap.size() + " links exceeded the difference threshold of " 
				+ threshold);
	}
	
	/**
	 * Compare the entire network.
	 * @param threshold the absolute fraction difference (increase/decrease)
	 *  	above which differences are recorded.
	 * @see {@link MyLinkstatComparator#compare(Network, int, double)}
	 */
	public void compare(double threshold){
		this.compare(null, 1, threshold);		
	}
	
	
	public void writeDifferenceToFile(String filename){
		if(differenceMap.size() > 0){
			log.info("Writing differences to " + filename);
			int counter = 0;
			int multiplier = 1;
			try {
				BufferedWriter bw = IOUtils.getBufferedWriter(filename);
				try{
					bw.write("LinkId,Diff");
					bw.newLine();
					for(Id id : differenceMap.keySet()){
						bw.write(id.toString());
						bw.write(",");
						bw.write(String.valueOf(differenceMap.get(id)));
						bw.newLine();
						
						// Report progress.
						if(++counter == multiplier){
							log.info("   links written: " + counter);
							multiplier *= 2;
						}
					}
					log.info("   links written: " + counter + " (Done)");
				}finally{
					bw.close();
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else{
			log.warn("There are no differences to write to file.");
		}
	}
}

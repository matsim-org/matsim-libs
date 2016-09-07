/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.patnaIndia.simTime;

import java.io.BufferedWriter;
import java.util.SortedMap;

import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.travelTime.ModalTravelTimeAnalyzer;

/**
 * @author amit
 */

public class TravelTimeComperator {
	
	private String respectiveFileDirectory = "../../../../repos/runs-svn/patnaIndia/run106/1pct/";
	private BufferedWriter writer;
	private static final String LAST_IT = "200";

	public static void main(String[] args) {
		TravelTimeComperator ttc = new TravelTimeComperator();
		ttc.openFile();
		ttc.startProcessing();
		ttc.closeFile();
	}

	public void openFile(){
		writer = IOUtils.getBufferedWriter(respectiveFileDirectory+"/travelTime.txt");
		try {
			writeString("scenario\tmode\ttravelTime\n");
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to a file. Reason :"+ e);
		}
	}

	private void writeString(String str){
		try{
			writer.write(str);
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to a file. Reason :"+ e);
		}
	}

	public void closeFile(){
		try{
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to a file. Reason :"+ e);
		}
	}

	public void startProcessing(){
		for (LinkDynamics ld : LinkDynamics.values() ) {
			for ( TrafficDynamics td : TrafficDynamics.values()){
				String queueModel = ld+"_"+td;
				for(int i=2;i<12;i++){
					String eventsFile = respectiveFileDirectory + "/output_"+queueModel+"_"+i+"/ITERS/it."+LAST_IT+"/"+LAST_IT+".events.xml.gz";
					ModalTravelTimeAnalyzer timeAnalyzer  = new ModalTravelTimeAnalyzer(eventsFile);
					timeAnalyzer.run();
					SortedMap<String,Double> modalTravelTime = timeAnalyzer.getMode2AvgTripTime();
					
					for (String mode : modalTravelTime.keySet()) {
						writeString(queueModel+"\t"+mode+"\t"+modalTravelTime.get(mode)+"\n");
					}
				}
			}
		}
	}
}

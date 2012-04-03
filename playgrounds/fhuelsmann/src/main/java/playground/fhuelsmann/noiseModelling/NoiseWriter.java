/* *********************************************************************** *
 * project: org.matsim.*
 * NoiseTool.java
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
package playground.fhuelsmann.noiseModelling;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;

public class NoiseWriter {
	private static final Logger logger = Logger.getLogger(NoiseWriter.class);
	private List<NoiseEventImpl> eventsList ;
	private Map<Id,Map<String,Double>> linkId2timePeriod2lme ;
	private Map<Id,Double> linkId2Lden ;
	private static String runDirectory = "../../run981/";
	

	public NoiseWriter (Map<Id,Map<String,Double>> linkId2timePeriod2lme , Map<Id,Double> linkId2Lden ){
		this.linkId2timePeriod2lme = linkId2timePeriod2lme;
		this.linkId2Lden = linkId2Lden;
		eventsList = new ArrayList<NoiseEventImpl> ();
		createEvents ();
	}
	
	private void createEvents (){ //1.change = private
		for (Entry<Id,Map<String,Double>> entry : linkId2timePeriod2lme.entrySet()){
			Id linkId = entry.getKey();
			Double l_DEN = linkId2Lden.get(linkId);
			Map<String,Double> l_mE = entry.getValue(); 
			double time = 0.0 ; 
			NoiseEventImpl event = new NoiseEventImpl (time,linkId,l_mE,l_DEN); 
			eventsList.add(event);
		}		
	}	
	public void writeEvents (){
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		String outputfile = runDirectory + "noiseEvents.xml";
		EventWriterXML eWriter = new EventWriterXML(outputfile);
		eventsManager.addHandler(eWriter);
		for(NoiseEventImpl event : eventsList){
			eventsManager.processEvent(event);
		}	
		eWriter.closeFile();
		logger.info("Finished writing output to " + outputfile);
		
	}
	
	public void writeInfosProStunde(Map <Id,double [][]> infos)throws IOException{
		File target = new File(runDirectory + "InfosProStunde.txt");
		FileOutputStream fos = new FileOutputStream(target);
		OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
		BufferedWriter bw = new BufferedWriter(osw);
		String str1 = "LinkId;timeperiod;HDV;TotalVehicles";
		bw.write(str1);
		bw.newLine();
		for (Entry<Id,double[][]> entry : infos.entrySet()){
			String linkId = entry.getKey()+"";									
			for (int i =0;i<24; ++i){
				String str0 = "";
				double total = entry.getValue()[i][0];
				double heavy = entry.getValue()[i][1];
				int timeclass = i+1;
				str0 = linkId+";"+timeclass+";"+heavy+";"+total ; 
				bw.write(str0);
				bw.newLine();
			}
			
		}
		bw.close();
		osw.close();
		fos.close();
		System.out.println("----------------------------vehicles per hour calculated--------------------------------");
	}
	
}
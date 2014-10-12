
/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemsLinkConverter.java
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

package playground.jbischoff.BAsignalsDemand;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.signalsystems.MatsimSignalSystemsReader;
import org.matsim.signalsystems.data.signalsystems.v20.SignalData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsDataImpl;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsReader20;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsWriter20;
import org.matsim.signalsystems.model.SignalSystem;
import org.xml.sax.SAXException;

import playground.jbischoff.lsacvs2kml.LinkConversionsData;


public class SignalSystemsLinkConverter {
	
	static final String INPUTFILE = "/Users/JB/Documents/Work/cottbus/signalSystemsCottbusByNodes_v2.0.xml";
	static final String OUTPUTFILE = "/Users/JB/Documents/Work/cottbus/cottbus_20_jb/signalSystemsCottbusByNodes_v2.0_conv.xml";
	
public static void main (String args0[]) throws JAXBException, SAXException, ParserConfigurationException, IOException{
	SignalSystemsData ssdata = new SignalSystemsDataImpl();
	SignalSystemsData newssdata = new SignalSystemsDataImpl();
	
	SignalSystemsReader20 ssread = new SignalSystemsReader20(ssdata, MatsimSignalSystemsReader.SIGNALSYSTEMS20);
	ssread.readFile(INPUTFILE);
	Map<Id<SignalSystem>,LinkConversionsData> convmap = readConv("/Users/JB/Desktop/BA-Arbeit/sim/convdata.csv");

	for (SignalSystemData ssd : ssdata.getSignalSystemData().values()){
		//if (convmap.get(ssd.getId())!=null){
		try {
		SignalSystemData nssd = newssdata.getFactory().createSignalSystemData(ssd.getId());
		for (SignalData sd : ssd.getSignalData().values()){
			
			SignalData nsd = newssdata.getFactory().createSignalData(sd.getId());
			nsd.setLinkId(convmap.get(ssd.getId()).getConv(sd.getLinkId()));
			System.out.println(nsd.getLinkId());
			/*for (Id tmr : sd.getTurningMoveRestrictions()){
				nsd.addTurningMoveRestriction(convmap.get(ssd.getId()).getConv(tmr));
				}*/
			for (Id<Lane> lid : sd.getLaneIds()){
				nsd.addLaneId(lid);
			}
			nssd.addSignalData(nsd);
		}
		newssdata.addSignalSystemData(nssd);
		}
		catch  (NullPointerException e) {
			// TODO Auto-generated catch block
			System.err.println("Can't find Conversion for "+ssd.getId()+" , will skip this one.");
		}
	}
	//}
	
	SignalSystemsWriter20 writer = new SignalSystemsWriter20(newssdata);
	writer.write(OUTPUTFILE);
	
}

public static  Map<Id<SignalSystem>,LinkConversionsData> readConv(String filename){
	Map<Id<SignalSystem>,LinkConversionsData> cdata = new HashMap<>();
	FileReader fr;
	BufferedReader br;
	try {
		fr = new FileReader(new File (filename));
		br = new BufferedReader(fr);
		
		String line = null;
		while ((line = br.readLine()) != null) {
			
			 String[] result = line.split(";");
			 Id<SignalSystem> ssid = Id.create(result[0], SignalSystem.class);
			 LinkConversionsData lcd;
			 if (cdata.get(ssid)==null)
			 	{lcd = new LinkConversionsData(ssid);
			 	 cdata.put(ssid, lcd);}
			 cdata.get(ssid).setConv(Id.create(result[1], Link.class), Id.create(result[2], Link.class));
			 
		}
		br.close();
		
		System.out.println(cdata.get(Id.create("17", Link.class)));
		
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
		
		
	
	
	return cdata;
	
	
}
	
}


/* *********************************************************************** *
 * project: org.matsim.*
 * MainDensityAnalysis.java
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

package playground.wrashid.nan;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.network.NetworkImpl;

import playground.wrashid.lib.GeneralLib;

public class MainDensityAnalysis {

	public static void main(String[] args) {
		//Controler controler = new Controler(args);
		
		//controler.
		
		//controler.run();
		
		NetworkImpl network = GeneralLib.readNetwork("C:/data/workspace/playgrounds/wrashid/test/scenarios/chessboard/network.xml");
		
		
		
		
		DensityInfoCollector dic=new DensityInfoCollector();
		//VolumesAnalyzer va=new VolumesAnalyzer(3600, 100000, network);
		
		EventsManagerImpl events = new EventsManagerImpl();
		events.addHandler(dic);
		
		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		String eventsFile="C:/data/workspace/playgrounds/wrashid/test/output/playground/wrashid/PSF2/chargingSchemes/dumbCharging/ITERS/it.0/0.events.txt";
		reader.readFile(eventsFile);
		
		
		
		
		
//		int[] volumesForLink = va.getVolumesForLink(new IdImpl("5"));
//		
//		for (int i=0;i<volumesForLink.length;i++){
//			System.out.println(i +" (hour):" + volumesForLink[i]);
//		}
		
		
	}
	
}

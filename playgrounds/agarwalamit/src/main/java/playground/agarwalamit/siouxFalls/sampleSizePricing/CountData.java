/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.siouxFalls.sampleSizePricing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.linkVolume.LinkVolumeHandler;
import playground.agarwalamit.utils.MapUtils;
import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * @author amit
 */
public class CountData extends AbstractAnalysisModule {

	private LinkVolumeHandler lvh;
	private SortedMap<Id<Link>, Double> linkId2Vol;
	private Map<Id<Link>, Map<Integer, Double>> linkId2TimeSlot2Vol;
	private final double countScaleFactor;

	private final String [] linkIds = {
			"8667___secondary","8668___secondary","4032___secondary","4034___secondary","2842___secondary","3342___secondary","10599___secondary",
			"11167___secondary","1585___secondary","3863___secondary","8363___secondary","11380___secondary","13352___secondary","9723___secondary",
			"13685___motorway","13696___motorway","1685___motorway","1684___motorway","7037___motorway","10353___motorway","10930___motorway","12772___motorway","6941___motorway",
			"724___primary","10913___primary","3595___primary","343___primary","2807___primary","6220___primary","2807___primary","7245___primary","13303___primary",
			"7021___primary","3598___primary","346___primary",
			"328___tertiary","11771___tertiary","2875___tertiary","9836___tertiary","12202___tertiary","11262___tertiary","4513___tertiary","7167___tertiary","1368___tertiary",
			"4452___unclassified","700___unclassified","10717___unclassified","5747___unclassified",
			"6796___trunk","931___trunk","4157___trunk"
	};

	public CountData(double countScaleFactor) {
		super(CountData.class.getSimpleName());
		this.countScaleFactor = countScaleFactor;
	}

	public static void main(String[] args) {
		double [] samplePopulation = { 0.01, 0.02, 0.03, 0.04, 0.05, 0.1, 0.15, 0.2, 0.3, 0.4,0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
		for(double d : samplePopulation){
			String outputFolder = "/Users/aagarwal/Desktop/ils4/agarwal/flowCapTest/f/f"+d+"/";
			double csf = 1/d;
			CountData cd = new CountData(csf);
			cd.run( outputFolder);
		}
	}

	private void run (String outputFolder){
		init();
		preProcessData();

		EventsManager manager = EventsUtils.createEventsManager();
		for(EventHandler eh :getEventHandler()){
			manager.addHandler(eh);
		}

		int lastIteration = 500;//sc.getConfig().controler().getLastIteration();
		MatsimEventsReader reader = new MatsimEventsReader(manager);
		reader.readFile(outputFolder+"/ITERS/it."+lastIteration+"/"+lastIteration+".events.xml.gz");
		postProcessData();
		String outFolder = outputFolder+"/analysis/";
		new File(outFolder).mkdir();
		writeResults(outFolder+"/counts.txt");
	}

	public void init(){
		this.lvh = new LinkVolumeHandler();
		this.lvh.reset(0);
		this.linkId2Vol = new TreeMap<>();
		this.linkId2TimeSlot2Vol = new HashMap<>();
		for(String str :this.linkIds){
			Id<Link> id = Id.create(str, Link.class);
			this.linkId2Vol.put(id, 0.0);
		}
	}


	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> eh = new LinkedList<>();
		eh.add(this.lvh);
		return eh ;
	}

	@Override
	public void preProcessData() {

	}

	@Override
	public void postProcessData() {
		this.linkId2TimeSlot2Vol = this.lvh.getLinkId2TimeSlot2LinkCount();
		getDesiredLinkVolumes();
	}

	@Override
	public void writeResults(String outputFile) {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try {
			writer.write("linkId \t vol \n");
			for(Id<Link> id:this.linkId2Vol.keySet()){
				writer.write(id.toString()+"\t"+this.linkId2Vol.get(id)+"\n");
			}
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written to file "+outputFile+". Reason - "+e);
		}
	}

	private void getDesiredLinkVolumes (){
		for(Id<Link> id :this.linkId2Vol.keySet()){
			if(this.linkId2TimeSlot2Vol.containsKey(id)) this.linkId2Vol.put(id, this.countScaleFactor*MapUtils.doubleValueSum(this.linkId2TimeSlot2Vol.get(id)));
			else this.linkId2Vol.put(id, 0.0);
		}
	}
}

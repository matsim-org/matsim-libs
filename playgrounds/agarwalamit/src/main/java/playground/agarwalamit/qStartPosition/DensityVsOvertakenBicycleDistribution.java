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
package playground.agarwalamit.qStartPosition;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.vsp.analysis.modules.AbstractAnalyisModule;

/**
 * @author amit
 */
public class DensityVsOvertakenBicycleDistribution extends AbstractAnalyisModule {

	private DensityVsPassingDistributionHandler dpd;
	private String eventFile;

	public DensityVsOvertakenBicycleDistribution(String eventFile) {
		super(DensityVsOvertakenBicycleDistribution.class.getSimpleName());
		Id linkId = new IdImpl("1");
		this.dpd = new DensityVsPassingDistributionHandler(linkId);
		this.eventFile = eventFile;
	}

	public static void main(String[] args) {
				String outputDir ="../../patnaIndiaSim/outputSS/2modes/prob5050P40/";
				String eventFile = outputDir+"/events.xml";
//		String outputDir = "./output/";
//		String eventFile =outputDir+"/events2000.xml";
		DensityVsOvertakenBicycleDistribution dobd = new DensityVsOvertakenBicycleDistribution(eventFile);
		dobd.preProcessData();
		dobd.writeResults(outputDir);
	}

	@Override
	public List<EventHandler> getEventHandler() {
		return null;
	}

	@Override
	public void preProcessData() {
		EventsManager manager = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(manager);
		manager.addHandler(this.dpd);
		reader.readFile(this.eventFile);
	}

	@Override
	public void postProcessData() {

	}

	@Override
	public void writeResults(String outputFolder) {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"rDensityVsAvgOvertakenBicycleCount.txt");
		try {
			writer.write("density \t numberOfBicyclesOvertaken \n");
			for(Entry<Double, Double> e:this.dpd.getDensity2OvertakenBicycleCount().entrySet()){
				writer.write(e.getKey()+"\t"+e.getValue()+"\n");
			}
			writer.close();

		} catch (IOException e) {
			throw new RuntimeException("Data is not written in file. Reason : "+e);
		}
	}

}

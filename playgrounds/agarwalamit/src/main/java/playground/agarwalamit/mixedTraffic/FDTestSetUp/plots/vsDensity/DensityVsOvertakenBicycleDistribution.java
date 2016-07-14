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
package playground.agarwalamit.mixedTraffic.FDTestSetUp.plots.vsDensity;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * @author amit
 */
public class DensityVsOvertakenBicycleDistribution extends AbstractAnalysisModule {

	private DensityVsPassingDistributionHandler dpd;
	private DensityVsFractionOfStoppedVehiclesHandler dfsv;
	private String eventFile;
	BufferedWriter writer1;

	public DensityVsOvertakenBicycleDistribution(String eventFile, String outputFolder) {
		super(DensityVsOvertakenBicycleDistribution.class.getSimpleName());
		Id<Link> linkId = Id.create("1",Link.class);
		writer1 = IOUtils.getBufferedWriter(outputFolder+"rDensityVsTotalOvertakenBicycleCount_2.txt");
		this.dpd = new DensityVsPassingDistributionHandler(linkId, writer1);
		this.dfsv = new DensityVsFractionOfStoppedVehiclesHandler(linkId, 1000);
		this.eventFile = eventFile;
	}

	public static void main(String[] args) {
//		String outputDir ="../../patnaIndiaSim/outputSS/3links1KmNoStuckEqualModalSplit/";
//		String eventFile = outputDir+"events.xml";
				String outputDir = "./outputPassinRate_LinkEnterDensity/1c4B/";
				String eventFile =outputDir+"/events.xml";
		DensityVsOvertakenBicycleDistribution dobd = new DensityVsOvertakenBicycleDistribution(eventFile, outputDir);
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
//		manager.addHandler(this.dfsv);
		reader.readFile(this.eventFile);
	}

	@Override
	public void postProcessData() {

	}

	@Override
	public void writeResults(String outputFolder) {
//		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"rDensityVsAvgOvertakenBicycleCount.txt");
//		try {
//			writer.write("density \t averageNumberOfBicyclesOvertaken \n");
//			for(Entry<Double, Double> e:this.dpd.getDensity2AverageOvertakenBicycleCount().entrySet()){
//				writer.write(e.getKey()+"\t"+e.getValue()+"\n");
//			}
//			writer.close();
//
//		} catch (IOException e) {
//			throw new RuntimeException("Data is not written in file. Reason : "+e);
//		}

		BufferedWriter writer3 = IOUtils.getBufferedWriter(outputFolder+"rDensityVsTotalOvertakenBicycleCount.txt");
		try {
			writer3.write("density \t numberOfBicyclesOvertaken \n");
			for(Entry<Double, Double> e:this.dpd.getDensity2TotalOvertakenBicycleCount().entrySet()){
				writer3.write(e.getKey()+"\t"+e.getValue()+"\n");
			}
			writer1.close();
			writer3.close();

		} catch (IOException e) {
			throw new RuntimeException("Data is not written in file. Reason : "+e);
		}
		
		
//		BufferedWriter writer2 = IOUtils.getBufferedWriter(outputFolder+"rDensityVsFractionOfStoppedVehicles.txt");
//		try {
//			writer2.write("density \t fractionOfStoppedVehicles \n");
//			for(Entry<Double, Double> e:this.dfsv.getDensityVsFractionOfStoppedVehicles().entrySet()){
//				writer2.write(e.getKey()+"\t"+e.getValue()+"\n");
//			}
//			writer2.close();
//
//		} catch (IOException e) {
//			throw new RuntimeException("Data is not written in file. Reason : "+e);
//		}
	}

}

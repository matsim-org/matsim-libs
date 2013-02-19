/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.mmoyo.analysis.stopZoneOccupancyAnalysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.pt.counts.OccupancyAnalyzer;
import org.matsim.pt.counts.PtCountSimComparisonKMLWriter;

public class KMZPtCountSimComparisonWriter {
	private static final Logger log = Logger.getLogger(KMZPtCountSimComparisonWriter.class);
	private Counts occupCounts = new Counts();
	private final Controler controler;
	private Network net;
	private double scalefactor;
	final String kmzFile = "configurablePTcountsCompare.kmz";
	final String txtCompFile = "configurablePTcountsCompare.txt";
	
	public KMZPtCountSimComparisonWriter (final Controler controler){
		this.controler = controler;
		
		//load data
		net = controler.getScenario().getNetwork();
		new MatsimCountsReader(occupCounts).readFile(controler.getConfig().ptCounts().getOccupancyCountsFileName());
		scalefactor = controler.getConfig().ptCounts().getCountsScaleFactor();
	}
	
	protected void write (final OccupancyAnalyzer ocupAnalizer,  final int itNum){
		
		PtCountComparisonAlgorithm4confTimeBinSize ccaOccupancy = new PtCountComparisonAlgorithm4confTimeBinSize (ocupAnalizer, occupCounts, net, scalefactor);
		ccaOccupancy.calculateComparison(); 
		
		//set and use kml writter
		Config config = controler.getConfig();
		PtCountSimComparisonKMLWriter kmlWriter = new PtCountSimComparisonKMLWriter(ccaOccupancy.getComparison(),
				ccaOccupancy.getComparison(), ccaOccupancy.getComparison(), TransformationFactory.getCoordinateTransformation(config
						.global().getCoordinateSystem(), TransformationFactory.WGS84), occupCounts, occupCounts,
						occupCounts);
		kmlWriter.setIterationNumber(itNum);

		//write counts comparison
		String kmlFile;
		String ocuppCompTxtFile;
		if(controler.getControlerIO()!=null){
			kmlFile = controler.getControlerIO().getIterationFilename(itNum, kmzFile);
			ocuppCompTxtFile = controler.getControlerIO().getIterationFilename(itNum, txtCompFile);
		}else{  //<-it happens when this method is invoked outside a simulation run
			String outDir = controler.getConfig().controler().getOutputDirectory() + "ITERS/it." + itNum + "/." + itNum + ".";
			kmlFile = outDir + kmzFile;
			ocuppCompTxtFile =  outDir + txtCompFile;
		}
		kmlWriter.writeFile(kmlFile);
		ccaOccupancy.write(ocuppCompTxtFile);
		log.info(kmlFile + " done.");
	}
	
}

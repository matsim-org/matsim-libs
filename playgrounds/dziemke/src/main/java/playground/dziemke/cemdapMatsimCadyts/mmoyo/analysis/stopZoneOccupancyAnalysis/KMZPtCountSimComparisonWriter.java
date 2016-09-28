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

package playground.dziemke.cemdapMatsimCadyts.mmoyo.analysis.stopZoneOccupancyAnalysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.algorithms.CountSimComparisonKMLWriter;
import org.matsim.pt.counts.OccupancyAnalyzer;

import playground.dziemke.cemdapMatsimCadyts.mmoyo.utils.KMZ_Extractor;


public final class KMZPtCountSimComparisonWriter {
	private static final Logger log = Logger.getLogger(KMZPtCountSimComparisonWriter.class);
	private Counts occupCounts = new Counts();
	private final Controler controler;
	private Network net;
	private double scalefactor;
	final String kmzFile = "configurablePTcountsCompare.kmz";
	final String txtCompFile = "configurablePTcountsCompare.txt";
	final String ITERS = "ITERS/it.";
	final String SL = "/.";
	final String PNT = ".";
	final String strDONE = " done.";
	final String S = "/";
	final String STR_ERRPLOT = "errorGraphErrorBiasOccupancy.png";
	final String STR_HOUROCCUPPLOT = "ptCountsOccup1.png";

	public KMZPtCountSimComparisonWriter (final Controler controler){
		this.controler = controler;

		//load data
		net = controler.getScenario().getNetwork();
		MatsimCountsReader reader = new MatsimCountsReader(occupCounts);
		reader.readFile(controler.getConfig().ptCounts().getOccupancyCountsFileName());

//		scalefactor = controler.getConfig().ptCounts().getCountsScaleFactor();
		scalefactor = 1. ;
		// yyyyyy otherwise it comes out way too large, but I don't know how it is put together. kai, sep'16

		reader= null; //M
	}


	 void write (final OccupancyAnalyzer ocupAnalizer,  final int itNum, final boolean stopZoneConversion){

		PtCountComparisonAlgorithm4confTimeBinSize ccaOccupancy = new PtCountComparisonAlgorithm4confTimeBinSize (ocupAnalizer, occupCounts, net, scalefactor);
		ccaOccupancy.calculateComparison();

		//set and use kml writter
		Config config = controler.getConfig();
		final CoordinateTransformation coordTransform = TransformationFactory.getCoordinateTransformation(config
		.global().getCoordinateSystem(), TransformationFactory.WGS84);
//		PtCountSimComparisonKMLWriter kmlWriterOrig = new PtCountSimComparisonKMLWriter(ccaOccupancy.getComparison(),
//				ccaOccupancy.getComparison(), ccaOccupancy.getComparison(), coordTransform, occupCounts, occupCounts,
//				occupCounts);
		
		CountSimComparisonKMLWriter kmlWriter = new CountSimComparisonKMLWriter(ccaOccupancy.getComparison(), this.occupCounts, coordTransform, "ptCountsOccup") ;

		
		kmlWriter.setIterationNumber(itNum);

		//write counts comparison
		String kmlFile;
		String ocuppCompTxtFile;
		String outDir;
		if(controler.getControlerIO()!=null){
			kmlFile = controler.getControlerIO().getIterationFilename(itNum, kmzFile);
			ocuppCompTxtFile = controler.getControlerIO().getIterationFilename(itNum, txtCompFile);
			outDir = controler.getControlerIO().getIterationPath(itNum) + S;
		}else{  //<-it happens when this method is invoked outside a simulation run
			outDir = controler.getConfig().controler().getOutputDirectory() + ITERS + itNum + SL + itNum + PNT;
			kmlFile = outDir + kmzFile;
			ocuppCompTxtFile =  outDir + txtCompFile;
		}
		kmlWriter.writeFile(kmlFile);
		ccaOccupancy.write(ocuppCompTxtFile);

		////extract the specific plot in the iteration folder
		String plotName=  (stopZoneConversion) ?  STR_HOUROCCUPPLOT: STR_ERRPLOT ;
		KMZ_Extractor extractor = new KMZ_Extractor(kmlFile,outDir);
		extractor.extractFile(plotName);
		log.info(kmlFile + strDONE);

		plotName= null;
		extractor= null;
		outDir = null;
		kmlFile = null;
		ccaOccupancy= null;
		kmlWriter = null;
	}

}

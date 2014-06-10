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
package playground.agarwalamit.siouxFalls.emissionAnalyzer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.utils.io.IOUtils;

import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsAnalyzer;

/**
 * @author amit
 */
	public class AbsoluteAndChangeInEmissions {

	private static String clusterPathDesktop = "/Users/aagarwal/Desktop/ils4/agarwal/siouxFalls/outputMCRCOff/";
	
	public static void main(String[] args) {

		BufferedWriter writer ;
		String eventFileLocation = "/ITERS/it.100/100.emission.events.xml.gz";
		
		
		
		SortedMap<String, Double> emissions1 = calculateTotalEmissions(clusterPathDesktop+"/run117/"+eventFileLocation);
		SortedMap<String, Double> emissions2 = calculateTotalEmissions(clusterPathDesktop+"/run118/"+eventFileLocation);
		SortedMap<String, Double> emissions3 = calculateTotalEmissions(clusterPathDesktop+"/run119/"+eventFileLocation);
		SortedMap<String, Double> emissions4 = calculateTotalEmissions(clusterPathDesktop+"/run120/"+eventFileLocation);

		String [] pollutants =  emissions1.keySet().toArray(new String [0]);

		//double [][] absoluteEmissions = new double [4][pollutants.length];
		//double[] [] relativeChangeInEmissions = new double [3][pollutants.length];
		
		
		double [] r1r2 = new double [pollutants.length];
		double [] r1r3 = new double [pollutants.length];
		double [] r1r4 = new double [pollutants.length];
		
		double [] r1 = new double [pollutants.length];
		double [] r2 = new double [pollutants.length];
		double [] r3 = new double [pollutants.length];
		double [] r4 = new double [pollutants.length];

		try {
			writer = IOUtils.getBufferedWriter(clusterPathDesktop+"/analysis/r/rAbsoluteEmissions.txt");
			writer.write("pollutants"+"\t"+"baseCase"+"\t"+"onlyEmissions"+"\t"+"onlyCongestion"+"\t"+"both"+"\n");

			for(Entry<String, Double> e : emissions1.entrySet()){
				writer.write(e.getKey()+"\t"+e.getValue()+"\t"+
						emissions2.get(e.getKey())+"\t"+
						emissions3.get(e.getKey())+"\t"+
						emissions4.get(e.getKey())+
						"\n");
			}
			writer.close();
			
			writer = IOUtils.getBufferedWriter(clusterPathDesktop+"/analysis/r/rChangeInEmissions.txt");
			writer.write("pollutants"+"\t"+"onlyEmissions"+"\t"+"onlyCongestion"+"\t"+"both"+"\n");
			//for(int j=0;j<relativeChangeInEmissions.length;j++){
			for(int i=0;i<pollutants.length;i++){
				r1r2[i] = ((emissions2.get(pollutants[i]) - emissions1.get(pollutants[i]))*100)/(emissions1.get(pollutants[i])) ;
				r1r3[i] = ((emissions3.get(pollutants[i]) - emissions1.get(pollutants[i]))*100)/(emissions1.get(pollutants[i])) ;
				r1r4[i] = ((emissions4.get(pollutants[i]) - emissions1.get(pollutants[i]))*100)/(emissions1.get(pollutants[i])) ;
			
//				double scalingParameterOfPollutant4Graph = scaledEmissionsForGivenPollutants(pollutants[i]);
//				r1[i] = emissions1.get(pollutants[i])*scalingParameterOfPollutant4Graph;
//				r2[i] = emissions2.get(pollutants[i])*scalingParameterOfPollutant4Graph;
//				r3[i] = emissions3.get(pollutants[i])*scalingParameterOfPollutant4Graph;
//				r4[i] = emissions4.get(pollutants[i])*scalingParameterOfPollutant4Graph;
				
				writer.write(pollutants[i]+"\t"+r1r2[i]+"\t"+r1r3[i]+"\t"+r1r4[i]+"\n");
			}
			//}
			writer.close();
		} catch (IOException e1) {
			throw new RuntimeException("Data is not written in file. Reason : "+e1);
		}
		
//		String xSeries [] = {"Only Emission","Only Congestion","Both"};
//		String[] xSeries2 = new String [] {"baseCase", "Only Emission","Only Congestion","Both"};
//		double[] [] relativeChangeInEmissions = { r1r2, r1r3, r1r4 };
//		double [][] absoluteEmissions = {r1,r2,r3,r4};
//		
//		for(int i=0;i<pollutants.length;i++){
//			DecimalFormat formatter = new DecimalFormat("0.#E0");
//			String scalingParameterOfPollutant4Graph = String.valueOf(formatter.format(1/scaledEmissionsForGivenPollutants(pollutants[i])));
//			pollutants[i] = pollutants[i] +" in g x "+ scalingParameterOfPollutant4Graph;
//		}
//		
		
//		getStackedBarChart(pollutants, relativeChangeInEmissions, xSeries, "% reduction in emission levels", "Internalization", "% reduction w.r.t. base case","relativeChangeInEmissions.png");
//		getStackedBarChart(pollutants, absoluteEmissions, xSeries2,  "Absolute emissions", "Emission runs","absolute Value Of Emissions", "absoluteEmissions.png");

	}

//	private static void getStackedBarChart(String[] stackedLegend,
//											double[] [] ySeries, 
//											String[] xSeries, 
//											String title,
//											String xAxisLabel,
//											String yAxisLabel,
//											String fileName) {
//		ConstructStackedBarChart barChart = new ConstructStackedBarChart();
//
//		int rows = ySeries.length;
//		int columns = ySeries[0].length;
//		double [] [] ySeriesTranspose = new double [columns] [rows];
//
//		for(int j =0; j < rows; j++){
//			for(int k =0; k<columns;k++) {
//				ySeriesTranspose[k][j]=ySeries[j][k];
//			}
//		}
//
//		CategoryDataset dataset = barChart.createDataSet(ySeriesTranspose, stackedLegend, xSeries);
//		barChart.createBarChart(title, xAxisLabel, yAxisLabel, dataset);
//		barChart.saveAsPng("./clusterOutput/"+fileName, 800, 600);
//	}

	private static SortedMap<String,Double> calculateTotalEmissions (String emissionsEventsFile){
		EmissionsAnalyzer analyzer = new EmissionsAnalyzer(emissionsEventsFile);
		analyzer.init(null);
		analyzer.preProcessData();
		analyzer.postProcessData();
		return analyzer.getTotalEmissions();
	}
	
	private static double scaledEmissionsForGivenPollutants (String pollutant){
		
		double scalingParameter=0.0;
		
		if(pollutant.equals(WarmPollutant.CO.toString())) scalingParameter = Math.pow(10,-4);
		else if(pollutant.equals(WarmPollutant.CO2_TOTAL.toString())) scalingParameter = Math.pow(10,-5);
		else if(pollutant.equals(WarmPollutant.FC.toString())) scalingParameter = Math.pow(10,-5);
		else if(pollutant.equals(WarmPollutant.HC.toString())) scalingParameter = Math.pow(10,-3);
		else if(pollutant.equals(WarmPollutant.NMHC.toString())) scalingParameter = Math.pow(10,-3);
		else if(pollutant.equals(WarmPollutant.NO2.toString())) scalingParameter = Math.pow(10,-2);
		else if(pollutant.equals(WarmPollutant.NOX.toString())) scalingParameter =Math.pow(10,-3);
		else if(pollutant.equals(WarmPollutant.PM.toString())) scalingParameter = Math.pow(10,-1);
		else if(pollutant.equals(WarmPollutant.SO2.toString())) scalingParameter = Math.pow(10,0);
		else {
			throw new RuntimeException("Unrecognised Pollutant Type.");
		}
		
		return scalingParameter;
	}

}

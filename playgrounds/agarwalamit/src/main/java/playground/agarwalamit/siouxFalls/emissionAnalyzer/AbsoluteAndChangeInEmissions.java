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

	private static String clusterPathDesktop = "/Users/aagarwal/Desktop/ils4/agarwal/siouxFalls/10Pct/";

	public static void main(String[] args) {

		BufferedWriter writer ;
		String eventFileLocation = "/ITERS/it.500/500.emission.events.xml.gz";



		SortedMap<String, Double> emissions1 = calculateTotalEmissions(clusterPathDesktop+"/BAU/"+eventFileLocation);
		SortedMap<String, Double> emissions2 = calculateTotalEmissions(clusterPathDesktop+"/EI/"+eventFileLocation);
		SortedMap<String, Double> emissions3 = calculateTotalEmissions(clusterPathDesktop+"/CI/"+eventFileLocation);
		//		SortedMap<String, Double> emissions4 = calculateTotalEmissions(clusterPathDesktop+"/run204/"+eventFileLocation);

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
						//						emissions4.get(e.getKey())+
						"\n");
			}
			writer.close();

			writer = IOUtils.getBufferedWriter(clusterPathDesktop+"/analysis/r/rChangeInEmissions.txt");
			writer.write("pollutants"+"\t"+"onlyEmissions"+"\t"+"onlyCongestion"+"\t"+"both"+"\n");
			//for(int j=0;j<relativeChangeInEmissions.length;j++){
			for(int i=0;i<pollutants.length;i++){
				r1r2[i] = ((emissions2.get(pollutants[i]) - emissions1.get(pollutants[i]))*100)/(emissions1.get(pollutants[i])) ;
				r1r3[i] = ((emissions3.get(pollutants[i]) - emissions1.get(pollutants[i]))*100)/(emissions1.get(pollutants[i])) ;
				//				r1r4[i] = ((emissions4.get(pollutants[i]) - emissions1.get(pollutants[i]))*100)/(emissions1.get(pollutants[i])) ;

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
	}


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

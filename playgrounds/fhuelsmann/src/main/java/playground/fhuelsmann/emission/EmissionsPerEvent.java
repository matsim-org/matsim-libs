/* *********************************************************************** *
 * project: org.matsim.*
 * FhEmissions.java
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
 *                                                                         
 * *********************************************************************** */
package playground.fhuelsmann.emission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import playground.fhuelsmann.emission.objects.HbefaObject;

public class EmissionsPerEvent {

	public Map<String, Double> calculateDetailedEmissions(Map<String, double[][]> hashOfPollutant, double travelTime, double linkLength){
		Map<String, Double> emissionsOfEvent = new HashMap<String, Double>();
		
		for( Entry<String, double[][]> entry : hashOfPollutant.entrySet() ){

			String pollutant = entry.getKey();
			double li = linkLength;
			double vij = (linkLength / 1000) / (travelTime / 3600);

			double vf = entry.getValue()[0][0]; // freeFlow
			double vc = entry.getValue()[3][0]; // Stop&Go

			double EFf = entry.getValue()[0][1];
			double EFc = entry.getValue()[3][1];

			double freeFlowFraction = 0.0;
			double stopGoFraction = 0.0;
			double stopGoTime = 0.0;

			if (vij < vc){
				double generatedEmissions = li / 1000 * EFc;
				emissionsOfEvent.put(pollutant, generatedEmissions);
//				arrayOfEmissions[getIndexOfPollutant(entry.getKey().toString())] = generatedEmissions;
			}
			else {
				stopGoTime= (li / 1000) / vij - (li / 1000) / vf;

				stopGoFraction = vc * stopGoTime;
				freeFlowFraction = (li / 1000) - stopGoFraction;
				double generatedEmissions = stopGoFraction * EFc + freeFlowFraction * EFf;
				emissionsOfEvent.put(pollutant, generatedEmissions);
//				arrayOfEmissions[getIndexOfPollutant(entry.getKey().toString())] = generatedEmissions;
			}
		}
		return emissionsOfEvent;
	}

	// Emission calculation based on stop&go and free flow fractions
	public static double [] emissionFreeFlowFractionCalculate(double vij,double li,
			double EFc,double EFf,double vc, double vf, double noxf, double noxc, 
			double co2rf,double co2rc,double co2tf, double co2tc,double no2f,double no2c,
			double pmf,double pmc /*,int freeVelocity*/){

		double stopGoVel = vc;
		//	in visumnetzlink1.txt the freeVelocity is 60.00 km/h;  average free flow speed in HBEFA = 57.1577 km/h which is taken here

		double freeFlowFraction = 0.0;
		double stopGoFraction = 0.0;
		double stopGoTime = 0.0;
		double mKrFraction = 0.0;
		double noxFractions = 0.0;
		double co2rFractions = 0.0;
		double co2tFractions = 0.0;
		double no2Fractions = 0.0;
		double pmFractions = 0.0;

		if (vij<stopGoVel){
			mKrFraction = li / 1000 * EFc;
			noxFractions=  li / 1000 * noxc;
			co2rFractions=  li / 1000 * co2rc;
			co2tFractions=  li / 1000 * co2tc;
			no2Fractions=  li / 1000 * no2c;
			pmFractions=  li / 1000 * pmc;
		}
		else {
			stopGoTime = (li / 1000) / vij -(li / 1000) / vf;

			stopGoFraction = stopGoVel * stopGoTime;
			freeFlowFraction = (li / 1000) - stopGoFraction;

			mKrFraction = stopGoFraction * EFc + freeFlowFraction * EFf;
			noxFractions =  stopGoFraction * noxc + freeFlowFraction * noxf;
			co2rFractions =  stopGoFraction * co2rc + freeFlowFraction * co2rf;
			co2tFractions =  stopGoFraction * co2tc + freeFlowFraction * co2tf;
			no2Fractions =  stopGoFraction * no2c + freeFlowFraction * no2f;
			pmFractions =  stopGoFraction * pmc + freeFlowFraction * pmf;
		}

		double[] fraction = new double[6];
		fraction[0] = mKrFraction;
		fraction[1] = noxFractions;
		fraction[2] = co2rFractions;
		fraction[3] = co2tFractions;
		fraction[4] = no2Fractions;
		fraction[5] = pmFractions;
		return fraction;
	}

	public double[] calculateAverageEmissions(ArrayList<String> listOfPollutant, int Hbefa_road_type, double travelTime, double linkLength, HbefaObject[][] HbefaTable) {
		int NumberOfPollutant = listOfPollutant.size();
		double[] outPutFraction= new double[NumberOfPollutant];

		if (Hbefa_road_type == 0){
			outPutFraction[0] = 0; //  mKrBasedOnFractions
			outPutFraction[1] = 0; //noxEmissionsBasedOnFractions
			//	outPutFraction[7]  =0; //co2repEmissionsBasedOnFractions
			outPutFraction[2] = 0; //co2EmissionsBasedOnFractions
			outPutFraction[3] = 0; //no2EmissionsBasedOnFractions
			outPutFraction[4] = 0; //pmEmissionsBasedOnFractions
		}
		else {	
			double vf = HbefaTable[Hbefa_road_type][0].getVelocity();
			double EFf = HbefaTable[Hbefa_road_type][0].getMkr();
			double noxf = HbefaTable[Hbefa_road_type][0].getEmissionFactorNox();
			double co2rf = HbefaTable[Hbefa_road_type][0].getEmissionFactorCo2Rep();
			double co2tf = HbefaTable[Hbefa_road_type][0].getEmissionFactorCo2Total();
			double no2f = HbefaTable[Hbefa_road_type][0].getNo2();
			double pmf = HbefaTable[Hbefa_road_type][0].getPm();

			double vc =HbefaTable[Hbefa_road_type][3].getVelocity();
			double EFc = HbefaTable[Hbefa_road_type][3].getMkr();
			double noxc = HbefaTable[Hbefa_road_type][3].getEmissionFactorNox();
			double co2rc = HbefaTable[Hbefa_road_type][3].getEmissionFactorCo2Rep();
			double co2tc = HbefaTable[Hbefa_road_type][3].getEmissionFactorCo2Total();
			double no2c = HbefaTable[Hbefa_road_type][3].getNo2();
			double pmc = HbefaTable[Hbefa_road_type][3].getPm();

			double li = linkLength;
			double vij = (linkLength / 1000) / (travelTime / 3600);

			//call of function double output
			double [] fractions =  emissionFreeFlowFractionCalculate(vij, li,
					EFc, EFf, vc,/*freeVelocity,*/vf, noxf, noxc, co2rf, co2rc, co2tf, co2tc, no2f, no2c, pmf, pmc);

			outPutFraction[0] =fractions[0]; // fcBasedOnFractions
			outPutFraction[1]  =fractions [1]; //noxEmissionsBasedOnFractions
			//			outPut2[]  =fractions [2]; //co2repEmissionsBasedOnFractions
			outPutFraction[2] = fractions [3]; //co2EmissionsBasedOnFractions
			outPutFraction[3] = fractions [4]; //no2EmissionsBasedOnFractions
			outPutFraction[4] = fractions [5]; //pmEmissionsBasedOnFractions
		}
		return outPutFraction;
	}

	private int getIndexOfPollutant(String pollutant){
		if (pollutant.equals("FC")) return 0;
		else if (pollutant.equals("NOx")) return 1;
		else if (pollutant.equals("CO2(total)")) return 2;
		else if (pollutant.equals("NO2")) return 3;
		else return 4;
	}
}
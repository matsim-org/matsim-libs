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

import java.util.Map;
import java.util.Map.Entry;

import playground.fhuelsmann.emission.objects.HbefaObject;

public class EmissionsPerEvent {
	
	/** The Order :
	 * listOfPollutant.add("Benzene");
		listOfPollutant.add("CH4");
		listOfPollutant.add("CO");
		listOfPollutant.add("CO(rep.)");
		listOfPollutant.add("CO2(total)");
		listOfPollutant.add("FC");
		listOfPollutant.add("HC");
		listOfPollutant.add("N2O");
		listOfPollutant.add("NH3");
		listOfPollutant.add("NMHC");
		listOfPollutant.add("NO2");
		listOfPollutant.add("NOX");
		listOfPollutant.add("Pb");
		listOfPollutant.add("PM");
		listOfPollutant.add("PN");
		listOfPollutant.add("SO2");
		*/
	
	public double [] emissionFactorCalculate(Map<String, double[][]> hashOfPollutant,double averageSpeed, double distance){
			
		int NumberOfPollutant = hashOfPollutant.size();
		// the result will be returned after calculating
		
		double[] arrayOfEmissions = new double[NumberOfPollutant];
		// for every Pollutant in the Order of the List in EmissionTool
		int indexOfPollutant=0;
		
		for( Entry<String, double[][]> Pollutant : hashOfPollutant.entrySet() ){
		
			double li=distance;
			double vij = averageSpeed;

			double vf=	Pollutant.getValue()[0][0]; // freeFlow
			double vh=  Pollutant.getValue()[1][0]; // heavy
			double vs= 	Pollutant.getValue()[2][0];
			double vc=  Pollutant.getValue()[3][0];//Stop And Go
			
			double EFf = Pollutant.getValue()[0][1];
			double EFh = Pollutant.getValue()[1][1];
			double EFs = Pollutant.getValue()[2][1];
			double EFc = Pollutant.getValue()[3][1];
		
			
			
			if (vh <= vij && vij<=vf){
				
				double a = vf - vij;
				double b = vij-vh;
				double tempResult = (a *EFh ) / (a+b) + (b * EFf ) / (a+b);
				arrayOfEmissions[indexOfPollutant]=tempResult*(li/1000);
				
				System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~average speed "+vij+"\n arrayOfEmissions"+arrayOfEmissions[0]+"\n distanz"+li);
		
			}else if (vs <= vij && vij<=vh){
				double a = vh - vij;
				double b = vij-vs;
				
				double tempResult = (a *EFs ) / (a+b) + (b * EFh ) / (a+b);
				arrayOfEmissions[indexOfPollutant]=tempResult*(li/1000);
			
				}


			if (vc <= vij && vij<=vs){
				double a = vs - vij;
				double b = vij-vc;
			
				double tempResult = (a *EFc ) / (a+b) + (b * EFs ) / (a+b);
				arrayOfEmissions[indexOfPollutant]=tempResult*(li/1000);
				}
			if (vij > vf){
				
				double tempResult = EFf;
				arrayOfEmissions[indexOfPollutant]=tempResult*(li/1000);
				}
			
			else if(vij<vc){
				double tempResult =EFc; 
				arrayOfEmissions[indexOfPollutant]=tempResult*(li/1000);
				}
			}// For loop 
		
		return  arrayOfEmissions;
		
	}

	public static double [] emissionFreeFlowFractionCalculate(double vij,double li,
			double EFc,double EFf,double vc, double vf, double noxf, double noxc, 
			double co2rf,double co2rc,double co2tf, double co2tc,double no2f,double no2c,
			double pmf,double pmc /*,int freeVelocity*/){

		
		
		double stopGoVel = vc;
		//	in visumnetzlink1.txt the freeVelocity is 60.00 km/h;  average free flow speed in HBEFA = 57.1577 km/h which is taken here

		double freeFlowFraction =0.0;
		double stopGoFraction =0.0;
		double stopGoTime =0.0;
		double mKrFraction =0.0;
		double noxFractions=0.0;
		double co2rFractions=0.0;
		double co2tFractions=0.0;
		double no2Fractions=0.0;
		double pmFractions=0.0;


		if (vij<stopGoVel){
			mKrFraction = li/1000*EFc;
			noxFractions=  li/1000*	noxc;
			co2rFractions=  li/1000*co2rc;
			co2tFractions=  li/1000*co2tc;
			no2Fractions=  li/1000*no2c;
			pmFractions=  li/1000*pmc;

		}

		else {
			stopGoTime= (li/1000)/vij -(li/1000)/vf;  //li/vij -li/freeVelocity;

			stopGoFraction = stopGoVel *stopGoTime;
			freeFlowFraction= (li/1000) - stopGoFraction;

			mKrFraction = stopGoFraction*	EFc + freeFlowFraction*	EFf;
			noxFractions=  stopGoFraction*	noxc + freeFlowFraction*noxf;
			co2rFractions=  stopGoFraction*	co2rc + freeFlowFraction*co2rf;
			co2tFractions=  stopGoFraction*	co2tc + freeFlowFraction*co2tf;
			no2Fractions=  stopGoFraction*	no2c + freeFlowFraction*no2f;
			pmFractions=  stopGoFraction*	pmc + freeFlowFraction*pmf;
		}

		double [] fraction = new double[6];
		fraction[0] =mKrFraction;
		fraction[1] =noxFractions;
		fraction[2]=co2rFractions;
		fraction[3]=co2tFractions;
		fraction[4]=no2Fractions;
		fraction[5]=pmFractions;
		
		return fraction;
	}

	public double[] collectInputForEmission(int Hbefa_road_type, double averageSpeed,double distance,HbefaObject[][] HbefaTable) {
	
		
		double[] outPut = new double[12];
	
		if (Hbefa_road_type ==0){
		
			outPut[0] =  0; //mKrBasedOnAverageSpeed
			//Nox
			outPut[1] = 0; // 	noxEmissionsBasedOnAverageSpeed 
			//CO2 rep -fossiler Anteil
//			outPut[2] =  0; // co2repEmissionsBasedOnAverageSpeed 
			//CO2 Total
			outPut[2] = 0;// co2EmissionsBasedOnAverageSpeed 
			//NO2
			outPut[3] =  0; //no2EmissionsBasedOnAverageSpeed
			//PM
			outPut[4] = 0; //pmEmissionsBasedOnAverageSpeed

			//mKr
			outPut[5] = 0; //  mKrBasedOnFractions
			//NOx
			outPut[6]  =0; //noxEmissionsBasedOnFractions
			//CO2 rep -fossiler Anteil
//			outPut[7]  =0; //co2repEmissionsBasedOnFractions
			//CO2 total
			outPut[7] = 0; //co2EmissionsBasedOnFractions
			//NO2
			outPut[8] = 0; //no2EmissionsBasedOnFractions
			//PM
			outPut[9] = 0; //pmEmissionsBasedOnFractions
			
			
		} else {	
			

			double vf =HbefaTable[Hbefa_road_type][0].getVelocity();
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

			double vij = averageSpeed;

			double li = distance;
			//      int freeVelocity = obj.getfreeVelocity();
		
			//call of function, double output
			double [] emissionFactorAndEmissions=  null;
			//mKr
			outPut[0] =  1.1; //mKrBasedOnAverageSpeed
			//Nox
			outPut[1] =  1.1; // 	noxEmissionsBasedOnAverageSpeed 
			//CO2 rep -fossiler Anteil
//			outPut[2] =  emissionFactorAndEmissions[2]; // co2repEmissionsBasedOnAverageSpeed 
			//CO2 Total
			outPut[2] =  1.1;// co2EmissionsBasedOnAverageSpeed 
			//NO2
			outPut[3] =  1.1; //no2EmissionsBasedOnAverageSpeed
			//PM
			outPut[4] =  1.1; //pmEmissionsBasedOnAverageSpeed

			//call of function double output
			double [] fractions=  emissionFreeFlowFractionCalculate(vij,li,
					EFc, EFf,vc,/*freeVelocity,*/vf, noxf, noxc,co2rf,co2rc,co2tf,co2tc,no2f,no2c,pmf,pmc);
			//mKr
			outPut[5] =fractions[0]; //  mKrBasedOnFractions
			//NOx
			outPut[6]  =fractions [1]; //noxEmissionsBasedOnFractions
			//CO2 rep -fossiler Anteil
//			outPut[8]  =fractions [2]; //co2repEmissionsBasedOnFractions
			//CO2 total
			outPut[7] = fractions [3]; //co2EmissionsBasedOnFractions
			//NO2
			outPut[8] = fractions [4]; //no2EmissionsBasedOnFractions
			//PM
			outPut[9] = fractions [5]; //pmEmissionsBasedOnFractions
			
			
			

		}
		return outPut;
	}



}
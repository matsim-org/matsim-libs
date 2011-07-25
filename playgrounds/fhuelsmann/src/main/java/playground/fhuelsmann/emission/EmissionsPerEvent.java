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
import java.util.Map;
import java.util.Map.Entry;

import playground.fhuelsmann.emission.objects.HbefaObject;

public class EmissionsPerEvent {

	public double [] emissionAvSpeedCalculateDetailed(Map<String, double[][]> hashOfPollutant,double averageSpeed, double distance){

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
			}
			else if (vs <= vij && vij<=vh){
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

	public double [] emissionFractionCalculateDetailed(Map<String, double[][]> hashOfPollutant, double averageSpeed, double distance){

		
		double[] arrayOfEmissions = new double[hashOfPollutant.size()];
		// for every pollutant in the Order of the List in EmissionTool

		for( Entry<String, double[][]> Pollutant : hashOfPollutant.entrySet() ){

			double li = distance;
			double vij = averageSpeed;

			double vf = Pollutant.getValue()[0][0]; // freeFlow
			double vc = Pollutant.getValue()[3][0]; //Stop And Go

			double EFf = Pollutant.getValue()[0][1];
			double EFc = Pollutant.getValue()[3][1];

			double freeFlowFraction = 0.0;
			double stopGoFraction = 0.0;
			double stopGoTime = 0.0;

			if (vij<vc){
				double result = li/1000*EFc;
				arrayOfEmissions[getIndexOfPollutant(Pollutant.getKey().toString())]=result;
			}
			else {
				stopGoTime= (li/1000)/vij -(li/1000)/vf;  //li/vij -li/freeVelocity;

				stopGoFraction = vc *stopGoTime;
				freeFlowFraction = (li/1000) - stopGoFraction;
				double result = stopGoFraction*	EFc + freeFlowFraction*	EFf;
				arrayOfEmissions[getIndexOfPollutant(Pollutant.getKey().toString())]=result;
			}
		}
	//	System.out.println("################fc"+arrayOfEmissions[0]);
	//	System.out.println("################nox"+arrayOfEmissions[1]);
	//	System.out.println("################co2"+arrayOfEmissions[2]);
	//	System.out.println("################no2"+arrayOfEmissions[3]);
	//	System.out.println("################p"+arrayOfEmissions[4]);
		
		return arrayOfEmissions;
	}


	// Emission calculation based on average speed, EFh=Emission Factor heavy; EFf=Emission Factor freeflow; 
	// EFs= Emission Factor saturated; EFc=Emission Factor congested/stop&go; 
	// vh= average speed heavy; vf=average speedfreeflow; 
	// vs= average speed saturated; vc=average speed congested/stop&go;li = link length; vij= calcuated average speed from event file
	public static double [] emissionFactorCalculate(double vh,
			double vij,double vf,double EFh, double li,
			double EFs,double EFc,double EFf,double vs,double vc, double noxf, double noxh, double noxs, double noxc,
			double co2rf,double co2rh,double co2rs,double co2rc, double co2tf,double co2th,double co2ts,double co2tc,
			double no2f,double no2h,double no2s,double no2c,
			double pmf,double pmh,double pms,double pmc){

		double EF=0.0; //emission factor of mKr
		double mkr= 0.0; 
		double nox =0.0;//NOx emission factor
		double noxEmissions=0.0;
		double co2r=0.0;//CO2 rep total emission factor - fossiler Anteil
		double co2rEmissions=0.0;
		double co2t=0.0;//CO2 total emission factor
		double co2tEmissions=0.0;
		double no2=0.0;//NO2 emission factor
		double no2Emissions=0.0;
		double pm=0.0;//PM emission factor
		double pmEmissions=0.0;


		if (vh <= vij && vij<=vf){
			double a = vf - vij;
			double b = vij-vh;
			//mKr
			EF = (a *EFh ) / (a+b) + (b * EFf ) / (a+b);
			mkr=EF*(li/1000);
			//Nox
			nox = (a *noxh ) / (a+b) + (b * noxf ) / (a+b);
			noxEmissions=nox*(li/1000);
			//CO2 rep
			co2r = (a *co2rh ) / (a+b) + (b * co2rf ) / (a+b);
			co2rEmissions=co2r*(li/1000);
			//CO2
			co2t = (a *co2th ) / (a+b) + (b * co2tf ) / (a+b);
			co2tEmissions=co2t*(li/1000);
			//NO2
			no2 = (a *no2h ) / (a+b) + (b * no2f ) / (a+b);
			no2Emissions=no2*(li/1000);
			//PM
			pm= (a *pmh ) / (a+b) + (b * pmf ) / (a+b);
			pmEmissions=pm*(li/1000);


		}
		else if (vs <= vij && vij<=vh){
			double a = vh - vij;
			double b = vij-vs;

			//mKrEF = (a *EFs ) / (a+b) + (b * EFh ) / (a+b);
			mkr=EF*(li/1000);
			//Nox
			nox = (a *noxs ) / (a+b) + (b * noxh ) / (a+b);
			noxEmissions=nox*(li/1000);
			//Co2 rep
			co2r = (a *co2rs ) / (a+b) + (b * co2rh ) / (a+b);
			co2rEmissions=co2r*(li/1000);
			//Co2
			co2t = (a *co2ts ) / (a+b) + (b * co2th ) / (a+b);
			co2tEmissions=co2t*(li/1000);
			//No2
			no2 = (a *no2s ) / (a+b) + (b * no2h ) / (a+b);
			no2Emissions=no2*(li/1000);
			//PM
			pm = (a *pms ) / (a+b) + (b * pmh ) / (a+b);
			pmEmissions=pm*(li/1000);

		}
		if (vc <= vij && vij<=vs){
			double a = vs - vij;
			double b = vij-vc;
			//mKr 
			EF = (a *EFc ) / (a+b) + (b * EFs ) / (a+b);
			mkr=EF*(li/1000);
			//Nox
			nox = (a *noxc ) / (a+b) + (b * noxs ) / (a+b);
			noxEmissions=nox*(li/1000);
			//CO2 rep
			co2r = (a *co2rc ) / (a+b) + (b * co2rs ) / (a+b);
			co2rEmissions=co2r*(li/1000);
			//CO2
			co2t = (a *co2tc ) / (a+b) + (b * co2ts ) / (a+b);
			co2tEmissions=co2t*(li/1000);
			//NO2
			no2 = (a *no2c ) / (a+b) + (b * no2s ) / (a+b);
			no2Emissions=no2*(li/1000);
			//PM
			pm = (a *pmc ) / (a+b) + (b * pms ) / (a+b);
			pmEmissions=pm*(li/1000);


		}
		if (vij > vf){
			//mKr
			EF = EFf;
			mkr=EF*(li/1000);
			//Nox
			nox = noxf;
			noxEmissions=nox*(li/1000);
			// CO2 rep
			co2r =co2rf;
			co2rEmissions=co2r*(li/1000);
			// CO2
			co2t =co2tf;
			co2tEmissions=co2t*(li/1000);
			// NO2
			no2 =no2f;
			no2Emissions=no2*(li/1000);
			// PM
			pm =pmf;
			pmEmissions=pm*(li/1000);

		}
		else if(vij<vc){
			//mKr
			EF =EFc; 
			mkr=EF*(li/1000);
			//Nox
			nox =noxc;
			noxEmissions=nox*(li/1000);
			// CO2 rep
			co2r =co2rc;
			co2rEmissions=co2r*(li/1000);
			// CO2
			co2t =co2tc;
			co2tEmissions=co2t*(li/1000);
			// NO2
			no2 =no2c;
			no2Emissions=no2*(li/1000);
			// PM
			pm =pmc;
			pmEmissions=pm*(li/1000);
		}
		double [] result = new double[6];
		//	result[0]=EF;
		result[0]=mkr;
		result[1]=noxEmissions;
		result[2]=co2rEmissions;
		result[3]=co2tEmissions;
		result[4]=no2Emissions;
		result[5]=pmEmissions;

		return  result;
	}


	// Emission calculation based on stop&go and free flow fractions
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



	public double[] collectInputForEmissionAverageSpeed(int Hbefa_road_type, double averageSpeed,double distance,HbefaObject[][] HbefaTable) {

		double[] outPut = new double[5];

		if (Hbefa_road_type ==0){

			outPut[0] =  0; //mKrBasedOnAverageSpeed
			outPut[1] = 0; // 	noxEmissionsBasedOnAverageSpeed 
			//			outPut[2] =  0; // co2repEmissionsBasedOnAverageSpeed 
			outPut[2] = 0;// co2EmissionsBasedOnAverageSpeed 
			outPut[3] =  0; //no2EmissionsBasedOnAverageSpeed
			outPut[4] = 0; //pmEmissionsBasedOnAverageSpeed

		} else {	

			double vf =HbefaTable[Hbefa_road_type][0].getVelocity();
			double EFf = HbefaTable[Hbefa_road_type][0].getMkr(); 
			double noxf = HbefaTable[Hbefa_road_type][0].getEmissionFactorNox();
			double co2rf = HbefaTable[Hbefa_road_type][0].getEmissionFactorCo2Rep();
			double co2tf = HbefaTable[Hbefa_road_type][0].getEmissionFactorCo2Total();
			double no2f = HbefaTable[Hbefa_road_type][0].getNo2();
			double pmf = HbefaTable[Hbefa_road_type][0].getPm();

			double vh =HbefaTable[Hbefa_road_type][1].getVelocity();
			double EFh = HbefaTable[Hbefa_road_type][1].getMkr();
			double noxh = HbefaTable[Hbefa_road_type][1].getEmissionFactorNox();
			double co2rh = HbefaTable[Hbefa_road_type][1].getEmissionFactorCo2Rep();
			double co2th = HbefaTable[Hbefa_road_type][1].getEmissionFactorCo2Total();
			double no2h = HbefaTable[Hbefa_road_type][1].getNo2();
			double pmh = HbefaTable[Hbefa_road_type][1].getPm();

			double vs =HbefaTable[Hbefa_road_type][2].getVelocity();
			double EFs = HbefaTable[Hbefa_road_type][2].getMkr();
			double noxs = HbefaTable[Hbefa_road_type][2].getEmissionFactorNox();
			double co2rs = HbefaTable[Hbefa_road_type][2].getEmissionFactorCo2Rep();
			double co2ts = HbefaTable[Hbefa_road_type][2].getEmissionFactorCo2Total(); 
			double no2s = HbefaTable[Hbefa_road_type][2].getNo2();
			double pms = HbefaTable[Hbefa_road_type][2].getPm();

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
			double [] emissionFactorAndEmissions=  emissionFactorCalculate(vh,vij,vf, EFh,li,
					EFs, EFc, EFf, vs, vc, 
					noxf, noxh,noxs, noxc,
					co2rf,co2rh,co2rs,co2rc,
					co2tf,co2th,co2ts,co2tc,
					no2f,no2h,no2s,no2c,
					pmf,pmh,pms,pmc);

			outPut[0] =  emissionFactorAndEmissions[0]; //mKrBasedOnAverageSpeed
			outPut[1] =  emissionFactorAndEmissions[1]; // 	noxEmissionsBasedOnAverageSpeed 
			//			outPut[] =  emissionFactorAndEmissions[2]; // co2repEmissionsBasedOnAverageSpeed 
			outPut[2] =  emissionFactorAndEmissions[3];// co2EmissionsBasedOnAverageSpeed 
			outPut[3] =  emissionFactorAndEmissions[4]; //no2EmissionsBasedOnAverageSpeed
			outPut[4] =  emissionFactorAndEmissions[5]; //pmEmissionsBasedOnAverageSpeed
		}
		return outPut;
	}

	public double[] collectInputForEmissionFraction( ArrayList<String> listOfPollutant,int Hbefa_road_type, double averageSpeed,double distance,HbefaObject[][] HbefaTable) {

		int NumberOfPollutant = listOfPollutant.size();

		double[] outPutFraction= new double[NumberOfPollutant];

		if (Hbefa_road_type ==0){

			outPutFraction[0] = 0; //  mKrBasedOnFractions
			outPutFraction[1]  =0; //noxEmissionsBasedOnFractions
			//	outPutFraction[7]  =0; //co2repEmissionsBasedOnFractions
			outPutFraction[2] = 0; //co2EmissionsBasedOnFractions
			outPutFraction[3] = 0; //no2EmissionsBasedOnFractions
			outPutFraction[4] = 0; //pmEmissionsBasedOnFractions

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


			//call of function double output
			double [] fractions=  emissionFreeFlowFractionCalculate(vij,li,
					EFc, EFf,vc,/*freeVelocity,*/vf, noxf, noxc,co2rf,co2rc,co2tf,co2tc,no2f,no2c,pmf,pmc);

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
		else  if (pollutant.equals("NOx")) return 1;
		else if (pollutant.equals("CO2(total)")) return 2;
		else   if (pollutant.equals("NO2")) return 3;
		else return 4;
	} 

	/**Public transport emissions**/
	/*	public double [] ptEmissionCalculate(double distance){


		double [] ptEmissions = new double [10];
		double co2Emissions = 123620.638;

		double li=distance;

		ptEmissions [2] = co2Emissions/1000*li/1000; //Bahn-mix Source=GEMIS
		ptEmissions [7] = co2Emissions/1000*li/1000; //Bahn-mix Source=GEMIS

		return  ptEmissions;

	}*/
}
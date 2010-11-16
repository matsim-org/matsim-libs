


/* *********************************************************************** *
 * project: org.matsim.*
 * DecentralizedChargerInfo.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
//
package playground.wrashid.sschieffer;


import org.matsim.api.core.v01.Id;
//
import playground.wrashid.PSF2.pluggable.parkingTimes.*;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.LinkedListValueHashMap;

import java.lang.*;
import java.util.ArrayList;
import java.io.*;


import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.analysis.solvers.LaguerreSolver;
import org.apache.commons.math.analysis.solvers.NewtonSolver;
import org.apache.commons.math.optimization.DifferentiableMultivariateVectorialOptimizer;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.fitting.PolynomialFitter;
import org.apache.commons.math.optimization.general.AbstractLeastSquaresOptimizer;
import org.apache.commons.math.optimization.general.GaussNewtonOptimizer;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.core.utils.charts.ChartUtil;
import org.matsim.core.utils.charts.XYLineChart;




public class DecentralizedChargerInfo {//implements DifferentiableMultivariateVectorialOptimizer {
	
	
	final DifferentiableMultivariateVectorialOptimizer optimizer;
	
	private SimpsonIntegrator functionIntegrator;
	private LaguerreSolver laguerreSolver;
	private NewtonSolver newtonSolver;
	private PolynomialFitter polyFit;
	
	private double peakLoad;
	private double [] slotBaseLoad; // 1D double directly read in from .txt
	private double [] [] timeSlotBaseLoad; // modification of slotBaseLoad [time in hours][load]
	private double [][] highLowIntervals; // [start time in hours][end time in hours][1 or -1 (1 if section above newBaseLoad]
	
	private int penetration; // number of PHEV vehicles in system
	private double averagePHEVConsumption; // passed as parameter in units [load/hour]
	private double totalPHEVConsumption; // total load is calculated
	private double peakBaseConsumption;//maxBaseConsumption
	private double constantBaseConsumption; //constant minBaseConsumption
	private double newBaseConsumption; // new level of constant min base load with PHEVs
	
	
	private double crit; // level of +/- accuracy for finding newBaseConsumption
	// set in constructor
	
	
	private PolynomialFunction polyFuncBaseLoad; // degree of 96
	private PolynomialFunction polyFuncBaseLoad24;
	private PolynomialFunction polyFuncBaseLoad48;
	private ArrayList<PolynomialFunction> probDensityFunctions; // ArrayList with PolynomialFunctons for each valley
	private double[][] probDensityRanges; // for each polynomialFunction in probDensity Functions: [start probability][end probability]
	private double[][] valleyTimes; // for each valley [start time in hours][end time in hours] 
	private double [] chargingSlots;// (96*1) double array with 1.0 or 0 entries for (non)avaible slots for charging	
	private int iterationsToFindNewBaseConsumption;
	
	private double totalCostBase;
	private double totalCostPHEV;
	private double priceBase;
	private double pricePeak;
	
	private XYSeries zeroLineData; //XY Series on zero line for plots
	private XYSeries constantLoadFigureData;//XY Series on line of constantBaseConsumption for plots
	private XYSeries peakLoadFigureData;//XY Series on line of peakBaseConsumption for plots
	  
	private XYSeries loadFitFuncFigureData; //XY Series of fitted base load function for plots
	private XYSeries loadFitFuncFigureData24;
	private XYSeries loadFitFuncFigureData48;
	  
	
	
	// constructor
	public DecentralizedChargerInfo(double peakLoad, int penetration, double averagePHEVConsumption, double priceBase, double pricePeak) throws OptimizationException, MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException{
		this.peakLoad=peakLoad;
		this.penetration=penetration;
		this.averagePHEVConsumption=averagePHEVConsumption;
		this.priceBase=priceBase;
		this.pricePeak=pricePeak;
		
		totalPHEVConsumption=averagePHEVConsumption*penetration;
		
		crit=5*averagePHEVConsumption;
		
		
		functionIntegrator= new SimpsonIntegrator(); 
		newtonSolver = new NewtonSolver();
		laguerreSolver = new LaguerreSolver();
		optimizer=new LevenbergMarquardtOptimizer();
		
		getBaseLoadCurve();
		
		if (flatteningPenetration()<penetration){ 
			double step=0.005*constantBaseConsumption; 
			highLowIntervals=getLowHighTariffIntervals(step);
			}
		else{
			highLowIntervals=getLowHighTariffIntervals();
		}
		probDensityFunctions=findProbabilityDensityFunctions();
		probDensityRanges=findProbabilityRanges();
		writeSummary();
		
	}
	
	
	// writes a .txt output file of main results from class 
	public void writeSummary(){
		
		try{
		    // Create file 
			String title=("C:\\Output\\DecentralizedChargerInfoGamma_results.txt");
		    FileWriter fstream = new FileWriter(title);
		    BufferedWriter out = new BufferedWriter(fstream);
		    out.write("Function of Base Load from File: \n");
		    // function of base Load
		    out.write(polyFuncBaseLoad.toString() +"\n \n");
		    
		    // new Base Consumption
		    out.write("New Constant Base Load with PHEVs: " +newBaseConsumption+"\n \n");
		    
			// iterations to converge to newBaseConsumption
		    out.write("Iterations for Gamma Search to converge to newBaseConsumption: " +iterationsToFindNewBaseConsumption+"\n \n");
		    // number of valleys
		    out.write("Number of valleys suitable for PHEV charging: " +probDensityFunctions.size()+"\n \n");
		    
			// times of valleys
		    for (int i=0; i<valleyTimes.length; i++){
		    	out.write("Valley"+i+" Start Time: " +valleyTimes[i][0]+ "; End Time: "+valleyTimes[i][1]+"\n \n");
			    
		    }
		    
			// probability density functions
		    for (int i=0; i<probDensityFunctions.size(); i++){
		    	out.write("Valley"+i+" Probability Density Function: "+ probDensityFunctions.get(i).toString());
		    	out.write("\t Starting Probability: " +probDensityRanges[i][0]+ "; Highest Probability: "+probDensityRanges[i][1]+"\n \n");
			    
		    }
		    
		    out.write("BasePrice: "+priceBase+"\n");
		    out.write("PeakPrice: "+pricePeak+"\n");
		    out.write("Total Costs before "+ totalCostBase+" \n \n");
		    out.write("Total Costs after"+ totalCostPHEV+" \n \n");
		    
		    out.write("Cost difference divided by number of PHEV users: "+((totalCostPHEV-totalCostBase)/penetration)+"\n");
		    //Close the output stream
		    out.close();
		    }catch (Exception e){}//Catch exception if any
		    
	}
	
	
	
	
	//return slot (1-96) for charging based on a random number between 0 and 1
	public int returnChargingSlot(double randomNumber) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		int chargingSlot=0;
		for (int i=0; i<probDensityRanges.length; i++){
			if (randomNumber>=probDensityRanges[i][0] && randomNumber<probDensityRanges[i][1]){
				//correct interval
				//chargingSlot
				// RN-probDensityRanges[i][0]=Integral(probDensityFunctions.get(i)) from 0 to t
				double start=valleyTimes[i][0];
				double end=valleyTimes[i][1];
				double error=0.001;
				boolean run=true;
				double lowerBracket=start;
				double upperBracket=end;
				while (run){
					
					double integralUptoMiddle=functionIntegrator.integrate(probDensityFunctions.get(i), start, (lowerBracket+upperBracket)/2);
					double diff=integralUptoMiddle-(randomNumber- probDensityRanges[i][0]);
					if(Math.abs(diff)<error){
						// solution found
						//(lowerBracket+upperBracket)/2
						double timeInHours=(lowerBracket+upperBracket)/2;
						// Time of one SLot=15minutes=0.25 hours
						chargingSlot=(int)(timeInHours/0.25 - (timeInHours%0.25));
						run=false;
					}
					else if(diff<0){
						lowerBracket=(lowerBracket+upperBracket)/2;
						
					}
					else{
						upperBracket=(lowerBracket+upperBracket)/2;
					}
				}// end run
			}//end if
		}// end for
		return chargingSlot;
	}
	
	
	// fit data from given [][] double to PolynomialFunction
	public PolynomialFunction fitCurve(double [][] data, int degree) throws OptimizationException{
		polyFit= new PolynomialFitter(degree, optimizer);
		for (int i=0;i<data.length;i++){
			polyFit.addObservedPoint(1.0,data[i][0], data[i][1] );
		  }
		PolynomialFunction poly;
		
			poly = polyFit.fit();
		
		return poly;
	}

	
	
	// integrate given PolynomialFunction over entire day to get total baseLoadConsumption
	public double totalBaseConsumption(PolynomialFunction p) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
			return functionIntegrator.integrate(p, 0.0, 24.0);
		
	}
	
	// returns necessary number of PHEVs to reach flattening
	public long flatteningPenetration() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		double flatteningPHEVLoad=24*peakLoad-totalBaseConsumption(polyFuncBaseLoad);
		return Math.round(flatteningPHEVLoad/averagePHEVConsumption);
	}
	
	
	// only if penetration<flatteningPenetration()
	public double[][] getLowHighTariffIntervals(double step) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		iterationsToFindNewBaseConsumption=0;
		boolean run=true;
		double [][] d=new double[1][3];
		//[start time in hours][end time in hours][1 or -1 (1 if section above newBaseLoad]
		d[0][0]=0;
		d[0][1]=24.0;
		d[0][2]=1;
		double currentTry=peakLoad;
		while (run){
			iterationsToFindNewBaseConsumption++;
			if ( Math.abs(currentTry*24.0-functionIntegrator.integrate(polyFuncBaseLoad, 0, 24.0)-totalPHEVConsumption)<crit){
				//solution found
				newBaseConsumption=currentTry;
				run=false;
			}
			else{
				currentTry=currentTry+step;
			}
				
		}
		return d;
	}
	
	
	
	public double[][] getLowHighTariffIntervals() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException{
		
		iterationsToFindNewBaseConsumption=0;
		double [][] d=null;
		
		double currentUpperTry=peakLoad;
		double currentLowerTry=constantBaseConsumption;
		
		boolean run=true;
		System.out.println("start finding new constant base Load with PHEVs");
		
		while (run){
			iterationsToFindNewBaseConsumption++;
			// create flexible solution array to save position of all intersects with currentTry
			ArrayList<Double> solution=new ArrayList<Double>(0);
			// create horizontal function y=currentTry
			double currentTry=(currentUpperTry+currentLowerTry)/2;
			double [] c = {currentTry};
			
			// create polyFuncBaseLoad-currentTry to find the roots per slot
			PolynomialFunction tempFunction = new PolynomialFunction (c);
			tempFunction = polyFuncBaseLoad.subtract(tempFunction);
			
			System.out.println("currentTry=" + currentTry);
			double segment=1.0/60;// 1 minute increments
			for (double i=0; i<24.0;i=(i+segment)){
				double localTempFuncValue=tempFunction.value(i);
				System.out.println(localTempFuncValue);
				if (Math.abs(localTempFuncValue)<0.1*constantBaseConsumption){
					try
					{
						double start=i-15*segment;
						double end=i+segment;
						if(i<15.0*segment){
							start=0.0;
						}
						if (i>24.0-15*segment){
							end=24.0;
						}
							
						double root= newtonSolver.solve(tempFunction, start, end);
						//double root= laguerreSolver.solve(tempFunction, i*24.0/segments, (i+1)*0.25);
						//check if solution is really valid solution
						//needs to be a double, needs to fall within the interval, value of objective function needs to be close to zero <0.1, >-0.1
						
						if (((Double)root).isNaN() ==false && root<=end && root>=start && tempFunction.value(root)<0.1&&tempFunction.value(root)>-0.1){
							System.out.println("added root=" + root);
							solution.add(root);
							i=i+15*segment; // move away from root so the same will not be found again in the next step
							
							}
					}
					catch(Exception e)
					{
						
						// do nothing
					}
				}
				
			}
			
			if (solution.isEmpty()){ 
				//no solutions for this currentTry
				//should not happen, since we start from peakLoad and go to half
				
				if(24*currentTry-totalBaseConsumption(polyFuncBaseLoad)-totalPHEVConsumption<0.0){
					// solution is in upper half
					currentLowerTry=currentTry;
				}
				else{
					currentUpperTry=currentTry;
				}
			}// end if empty
			
			else {
				// double [start][end][1 positive/ -1 negative]
				// for n solutions there are n+1 intervals
				d= new double [solution.size()+1][3];
				// first entry
				d[0][0]=0; 
				//last entry
				d[solution.size()][1]=24.0;// last entry
				
				// fill in other entries and mark as lower (-1)or above (1) currentTry
				for (int i=0; i<solution.size(); i++){
					d[i][1]=(Double)(solution.get(i));
					d[i+1][0]=(Double)(solution.get(i));
					if (tempFunction.value((d[i][1]+d[i][0])/2)<0){
						d[i][2]=-1; // baseLoadfunction is below currentTry
					}
					else {
						d[i][2]=1;
					}
				}
				// for last entry of d indicate if lower or above tempFunction
				if (tempFunction.value((d[d.length-1][1]+d[d.length-1][0])/2)<0){
					d[d.length-1][2]=-1; // baseLoadfunction is below currentTry
				}
				else {
					d[d.length-1][2]=1;
				}
				
				
				System.out.println("segments found" + d.length);
				// find integral for all d[i][2]=-1;
				double funcInt=0;
				double totInt=0;
				for (int i=0; i<d.length; i++){
					if (d[i][2]<0){
							funcInt=funcInt+functionIntegrator.integrate(polyFuncBaseLoad,d[i][0], d[i][1]);
							totInt = totInt+ Math.abs(d[i][0]-d[i][1])*currentTry;
					}
					// check if total integral between baseLoad and currentTry is equal to PHEV Consumption
				}
				
					if (Math.abs(totInt-funcInt-totalPHEVConsumption)<crit){
						System.out.println("Integral:"+(totInt-funcInt) + " totalPHEVConsumption: " +totalPHEVConsumption + "; Difference= "+(totInt-funcInt-totalPHEVConsumption));
						
						newBaseConsumption=currentTry;
						run=false;
					}
					else if(totInt-funcInt-totalPHEVConsumption<0){
						//totInt-funcInt= available for charging
						// totalPHEVConsumption = necessary for charging
						// if negative--> newBaseLoad needs to be higher
						currentLowerTry=currentTry;
					}
					else{
						currentUpperTry=currentTry;
					}
				
				//System.out.println("Integral:"+(totInt-funcInt) + " totalPHEVConsumption: " +totalPHEVConsumption + "; Difference= "+(totInt-funcInt-totalPHEVConsumption));
				
			}// end else, solution analysis
			plotFunctionForStep(tempFunction, currentTry, solution);
		}// end while
		System.out.println("Found Solution for crit value of: "	+crit);
		return d;
	}
	
	
	
	public void getBaseLoadCurve() throws OptimizationException, IOException{
		// TODO: think about return type, e.g. double array
		// read this file
			
		  slotBaseLoad = GeneralLib.readMatrix(96, 1, true, "test\\input\\playground\\wrashid\\sschieffer\\baseLoadCurve15minBins.txt")[0];
			  
		  peakBaseConsumption=0.0;
		  constantBaseConsumption=1.0*peakLoad;
		  zeroLineData = new XYSeries("Zero Line");
		  XYSeries loadFigureData = new XYSeries("Baseload Data from File");
		  timeSlotBaseLoad= new double[slotBaseLoad.length][2];
		  
		  for (int i=0;i<24*4;i++){
			  timeSlotBaseLoad[i][0]=((double) i)*0.25;
			  timeSlotBaseLoad[i][1]=slotBaseLoad[i]*peakLoad;
			  loadFigureData.add(timeSlotBaseLoad[i][0], timeSlotBaseLoad[i][1]);
			  zeroLineData.add(i*0.25,0);
			  
			  if(timeSlotBaseLoad[i][1]>peakBaseConsumption){
				  peakBaseConsumption= timeSlotBaseLoad[i][1];
			  }
			  if(timeSlotBaseLoad[i][1]<constantBaseConsumption){
				  constantBaseConsumption= timeSlotBaseLoad[i][1];
			  }
		  }
		  polyFuncBaseLoad=fitCurve(timeSlotBaseLoad, 96);
		  polyFuncBaseLoad24=fitCurve(timeSlotBaseLoad, 24);
		  polyFuncBaseLoad48=fitCurve(timeSlotBaseLoad, 48);
		  
		  constantLoadFigureData = new XYSeries("Constant Baseload Data from File");
		  peakLoadFigureData = new XYSeries("Peak Baseload Data from File");
		  
		  loadFitFuncFigureData = new XYSeries("Fitted Function Baseload Data (deg=96)");
		  loadFitFuncFigureData24 = new XYSeries("Fitted Function Baseload Data (deg=24)");
		  loadFitFuncFigureData48 = new XYSeries("Fitted Function Baseload Data (deg=48)");
		  
		  for (int i=0;i<24*4;i++){
			  loadFitFuncFigureData.add(i*0.25, polyFuncBaseLoad.value(i*0.25));
			  loadFitFuncFigureData24.add(i*0.25, polyFuncBaseLoad24.value(i*0.25));
			  loadFitFuncFigureData48.add(i*0.25, polyFuncBaseLoad48.value(i*0.25));
			  
			  constantLoadFigureData.add(i*0.25, constantBaseConsumption);
			  peakLoadFigureData.add(i*0.25, peakBaseConsumption);
		  }
		  
		  XYSeriesCollection dataset = new XYSeriesCollection();
	      dataset.addSeries(loadFigureData);
	      dataset.addSeries(loadFitFuncFigureData);
	      dataset.addSeries(loadFitFuncFigureData24);
	      dataset.addSeries(loadFitFuncFigureData48);
	      
	      dataset.addSeries(constantLoadFigureData);
	      dataset.addSeries(peakLoadFigureData);
	      
		  JFreeChart chart = ChartFactory.createXYLineChart("Base Load from File and approximated functions", "time in hours", "Load", dataset, PlotOrientation.VERTICAL, true, true, false);
		  ChartUtilities.saveChartAsPNG(new File("C:\\Output\\baseLoadGraph.png") , chart, 800, 600);
		  //saveToFile(chart, "base Load",800, 600);
		  //(chart).saveAsPng("Base Load Curve", 800, 600);
		  
		  }
	
	
	public void plotFunctionForStep(PolynomialFunction p, double d, ArrayList<Double> intersects) throws IOException{
		 XYSeries figureData = new XYSeries("Current Objective Function");
		 XYSeries currentTryLevel = new XYSeries("currentTry");
		  
		  for (int i=0;i<24*4;i++){
			  figureData.add(i*0.25, p.value(i*0.25));
			  currentTryLevel.add(i*0.25, d);
		  }
		  
		  XYSeriesCollection dataset = new XYSeriesCollection();
	      dataset.addSeries(figureData);
	      dataset.addSeries(currentTryLevel);
	      dataset.addSeries(loadFitFuncFigureData);
	      dataset.addSeries(zeroLineData);
	      
		  if (intersects.isEmpty() ==false){
			  // if intersects have been found draw intersects
			  for (int i=0; i<intersects.size(); i++){
				  String label= ("Intersect at "+intersects.get(i));
				  XYSeries intersectData = new XYSeries(label);
				  intersectData.add((double)intersects.get(i), 0);
				  intersectData.add((double)intersects.get(i), peakLoad);
				  dataset.addSeries(intersectData);
			  }
		  }
		  
	      
	      String title= "Load and Solution graph for try "+d;
		  JFreeChart chart = ChartFactory.createXYLineChart(title, "time in hours", "Load", dataset, PlotOrientation.VERTICAL, true, true, false);
		  ChartUtilities.saveChartAsPNG(new File("C:\\Output\\"+title+".png") , chart, 800, 600);
		  
	}
	
	public double[][] getValleyTimes(){
		int count=0;
		for (int i=0; i<highLowIntervals.length; i++){
			// if [][2]=-1 --> valley--> probability density function
			if (highLowIntervals[i][2]<0){
				count++;
			}
		}
		
		valleyTimes=new double[count][2];
		count=0;
		for (int i=0; i<highLowIntervals.length; i++){
			// if [][2]=-1 --> valley--> probability density function
			if (highLowIntervals[i][2]<0){
				valleyTimes[count][0]=highLowIntervals[i][0];
				valleyTimes[count][1]=highLowIntervals[i][1];
				count++;
			}
		}
		return valleyTimes;
		
	}
	
	public ArrayList<PolynomialFunction> findProbabilityDensityFunctions() throws OptimizationException, MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException{
		double [][] valleyTimes=getValleyTimes();
		// find number of valleys with given penetration
		ArrayList<PolynomialFunction> ProbFuncArray=new ArrayList<PolynomialFunction>(0);
		XYSeriesCollection dataset = new XYSeriesCollection();
		double valleyInt=0;
		
		for (int i=0; i<valleyTimes.length; i++){
			// if [][2]=-1 --> valley--> probability density function
			
				String label=("Available Slots in Valley "+ i);
				XYSeries newValley = new XYSeries(label);
				double dx=(valleyTimes[i][1]-valleyTimes[i][0]);
				double segments=dx/(2.5/60); // every 2.5 minutes
				
				for (double x=valleyTimes[i][0]; x<=valleyTimes[i][1]; x=x+dx/segments){
					newValley.add(x,newBaseConsumption-polyFuncBaseLoad.value(x));
					polyFit.addObservedPoint(1.0,x, newBaseConsumption-polyFuncBaseLoad.value(x) );
				}
				dataset.addSeries(newValley);
				PolynomialFunction poly = polyFit.fit();
				ProbFuncArray.add(poly);
				valleyInt=valleyInt+functionIntegrator.integrate(poly, valleyTimes[i][0], valleyTimes[i][1]);
			}
		
		String title= "Probability Density Functions ";
		JFreeChart chart = ChartFactory.createXYLineChart(title, "time in hours", "Free Load for PHEV charging", dataset, PlotOrientation.VERTICAL, true, true, false);
		ChartUtilities.saveChartAsPNG(new File("C:\\Output\\"+title+".png") , chart, 800, 600);
		  
				
		// now the integral of all probability function and their respective time intervals needs to be 1
		totalCostBase=constantBaseConsumption*priceBase*24.0 + (functionIntegrator.integrate(polyFuncBaseLoad, 0, 24.0)-constantBaseConsumption*24.0)*pricePeak;
		
		totalCostPHEV=(-newBaseConsumption*24.0 + (functionIntegrator.integrate(polyFuncBaseLoad, 0, 24.0)+valleyInt))*pricePeak + newBaseConsumption*24.0*priceBase;
		
		
		double [] c = {1.0/valleyInt};
		
		// create polyFuncBaseLoad-currentTry to find the roots per slot
		PolynomialFunction scalingFunction = new PolynomialFunction (c);
		for (int j=0; j<ProbFuncArray.size();j++){
			ProbFuncArray.set(j,(ProbFuncArray.get(j).multiply(scalingFunction)) );
		}
		
		return ProbFuncArray;
	}
	
	public double[][] findProbabilityRanges() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		//row - corresponds to entry in probDensityFunction
		// column 1= start column 2 = end
		double[][] ranges=new double[probDensityFunctions.size()][2];
		ranges[0][0]=0.0;
		ranges[probDensityFunctions.size()-1][1]=1.0;
		
		for (int i=0; i<probDensityFunctions.size()-1;i++){
			ranges[i][1]=ranges[i][0]+functionIntegrator.integrate(probDensityFunctions.get(i), valleyTimes[i][0], valleyTimes[i][1]);
			ranges[i+1][0]=ranges[i][0]+functionIntegrator.integrate(probDensityFunctions.get(i), valleyTimes[i][0], valleyTimes[i][1]);
		}
		return ranges;
	}	
	
	public void translateProbabilityRanges2Slots(double part){
		// where part is the minimum percentage of a slot that needs to be filled with the probability density function
		chargingSlots=new double[96];
		for (int i=0; i<valleyTimes.length-1;i++){
			double startTime=valleyTimes[i][0];
			double endTime=valleyTimes[i][1];
			boolean run=true;
			while (run){
				double startOfInterval=startTime-startTime%0.25;
				double endOfInterval=startOfInterval+0.25;
				double thisEndTime=0;
				// check if at least part % of 15 min interval are filled
				if (endOfInterval<endTime){
					thisEndTime=endOfInterval;
				}
				else{
					thisEndTime=endTime;
				}
				if ((startTime-startOfInterval) + (endOfInterval-thisEndTime)>part*0.25){
					int newSlot=(int)(startOfInterval/0.25);
					chargingSlots[newSlot]=1;
					
				}
				if (thisEndTime-startOfInterval<0.25){
					// then this was last possible slotEntry
					run=false;
				}
				startTime=endOfInterval;
			
			}
		}
	}
	
	
	public double[][] getSlotLoad(){
		return timeSlotBaseLoad;
	}
	
	
	public double getNewBaseConsumption(){
		return newBaseConsumption;
	}
	
	/*
	 * in first iteration the result is same as getBaseLoadCurve.
	 * 
	 */
	public void getTotalLoadInPreviousIteration(){
		// TODO: think about this (what should be return type, or should this be solved differently?)
		
		// e.g. 15min bins (double[])
	}
	
	/*
	 * remains constant during simulation.
	 */
	
	
	
//	
//	public void getAdaptedProbabilityCurve(Id agentId){
//		// TODO: implement method.
//	}
//	
//	public  LinkedListValueHashMap<Id, Double> getEnergyConsumptionOfLegs(){
//		// TODO: rw
//		return null;
//	}
//	
//	public LinkedListValueHashMap<Id, ParkingInterval> getParkingTimeIntervals(){
//		// TODO: rw
//		
//		return null;
//	}
//	
//	public double getSOCOfAgent(Id agentId, double time){
//		
//		// TODO: will find out...
//		
//		return -1.0;
//	}
	
	
}

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



public class DecentralizedChargerInfo {
	
	
	final DifferentiableMultivariateVectorialOptimizer optimizer;
	
	private SimpsonIntegrator functionIntegrator;
	private NewtonSolver newtonSolver;
	private PolynomialFitter polyFit;
	
	private double peakLoad;
	private double [] slotBaseLoad; // 1D double directly read in from .txt
	private double [] [] timeSlotBaseLoad; // modification of slotBaseLoad [time in hours][load]
	private double [][] highLowIntervals; // [start time in hours][end time in hours][1 or -1 (1 if section above newBaseLoad]
	
	private double penetration; // number of PHEV vehicles in system
	private double averagePHEVConsumption; // passed as parameter in units [load/hour]
	private double totalPHEVConsumption; // total load is calculated
	private double peakBaseConsumption;//maxBaseConsumption
	private double constantBaseConsumption; //constant minBaseConsumption
	private double newBaseConsumption; // new level of constant min base load with PHEVs
	
	
	private double crit; // level of +/- accuracy for finding newBaseConsumption
	// set in constructor
	
	
	private PolynomialFunction polyFuncBaseLoad; // degree of 96
	private PolynomialFunction polyFuncBaseLoad24; // degree of 24
	private PolynomialFunction polyFuncBaseLoad48; // degree of 48
	private ArrayList<PolynomialFunction> probDensityFunctions; // ArrayList with PolynomialFunctons for each valley
	private double[][] probDensityRanges; // for each polynomialFunction in probDensity Functions: [start probability][end probability]
	private double[][] valleyTimes; // for each valley [start time in hours][end time in hours] 
	private double [] chargingSlots;// (96*1) double array with 1.0 or 0 entries for (non)avaible slots for charging	
	private int iterationsToFindNewBaseConsumption;
	
	private double totalCostBase;
	private double totalCostPHEV;
	private double priceBase;
	private double pricePeak;
	
	final double secondsPerMin=60;
	final double secondsPer15Min=15*60;
	final double secondsPerDay=24*60*60;
	
	private XYSeries zeroLineData; //XY Series on zero line for plots
	private XYSeries constantLoadFigureData;//XY Series on line of constantBaseConsumption for plots
	private XYSeries peakLoadFigureData;//XY Series on line of peakBaseConsumption for plots
	  
	private XYSeries loadFitFuncFigureData; //XY Series of fitted base load function for plots
	private XYSeries loadFitFuncFigureData24;
	private XYSeries loadFitFuncFigureData48;
	  
	final String outputPath="C:\\Output\\";
	
	/**
	 * constructor calls a sequence of methods to find 
	 * a function for the regular base load
	 * the valley times and 
	 * probability density functions for the given parameters
	 * 
	 * @param peakLoad - is a double in Watts corresponding to the daily peakLoad = 100%
	 * @param penetration - the number of PHEV cars in the system
	 * @param averagePHEVConsumption - average daily consumption per car in Watt
	 * @param priceBase - price for electricity in base load
	 * @param pricePeak - price for electricity in peak times
	 * @throws OptimizationException
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public DecentralizedChargerInfo(double peakLoad, double penetration, double averagePHEVConsumption, double priceBase, double pricePeak) throws OptimizationException, MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException{
		this.peakLoad=peakLoad;
		this.penetration=penetration;
		this.averagePHEVConsumption=averagePHEVConsumption;
		this.priceBase=priceBase;
		this.pricePeak=pricePeak;
		
		totalPHEVConsumption=averagePHEVConsumption*penetration;
		
		crit=5*averagePHEVConsumption;
		
		functionIntegrator= new SimpsonIntegrator(); 
		newtonSolver = new NewtonSolver();
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
	
	
	/**
	 * reads in data from input txt file from 15 min data bins 
	 * fits the data to a functions of degrees 24,48 and 96 
	 * plots and saves the results in the outputPath
	 * @throws OptimizationException
	 * @throws IOException
	 */
	public void getBaseLoadCurve() throws OptimizationException, IOException{
		
		  slotBaseLoad = GeneralLib.readMatrix(96, 1, false, "test\\input\\playground\\wrashid\\sschieffer\\baseLoadCurve15minBins.txt")[0];
			  
		  peakBaseConsumption=0.0;
		  constantBaseConsumption=1.0*peakLoad;
		  zeroLineData = new XYSeries("Zero Line");
		  XYSeries loadFigureData = new XYSeries("Baseload Data from File");
		  timeSlotBaseLoad= new double[slotBaseLoad.length][2];
		  
		  for (int i=0;i<96;i++){ // loop over 96 bins
			  timeSlotBaseLoad[i][0]=((double) i)*secondsPer15Min;
			  timeSlotBaseLoad[i][1]=slotBaseLoad[i]*peakLoad;
			  loadFigureData.add(timeSlotBaseLoad[i][0], timeSlotBaseLoad[i][1]);
			  zeroLineData.add(i*secondsPer15Min,0);
			  
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
		  
		  for (int i=0;i<96;i++){
			  loadFitFuncFigureData.add(i*secondsPer15Min, polyFuncBaseLoad.value(i*secondsPer15Min));
			  loadFitFuncFigureData24.add(i*secondsPer15Min, polyFuncBaseLoad24.value(i*secondsPer15Min));
			  loadFitFuncFigureData48.add(i*secondsPer15Min, polyFuncBaseLoad48.value(i*secondsPer15Min));
			  
			  constantLoadFigureData.add(i*secondsPer15Min, constantBaseConsumption);
			  peakLoadFigureData.add(i*secondsPer15Min, peakBaseConsumption);
		  }
		  
		  XYSeriesCollection dataset = new XYSeriesCollection();
	      dataset.addSeries(loadFigureData);
	      dataset.addSeries(loadFitFuncFigureData);
	      dataset.addSeries(loadFitFuncFigureData24);
	      dataset.addSeries(loadFitFuncFigureData48);
	      
	      dataset.addSeries(constantLoadFigureData);
	      dataset.addSeries(peakLoadFigureData);
	      
		  JFreeChart chart = ChartFactory.createXYLineChart("Base Load [Watts] from File and approximated functions", "time in seconds", "Load", dataset, PlotOrientation.VERTICAL, true, true, false);
		  ChartUtilities.saveChartAsPNG(new File(outputPath+ "baseLoadGraph.png") , chart, 800, 600);
		 
		  }
	
	
	
	/**
	 * Fits the data from a double array to a polynomial function
	 * 
	 * @param data - data in double array form (n*2)
	 * @param degree - degree of polynomial
	 * @return
	 * @throws OptimizationException
	 */
	public PolynomialFunction fitCurve(double [][] data, int degree) throws OptimizationException{
		polyFit= new PolynomialFitter(degree, optimizer);
		for (int i=0;i<data.length;i++){
			polyFit.addObservedPoint(1.0,data[i][0], data[i][1] );
		  }
		PolynomialFunction poly;
		
			poly = polyFit.fit();
		
		return poly;
	}

	
	
	/**
	 * Integrates given function p over the entire day
	 */
	public double totalBaseConsumption(PolynomialFunction p) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
			return functionIntegrator.integrate(p, 0.0, secondsPerDay);
		
	}
	
	
	/**
	 * returns the number of PHEVs necessary to reach complete baseLoad flattening
	 */
	public long flatteningPenetration() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		double flatteningPHEVLoad=24*peakLoad-totalBaseConsumption(polyFuncBaseLoad);
		return Math.round(flatteningPHEVLoad/averagePHEVConsumption);
	}
	
	
	/**
	 * returns a double Array with the low and high tariff intervals 
	 * for the current PHEV penetration and baseLoad
	 * - first it finds the new base Consumption level
	 * - then it finds the intervals
	 * 
	 * only called, if penetration>flatteningPenetration()
	 * @param step - since there is no clear upper bound on the case penetration>flatteningPenetration
	 * we increase currentTry by a given small step until the newBaseConsumption is found
	 * @return
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 */
	public double[][] getLowHighTariffIntervals(double step) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		iterationsToFindNewBaseConsumption=0;
		boolean run=true;
		double [][] d=new double[1][3];
		//[start time in hours][end time in hours][1 or -1 (1 if section above newBaseLoad]
		d[0][0]=0;
		d[0][1]=secondsPerDay;
		d[0][2]=1; // since penetration>flatteningPenetration always 1 and only one interval
		double currentTry=peakLoad;
		while (run){
			iterationsToFindNewBaseConsumption++;
			if ( Math.abs(currentTry*secondsPerDay-functionIntegrator.integrate(polyFuncBaseLoad, 0, secondsPerDay)-totalPHEVConsumption)<crit){
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
	
	
	/**
	 * returns a double Array with the low and high tariff intervals 
	 * for the current PHEV penetration and baseLoad
	 * - first it finds the new base Consumption level
	 * - then it finds the intervals
	 * 
	 * this method is called if penetration<flatteningPenetration
	 * it uses a bisecting method (bisecting the interval between currentTry and the known upper or lower bound (peakLoad or baseLoad))to come closer to the new BaseConsumption level
	 * 
	 * @return
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
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
			double segment=secondsPerMin;
			for (double i=0; i<secondsPerDay;i=(i+segment)){
				double localTempFuncValue=tempFunction.value(i);
				System.out.println(localTempFuncValue);
				// if close to a solution at time i, try to find an intersect with newtomSolver
				if (Math.abs(localTempFuncValue)<0.1*constantBaseConsumption){
					try
					{
						double start=i-15*segment;
						double end=i+segment;
						if(i<15.0*segment){
							start=0.0;
						}
						if (i>secondsPerDay-15*segment){
							end=secondsPerDay;
						}
							
						double root= newtonSolver.solve(tempFunction, start, end);
						/*
						 * check if solution is really valid solution
						 * needs to be a double, needs to fall within the interval, value of objective function needs to be close to zero <0.1, >-0.1
						
						 */
						
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
			
			if (solution.isEmpty()){ 	//no solutions for this currentTry
			//should not happen, since we start from peakLoad and go to half
				
				if(secondsPerDay*currentTry-totalBaseConsumption(polyFuncBaseLoad)-totalPHEVConsumption<0.0){
					// solution is in upper half
					currentLowerTry=currentTry;
				}
				else{
					currentUpperTry=currentTry;
				}
			}// end if empty
			
			else {
				/*
				 * Save found intersects in double array
				 * double [start][end][1 positive/ -1 negative]
				 * for n solutions there are n+1 intervals
				 */
				 
				d= new double [solution.size()+1][3];
				// first entry
				d[0][0]=0; 
				//last entry
				d[solution.size()][1]=secondsPerDay;// last entry
				
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
						/*
						 * totInt-funcInt= available for charging
						 totalPHEVConsumption = necessary for charging
						 if negative--> newBaseLoad needs to be higher
						 */
						
						currentLowerTry=currentTry;
					}
					else{
						currentUpperTry=currentTry;
					}
			}// end else, solution analysis
			plotFunctionForStep(tempFunction, currentTry, solution);
		}// end while
		System.out.println("Found Solution for crit value of: "	+crit);
		return d;
	}
	
	
	
	/**
	 * method is called as part of getLowHighTariffIntervals()
	 * plots the currentTry and found intervals as png
	 * and saves the file in output
	 * 
	 * @param p
	 * @param d
	 * @param intersects
	 * @throws IOException
	 */
	public void plotFunctionForStep(PolynomialFunction p, double d, ArrayList<Double> intersects) throws IOException{
		 XYSeries figureData = new XYSeries("Current Objective Function");
		 XYSeries currentTryLevel = new XYSeries("currentTry");
		  
		  for (int i=0;i<96;i++){
			  figureData.add(i*secondsPer15Min, p.value(i*secondsPer15Min));
			  currentTryLevel.add(i*secondsPer15Min, d);
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
		  ChartUtilities.saveChartAsPNG(new File(outputPath+title+".png") , chart, 800, 600);
		  
	}
	
	
	/**
	 * reads entries of highLowIntervals and extracts the start and end times of valleys
	 * saves times in double Array 
	 * @return
	 */
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
	
	
	/**
	 * derives probability density functions for each valley from
	 * -the valleyTimes
	 * -the fittedFunction
	 * - the newBaseConstumption
	 * @return
	 * @throws OptimizationException
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
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
				double segments=dx/(secondsPerMin); // every minute
				
				for (double x=valleyTimes[i][0]; x<=valleyTimes[i][1]; x=x+dx/segments){
					newValley.add(x,newBaseConsumption-polyFuncBaseLoad.value(x));
					polyFit.addObservedPoint(1.0,x, newBaseConsumption-polyFuncBaseLoad.value(x) );
				}
				dataset.addSeries(newValley);
				PolynomialFunction poly = polyFit.fit();
				ProbFuncArray.add(poly);
				valleyInt=valleyInt+functionIntegrator.integrate(poly, valleyTimes[i][0], valleyTimes[i][1]);
			}
		
		String title= "Unscaled Probability Density Functions ";
		JFreeChart chart = ChartFactory.createXYLineChart(title, "time in seconds", "Free Load for PHEV charging", dataset, PlotOrientation.VERTICAL, true, true, false);
		ChartUtilities.saveChartAsPNG(new File(outputPath+title+".png") , chart, 800, 600);
		  
				
		// now the integral of all probability function and their respective time intervals needs to be 1
		double [] c = {1.0/valleyInt};
		
		// create polyFuncBaseLoad-currentTry to find the roots per slot
		PolynomialFunction scalingFunction = new PolynomialFunction (c);
		for (int j=0; j<ProbFuncArray.size();j++){
			ProbFuncArray.set(j,(ProbFuncArray.get(j).multiply(scalingFunction)) );
		}
		
		//TODO CHeck these functions
		totalCostBase=constantBaseConsumption*priceBase*secondsPerDay + (functionIntegrator.integrate(polyFuncBaseLoad, 0, secondsPerDay)-constantBaseConsumption*secondsPerDay)*pricePeak;
		
		totalCostPHEV=(-newBaseConsumption*secondsPerDay + (functionIntegrator.integrate(polyFuncBaseLoad, 0,secondsPerDay)+valleyInt))*pricePeak + newBaseConsumption*secondsPerDay*priceBase;
		
		
		return ProbFuncArray;
	}
	
	/**
	 * checks each probability function in Array probDensityFunctions
	 * for function i
	 * it saves the corresponding start and end probability in a new Array at position i
	 * @return
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 */
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
	

	
	public double[][] getSlotLoad(){
		return timeSlotBaseLoad;
	}
	
	
	public double getNewBaseConsumption(){
		return newBaseConsumption;
	}
	

	/**
	 * saves a textfile in the specified output path containing:
	 * -the function of the baseLoadCurve
	 * -the new constant Base Load
	 * -number of valleys and the corresponding probability density functions
	 * ...
	 */
	public void writeSummary(){
		
		try{
		    // Create file 
			String title=(outputPath + "DecentralizedChargerInfoGamma_results.txt");
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
	
	

	
	
}

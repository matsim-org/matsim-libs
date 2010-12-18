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
import org.apache.commons.math.optimization.SimpleVectorialValueChecker;
import org.apache.commons.math.optimization.VectorialConvergenceChecker;
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
	final VectorialConvergenceChecker checker;
	
	private SimpsonIntegrator functionIntegrator;
	private NewtonSolver newtonSolver;
	private PolynomialFitter polyFit;
	
	private int degValleyFitter=5; 
	
	
	private double [] []slotBaseLoad; // 1D double directly read in from .txt
	private double [][] highLowIntervals; // [start time in hours][end time in hours][1 or -1 (1 if section above newBaseLoad]
	
	private double penetration; // number of PHEV vehicles in system
	private double flatteningPenetration;
	private double averagePHEVConsumption; // passed as parameter in units [load/hour]
	private double totalPHEVConsumption; // total load is calculated
	private double peakBaseConsumption;//maxBaseConsumption
	private double constantBaseConsumption; //constant minBaseConsumption
	private double newBaseConsumption; // new level of constant min base load with PHEVs
	
	private double totalCostBase;
	private double totalCostPHEV;
	
	private double crit; // level of +/- accuracy for finding newBaseConsumption
	// set in constructor
	
	private PolynomialFunction polyFuncBaseLoad96; // degree of 96
	private PolynomialFunction polyFuncBaseLoad24; // degree of 24
	private PolynomialFunction polyFuncBaseLoad48; // degree of 48
	private PolynomialFunction polyFuncBaseLoadWorking;
	private ArrayList<PolynomialFunction> probDensityFunctions; // ArrayList with PolynomialFunctons for each valley
	private double[][] probDensityRanges; // for each polynomialFunction in probDensity Functions: [start probability][end probability]
	private double[][] valleyTimes; // for each valley [start time in hours][end time in hours] 
	private double[][] peakTimes;
	private double [] chargingSlots;// (96*1) double array with 1.0 or 0 entries for (non)avaible slots for charging	
	private int iterationsToFindNewBaseConsumption;
	
	
	private XYSeries loadFigureData;
	private XYSeries zeroLineData; //XY Series on zero line for plots
	private XYSeries constantLoadFigureData;//XY Series on line of constantBaseConsumption for plots
	private XYSeries peakLoadFigureData;//XY Series on line of peakBaseConsumption for plots
	  
	private XYSeries loadFitFuncFigureData96; //XY Series of fitted base load function for plots
	private XYSeries loadFitFuncFigureData24;
	private XYSeries loadFitFuncFigureData48;
	private XYSeries loadFitFuncFigureDataWorking;
	  
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
	public DecentralizedChargerInfo(double penetration, double averagePHEVConsumption) throws OptimizationException, MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException{
		
		this.penetration=penetration;
		this.averagePHEVConsumption=averagePHEVConsumption;
		
		System.out.println("peakLoad: "+Main.peakLoad);
		totalPHEVConsumption=averagePHEVConsumption*penetration; //Watts
		
		crit=5*averagePHEVConsumption; // Watts
		
		functionIntegrator= new SimpsonIntegrator(); 
		newtonSolver = new NewtonSolver();
		
		checker=new SimpleVectorialValueChecker(Main.peakLoad*0.05, Main.peakLoad*0.05);
		
		//GaussNewtonOptimizer(boolean useLU) 
		LevenbergMarquardtOptimizer levenbergMarquardtOptimizer = new LevenbergMarquardtOptimizer();
		GaussNewtonOptimizer gaussNewtonOptimizer= new GaussNewtonOptimizer(true); //useLU - true, faster  else QR more robust
		
		levenbergMarquardtOptimizer.setMaxIterations(1000000000);
		levenbergMarquardtOptimizer.setConvergenceChecker(checker);
		
		gaussNewtonOptimizer.setMaxIterations(1000000000);		
		gaussNewtonOptimizer.setConvergenceChecker(checker);
		
		//optimizer=levenbergMarquardtOptimizer;
		optimizer=gaussNewtonOptimizer;
		
	}
	
	
	public void findHighLowIntervals() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException{
		
		flatteningPenetration=flatteningPenetration();
		if (flatteningPenetration<penetration){ 
			
			double step=0.005*constantBaseConsumption; 
			highLowIntervals=getLowHighTariffIntervals(step);
			}
		else{
			highLowIntervals=getLowHighTariffIntervals();
		}
	}
	
	
	public void loadBaseLoadCurveFromTextFile() throws IOException{
		// read in double matrix with time and load value
		slotBaseLoad = GeneralLib.readMatrix(96, 2, false, "test\\input\\playground\\wrashid\\sschieffer\\baseLoadCurve15minBinsSecLoad.txt");
		
		/*
		 * Loop over matrix to find
		 * - peakBaseConsumption (maxValue)
		 * - constantBaseConsumption (minValue)
		 * - put in zeroLineData - helpful for graphs
		 * - put matrix data in XYSeries Format for graphing
		 */
		
		peakBaseConsumption=0.0;
		constantBaseConsumption=Main.peakLoad;
		zeroLineData = new XYSeries("Zero Line");
		loadFigureData = new XYSeries("Baseload Data from File");
		  
		  for (int i=0;i<96;i++){ // loop over 96 bins
			  //slotBaseLoad[i][0]=((double)i)*Main.secondsPer15Min;//time
			  slotBaseLoad[i][1]=slotBaseLoad[i][1]*Main.peakLoad;//multiply percentage with peakLoad
			  loadFigureData.add(slotBaseLoad[i][0], slotBaseLoad[i][1]);
			  zeroLineData.add(slotBaseLoad[i][0],0);
			  
			  if(slotBaseLoad[i][1]>peakBaseConsumption){
				  peakBaseConsumption= slotBaseLoad[i][1];
			  }
			  if(slotBaseLoad[i][1]<constantBaseConsumption){
				  constantBaseConsumption= slotBaseLoad[i][1];
			  }
		  }
		  
		  XYSeriesCollection dataset = new XYSeriesCollection();
	      dataset.addSeries(loadFigureData);
	      dataset.addSeries(zeroLineData);
	     
		  JFreeChart chart = ChartFactory.createXYLineChart("Base Load [Watts] from File", "time in seconds", "Load", dataset, PlotOrientation.VERTICAL, true, true, false);
		  ChartUtilities.saveChartAsPNG(new File(outputPath+ "baseLoadGraph.png") , chart, 800, 600);
		 
	}
	
	/**
	 * 
	 * fits the data to a functions of degrees 24,48 and 96 
	 * plots and saves the results in the outputPath
	 * @throws OptimizationException
	 * @throws IOException
	 */
	public void getFittedLoadCurve() throws OptimizationException, IOException{
		
		System.out.println("fitting curve of degree "+ 24);
		polyFuncBaseLoad24=fitCurve(slotBaseLoad, 24);  
		System.out.println("fitting curve of degree "+ 48);
		polyFuncBaseLoad48=fitCurve(slotBaseLoad, 48);
		System.out.println("fitting curve of degree "+ 96);
		polyFuncBaseLoad96=fitCurve(slotBaseLoad, 96);
		
		  constantLoadFigureData = new XYSeries("Constant Baseload Data from File");
		  peakLoadFigureData = new XYSeries("Peak Baseload Data from File");
		  
		  loadFitFuncFigureData96 = new XYSeries("Fitted Function Baseload Data (deg=96)");
		  loadFitFuncFigureData24 = new XYSeries("Fitted Function Baseload Data (deg=24)");
		  loadFitFuncFigureData48 = new XYSeries("Fitted Function Baseload Data (deg=48)");
		  
		  for (int i=0;i<96;i++){
			  loadFitFuncFigureData96.add(i*Main.secondsPer15Min, polyFuncBaseLoad96.value(i*Main.secondsPer15Min));
			  loadFitFuncFigureData24.add(i*Main.secondsPer15Min, polyFuncBaseLoad24.value(i*Main.secondsPer15Min));
			  loadFitFuncFigureData48.add(i*Main.secondsPer15Min, polyFuncBaseLoad48.value(i*Main.secondsPer15Min));
			  
			  constantLoadFigureData.add(i*Main.secondsPer15Min, constantBaseConsumption);
			  peakLoadFigureData.add(i*Main.secondsPer15Min, peakBaseConsumption);
		  }
		  
		  plotLoadSeries("fittedLoadGraph24.png", loadFitFuncFigureData24);		  
		  plotLoadSeries("fittedLoadGraph48.png", loadFitFuncFigureData48);
		  plotLoadSeries("fittedLoadGraph96.png", loadFitFuncFigureData96);
		  
		  // assign the polyFunction to use in this class
		  polyFuncBaseLoadWorking=polyFuncBaseLoad24;
		  loadFitFuncFigureDataWorking=loadFitFuncFigureData24;
		  }
	
	/**
	 * 
	 * @param nameToSavePNG  name under which plot is saved in output path, e.g. plot.png
	 * @param xy  load Series to be plotted next to constantLoad, peakLoad, readInLoad
	 * @throws IOException
	 */
	public void plotLoadSeries(String nameToSavePNG, XYSeries xy) throws IOException{
		  XYSeriesCollection dataset = new XYSeriesCollection();
		  dataset.addSeries(loadFigureData);
		  dataset.addSeries(xy);
	      dataset.addSeries(constantLoadFigureData);
	      dataset.addSeries(peakLoadFigureData);
	      
		  JFreeChart chart = ChartFactory.createXYLineChart("Base Load [Watts] from File and approximated functions", "time in seconds", "Load", dataset, PlotOrientation.VERTICAL, true, true, false);
		  ChartUtilities.saveChartAsPNG(new File(outputPath+ nameToSavePNG) , chart, 800, 600);
		 
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
			//System.out.println("adding point "+ data[i][0]+", " +data[i][1]+" as element " + i);
			polyFit.addObservedPoint(1.0,data[i][0], data[i][1] );
		  }
		 
		 PolynomialFunction poly = polyFit.fit();
		
		return poly;
	}

	
	
	/**
	 * Integrates given function p over the entire day
	 */
	public double totalBaseConsumption(PolynomialFunction p) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		return functionIntegrator.integrate(p, 0.0, Main.secondsPerDay);
	}
	
	
	/**
	 * returns the number of PHEVs necessary to reach complete baseLoad flattening
	 */
	public double flatteningPenetration() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		double totalValleyToFill=Main.secondsPerDay*Main.peakLoad-totalBaseConsumption(polyFuncBaseLoadWorking);
		return (totalValleyToFill/averagePHEVConsumption);
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
		d[0][1]=Main.secondsPerDay;
		d[0][2]=1; // since penetration>flatteningPenetration always 1 and only one interval
		double currentTry=Main.peakLoad;
		while (run){
			iterationsToFindNewBaseConsumption++;
			if ( Math.abs(currentTry*Main.secondsPerDay-functionIntegrator.integrate(polyFuncBaseLoadWorking , 0, Main.secondsPerDay)-totalPHEVConsumption)<crit){
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
		
		double currentUpperTry=Main.peakLoad;
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
			tempFunction = polyFuncBaseLoadWorking.subtract(tempFunction);
			
			System.out.println("currentTry=" + currentTry);
			double segment=Main.secondsPerMin;
			for (double i=0; i<Main.secondsPerDay;i=(i+segment)){
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
						if (i>Main.secondsPerDay-15*segment){
							end=Main.secondsPerDay;
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
				
				if(Main.secondsPerDay*currentTry-totalBaseConsumption(polyFuncBaseLoadWorking)-totalPHEVConsumption<0.0){
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
				d[solution.size()][1]=Main.secondsPerDay;// last entry
				
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
							funcInt=funcInt+functionIntegrator.integrate(polyFuncBaseLoadWorking,d[i][0], d[i][1]);
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
		System.out.println("Found Solution within range of +- "	+crit+ "Watts");
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
			  figureData.add(i*Main.secondsPer15Min, p.value(i*Main.secondsPer15Min));
			  currentTryLevel.add(i*Main.secondsPer15Min, d);
		  }
		  
		  XYSeriesCollection dataset = new XYSeriesCollection();
	      dataset.addSeries(figureData);
	      dataset.addSeries(currentTryLevel);
	      dataset.addSeries(loadFitFuncFigureDataWorking);
	      dataset.addSeries(zeroLineData);
	      
		  if (intersects.isEmpty() ==false){
			  // if intersects have been found draw intersects
			  for (int i=0; i<intersects.size(); i++){
				  String label= ("Intersect at "+intersects.get(i));
				  XYSeries intersectData = new XYSeries(label);
				  intersectData.add((double)intersects.get(i), 0);
				  intersectData.add((double)intersects.get(i), Main.peakLoad);
				  dataset.addSeries(intersectData);
			  }
		  }
		  
	      String title= "Load and Solution graph for try "+d;
		  JFreeChart chart = ChartFactory.createXYLineChart(title, "time in hours", "Load", dataset, PlotOrientation.VERTICAL, true, true, false);
		  ChartUtilities.saveChartAsPNG(new File(outputPath+title+".png") , chart, 800, 600);
		  
	}
	
	
	public double [][] getPeakTimes(double [][] valleys2ComareTo){
		// if defineCase=0; valleyTimes are not at beginning or end of day
		// if defineCase=1; valleyTime is at beginning of day
		// if defineCase=2; valleyTime is at  end of day
		// if defineCase=3; valleyTime are at beginning and end
		
		int defineCase;
		if (valleys2ComareTo[0][0]==0) //valleyAtBeginnging
		{
			defineCase=1;
			if (valleys2ComareTo[valleys2ComareTo.length-1][1]==Main.secondsPerDay){
				// at beginning and end
				defineCase=3;
			}
		}
		
		else if ( valleys2ComareTo[valleys2ComareTo.length-1][1]==Main.secondsPerDay){
			defineCase=2;
		}
		else{defineCase=0;}
		
		// create peakTimes
		
		if (defineCase==0){
			// if defineCase=0; valleyTimes are not at beginning or end of day
			peakTimes= new double[valleys2ComareTo.length+1][2];
			peakTimes[0][0]=0;
			peakTimes[0][1]=valleys2ComareTo[0][0];
			for (int i=0; i<valleys2ComareTo.length-1; i++){
				peakTimes[i+1][0]=valleys2ComareTo[i][1];
				peakTimes[i+1][1]=valleys2ComareTo[i+1][0];                                 
			}
			peakTimes[peakTimes.length-1][0]=valleys2ComareTo[valleys2ComareTo.length-1][1]; 
			peakTimes[peakTimes.length-1][1]=Main.secondsPerDay;
		}
		
		if (defineCase==1){
			// if defineCase=1; valleyTime is at beginning of day
			peakTimes= new double[valleys2ComareTo.length][2];
			for (int i=0; i<peakTimes.length-1; i++){
				peakTimes[i][0]=valleys2ComareTo[i][1];
				peakTimes[i][1]=valleys2ComareTo[i+1][0];
			}
			peakTimes[peakTimes.length-1][0]=valleys2ComareTo[valleys2ComareTo.length-1][1];
			peakTimes[peakTimes.length-1][1]=Main.secondsPerDay;
		}
		if (defineCase==2){
			// if defineCase=1; valleyTime is at end of day
			peakTimes= new double[valleys2ComareTo.length][2];
			peakTimes[0][0]=0;
			peakTimes[0][1]=valleys2ComareTo[0][0];
			for (int i=1; i<peakTimes.length; i++){
				peakTimes[i][0]=valleys2ComareTo[i-1][1];
				peakTimes[i][1]=valleys2ComareTo[i][0];                                 
			}
		}
		
		if (defineCase==3){
			// if defineCase=2; valleyTime are at beginning and end
			peakTimes= new double[valleys2ComareTo.length-1][2];
			for (int i=0; i<peakTimes.length; i++){
				peakTimes[i][0]=valleys2ComareTo[i][1];
				peakTimes[i][1]=valleys2ComareTo[i+1][0];   
			}
		}
		
		return peakTimes;
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
	public void findProbabilityDensityFunctions() throws OptimizationException, MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException{
		double [][] valleyTimes=getValleyTimes();
		// find number of valleys with given penetration
		//initialize ProbFuncArrayWithInitial size 0
		ArrayList<PolynomialFunction> ProbFuncArray=new ArrayList<PolynomialFunction>(0);
		XYSeriesCollection dataset = new XYSeriesCollection();
		double valleyInt=0;
		
		for (int i=0; i<valleyTimes.length; i++){
			// if [][2]=-1 --> valley--> probability density function
			
				String label=("Available Slots in Valley "+ i);
				XYSeries newValley = new XYSeries(label);
				double dx=(valleyTimes[i][1]-valleyTimes[i][0]);
				double segments=dx/(Main.secondsPerMin); // every minute
				
				
				PolynomialFitter valleyFitter=new PolynomialFitter(degValleyFitter,optimizer);
				
				for (double x=valleyTimes[i][0]; x<=valleyTimes[i][1]; x=x+dx/segments){
					newValley.add(x,newBaseConsumption-polyFuncBaseLoadWorking.value(x));
					double toAdd=newBaseConsumption-polyFuncBaseLoadWorking.value(x);
					System.out.println("Adding point "+x + ", " + toAdd);
					valleyFitter.addObservedPoint(1.0,x,  toAdd);
				}
				
				dataset.addSeries(newValley);
				dataset.addSeries(loadFitFuncFigureDataWorking);
				dataset.addSeries(loadFigureData);
				PolynomialFunction poly = valleyFitter.fit();
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
		totalCostBase=constantBaseConsumption*Main.priceBase*Main.secondsPerDay + (functionIntegrator.integrate(polyFuncBaseLoadWorking, 0, Main.secondsPerDay)-constantBaseConsumption*Main.secondsPerDay)*Main.pricePeak;
		
		totalCostPHEV=(-newBaseConsumption*Main.secondsPerDay + (functionIntegrator.integrate(polyFuncBaseLoadWorking, 0,Main.secondsPerDay)+valleyInt))*Main.pricePeak + newBaseConsumption*Main.secondsPerDay*Main.priceBase;
		
		probDensityFunctions=ProbFuncArray;
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
	public void findProbabilityRanges() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		//row - corresponds to entry in probDensityFunction
		// column 1= start column 2 = end
		double[][] ranges=new double[probDensityFunctions.size()][2];
		ranges[0][0]=0.0;
		ranges[probDensityFunctions.size()-1][1]=1.0;
		
		for (int i=0; i<probDensityFunctions.size()-1;i++){
			ranges[i][1]=ranges[i][0]+functionIntegrator.integrate(probDensityFunctions.get(i), valleyTimes[i][0], valleyTimes[i][1]);
			ranges[i+1][0]=ranges[i][0]+functionIntegrator.integrate(probDensityFunctions.get(i), valleyTimes[i][0], valleyTimes[i][1]);
		}
		probDensityRanges= ranges;
	}	
	
	
	public double[][] getSlotLoad(){
		return slotBaseLoad;
	}
	
	
	public double getNewBaseConsumption(){
		return newBaseConsumption;
	}
	

	public void setProbabilityDensityFunction(PolynomialFunction p){
		probDensityFunctions= new ArrayList<PolynomialFunction>(0);
		probDensityFunctions.add(p);
	}
	
	
	public void setProbabilityRanges(double [][] newRanges){
		probDensityRanges=newRanges;
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
		    out.write(polyFuncBaseLoadWorking.toString() +"\n \n");
		    
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
		    	out.write("Valley"+i+" Probability Density Function: "+ probDensityFunctions.get(i).toString() +"\n ");
		    	out.write("\n \t Starting Probability: " +probDensityRanges[i][0]+ "; Highest Probability: "+probDensityRanges[i][1]+"\n \n");
			    
		    }
		    
		   
		    
		    out.write("BasePrice: "+Main.priceBase+"\n");
		    out.write("PeakPrice: "+Main.pricePeak+"\n");
		    out.write("Total Costs before "+ totalCostBase+" \n \n");
		    out.write("Total Costs after"+ totalCostPHEV+" \n \n");
		    
		    out.write("Cost difference divided by number of PHEV users: "+((totalCostPHEV-totalCostBase)/penetration)+"\n");
		    //Close the output stream
		    out.close();
		    }catch (Exception e){}//Catch exception if any
		    
	}
	
	
	/**
	 * 
	 * @param startSecond start of Parking time in seconds
	 * @param endSecond end of Parking time in seconds
	 * @return returns the number of seconds which lie within feasible charging intervals and are within time intervals of minimum length = slotLength
	 */
	public double getFeasibleChargingTimeInInterval(double startSecond, double endSecond, double[][] valleys2ComareTo){
		
		double totalFeasibleTime=0;
		if(startSecond>endSecond){
			// recursive call, only if overnight parking
			totalFeasibleTime+=getFeasibleChargingTimeInInterval(0, endSecond,valleys2ComareTo);
			totalFeasibleTime+=getFeasibleChargingTimeInInterval(startSecond, Main.secondsPerDay,valleys2ComareTo);
		}
		else{
			// Loop over all valleys
			// if start and end second are within valley
			for (int i=0; i<valleys2ComareTo.length; i++){
				if (valleys2ComareTo[i][0]<=startSecond && valleys2ComareTo[i][1]>=startSecond){
					//if startSecond is within  interval
					if (valleys2ComareTo[i][0]<=endSecond && valleys2ComareTo[i][1]>=endSecond){
						//if endSecond is within  interval
						if (endSecond-startSecond>=Main.slotLength){
							totalFeasibleTime=endSecond-startSecond;
							
						}
						
					}
					// if start second within but endsecond after than valleyEnd
					else{
						if (valleys2ComareTo[i][1]-startSecond>=Main.slotLength){
							totalFeasibleTime=valleys2ComareTo[i][1]-startSecond;
						}
					}
				}
				// if end second is within valley
				if (valleys2ComareTo[i][0]<=endSecond && valleys2ComareTo[i][1]>=endSecond && startSecond<valleys2ComareTo[i][0]){
					if (endSecond-valleys2ComareTo[i][0]>=Main.slotLength){
						totalFeasibleTime=endSecond-valleys2ComareTo[i][0];
					}
					
				}
				
				if (valleys2ComareTo[i][1]<endSecond && valleys2ComareTo[i][0]>startSecond){
					if (valleys2ComareTo[i][1]-valleys2ComareTo[i][0]>=Main.slotLength){
						totalFeasibleTime=valleys2ComareTo[i][1]-valleys2ComareTo[i][0];
					}
				}
			}
		}	
		return totalFeasibleTime;
	}

	
	
public ArrayList<double[][]> getFeasibleChargingIntervalInParkingInterval(ArrayList<double[][]> list, double startSecond, double endSecond, double[][] valleys2ComareTo){
		
		if(startSecond>endSecond){
			// recursive call, only if overnight parking
			list =getFeasibleChargingIntervalInParkingInterval(list, 0, endSecond,valleys2ComareTo);
			list =getFeasibleChargingIntervalInParkingInterval(list, startSecond, Main.secondsPerDay,valleys2ComareTo);
			
		}
		else{
			// Loop over all valleys
			// if start and end second are within valley
			for (int i=0; i<valleys2ComareTo.length; i++){
				if (valleys2ComareTo[i][0]<=startSecond && valleys2ComareTo[i][1]>=startSecond){
					//if startSecond is within  interval
					if (valleys2ComareTo[i][0]<=endSecond && valleys2ComareTo[i][1]>=endSecond){
						//if endSecond is within  interval
						if (endSecond-startSecond>=Main.slotLength){
							list.add(new double[][] {{startSecond, endSecond}});
							//totalFeasibleTime=endSecond-startSecond;
							
						}
						
					}
					// if start second within but endsecond after than valleyEnd
					else{
						if (valleys2ComareTo[i][1]-startSecond>=Main.slotLength){
							list.add(new double[][] {{startSecond, valleys2ComareTo[i][1]}});
							//totalFeasibleTime=valleys2ComareTo[i][1]-startSecond;
						}
					}
				}
				// if end second is within valley
				if (valleys2ComareTo[i][0]<=endSecond && valleys2ComareTo[i][1]>=endSecond && startSecond<valleys2ComareTo[i][0]){
					if (endSecond-valleys2ComareTo[i][0]>=Main.slotLength){
						list.add(new double[][] {{valleys2ComareTo[i][0], endSecond}});
						//totalFeasibleTime=endSecond-valleys2ComareTo[i][0];
					}
					
				}
				
				if (valleys2ComareTo[i][1]<endSecond && valleys2ComareTo[i][0]>startSecond){
					if (valleys2ComareTo[i][1]-valleys2ComareTo[i][0]>=Main.slotLength){
						list.add(new double[][] {{valleys2ComareTo[i][0], valleys2ComareTo[i][1]}});
						//totalFeasibleTime=valleys2ComareTo[i][1]-valleys2ComareTo[i][0];
					}
				}
			}
		}	
		return list;
	}

	


	/**
	 * 
	 * @param parkingTimesCurrentAgent pass the double [][] of parkingTimes of current Agent to pick suitable charging Times from these
	 * @param noOfSlotsToBook number of slots to book within valleys
	 * @param slotsInPeakTime number of slots to book within other times
	 * @return returns a double array with start and end charging times for agent
	 * @throws IllegalArgumentException 
	 * @throws FunctionEvaluationException 
	 * @throws MaxIterationsExceededException 
	 */
	public double[][] bookSlots(double[][] parkingTimesCurrentAgent, int noOfSlotsToBook, int slotsInPeakTime) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		//assumes  small number of slots in peakTime --> only one longer charging slot for all nonValleyChargingTimes
		double[][] chargingTimes;
		if (slotsInPeakTime==0)
		{
			chargingTimes = new double[noOfSlotsToBook][2];
		}
		else{
			chargingTimes = new double[noOfSlotsToBook+slotsInPeakTime][2];
		}
		
		
		//book valleySlots
		for (int i=0; i< noOfSlotsToBook;i++){
			boolean slotFound=false;
			double [][] trySlot;
			while (slotFound==false){
				double rand= Math.random();
				trySlot= returnRandomChargingSlot(rand);
				// check if within parkingTimes 
				 if (checkIfSlotWithinParkingTimeOfAgent(trySlot, parkingTimesCurrentAgent)){
					 //check if not overlapping with existing slots
					 if (checkForOverlappingSlots( trySlot, chargingTimes, i)){
						 chargingTimes[i][0]=trySlot[0][0];
						 chargingTimes[i][1]=trySlot[0][1];
						 slotFound=true;
					 }
				 }
			}
		}
		
		// peakSlotstoBook 
		double [][]parkAndPeakTimesCurrentAgent=getParkAndPeakTimesCurrentAgent(parkingTimesCurrentAgent);
		
		
		for (int i=noOfSlotsToBook; i< noOfSlotsToBook+slotsInPeakTime;i++){
			boolean slotFound=false;
			double [][] trySlot=new double [1][2];
			while (slotFound==false){
				double rand= Math.random();
				trySlot[0][0]=rand*(Main.secondsPerDay-Main.slotLength);
				trySlot[0][1]=rand*(Main.secondsPerDay-Main.slotLength)+Main.slotLength;
					
				// check if within parkAndPeakTimes 
				 if (checkIfSlotWithinParkingTimeOfAgent(trySlot, parkAndPeakTimesCurrentAgent)){
					 //check if not overlapping with existing slots
					 if (checkForOverlappingSlots( trySlot, chargingTimes, i)){
						 chargingTimes[i][0]=trySlot[0][0];
						 chargingTimes[i][1]=trySlot[0][1];
						 slotFound=true;
					 }
				 }
			}
		}
				
		return chargingTimes;
	}
	
	
	
	
	/**
	 * books slots in all times where agent parks within valleys and books "slotsInPeakTime" additional slots in peak time
	 * @param parkingTimesCurrentAgent
	 * @param valleys2ComareTo
	 * @param slotsInPeakTime
	 * @return
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 */
	public double[][] bookSlotsAll(double[][] parkingTimesCurrentAgent, double [][] valleys2ComareTo, int slotsInPeakTime) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException {
		
		ArrayList<double[][]> list= new ArrayList<double [][]>(0);
		for (int i=0; i<parkingTimesCurrentAgent.length; i++){
			list=getFeasibleChargingIntervalInParkingInterval(list, parkingTimesCurrentAgent[i][0], parkingTimesCurrentAgent[i][1], valleys2ComareTo);

		}
		
		// create double for charging times with space for All Valley Times and peakTimeSlot
		double[][] chargingTimes;
		if (slotsInPeakTime==0)
		{
			chargingTimes = new double[list.size()][2];
		}
		else{
			chargingTimes = new double[list.size()+slotsInPeakTime][2];
			}
		
		// copy entries from list with parking times within valleys to charging TImes
		for (int i=0; i<list.size(); i++){
			
			double [][] entry=list.get(i);
			for (int j=0; j<entry.length; j++){
				chargingTimes[i][0]=entry[j][0];
				chargingTimes[i][1]=entry[j][1];
			}
		}
		
		
		double [][]parkAndPeakTimesCurrentAgent=getParkAndPeakTimesCurrentAgent(parkingTimesCurrentAgent);
		
		
		for (int i=list.size(); i< list.size()+slotsInPeakTime;i++){
			boolean slotFound=false;
			double [][] trySlot=new double [1][2];
			while (slotFound==false){
				double rand= Math.random();
				trySlot[0][0]=(int)Math.round(rand*(Main.secondsPerDay-Main.slotLength));
				trySlot[0][1]=trySlot[0][0]+(int)Main.slotLength;
				
				// check if within parkAndpeakTimes 
				 if (checkIfSlotWithinParkingTimeOfAgent(trySlot, parkAndPeakTimesCurrentAgent)){
					 //check if not overlapping with existing slots
					 if (checkForOverlappingSlots( trySlot, chargingTimes, i)){
						 chargingTimes[i][0]=trySlot[0][0];
						 chargingTimes[i][1]=trySlot[0][1];
						 slotFound=true;
					 }
				 }
			}
		}
		
		return chargingTimes;
	}
	
	
	
	
	/**
	 * returns a double [][] with the time intervals in peak TIme in which current agent parks
	 * @param parkingTimesCurrentAgent
	 * @return
	 */
	public double [][] getParkAndPeakTimesCurrentAgent(double [][] parkingTimesCurrentAgent){
		// identify times of parking and peak - save in parkAndPeakTimesList
		peakTimes=getPeakTimes(valleyTimes);
		ArrayList<double[][]> parkAndPeakTimesList= new ArrayList<double[][]>(0);
		for (int i=0; i<parkingTimesCurrentAgent.length; i++){
			parkAndPeakTimesList=getFeasibleChargingIntervalInParkingInterval(parkAndPeakTimesList, parkingTimesCurrentAgent[i][0], parkingTimesCurrentAgent[i][1], peakTimes);

		}
		
		// transfer ArrayList to double [][]
		double [][] parkAndPeakTimesCurrentAgent=new double[parkAndPeakTimesList.size()][2];
		
		for (int i=0; i<parkAndPeakTimesList.size(); i++){
			
			double [][] entry=parkAndPeakTimesList.get(i);
			for (int j=0; j<entry.length; j++){
				parkAndPeakTimesCurrentAgent[i][0]=entry[j][0];
				parkAndPeakTimesCurrentAgent[i][1]=entry[j][1];
			}
		}
		
		return parkAndPeakTimesCurrentAgent;
	}
	
	
	
	/**
	 * 
	 * @param trySlot - pass double [][] of slot that is tested for suitability for charging Times
	 * @param chargingTimes - double[][] of already entered charging Times
	 * @param i - integer up to  which charging Times have already been entered into chargingTimes
	 * @return return true if no overlap
	 */
	public boolean checkForOverlappingSlots(double [][] trySlot,double [][] chargingTimes, int j){
		boolean overlapCheck=true;
		for (int i=0; i<j; i++){
			if ((chargingTimes [i][0] <trySlot[0][0] && chargingTimes [i][1] >trySlot[0][0])||  (chargingTimes [i][0] <trySlot[0][1] && chargingTimes [i][1] >trySlot[0][1])){
				// if there is an overlap, return value equals false
				overlapCheck=false;
			}
		}
		return overlapCheck;
		
	}
	
	
	
	public boolean checkIfSlotWithinParkingTimeOfAgent(double[][] trySlot, double[][] parkingTimesCurrentAgent){
		boolean checkSlot=false;
		for (int i=0; i<parkingTimesCurrentAgent.length; i++){
			if (trySlot[0][0]>=parkingTimesCurrentAgent[i][0] && trySlot[0][0]<=parkingTimesCurrentAgent[i][1] && trySlot[0][1]>parkingTimesCurrentAgent[i][0] && trySlot[0][0]<=parkingTimesCurrentAgent[i][1]){
				checkSlot=true;
			}
		}
		// special check for first overnight slot
		if (parkingTimesCurrentAgent[0][0]>parkingTimesCurrentAgent[0][1]){
			if (trySlot[0][0]<=parkingTimesCurrentAgent[0][1] && trySlot[0][1]<=parkingTimesCurrentAgent[0][1] ){
				checkSlot=true;
			}
			if (trySlot[0][0]>=parkingTimesCurrentAgent[0][0] && trySlot[0][1]<=Main.secondsPerDay){
				checkSlot=true;
			}
		}
		
		return checkSlot;
	}

	public void setValleyTimes(double [][]newValleyTimes){
		valleyTimes=newValleyTimes;
	}
	
	//
	public double[][] returnRandomChargingSlot(double randomNumber) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		double [][] slotEntry=new double [1][2];
		
		for (int i=0; i<probDensityRanges.length; i++){
			if (randomNumber>=probDensityRanges[i][0] && randomNumber<probDensityRanges[i][1]){
			
				double start=valleyTimes[i][0];
				double end=valleyTimes[i][1];
				double error=0.01; // percentage off error
				boolean run=true;
				double lowerBracket=start;
				double upperBracket=end;
				
				while (run){
					
					double integralUptoMiddle=functionIntegrator.integrate(probDensityFunctions.get(i), start, (lowerBracket+upperBracket)/2);
					
					double diff=integralUptoMiddle-(randomNumber- probDensityRanges[i][0]);
					if(Math.abs(diff)<error){
						slotEntry[0][0]=Math.round((lowerBracket+upperBracket)/2);
						slotEntry[0][1]=Math.round((lowerBracket+upperBracket)/2) + Main.slotLength;
						run=false;
						// final check if slot Fully within given Valley
						if (slotEntry[0][1]>valleyTimes[i][1]){
							slotEntry[0][1]=valleyTimes[i][1];
							slotEntry[0][0]=valleyTimes[i][1]-Main.slotLength;
						}
						
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
		
		
		return slotEntry;
	}
	
	
}

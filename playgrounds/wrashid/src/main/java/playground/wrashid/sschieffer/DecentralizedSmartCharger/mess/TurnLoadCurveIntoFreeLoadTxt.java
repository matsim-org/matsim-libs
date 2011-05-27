package playground.wrashid.sschieffer.DecentralizedSmartCharger.mess;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.analysis.solvers.UnivariateRealSolver;
import org.apache.commons.math.analysis.solvers.UnivariateRealSolverFactory;
import org.apache.commons.math.optimization.OptimizationException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.DSC.DecentralizedSmartCharger;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.DSC.Schedule;

/**
 * this is a help class to create txt files describing the distribution of free load over a day
 * based on actual load curves
 * 
 * Parameters:
 * <li> load curve in 15 min bins [sec, % of highest load[Watt]]
 * <li>  double highest load in day [Watt]
 * <li> y intercept where the curve shall be cut to produce the txt file
 * 
 * -- can easily be changed to also take PolynomialFunction to produce this file
 * 
 * @author Stella
 *
 */
public class TurnLoadCurveIntoFreeLoadTxt {
	
	private String file;
	private PolynomialFunction func;
	
	private XYSeries loadFigureData;
	private XYSeries fittedLoadFigureData;
	
	UnivariateRealSolverFactory factory = UnivariateRealSolverFactory.newInstance();
	UnivariateRealSolver solverNewton = factory.newNewtonSolver();
	
	private double peakWattOnGrid;
	private double assumptionBase;
	
	
	public TurnLoadCurveIntoFreeLoadTxt(
			String file, 
			double peakWattOnGrid, 
			double assumptionBase,
			String outputFile) throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException, IOException{
		this.file=file;
		file= "test\\input\\playground\\wrashid\\sschieffer\\baseLoadCurve15minBinsSecLoad.txt";
		this.peakWattOnGrid=peakWattOnGrid;
		peakWattOnGrid=Math.pow(10, 6);
		this.assumptionBase=assumptionBase;
		
		readLoadFile(file,peakWattOnGrid);
		
		makeFile(assumptionBase, outputFile);
	}
	
	
	
	
	
	
	/**
	 * reads in residential load from file, makes the corresponding XYSeries for visualization purposes and
	 * fits the curve to a function
	 * 
	 * @param file
	 * @param peakWattOnGrid
	 * @throws OptimizationException
	 */
	private void readLoadFile(String file, double peakWattOnGrid) throws OptimizationException{
				
		double[][] slotBaseLoad = GeneralLib.readMatrix(96, 2, false, file);
		
		loadFigureData = new XYSeries("Baseload Data from File");
		fittedLoadFigureData = new XYSeries("Fitted baseload Data from File");
		 
		for(int i=0; i<slotBaseLoad.length; i++){
			slotBaseLoad[i][1]=slotBaseLoad[i][1]*peakWattOnGrid;
			loadFigureData.add(slotBaseLoad[i][0], slotBaseLoad[i][1]);
			
		}
				func= DecentralizedSmartCharger.fitCurve(slotBaseLoad);
		for(int i=0; i<slotBaseLoad.length; i++){
			
			fittedLoadFigureData.add(slotBaseLoad[i][0], 
					func.value(slotBaseLoad[i][0]));
			
		}
		
	}
	
	
	
	/**
	 * finds the optimal and suboptimal times according to the new passed base load 
	 * and returns the schedule 
	 * 
	 * @param assumptionBase
	 * @return
	 * @throws ConvergenceException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public void makeFile(double assumptionBase, String outputFile) throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException, IOException{
		Schedule deterministicSchedule= new Schedule();
		
		double start=0;
		
		boolean before;
		boolean after;
		
		PolynomialFunction objective= func.add(new PolynomialFunction(new double[]{-assumptionBase}));
		objective= objective.negate();
		
		create15MinBinTextFromFunction(objective, outputFile);
	}
		
		
	
	
	
	public void create15MinBinTextFromFunction(PolynomialFunction objective, String outputFile){
		
		try{
		    // Create file 
			String title=(DecentralizedSmartCharger.outputPath + outputFile);
		    FileWriter fstream = new FileWriter(title);
		    BufferedWriter out = new BufferedWriter(fstream);
		    //out.write("Penetration: "+ Main.penetrationPercent+"\n");
		    for(int sec=0; sec<DecentralizedSmartCharger.SECONDSPERDAY;){
		    	out.write(sec + "\t" + objective.value(sec)+ "\n");
		    	sec+=15*60;
		    }
		    out.close();
		    }catch (Exception e){
		    	//Catch exception if any
		    }
	}
	
}

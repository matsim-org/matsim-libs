package playground.wrashid.sschieffer.SetUp;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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
import playground.wrashid.sschieffer.DSC.DecentralizedSmartCharger;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.LoadDistributionInterval;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.Schedule;

public class LoadFileReader {

	
	static UnivariateRealSolverFactory factory = UnivariateRealSolverFactory.newInstance();
	static UnivariateRealSolver solverNewton = factory.newNewtonSolver();
	
	
	private XYSeries loadFigureData;
	private XYSeries fittedLoadFigureData;
	private PolynomialFunction func;
	
	public LoadFileReader(String file, 
			int numEntries, 
			String nameXYSeriesFromFile, 
			String nameXYSeriesFitted) throws Exception{
		
		readLoadFile(file, numEntries, nameXYSeriesFromFile, nameXYSeriesFitted);
	}
	
	
	public XYSeries getLoadFigureData(){
		return loadFigureData;
	}
	
	public XYSeries getFittedLoadFigureData(){
		return fittedLoadFigureData;
	}
	
	public PolynomialFunction getFittedFunction(){
		return func;
	}
	
	
	private void readLoadFile(String file, 
			int numEntries, 
			String nameXYSeriesFromFile, 
			String nameXYSeriesFitted) throws Exception{
		// maximum Leistung Schweizer netz an einem Tag ca. 10600MW
		
		
		double[][] slotBaseLoad = GeneralLib.readMatrix(numEntries, 2, false, file);
		
		loadFigureData = new XYSeries(nameXYSeriesFromFile);
		fittedLoadFigureData = new XYSeries(nameXYSeriesFitted);
		 
		for(int i=0; i<slotBaseLoad.length; i++){
			slotBaseLoad[i][1]=slotBaseLoad[i][1];
			loadFigureData.add(slotBaseLoad[i][0], slotBaseLoad[i][1]);
			
		}
				func= DecentralizedSmartCharger.fitCurve(slotBaseLoad);
		for(int i=0; i<slotBaseLoad.length; i++){
			
			fittedLoadFigureData.add(slotBaseLoad[i][0], 
					func.value(slotBaseLoad[i][0]));
			
		}
		
	}
	
	
	public static void create15MinBinTextFromFunction(PolynomialFunction objective, String outputPathFile){
		
		try{
		    // Create file 
			
		    FileWriter fstream = new FileWriter(outputPathFile);
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
	
	
	public static void create15MinBinTextFromSchedule(Schedule schedule, String outputPathFile){
		
		try{
		    // Create file 
			
		    FileWriter fstream = new FileWriter(outputPathFile);
		    BufferedWriter out = new BufferedWriter(fstream);
		    //out.write("Penetration: "+ Main.penetrationPercent+"\n");
		    
		    for(int timeInSchedule=0; timeInSchedule<schedule.getNumberOfEntries(); timeInSchedule++){
		    	LoadDistributionInterval lThis= (LoadDistributionInterval)schedule.timesInSchedule.get(timeInSchedule);
		    	double start= Math.ceil(lThis.getStartTime()/(15*60));
		    	double end = Math.floor(lThis.getEndTime()/(15*60));
		    	for(double sec= start*15*60.0; sec<=end*15*60.0;){
			    	out.write(sec + "\t" + lThis.getPolynomialFunction().value(sec)+ "\n");
			    	sec+=15*60;
			    }
		    }
		    out.close();
		    }catch (Exception e){
		    	//Catch exception if any
		    }
	}
	
	
	/**
	 * visualizes the fitted and original read in load data
	 * @throws IOException
	 */
	public static void visualizeTwoHubSeries(String outputPathFile,String title, String YAxis, 
			XYSeries seriesReadIn, XYSeries seriesFitted) throws IOException{
		XYSeriesCollection graph= new XYSeriesCollection();
		graph.addSeries(seriesReadIn);
		graph.addSeries(seriesFitted);
		
		JFreeChart chart = ChartFactory.createXYLineChart(title, 
				"time [s]", 
				YAxis, 
				graph, 
				PlotOrientation.VERTICAL, 
				true, true, false);
		chart.setBackgroundPaint(Color.white);
		
		final XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.gray); 
        plot.setRangeGridlinePaint(Color.gray);
		
        // read in
        plot.getRenderer().setSeriesPaint(0, Color.black);
        plot.getRenderer().setSeriesStroke(
                0, 
              
                new BasicStroke(
                    3.0f,  //float width
                    BasicStroke.CAP_ROUND, //int cap
                    BasicStroke.JOIN_ROUND, //int join
                    1.0f, //float miterlimit
                    new float[] {2.0f, 3f}, //float[] dash
                    0.0f //float dash_phase
                )
            );
     // fitted
        plot.getRenderer().setSeriesPaint(1, Color.red);
        plot.getRenderer().setSeriesStroke(
                1, 
                new BasicStroke(
                    3.0f,  //float width
                    BasicStroke.CAP_ROUND, //int cap
                    BasicStroke.JOIN_ROUND, //int join
                    1.0f, //float miterlimit
                    new float[] {2.0f, 3f}, //float[] dash
                    0.0f //float dash_phase
                )
            );
        
       
        ChartUtilities.saveChartAsPNG(new File(outputPathFile) , chart, 1000, 1000);
       
	}
	
	
	/**
	 * finds the optimal and suboptimal times from a function
	 * and returns the schedule 
	 * 
	 * @param assumptionBase
	 * @return
	 * @throws ConvergenceException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public static Schedule makeSchedule(PolynomialFunction objective) throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException, IOException{
		Schedule optSuboptSchedule= new Schedule();
		
		double start=0;
		
		boolean before;
		boolean after;
				
		if(objective.value(0)<0){
			before=false; after=false;
		}else{ before=true; after=true;}
		
		for(int i=60; i<DecentralizedSmartCharger.SECONDSPERDAY; i++){
			if (objective.value(i)<0){
				after=false;
			}else{after=true;}
			
			if(after!=before){
				//find end of interval
				double newEnd=solverNewton.solve(objective, i-60, i, i-30);
				// make interval
				LoadDistributionInterval l = new LoadDistributionInterval(
						start, newEnd, objective, before);
				optSuboptSchedule.addTimeInterval(l);
				
				// new start
				start=newEnd;
			}
			before=after;
			i+=59;// go in 60 sec steps
		}
		
		//last interval not closed yet
		LoadDistributionInterval l = new LoadDistributionInterval(
				start, DecentralizedSmartCharger.SECONDSPERDAY, 
				objective, 
				after);
		optSuboptSchedule.addTimeInterval(l);
		
		
		return optSuboptSchedule;
	}
	
	
	
	
	
	
}

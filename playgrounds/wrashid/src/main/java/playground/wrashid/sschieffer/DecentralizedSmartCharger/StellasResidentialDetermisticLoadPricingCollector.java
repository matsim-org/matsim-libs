package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
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
import playground.wrashid.lib.obj.LinkedListValueHashMap;

public class StellasResidentialDetermisticLoadPricingCollector extends DetermisticLoadPricingCollector{
	
	
	private LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution=
		new  LinkedListValueHashMap<Integer, Schedule>();
	
	private LinkedListValueHashMap<Integer, Schedule> hubPricingDistribution=
		new  LinkedListValueHashMap<Integer, Schedule>();
	
	
	private PolynomialFunction func;
	
	private XYSeries loadFigureData;
	private XYSeries fittedLoadFigureData;
	
	UnivariateRealSolverFactory factory = UnivariateRealSolverFactory.newInstance();
	UnivariateRealSolver solverNewton = factory.newNewtonSolver();
	
	private double peakWattOnGrid=Math.pow(10, 6);
	private double assumptionBase=peakWattOnGrid*0.85;
	
	
	final int pricingLevels=1;
	final private int divHubsX=2;
	final private int divHubsY=2;
	
	int numhubs= divHubsX*divHubsY;
	
	public StellasResidentialDetermisticLoadPricingCollector() throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException, IOException{
		
	}
	
	@Override
	public void setUp() throws IOException, ConvergenceException, FunctionEvaluationException, IllegalArgumentException{
		
		String file= "test\\input\\playground\\wrashid\\sschieffer\\baseLoadCurve15minBinsSecLoad.txt";
		readLoadFile(file, peakWattOnGrid);
		
		
		setUpHubLoadFromReader(numhubs);
		setUpPricingLevels();
	}
	
	
	@Override
	public LinkedListValueHashMap<Integer, Schedule> getDeterministicHubLoad(){
		return hubLoadDistribution;
	}
	
	@Override
	public LinkedListValueHashMap<Integer, Schedule> getDeterministicPriceDistribution(){
		return hubPricingDistribution;
	}
	
	
	
	
	private void  setUpHubLoadFromReader(int numhubs) throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException, IOException{
		
		for (int i=1; i<=numhubs; i++){
			hubLoadDistribution.put(i, getHubLoadSchedule());
		}
		
	}
	
	
	
	/**
	 * makes pricing levels according to the deterministic load schedule
	 * 
	 */
	private void  setUpPricingLevels(){
		
		double energyPricePerkWh=0.25;
		double standardConnectionElectricityJPerSecond= 3500; 
		double optimalPrice=energyPricePerkWh*1/1000*1/3600*standardConnectionElectricityJPerSecond;//0.25 CHF per kWh		
		double suboptimalPrice=optimalPrice*3; // cost/second  
				
		//**********************
		// DEFINE HUBS 
		//**********************
		for(Integer h: hubLoadDistribution.getKeySet()){
			
			Schedule s= hubLoadDistribution.getValue(h);
			Schedule pricing = new Schedule();
			for(int i=0; i<s.getNumberOfEntries(); i++){
				
				LoadDistributionInterval lCurrent=(LoadDistributionInterval)s.timesInSchedule.get(i);
				if (lCurrent.isOptimal()){
					pricing.addTimeInterval(new LoadDistributionInterval(
							lCurrent.getStartTime(), 
							lCurrent.getEndTime(), 
							new PolynomialFunction(new double[]{optimalPrice}), 
							true));
				}else{
						pricing.addTimeInterval(new LoadDistributionInterval(
								lCurrent.getStartTime(), 
								lCurrent.getEndTime(), 
								new PolynomialFunction(new double[]{suboptimalPrice}), 
								false));
					
				}
			}
			// save to pricing
			hubPricingDistribution.put(h, pricing);
			/*pricing.printSchedule();
			s.printSchedule();*/
		}
	}
	
	
	
	
	private Schedule getHubLoadSchedule() throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException, IOException{
		return makeSchedule(assumptionBase);
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
		// maximum Leistung Schweizer netz an einem Tag ca. 10600MW
		
		
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
	public Schedule makeSchedule(double assumptionBase) throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException, IOException{
		Schedule deterministicSchedule= new Schedule();
		
		double start=0;
		
		boolean before;
		boolean after;
		
		PolynomialFunction objective= func.add(new PolynomialFunction(new double[]{-assumptionBase}));
		objective= objective.negate();
		
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
				deterministicSchedule.addTimeInterval(l);
				
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
		deterministicSchedule.addTimeInterval(l);
		//deterministicSchedule.printSchedule();
		return deterministicSchedule;
	}
	
	
	
	/**
	 * visualizes the fitted and original read in load data
	 * @throws IOException
	 */
	private void visualize(String outputPath) throws IOException{
		XYSeriesCollection graph= new XYSeriesCollection();
		graph.addSeries(loadFigureData);
		graph.addSeries(fittedLoadFigureData);
		
		JFreeChart chart = ChartFactory.createXYLineChart("Load demand over day", 
				"time [s]", 
				"Watt", 
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
        
        String s= outputPath+ "Hub\\determisticLoadOverDayReadIn.png";
        ChartUtilities.saveChartAsPNG(new File(s) , chart, 1000, 1000);
       
	}
	
}

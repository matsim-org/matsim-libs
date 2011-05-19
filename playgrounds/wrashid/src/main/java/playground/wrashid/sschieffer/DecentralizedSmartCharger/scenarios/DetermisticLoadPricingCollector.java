package playground.wrashid.sschieffer.DecentralizedSmartCharger.scenarios;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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
import playground.wrashid.sschieffer.DecentralizedSmartCharger.DecentralizedSmartCharger;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.DomainFinder;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.LoadDistributionInterval;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.Schedule;

public class DetermisticLoadPricingCollector {
	
	private ArrayList<HubInfo> myHubInfo;
	
	private HashMap<Integer, Schedule> hubLoadDistribution=
		new  HashMap<Integer, Schedule>();
	
	private HashMap<Integer, Schedule> hubPricingDistribution=
		new  HashMap<Integer, Schedule>();
	
	
	private PolynomialFunction func;
	
	private XYSeries loadFigureData;
	private XYSeries fittedLoadFigureData;
	
	UnivariateRealSolverFactory factory = UnivariateRealSolverFactory.newInstance();
	UnivariateRealSolver solverNewton = factory.newNewtonSolver();
	

	
	public DetermisticLoadPricingCollector(	ArrayList<HubInfo> myHubInfo) throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException, IOException{
			this.myHubInfo=myHubInfo;
	}
	
	
	public void setUp() throws IOException, ConvergenceException, FunctionEvaluationException, IllegalArgumentException{
		for(int hub=0;hub< myHubInfo.size();hub++){
			int hubId= myHubInfo.get(hub).getId();
			
			String file= myHubInfo.get(hub).getFreeLoadTxt();
			readLoadFile(file);
			
			hubLoadDistribution.put(hubId, makeSchedule(func));
			
			setUpContinuousPricingLevels(hub, hubId);
			
			visualize(DecentralizedSmartCharger.outputPath, myHubInfo.get(hub).getId());
		}
		
	}
	
	
	public HashMap<Integer, Schedule> getDeterministicHubLoad(){
		return hubLoadDistribution;
	}
	
	
	public HashMap<Integer, Schedule> getDeterministicPriceDistribution(){
		return hubPricingDistribution;
	}
	
	
	
	
	/**
	 * sets up continuous pricing levels for each hub 
	 * in each hub the lowest load will correspond to the worst price, the highest available load will have the best price
	 * 
	 * a function is set up 
	 */
	private void  setUpContinuousPricingLevels(int h, int hubId){
					
		double priceMaxPerkWh = myHubInfo.get(h).getPriceMax();
		double priceMinPerkWh = myHubInfo.get(h).getPriceMin();
		
		double standardConnectionElectricityW= 3500.0; 
		double kWPerW= 1.0/1000.0;
		double hPerSec=1.0/3600.0;
		// CHF/Kwh*h/s*W = CHF/s
		double optimalPrice=priceMinPerkWh*kWPerW*hPerSec*standardConnectionElectricityW;		
		double suboptimalPrice=priceMaxPerkWh*kWPerW*hPerSec*standardConnectionElectricityW;	
		
		
		DomainFinder myDomain=new DomainFinder();		
	
			//max and min load in hub
			double minDomain=Double.MAX_VALUE;
			double maxDomain= -Double.MAX_VALUE;
			
			Schedule s= hubLoadDistribution.get(hubId);
			Schedule pricing = new Schedule();
			
			for(int i=0; i<s.getNumberOfEntries(); i++){				
				LoadDistributionInterval currentInterval= (LoadDistributionInterval)s.timesInSchedule.get(i);
				myDomain.setFunctionAndRange(
						currentInterval.getStartTime(), 
						currentInterval.getEndTime(), 
						currentInterval.getPolynomialFunction());
				minDomain= Math.min(myDomain.getDomainMin(), minDomain);
				maxDomain= Math.max(myDomain.getDomainMax(), maxDomain);
			}
			
			//HAVE MIN AND MAX LOAD OVER DAY NOW
			//Make pricing schedule: price= worstPrice - referenceLoad/amplitudeLoad *(worstPrice-bestPrice)
			for(int i=0; i<s.getNumberOfEntries(); i++){
				LoadDistributionInterval currentInterval= (LoadDistributionInterval)s.timesInSchedule.get(i);
				PolynomialFunction pricingFunc= 
					new PolynomialFunction (currentInterval.getPolynomialFunction().getCoefficients().clone());
				
				//pricing FUnc up to here only copy of loadDistribution
				
				// make pricing Func reference load = Load-minL
				pricingFunc=pricingFunc.add(new PolynomialFunction(new double[]{-minDomain}));
				
				// multiply lref with (worstPrice-bestPrice/dL)
				pricingFunc=pricingFunc.multiply(new PolynomialFunction(
						new double[]{(-1*(suboptimalPrice-optimalPrice)/(maxDomain-minDomain))})
						);
				
				// now add worst price				
				pricingFunc=pricingFunc.add(new PolynomialFunction(new double[]{suboptimalPrice}));
				
				pricing.addTimeInterval(new LoadDistributionInterval(
						currentInterval.getStartTime(), 
						currentInterval.getEndTime(), 
						pricingFunc, 
						currentInterval.isOptimal()));
		
			hubPricingDistribution.put(hubId, pricing);
			
		}
	}
	
	
	
	/**
	 * reads in residential load from file, makes the corresponding XYSeries for visualization purposes and
	 * fits the curve to a function
	 * 
	 * @param file
	 * @param peakWattOnGrid
	 * @throws OptimizationException
	 */
	private void readLoadFile(String file) throws OptimizationException{
		// maximum Leistung Schweizer netz an einem Tag ca. 10600MW
		
		
		double[][] slotBaseLoad = GeneralLib.readMatrix(96, 2, false, file);
		
		loadFigureData = new XYSeries("Baseload Data from File");
		fittedLoadFigureData = new XYSeries("Fitted baseload Data from File");
		 
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
	
	
	
	/**
	 * finds the optimal and suboptimal times
	 * and returns the schedule 
	 * 
	 * @param assumptionBase
	 * @return
	 * @throws ConvergenceException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public Schedule makeSchedule(PolynomialFunction objective) throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException, IOException{
		Schedule deterministicSchedule= new Schedule();
		
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
		
		/*double totalSchedule=0;
		for(int i=0; i<deterministicSchedule.getNumberOfEntries(); i++){
			LoadDistributionInterval entry= (LoadDistributionInterval)deterministicSchedule.timesInSchedule.get(i);
			
			if (entry.isOptimal()){
				totalSchedule+=DecentralizedSmartCharger.functionIntegrator.integrate(entry.getPolynomialFunction(),
						entry.getStartTime(), entry.getEndTime());
			}
		}
		//deterministicSchedule.printSchedule();
*/		return deterministicSchedule;
	}
	
	
	
	/**
	 * visualizes the fitted and original read in load data
	 * @throws IOException
	 */
	private void visualize(String outputPath, int hub) throws IOException{
		XYSeriesCollection graph= new XYSeriesCollection();
		graph.addSeries(loadFigureData);
		graph.addSeries(fittedLoadFigureData);
		
		JFreeChart chart = ChartFactory.createXYLineChart("Free Load  over day at hub " + hub, 
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
	
	public void create15MinBinTextFromFunction(PolynomialFunction objective){
		
		try{
		    // Create file 
			String title=(DecentralizedSmartCharger.outputPath + "_freeLoad15MinSec.txt");
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

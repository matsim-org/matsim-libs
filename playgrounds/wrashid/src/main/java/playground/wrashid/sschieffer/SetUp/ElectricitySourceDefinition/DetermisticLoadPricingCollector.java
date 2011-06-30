package playground.wrashid.sschieffer.SetUp.ElectricitySourceDefinition;

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
import playground.wrashid.sschieffer.DSC.DecentralizedSmartCharger;
import playground.wrashid.sschieffer.SetUp.DomainFinder;
import playground.wrashid.sschieffer.SetUp.LoadFileReader;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.LoadDistributionInterval;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.Schedule;

public class DetermisticLoadPricingCollector {
	
	private ArrayList<HubInfoDeterministic> myHubInfo;
	
	private HashMap<Integer, Schedule> hubLoadDistribution=
		new  HashMap<Integer, Schedule>();
	
	private HashMap<Integer, Schedule> hubPricingDistribution=
		new  HashMap<Integer, Schedule>();
	
	
	
	private double allHubsPriceMax,	allHubsPriceMin;
	
	
	public DetermisticLoadPricingCollector(	ArrayList<HubInfoDeterministic> myHubInfo) throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException, IOException{
			this.myHubInfo=myHubInfo;
			findHighestAndLowestPricesInAllHubs();
	}
	
	
	public double getHighestPriceKWHAllHubs(){
		return allHubsPriceMax;
	}
	
	
	public double getLowestPriceKWHAllHubs(){
		return allHubsPriceMin;
	}
	
	
	public void findHighestAndLowestPricesInAllHubs(){
		allHubsPriceMax=-(Double.MAX_VALUE);
		allHubsPriceMin= Double.MAX_VALUE;
		for(int i=0; i< myHubInfo.size(); i++){
			if (myHubInfo.get(i).getPriceMax()>allHubsPriceMax){
				allHubsPriceMax=myHubInfo.get(i).getPriceMax();
			}
			if(myHubInfo.get(i).getPriceMin()<allHubsPriceMin){
				allHubsPriceMin=myHubInfo.get(i).getPriceMin();
			}
		}
	}
	
	public ArrayList<HubInfoDeterministic> getHubInfo(){
		return myHubInfo;
	}
	
	public HashMap<Integer, Schedule> getDeterministicHubLoad(){
		return hubLoadDistribution;
	}
	
	
	public HashMap<Integer, Schedule> getDeterministicPriceDistribution(){
		return hubPricingDistribution;
	}
	
	public void setUp() throws Exception{
		
		for(int hub=0;hub< myHubInfo.size();hub++){
			int hubId= myHubInfo.get(hub).getId();
			
			String file= myHubInfo.get(hub).getDeterministicFreeLoadTxt();
			LoadFileReader deterministicFreeLoad = new LoadFileReader(file,  
					97, 
					"deterministic free load for hub "+ hubId +" from file", 
					"deterministic free load for hub "+ hubId +" fitted");
		
			hubLoadDistribution.put(hubId, 
					LoadFileReader.makeSchedule(deterministicFreeLoad.getFittedFunction()));
			setUpContinuousPricingLevels(hub, hubId);						
		}
		
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
		
		 
		double kWPerW= 1.0/1000.0;
		double hPerSec=1.0/3600.0;
		// CHF/Kwh*h/s*W = CHF/s
		double optimalPrice=priceMinPerkWh*kWPerW*hPerSec*DecentralizedSmartCharger.STANDARDCONNECTIONSWATT;		
		double suboptimalPrice=priceMaxPerkWh*kWPerW*hPerSec*DecentralizedSmartCharger.STANDARDCONNECTIONSWATT;	
		
		
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
	
	
	
	
	
	
}

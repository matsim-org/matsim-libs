package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.analysis.solvers.UnivariateRealSolver;
import org.apache.commons.math.analysis.solvers.UnivariateRealSolverFactory;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import playground.wrashid.lib.obj.LinkedListValueHashMap;

public class testPHEVPricingMessPrep extends TestCase{

	
	/*
	 * //0,142500 €/kWh - 1 hour at 1kW
	// 0,4275 1 hour at 3.5kW
	//  per second =  0,4275/(3600)
	 */
	final double optimalPrice=10; // cost/second - CAREFUL would have to implement different multipliers for high speed or regular connection
	final double suboptimalPrice=optimalPrice*3; // cost/second  
	final double gasprice=10;
	
	
	final String outputPath="C:\\Users\\stellas\\Output\\";
	
	UnivariateRealSolverFactory factory = UnivariateRealSolverFactory.newInstance();
	UnivariateRealSolver solverBisection = factory.newBisectionSolver();
	UnivariateRealSolver solverNewton = factory.newNewtonSolver();
	
	public testPHEVPricingMessPrep(){
		
	}
	
	
	
	public void testAll() throws IOException{
		System.out.println("**************************************");
		System.out.println("**************************************");
		System.out.println("SOLUTION ALL");
		
		
		System.out.println("**************************************");
		System.out.println("SOLUTION CONSTANT");
		LinkedListValueHashMap<Integer, Schedule> pricingHubDistribution=readPricingHubDistribution(optimalPrice, suboptimalPrice);//linear functions
		//with one intersect
		
			Schedule s= pricingHubDistribution.getValue(1);
			for(int j=0; j<s.getNumberOfEntries(); j++){
				
				LoadDistributionInterval l= (LoadDistributionInterval)s.timesInSchedule.get(j);
				
				PolynomialFunction func= l.getPolynomialFunction();
				
				double [] d= func.getCoefficients();
				d[0]=d[0]-gasprice;
				
				PolynomialFunction f = new PolynomialFunction(d);
				ArrayList<ChargingInterval> badIntervals= new ArrayList<ChargingInterval> ();
				checkRoot(l, f,  badIntervals);
				System.out.println("badIntervals:");
				printArrayList(badIntervals);
				String str="constant";
				str=str.concat("_interval_"+j);
				drawTwoFuncs(gasprice, f, str);
				
			}
		
		
		
		System.out.println("**************************************");
		System.out.println("SOLUTION LINEAR");
		pricingHubDistribution=readPricingHubDistribution2(optimalPrice, suboptimalPrice);//linear functions
		//with one intersect
		
			s= pricingHubDistribution.getValue(1);
			for(int j=0; j<s.getNumberOfEntries(); j++){
				
				LoadDistributionInterval l= (LoadDistributionInterval)s.timesInSchedule.get(j);
				
				PolynomialFunction func= l.getPolynomialFunction();
				
				double [] d= func.getCoefficients();
				d[0]=d[0]-gasprice;
				
				PolynomialFunction f = new PolynomialFunction(d);
				ArrayList<ChargingInterval> badIntervals= new ArrayList<ChargingInterval> ();
				checkRoot(l, f,  badIntervals);
				System.out.println("badIntervals:");
				printArrayList(badIntervals);
				String str="linear";
				str=str.concat("_interval_"+j);
				drawTwoFuncs(gasprice, f, str);
				
			}
			
			System.out.println("**************************************");
			System.out.println("SOLUTION PARABOLIC");
			pricingHubDistribution=readPricingHubDistribution3(optimalPrice, suboptimalPrice);//linear functions
			//with one intersect
			
				s= pricingHubDistribution.getValue(1);
				for(int j=0; j<s.getNumberOfEntries(); j++){
					
					LoadDistributionInterval l= (LoadDistributionInterval)s.timesInSchedule.get(j);
					
					PolynomialFunction func= l.getPolynomialFunction();
					
					double [] d= func.getCoefficients();
					d[0]=d[0]-gasprice;
					
					PolynomialFunction f = new PolynomialFunction(d);
					ArrayList<ChargingInterval> badIntervals= new ArrayList<ChargingInterval> ();
					checkRoot(l, f,  badIntervals);
					System.out.println("badIntervals:");
					printArrayList(badIntervals);
					String str="parabolic";
					str=str.concat("_interval_"+j);
					drawTwoFuncs(gasprice, f, str);
					
					// pricing function remains
					// deterministic load for badINtervals needs to be changed
					
					
					
				}
			
		
	}
	
	
	
	public void printArrayList(ArrayList<ChargingInterval> b){
		for(int i=0; i<b.size();i++){
			b.get(i).printInterval();
		}
	}
	
	
	/*
	public void testRootForSolutionCase(){
		try {
			System.out.println("**************************************");
			System.out.println("**************************************");
			System.out.println("SOLUTION CASE");
			
			final LinkedListValueHashMap<Integer, Schedule> pricingHubDistribution=readPricingHubDistribution2(optimalPrice, suboptimalPrice);//linear functions
			//with one intersect
			
			
				
				Schedule s= pricingHubDistribution.getValue(1);
				
				for(int j=0; j<s.getNumberOfEntries(); j++){
					
					LoadDistributionInterval l= (LoadDistributionInterval)s.timesInSchedule.get(j);
					
					PolynomialFunction func= l.getPolynomialFunction();
					
					double [] d= func.getCoefficients();
					d[0]=d[0]-gasprice;
					
					PolynomialFunction f = new PolynomialFunction(d);
					
					
					System.out.println("**************************************");
					System.out.println("Newton");
					try {
						double c;
						
						c = solverNewton.solve(f, l.getStartTime(), l.getEndTime());
						System.out.println("c: "+c);
						System.out.println("start: "+l.getStartTime()+", end: "+ l.getEndTime());
						
						drawTwoFuncs(gasprice, f, "solution case linear");
						
					} catch (ConvergenceException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
						
						System.out.println("convergence exception");
					} catch (FunctionEvaluationException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
						System.out.println("function eval exception");
					}
					
					
					
				}
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	
	public void testRootForMultipleSolutionCase(){
		try {
			System.out.println("**************************************");
			System.out.println("**************************************");
			System.out.println("MULTIPLE SOLUTION CASE");
		
			final LinkedListValueHashMap<Integer, Schedule> pricingHubDistribution=readPricingHubDistribution3(optimalPrice, suboptimalPrice);//linear functions
			//with one intersect
			
			
				
				Schedule s= pricingHubDistribution.getValue(1);
				
				for(int j=0; j<s.getNumberOfEntries(); j++){
					
					LoadDistributionInterval l= (LoadDistributionInterval)s.timesInSchedule.get(j);
					
					PolynomialFunction func= l.getPolynomialFunction();
					
					double [] d= func.getCoefficients();
					d[0]=d[0]-gasprice;
					
					PolynomialFunction f = new PolynomialFunction(d);
					
					
					System.out.println("**************************************");
					System.out.println("Newton");
					try {
						double c;
						
						c = solverNewton.solve(f, l.getStartTime(), l.getEndTime());
						System.out.println("c: "+c);
						System.out.println("start: "+l.getStartTime()+", end: "+ l.getEndTime());
						
						c = solverNewton.solve(f, -100, l.getEndTime());
						System.out.println("c: "+c);
						System.out.println("start: -100, end: "+ l.getEndTime());
						
						drawTwoFuncs(gasprice, f, "multiple solution case parabolic");
						
					} catch (ConvergenceException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
						
						System.out.println("convergence exception");
					} catch (FunctionEvaluationException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
						System.out.println("function eval exception");
					}
					
					
					
				}
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	
	
	public void testRootForNoSolutionCase(){
		try {
			System.out.println("**************************************");
			System.out.println("**************************************");
			System.out.println("NO SOLUTION CASE");
			
			final LinkedListValueHashMap<Integer, Schedule> pricingHubDistribution=readPricingHubDistribution(optimalPrice, suboptimalPrice);
			
			
				
				Schedule s= pricingHubDistribution.getValue(1);
				
				ArrayList<ChargingInterval> badIntervals =new ArrayList<ChargingInterval>(0);
				
				for(int j=0; j<s.getNumberOfEntries(); j++){
					
					LoadDistributionInterval l= (LoadDistributionInterval)s.timesInSchedule.get(j);
					
					PolynomialFunction func= l.getPolynomialFunction();
					
					double [] d= func.getCoefficients();
					d[0]=d[0]-gasprice;
					int degree= func.degree();
					System.out.println("degree of constant"+ degree); //0
					PolynomialFunction f = new PolynomialFunction(d);
					
					
					
					System.out.println("**************************************");
					System.out.println("Newton");
					try {
						double c;
						c = solverNewton.solve(f, l.getStartTime(), l.getEndTime());
						System.out.println("c: "+c);
						System.out.println("start: "+l.getStartTime()+", end: "+ l.getEndTime());
						
						drawTwoFuncs(gasprice, f, "no solution ");
						
					} catch (ConvergenceException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
						System.out.println("Function: "+ f.toString());
						System.out.println("convergence exception");
						drawTwoFuncs(gasprice, f, "no solution exception");
					} catch (FunctionEvaluationException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
						System.out.println("function eval exception");
					}
					
					//________________> Learning LESSON.. CONVERGENCE EXCEPTION IF LINEAR AND NO INTERSECTION WITH SOLUTION
					
				}
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	*/
	
	
	public void checkRoot(LoadDistributionInterval l, PolynomialFunction objective, ArrayList<ChargingInterval> badIntervals){
		
		
		//*********************************
		
		if(objective.degree()==0){
			//constant
			if(objective.getCoefficients()[0]>0.0){
				// then entire interval is 
				badIntervals.add(new ChargingInterval(l.getStartTime(), l.getEndTime()));
			}
			
		}else{
			if(objective.degree()==1){
				//linear
				double c;
				try {
					c = solverNewton.solve(objective, l.getStartTime(), l.getEndTime());
					System.out.println("c: "+c);
					System.out.println("start: "+l.getStartTime()+", end: "+ l.getEndTime());
					if(c<=l.getEndTime() && c>=l.getStartTime()){
						
						//contains bad interval
						
						if(objective.value((l.getStartTime()+c)/2)>0){
							badIntervals.add(new ChargingInterval(l.getStartTime(), c));
						}else{
							badIntervals.add(new ChargingInterval( c,l.getEndTime()));
						}
						
					}
				} catch (ConvergenceException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (FunctionEvaluationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				
				
			}else{
				//anything else
				ArrayList<Double> roots = new ArrayList<Double>(0);
				//loop and craziness
				for(double i=l.getStartTime(); i<=l.getEndTime(); i++){
					if(Math.abs(objective.value(i))<=optimalPrice/10){
						
						
						try {
							double c = solverNewton.solve(objective, l.getStartTime(), l.getEndTime(), i);
							System.out.println("c: "+c);
							System.out.println("start: "+l.getStartTime()+", end: "+ l.getEndTime()+ " guess "+ i);
							if(c<=l.getEndTime() && c>=l.getStartTime()){
								roots.add(c);
								
							}
						} catch (ConvergenceException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (FunctionEvaluationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					}
					
				}
				
				double start= l.getStartTime();
				boolean good;
				if(objective.value(l.getStartTime())<0)
				{good=true;}else{good=false;}
				
				
				// now clean up root array... find bad intervals
				for(int i=0; i<roots.size();i++){
					
					if(start!=roots.get(i) ){
						if(good){
							start=roots.get(i);
							good=false;
						}else{
							badIntervals.add(new ChargingInterval(start, roots.get(i)));
							start=roots.get(i);
							good=true;
						}
					}
					
				}
				if(start!=l.getEndTime()){
					//add last interval
					if(!good){
						badIntervals.add(new ChargingInterval(start, l.getEndTime()));
					}
				}
				
				
			}
			
		}
	}
	
	
	
	
	
	public LinkedListValueHashMap<Integer, Schedule> readHubs() throws IOException{
		LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution1= new  LinkedListValueHashMap<Integer, Schedule>();
		hubLoadDistribution1.put(1, makeBullshitSchedule());
		hubLoadDistribution1.put(2, makeBullshitSchedule());
		hubLoadDistribution1.put(3, makeBullshitSchedule());
		hubLoadDistribution1.put(4, makeBullshitSchedule());
		return hubLoadDistribution1;
		
	}
	
	
	public Schedule makeBullshitSchedule() throws IOException{
		
		Schedule bullShitSchedule= new Schedule();
		
		double[] bullshitCoeffs = new double[]{100*3500, 500*3500/(62490.0), 0};// 
		double[] bullshitCoeffs2 = new double[]{914742, -100*3500/(DecentralizedSmartCharger.SECONDSPERDAY-62490.0), 0};
		//62490*(100*3500)/(24*3600-62490))
		PolynomialFunction bullShitFunc= new PolynomialFunction(bullshitCoeffs);
		PolynomialFunction bullShitFunc2= new PolynomialFunction(bullshitCoeffs2);
		LoadDistributionInterval l1= new LoadDistributionInterval(
				0.0,
				62490.0,
				bullShitFunc,//p
				true//boolean
		);
		//l1.makeXYSeries();
		bullShitSchedule.addTimeInterval(l1);
		
		
		LoadDistributionInterval l2= new LoadDistributionInterval(					
				62490.0,
				DecentralizedSmartCharger.SECONDSPERDAY,
				bullShitFunc2,//p
				false//boolean
		);
		//l2.makeXYSeries();
		bullShitSchedule.addTimeInterval(l2);
		
		//bullShitSchedule.visualizeLoadDistribution("BullshitSchedule");	
		return bullShitSchedule;
	}
	
	
	
	
	public LinkedListValueHashMap<Integer, Schedule> readPricingHubDistribution(double optimal, double suboptimal) throws IOException{
		
		LinkedListValueHashMap<Integer, Schedule> pricing= readHubs();
		
		PolynomialFunction pOpt = new PolynomialFunction(new double[] {optimal});
		PolynomialFunction pSubopt = new PolynomialFunction(new double[] {suboptimal});
		
		for(Integer i: pricing.getKeySet()){
			for(int j=0; j<pricing.getValue(i).getNumberOfEntries(); j++){
				
					LoadDistributionInterval l = (LoadDistributionInterval) pricing.getValue(i).timesInSchedule.get(j);
					
					if(l.isOptimal()){
						pricing.getValue(i).timesInSchedule.set(j, 
								new LoadDistributionInterval(
								l.getStartTime(),
								l.getEndTime(), 
								pOpt, 
								true));
						
						
					}else{
						pricing.getValue(i).timesInSchedule.set(j, 
								new LoadDistributionInterval(
										l.getStartTime(),
										l.getEndTime(), 
										pSubopt, 
										false));
												
					}
				
			}
			//pricing.getValue(i).printSchedule();
		}
		return pricing;
	/*	final LinkedListValueHashMap<Integer, Schedule> pricingHubDistribution;
		final LinkedListValueHashMap<Integer, Schedule> connectivityHubDistribution;*/
		
	}
	
	
	
	
public LinkedListValueHashMap<Integer, Schedule> readPricingHubDistribution2(double optimal, double suboptimal) throws IOException{
		
		LinkedListValueHashMap<Integer, Schedule> pricing= readHubs();
		
		PolynomialFunction pOpt = new PolynomialFunction(new double[] {0,suboptimal});
		PolynomialFunction pSubopt = new PolynomialFunction(new double[] {0, optimal});
		
		for(Integer i: pricing.getKeySet()){
			for(int j=0; j<pricing.getValue(i).getNumberOfEntries(); j++){
				
					LoadDistributionInterval l = (LoadDistributionInterval) pricing.getValue(i).timesInSchedule.get(j);
					
					if(l.isOptimal()){
						pricing.getValue(i).timesInSchedule.set(j, 
								new LoadDistributionInterval(
								l.getStartTime(),
								l.getEndTime(), 
								pOpt, 
								true));
						
						
					}else{
						pricing.getValue(i).timesInSchedule.set(j, 
								new LoadDistributionInterval(
										l.getStartTime(),
										l.getEndTime(), 
										pSubopt, 
										false));
												
					}
				
			}
			//pricing.getValue(i).printSchedule();
		}
		return pricing;
	/*	final LinkedListValueHashMap<Integer, Schedule> pricingHubDistribution;
		final LinkedListValueHashMap<Integer, Schedule> connectivityHubDistribution;*/
		
	}
	
	

public LinkedListValueHashMap<Integer, Schedule> readPricingHubDistribution3(double optimal, double suboptimal) throws IOException{
	
	LinkedListValueHashMap<Integer, Schedule> pricing= readHubs();
	
	PolynomialFunction pOpt = new PolynomialFunction(new double[] {2, 100/29, 3+100/29});
	PolynomialFunction pSubopt = new PolynomialFunction(new double[] {2, 100/29, 3+100/29});
	
	for(Integer i: pricing.getKeySet()){
		for(int j=0; j<pricing.getValue(i).getNumberOfEntries(); j++){
			
				LoadDistributionInterval l = (LoadDistributionInterval) pricing.getValue(i).timesInSchedule.get(j);
				
				if(l.isOptimal()){
					pricing.getValue(i).timesInSchedule.set(j, 
							new LoadDistributionInterval(
							l.getStartTime(),
							l.getEndTime(), 
							pOpt, 
							true));
					
					
				}else{
					pricing.getValue(i).timesInSchedule.set(j, 
							new LoadDistributionInterval(
									l.getStartTime(),
									l.getEndTime(), 
									pSubopt, 
									false));
											
				}
			
		}
		//pricing.getValue(i).printSchedule();
	}
	return pricing;
/*	final LinkedListValueHashMap<Integer, Schedule> pricingHubDistribution;
	final LinkedListValueHashMap<Integer, Schedule> connectivityHubDistribution;*/
	
}



	public void drawTwoFuncs(double d, PolynomialFunction func, String str){
		
		final DrawingSupplier supplier = new DefaultDrawingSupplier();
		XYSeries series = new XYSeries("Func");
		
		for(int i=0; i<3600.0; i++){
			series.add(i, func.value(i));
		}
		
	
		
		XYSeries series1 = new XYSeries("gas");
		series1.add(0, d);
		series1.add(3600.0, d);
		
		
		
		XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);
        dataset.addSeries(series1);
        
        JFreeChart chart = ChartFactory.createXYLineChart(
        		str, "x label", "y label", dataset, 
        		PlotOrientation.VERTICAL, false, true, false);
        
        final XYPlot plot = chart.getXYPlot();
        plot.setDrawingSupplier(supplier);
    //controler.getPopulation().getPersons().keySet()
        //plot.getRenderer().setSeriesStroke(arg0, arg1)
        //setSeriesStroke(int series, Stroke stroke)
        plot.getRenderer().setSeriesPaint(0, Color.black);
        
        chart.setBackgroundPaint(Color.white);
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.gray); 
        plot.setRangeGridlinePaint(Color.gray); 
        
      
        try {
			ChartUtilities.saveChartAsPNG(new File(outputPath + str+ "name.png"), chart, 800, 600);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	
}

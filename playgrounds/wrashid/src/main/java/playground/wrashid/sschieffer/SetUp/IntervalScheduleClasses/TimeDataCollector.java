package playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.DifferentiableMultivariateVectorialOptimizer;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.SimpleVectorialValueChecker;
import org.apache.commons.math.optimization.VectorialConvergenceChecker;
import org.apache.commons.math.optimization.fitting.PolynomialFitter;
import org.apache.commons.math.optimization.general.GaussNewtonOptimizer;
import org.jfree.data.xy.XYSeries;

import playground.wrashid.sschieffer.DSC.DecentralizedSmartCharger;

/**
 * is a convenience class that 
 * <ul>
 * <li>collects point data,
 * <li>fits the point data and 
 * thus allows an easy way to visualize the data in a graph afterwards
 * 
 * </ul> 
 * only xy data is possible; to add data points, specify the entry row and state the x and y coordinate
 * e.g. row=minute 1;
 * x=second 60;
 * y=load of 1000W
 * 
 * @author Stella
 *
 */
public class TimeDataCollector {

	private DifferentiableMultivariateVectorialOptimizer optimizer;
	private VectorialConvergenceChecker checker= new SimpleVectorialValueChecker(10000,-10000);//relative tol, absolute tol
	private GaussNewtonOptimizer gaussNewtonOptimizer= new GaussNewtonOptimizer(true); 
	private PolynomialFitter fitter;
	private double[][] data;
	
	int num;
	double secBin;
	private PolynomialFunction func;//= new PolynomialFunction(new double[]{0});
	
	private XYSeries xy;
	
	
	/**
	 * pass an array with data
	 * the data should have an entry at start second.... end second
	 * in regular intervals
	 * it is assumed, that the data is distributed over an entire day
	 * 
	 * @param data
	 */
	public TimeDataCollector(double[][] data){
		this.data=data;
		this.num= data.length;
		int intervals= num-1;
		this.secBin=DecentralizedSmartCharger.SECONDSPERDAY/intervals;
		
		optimizer= new GaussNewtonOptimizer(true); //useLU - true, faster  else QR more robust
		optimizer.setMaxIterations(10000);		
		optimizer.setConvergenceChecker(checker);		
		fitter= new PolynomialFitter(20, optimizer);
	}
	
	
	
	
	public TimeDataCollector(int numberOfDataPoints){
		data= new double[numberOfDataPoints][2];
		this.num= numberOfDataPoints;
		int intervals= num-1;
		this.secBin=DecentralizedSmartCharger.SECONDSPERDAY/intervals;
		
		optimizer= new GaussNewtonOptimizer(true); //useLU - true, faster  else QR more robust
		optimizer.setMaxIterations(100000);		
		optimizer.setConvergenceChecker(checker);		
		fitter= new PolynomialFitter(20, optimizer);
	}
	

	
	private void makeXYSeries(String nameForSeries){
		
		xy  = new XYSeries(nameForSeries);
		
		for(int i=0; i<data.length; i++){
			xy.add(getXAtEntry(i), getYAtEntry(i));
		}
	}
	
	
	private void makeXYSeriesFromFunction(String nameForSeries){
		
		xy  = new XYSeries(nameForSeries);
		for(int i=0; i<data.length; i++){
			xy.add(getXAtEntry(i), func.value(getXAtEntry(i)));
		}
	}
	
	
	public void addDataPoint(int entry, double x, double y){
		data[entry][0]= x;
		data[entry][1]= y;
	}
	
	
	
	public void fitFunction() throws Exception {
		try {
			this.func= fitCurve(data);
	    } catch (Exception e) {
	        // if singular with all entries =one value e.g.0
	    	if(allDataSameValue()){
	    		this.func= new PolynomialFunction(new double[]{getYAtEntry(0)});
	    	}else{
	    		e.printStackTrace();
	    		System.out.println("Fitting data in TimeDataCollector has encountered a problem, here first few entries of the double array");
	    		for(int i=0; i< Math.min(10, data.length); i++){
	    			System.out.println(data[i][0]+", "+data[i][1]);
	    		}
	    		throw e;
	    	}
	    }
		
	}
	
	
	
	private PolynomialFunction fitCurve(double [][] data) throws OptimizationException{
		
		fitter.clearObservations();
		
		for (int i=0;i<data.length;i++){
			fitter.addObservedPoint(1.0, data[i][0], data[i][1]);
		  }		
		
		PolynomialFunction poly = fitter.fit();
		poly= excludeNAN(poly);
		return poly;
	}
	
	
	
	private PolynomialFunction excludeNAN(PolynomialFunction poly){
		double [] coeffs= poly.getCoefficients();
		for(int i=0; i<coeffs.length; i++){
			if (Double.isNaN(coeffs[i])){
				coeffs[i]=0.0;
				System.out.println("Careful: the function fit is really bad - NaN was excluded from the POlynomialFUnction");
			}
		}
		poly=new PolynomialFunction(coeffs);
		return poly;
	}
	
	
	public boolean allDataSameValue(){
		double firstEntry= getYAtEntry(0);
		
		boolean allSame=true;
		for(int i=0; i< data.length; i++){
			if (getYAtEntry(i)!=firstEntry){
				allSame=false;
			}
		}
		return allSame;
	}
	
	
	
	public Schedule reFitFunctionInIntervalsOfSchedule96Bin(Schedule timeSchedule) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, OptimizationException{
		double [][] refit;
		
		Schedule newS= new Schedule();
		
		for(int i=0; i<timeSchedule.getNumberOfEntries(); i++){
			
			PolynomialFunction fittedFunc;
			double startTime= timeSchedule.timesInSchedule.get(i).getStartTime();
			double endTime= timeSchedule.timesInSchedule.get(i).getEndTime();
			
			if(endTime-startTime>=3*secBin){
				int minAbove= (int)Math.ceil(startTime/(secBin));
				int maxBelow= (int)Math.floor(endTime/(secBin));
				
				refit= new double[maxBelow-minAbove+1][2];
				for(int entry=minAbove; entry<=maxBelow; entry++){
					if(entry==data.length){
						break;
					}
					refit[entry-minAbove][0]= getXAtEntry(entry);
					refit[entry-minAbove][1]= getYAtEntry(entry);
				}
				fittedFunc= fitCurve(refit);
			}else{
				int numBins=4;
				double alternativeBin= (endTime-startTime)/numBins;
				
				refit= new double[numBins+1][2];
				int count=0; 
				for(double time=startTime;time<=endTime; ){
					
					refit[count][0]= time;
					refit[count][1]= extrapolateValueAtTimeFromDataCollector(time);
					count++;
					time+=alternativeBin;
				}
				fittedFunc= fitCurve(refit);
			}
			
			newS.addTimeInterval(new LoadDistributionInterval(startTime, endTime, fittedFunc, true));			
		}
		return newS;
	}
	
	
	public PolynomialFunction getFunction() throws Exception{
		fitFunction();
		return this.func;
	}
	
	public double getXAtEntry(int i){
		return data[i][0];
	}
	
	public double getYAtEntry(int i){
		return data[i][1];
	}
	
	
	/**
	 * is called to extrapolate Values from COnnectivity FUnction
	 * assumes that timeDataCollector is set up in minute steps
	 * 
	 * @param time
	 * @return
	 */
	public double extrapolateValueAtTimeFromDataCollector(double time){
		// assuming 1 minute bins ie. one data point for one minute
		int minAbove= (int)Math.ceil(time/secBin);
		int minBelow= (int)Math.floor(time/secBin);
		if(minAbove==data.length){
			minAbove=data.length-1;
		}
		
		// extrapolate with linear function - f = a + b * x
		double gradient=(getYAtEntry(minAbove)-getYAtEntry(minBelow))/secBin; // rise/run
		return getYAtEntry(minBelow)+ gradient* (time-getXAtEntry(minBelow));
		
	}
	
	public XYSeries getXYSeries(String nameForSeries){
		makeXYSeries(nameForSeries);
		return xy;
	}
	
	public XYSeries getXYSeriesFromFunction(String nameForSeries){
		makeXYSeriesFromFunction(nameForSeries);
		return xy;
	}
	
	public double getSecPerBin(){
		return secBin;
	}
	
	public void increaseYEntryAtEntryByDouble(int entry, double increase){
		addDataPoint(entry, getXAtEntry(entry), getYAtEntry(entry)+increase);
		
	}
	
	
	public void increaseYEntryOf96EntryBinCollectorBetweenSecStartEnd(double start, double end, double increase){
		double first= Math.ceil(start/(secBin));
		double last= Math.floor(end/(secBin));
		if(last==data.length){
			last=data.length-1;
		}
		if(first==data.length){
			last=data.length-1;
		}
		if(last<=first){
			// do at least for the firstEntry
			int firstEntry= (int) (first);			
			increaseYEntryAtEntryByDouble(firstEntry, increase);
		}
		
		int firstEntry= (int) (first);
		for( int i=0; i< (int)(last-first); i++){			
			increaseYEntryAtEntryByDouble(firstEntry+i, increase);
		}
	}
	
	
	public void increaseYEntryOf96EntryBinCollectorBetweenSecStartEndByFunction(
			double start, double end, PolynomialFunction func){
		double first= Math.ceil(start/(secBin));
		double last= Math.floor(end/(secBin));
		
		if(last==data.length){
			last=data.length-1;
		}
		if(first==data.length){
			last=data.length-1;
		}
		if(last<=first){
			// do at least for the firstEntry
			int firstEntry= (int) (first);
			double increase= func.value(firstEntry);
			increaseYEntryAtEntryByDouble(firstEntry, increase);
		}
		
		int firstEntry= (int) (first);
		for( int i=0; i< (int)(last-first); i++){
			double increase= func.value(firstEntry+i*(secBin));
			increaseYEntryAtEntryByDouble(firstEntry+i, increase);
		}
	}
	
	
	
}

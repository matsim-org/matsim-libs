package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.OptimizationException;
import org.jfree.data.xy.XYSeries;

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

	
	private double[][] data;
	
	private PolynomialFunction func;//= new PolynomialFunction(new double[]{0});
	
	private XYSeries xy;
	
	
	public TimeDataCollector(int numberOfDataPoints){
		data= new double[numberOfDataPoints][2];
	}
	

	
	private void makeXYSeries(String nameForSeries){
		
		xy  = new XYSeries(nameForSeries);
		
		for(int i=0; i<data.length; i++){
			xy.add(getXAtEntry(i), getYAtEntry(i));
		}
	}
	
	
	public void addDataPoint(int entry, double x, double y){
		data[entry][0]= x;
		data[entry][1]= y;
	}
	
	public void fitFunction() throws OptimizationException{
		func= DecentralizedSmartCharger.myHubLoadReader.fitCurve(data);
		
	}
	
	
	public PolynomialFunction getFunction(){
		return func;
	}
	
	public double getXAtEntry(int i){
		return data[i][0];
	}
	
	public double getYAtEntry(int i){
		return data[i][1];
	}
	
	
	public XYSeries getXYSeries(String nameForSeries){
		makeXYSeries(nameForSeries);
		return xy;
	}
	
}

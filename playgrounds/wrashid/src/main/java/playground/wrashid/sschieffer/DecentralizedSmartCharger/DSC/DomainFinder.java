package playground.wrashid.sschieffer.DecentralizedSmartCharger.DSC;

import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.analysis.solvers.UnivariateRealSolver;
import org.apache.commons.math.analysis.solvers.UnivariateRealSolverFactory;

public class DomainFinder {

	
	private PolynomialFunction func, dFunc;
	
	private double domainMin, domainMax;
	private double rangeMin, rangeMax;
	
	UnivariateRealSolverFactory factory = UnivariateRealSolverFactory.newInstance();
	UnivariateRealSolver solverNewton = factory.newNewtonSolver();
	
	
	public DomainFinder(){
		
	}
	
	public DomainFinder(double rangeMin, double rangeMax, PolynomialFunction func){
		this.func=func;
		this.rangeMax=rangeMax;
		this.rangeMin=rangeMin;
		dFunc= (PolynomialFunction)func.derivative();
		findRange();
	}
	
	
	public void setFunctionAndRange(double rangeMin, double rangeMax, PolynomialFunction func){
		this.func=func;
		this.rangeMax=rangeMax;
		this.rangeMin=rangeMin;
		dFunc= (PolynomialFunction)func.derivative();
		findRange();
	}
	
	
	public double getDomainMax(){
		return domainMax;
	}
	
	
	public double getDomainMin(){
		return domainMin;
	}
	
	public double getInterval(){
		return (rangeMax-rangeMin);
	}
	
	
	
	public void findRange(){
		
		domainMin=Double.MAX_VALUE;
		domainMax= - Double.MAX_VALUE;
		
		double approxXMin=0;
		double approxXMax=0;
		for(double i=rangeMin; i<=rangeMax; ){
			double val=func.value(i);
			if(val>domainMax){
				domainMax= Math.max(val, domainMax);
				approxXMax=i;
			}
			if(val<domainMin){
				domainMin= Math.min(func.value(i), domainMin);
				approxXMin=i;
			}
			
			i+=(getInterval()/10.0);
		}
		
		//now find exact solution
		try {
			if(approxXMax==rangeMin || approxXMax==rangeMax){
				domainMax=func.value(approxXMax);
			}else{
				approxXMax=solverNewton.solve(dFunc, rangeMin, rangeMax, approxXMax);
				domainMax=func.value(approxXMax);
			}
			
			if(approxXMin==rangeMin || approxXMin==rangeMax){
				domainMin=func.value(approxXMin);
			}else{
				approxXMin=solverNewton.solve(dFunc, rangeMin, rangeMax, approxXMin);
				domainMin=func.value(approxXMin);
			}
			
		} catch (Exception e) {
			System.out.println("Exception in findRange in DomainFInder");
			e.printStackTrace();
			}
		
	}
	
	
}

package playground.wrashid.sschieffer.mess;

import org.apache.commons.math.ArgumentOutsideDomainException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.ComposableFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialSplineFunction;

public class FunctionParty {

	/**
	 * @param args
	 * @throws FunctionEvaluationException 
	 */
	public static void main(String[] args) throws FunctionEvaluationException {
		
	}
	
	
	
	
	public void splineStuff() throws FunctionEvaluationException{
		//since 3.0 but only 2.2.
		/*
		 * StepFunction step = new StepFunction(new double[]{0, 1, 2, 3, 4, 5},
				new double[]{0, 1, 1, 1, 1, 0});
		
		HarmonicOscillator(double amplitude, double omega, double phase) 
		 */
		//UnivariateRealFunction composed = new UnivariateRealFunction();
		
		
		//PolynomialFunction combinedFunc= new PolynomialFunction(new double[]{0});
		
		PolynomialFunction[] ps1= new PolynomialFunction[] {new PolynomialFunction(new double[]{1})};
		
		ComposableFunction splinecomp;
		UnivariateRealFunction splineUni = (UnivariateRealFunction) new PolynomialSplineFunction(new double[]{0, 3}, ps1);
		splinecomp= (ComposableFunction)splineUni;
		
		for(int i=0; i<=5; i++){
			System.out.println("At "+i+" splineUni has value of :"+ splineUni.value((double)i));
		}
		
		
		
		PolynomialFunction[] ps2= new PolynomialFunction[] {new PolynomialFunction(new double[]{5})};
		PolynomialSplineFunction spline2 = new PolynomialSplineFunction(new double[]{1.5, 4}, ps2);
		
		for(int i=0; i<=5; i++){
			System.out.println("At "+i+" spline 2 has value of :"+ spline2.value((double)i));
		}
		
		splinecomp=splinecomp.add(spline2);
		for(int i=0; i<=5; i++){
			System.out.println("At "+i+" combined has value of :"+ splinecomp.value((double)i));
		}
		
	}

}

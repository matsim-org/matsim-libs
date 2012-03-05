package interpolation;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math.analysis.interpolation.UnivariateRealInterpolator;



public class Test {
	
	public static void main(String[] args){
		
		double x[] = { 0.0, 1.0, 2.0 };
		double y[] = { 1.0, -1.0, 2.0};
		try {
			UnivariateRealInterpolator interpolator = new SplineInterpolator();
			UnivariateRealFunction function = interpolator.interpolate(x, y);
			double interpolationX = 0.5;
			double interpolatedY;
		
			interpolatedY = function.value( interpolationX );
			
			System.out.println("f(" + interpolationX + ") = " + interpolatedY);
		} catch (FunctionEvaluationException e) {
			e.printStackTrace();
		}
		catch(MathException me){
			me.printStackTrace();
		}
	}

}

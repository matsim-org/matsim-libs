package playground.anhorni.random;

import java.util.Random;


public class RandomFromVarDistr {
	
	private long seed = 109877L;
	private Random rnd;
	
	public RandomFromVarDistr() {
		this.rnd = new Random(this.seed);
	}
	
	public void setSeed(long seed) {
		this.seed = seed;
		this.rnd = new Random(this.seed);
	}
	
	public double getUniform(double h) {
		return this.rnd.nextDouble() * h;	
	}
	
	public double getNegLinear(double h) {
		double u = this.rnd.nextDouble();
		return h * (1 - Math.sqrt(1 - u));
	}
	
	public double getGaussian(double mean, double sigma) {
		return mean + sigma  * rnd.nextGaussian();
	}
	
	// not used in LEGO:
	// Wikipedia:  X=mu-beta ln(-ln(U)) 
//	public double getStandardGumbel(double mu, double beta, double sigma) {
//		double uniform = rnd.nextDouble();
//		
//		// ln(-ln(0)) undefined
//		while (uniform == 0.0) {
//			uniform = rnd.nextDouble();
//		}
//		// ln(0) = infinity
//		if (uniform == 1.0) {
//			return Double.MAX_VALUE;
//		}		
//		double r = mu - beta * Math.log(-Math.log(uniform));
//		//scale to sigma^2 = sigma_in: 
//		//var(aX) 	= a^2 var(X) 	= beta^2 * PI^2 /(6.0) scale to 1
//		//stdev(aX) = a * stdev(X) 	= beta * PI / sqrt(6.0) scale to 1
//		
//		// => a = sqrt(6) / (PI * beta)
//		
//		// sigma_gumbel = beta * PI / sqrt(6.0)
//		return (r * sigma * Math.sqrt(6.0) / (Math.PI * beta));
//	}
}

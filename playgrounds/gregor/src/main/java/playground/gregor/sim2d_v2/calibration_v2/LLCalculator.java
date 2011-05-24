package playground.gregor.sim2d_v2.calibration_v2;

public class LLCalculator {
	private double mse = 0;
	private int n = 0;

	synchronized public void addSE(double se) {

		this.mse += se;
		this.n++;
	}

	public double getLL() {
		double ll = -this.n/2 * Math.log(2*Math.PI/this.n*this.mse)-this.n/2;
		//		if (ll > 0) {
		//			System.out.println("LL:" + ll + "  exp(LL):" + Math.exp(ll));
		//		}
		return ll;
	}

	public double getMSE() {
		return this.mse/this.n;
	}

	public double getSamples() {
		return this.n;
	}
}

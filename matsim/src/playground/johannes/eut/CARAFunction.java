/**
 * 
 */
package playground.johannes.eut;


/**
 * @author illenberger
 * 
 */
public class CARAFunction implements ArrowPrattRiskAversionI {

	private static final double timescale = 3600.0;

	private double rho;

	public CARAFunction(double rho) {
		setRiskAversionIndex(rho);
	}

	public void setRiskAversionIndex(double rho) {
		if(rho == 0) {
//			Gbl.getLogger().warning("Zero is not allowed for rho in the CARA-Function. Changed to 0.0001.");
			this.rho = Math.pow(10, -10);
		} else
			this.rho = rho;
	}
	
	public double getRiskAversionIndex() {
		return rho;
	}
	
	public double evaluate(double travelTime) {
		return -(1 - Math.exp(rho * ((double) travelTime / timescale))) / rho;
	}

	public int getTravelTime(double value) {
		return (int) (timescale * Math.log(value * rho + 1) / rho);
	}

}

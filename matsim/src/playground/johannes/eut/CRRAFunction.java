/**
 * 
 */
package playground.johannes.eut;



/**
 * @author illenberger
 * 
 */
public class CRRAFunction implements ArrowPrattRiskAversionI {

	private static final double timescale = 60.0;
	
	private double rho;

	public CRRAFunction(double rho) {
		this.rho = rho;
	}
	
	public double getRiskAversionIndex() {
		return rho;
	}

	public void setRiskAversionIndex(double rho) {
		if(rho == -1) {
//			Gbl.getLogger().severe("-1 is not allowed for rho!");
		} else {
			this.rho = rho;
		}
		
	}	

	public double evaluate(double travelTime) {
		return Math.pow((double)travelTime/timescale, (1.0 + rho)) / (1.0 + rho);
	}

	public int getTravelTime(double value) {
		return (int) (Math.pow(value * (rho + 1), 1.0/(rho + 1)) * timescale);
	}
}

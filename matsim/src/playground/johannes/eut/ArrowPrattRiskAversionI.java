/**
 * 
 */
package playground.johannes.eut;

/**
 * @author illenberger
 *
 */
public interface ArrowPrattRiskAversionI {
	
	public void setRiskAversionIndex(double rho);
	
	public double getRiskAversionIndex();

	public double evaluate(double travelTime);
	
	public int getTravelTime(double value);
}

package floetteroed.opdyts.ntimestworoutes;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public interface TwoRoutesReplanner {

	public void setQ1(double q1);
	
	public void update(double cost1, double cost2);

	public double getRealizedQ1();

	public double getRealizedQ2();

}

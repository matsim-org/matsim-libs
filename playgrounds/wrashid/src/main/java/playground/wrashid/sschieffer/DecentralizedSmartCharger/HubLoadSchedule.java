package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class HubLoadSchedule {

	
	private String name;
	private Schedule hubSchedule;
	
	public HubLoadSchedule(String name){
		this.name=name;
		
	}
	
	/*
	 * intervals cannot overlap
	 * 
	 */
	public void addLoadInterval(double start, double end, PolynomialFunction func, boolean optimalOrNot){
		
		// check if cuts 0
		// then need to split it
		
		LoadDistributionInterval l= new LoadDistributionInterval(start, 
				end, 
				func,
				optimalOrNot);
		hubSchedule.addTimeInterval(l);
		hubSchedule.sort();
		
	}
	
	public Schedule getHubLoadSchedule(){
		return hubSchedule;
	}
}

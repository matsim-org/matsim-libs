package playground.wrashid.sschieffer.DecentralizedSmartCharger.mess;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

import playground.wrashid.sschieffer.DecentralizedSmartCharger.DSC.LoadDistributionInterval;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.DSC.Schedule;


/**
 * Convenience class to make it easier to establish your own scenarios 
 * with specific load Schedules</br>
 * 
 * example of how you can implement a new loadSchedule for a hub for a day
 * with two different Load Functions</br></br>
 * HubLoadSchedule h1= new HubLoadSchedule("Hub 1");
		
		h1.addLoadInterval(
				0.0, //start
				62490.0, //end
				new PolynomialFunction(new double[]{100*3500, 500*3500/(62490.0), 0}), 
				true);
		h1.addLoadInterval(				
				62490.0, 
				DecentralizedSmartCharger.SECONDSPERDAY,
				new PolynomialFunction(new double[]{914742, -100*3500/(DecentralizedSmartCharger.SECONDSPERDAY-62490.0), 0}), 
				false);
		
		hubLoadDistribution.put(1, h1.getHubLoadSchedule());
 * @author Stella
 *
 */
public class HubLoadSchedule {

	
	private String name;
	private Schedule hubSchedule= new Schedule();
	
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

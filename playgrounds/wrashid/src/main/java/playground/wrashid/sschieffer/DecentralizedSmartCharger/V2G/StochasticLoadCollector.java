package playground.wrashid.sschieffer.DecentralizedSmartCharger.V2G;

import java.util.HashMap;
import java.util.Set;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;

import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.DecentralizedChargingSimulation;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.DecentralizedSmartCharger;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.LoadDistributionInterval;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.Schedule;


/**
 * makes stochastic schedules for agent vehicles, hubsources and hubs in general
 * <li> hub in general 3 hours, 2 hours down, 1 hour up
 * <li> hub source - currently null
 * <li> every agent has 1*30 min source or sink
 * 
 * @author Stella
 *
 */
public class StochasticLoadCollector {
	
	private Controler controler;
	private DecentralizedChargingSimulation mySimulation;
	
	private HashMap<Integer, Schedule> stochasticHubLoadDistribution=
		new  HashMap<Integer, Schedule>();
	
	private HashMap<Integer, Schedule> stochasticHubSource=
		new  HashMap<Integer, Schedule>();
	
	private static HashMap<Id, Schedule> agentSource= 
		new HashMap<Id, Schedule>();
	
	
	
	public StochasticLoadCollector(DecentralizedChargingSimulation mySimulation){
		this.controler=mySimulation.controler;
		this.mySimulation=mySimulation;
	}
	
	
	public HashMap<Integer, Schedule> getStochasticHubLoad(){
		readStochasticHubLoad(mySimulation.mySmartCharger.myHubLoadReader.getHubKeySet());
		return stochasticHubLoadDistribution;
	}
	
	public HashMap<Id, Schedule> getStochasticAgentVehicleSources(){
		//makeAgentVehicleSource(controler);
		return null;
		//return agentSource;
	}
	
	public HashMap<Integer, Schedule> getStochasticHubSources(){
		//makeSourceHub();
		return null;
		//return stochasticHubSource;
	}
	
	
	public void makeAgentVehicleSource(Controler controler){
		
		// at random agents get 30 minutes source
		for(Id id : mySimulation.mySmartCharger.vehicles.keySet()){
			double secs= 0.5*3600;
			
			double buffer= DecentralizedSmartCharger.SECONDSPERDAY-secs;
			double startSec= Math.random()*buffer;
			
			if(Math.random()<0.5){
				
				Schedule bullShitPlus= new Schedule();
				PolynomialFunction pPlus = new PolynomialFunction(new double[] {3500.0});
				bullShitPlus.addTimeInterval(new LoadDistributionInterval(startSec, startSec+secs, pPlus, true));
				agentSource.put(id, bullShitPlus);	
			}else{
				Schedule bullShitMinus= new Schedule();
				PolynomialFunction pMinus = new PolynomialFunction(new double[] {-3500.0});
				bullShitMinus.addTimeInterval(new LoadDistributionInterval(startSec, startSec+secs, pMinus, false));
				
				agentSource.put(id, bullShitMinus);	
			}
		}
		
	}
	
	
	
	public void readStochasticHubLoad(Set<Integer> hubSet){
		
		for (Integer i: hubSet){
			
			// 4 * 15 minutes = 1 hour
			// 15 minutes in 6 hours
			double secs= 15*60.0;									
			double startSec= 0.0;
			
			Schedule bullShitStochastic= new Schedule();
			int numPpl= (int)(mySimulation.mySmartCharger.vehicles.keySet().size()*1.5);
			PolynomialFunction p1 = new PolynomialFunction(new double[] {numPpl*3500});
			PolynomialFunction p2 = new PolynomialFunction(new double[] {-numPpl*3500});
			PolynomialFunction p3 = new PolynomialFunction(new double[] {numPpl*3500});
			PolynomialFunction p4 = new PolynomialFunction(new double[] {numPpl*3500});
			
			bullShitStochastic.addTimeInterval(new LoadDistributionInterval(startSec, startSec+secs, p1, true));
			double newStart=startSec+(DecentralizedSmartCharger.SECONDSPERDAY/4);
			bullShitStochastic.addTimeInterval(new LoadDistributionInterval(newStart, newStart+secs, p2, true));
			newStart+=(DecentralizedSmartCharger.SECONDSPERDAY/4);
			bullShitStochastic.addTimeInterval(new LoadDistributionInterval(newStart, newStart+secs, p3, true));
			newStart+=(DecentralizedSmartCharger.SECONDSPERDAY/4);
			bullShitStochastic.addTimeInterval(new LoadDistributionInterval(newStart, newStart+secs, p4, true));
			
			stochasticHubLoadDistribution.put(i, bullShitStochastic);
		}
	
		
	}
	
	
	/**
	 * only hub 1 has a source of 3500 between 50000 and 62490
	 */
	public void makeSourceHub(){
		
		Schedule bullShit= new Schedule();
		PolynomialFunction p = new PolynomialFunction(new double[] {3500});
		
		bullShit.addTimeInterval(new LoadDistributionInterval(50000.0, 62490.0, p, true));
		
		stochasticHubSource.put(1, bullShit);		
		
		
	}
	
	
}

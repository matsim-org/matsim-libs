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
 * <li> hub in general 6 hours
 * <li> hub source - only hub 1 has a short interval with a hubsource
 * <li> every agent has 2 small sources
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
		makeAgentVehicleSource(controler);
		return agentSource;
	}
	
	public HashMap<Integer, Schedule> getStochasticHubSources(){
		makeSourceHub();
		return stochasticHubSource;
	}
	
	
	public void makeAgentVehicleSource(Controler controler){
		
		
		//Id
		for(Id id : controler.getPopulation().getPersons().keySet()){
			if(Math.random()<0.5){
				Schedule bullShitPlus= new Schedule();
				PolynomialFunction pPlus = new PolynomialFunction(new double[] {3500.0});
				bullShitPlus.addTimeInterval(new LoadDistributionInterval(25000, 26000.0, pPlus, true));
				
				agentSource.put(id, bullShitPlus);	
			}else{
				Schedule bullShitMinus= new Schedule();
				PolynomialFunction pMinus = new PolynomialFunction(new double[] {-3500.0});
				bullShitMinus.addTimeInterval(new LoadDistributionInterval(0, 2000.0, pMinus, true));
				
				agentSource.put(id, bullShitMinus);	
			}
		}
		
	}
	
	
	
	public void readStochasticHubLoad(Set<Integer> hubSet){
		
		
		for (Integer i: hubSet){
			
			// 6 hours of the day
			double secs= 6*60*60;
			
			double buffer= DecentralizedSmartCharger.SECONDSPERDAY-secs;
			
			double startSec= Math.random()*buffer;
			Schedule bullShitStochastic= new Schedule();
			PolynomialFunction p = new PolynomialFunction(new double[] {3500});
			
			bullShitStochastic.addTimeInterval(new LoadDistributionInterval(startSec, startSec+secs, p, true));
			
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

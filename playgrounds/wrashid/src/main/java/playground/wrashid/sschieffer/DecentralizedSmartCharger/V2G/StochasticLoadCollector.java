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

public class StochasticLoadCollector {
	
	private Controler controler;
	
	private HashMap<Integer, Schedule> stochasticHubLoadDistribution=
		new  HashMap<Integer, Schedule>();
	
	private HashMap<Integer, Schedule> stochasticHubSource=
		new  HashMap<Integer, Schedule>();
	
	private static HashMap<Id, Schedule> agentSource= 
		new HashMap<Id, Schedule>();
	
	
	
	public StochasticLoadCollector(DecentralizedChargingSimulation simulation){
		this.controler=simulation.controler;
		makeAgentVehicleSource(controler);
		readStochasticLoad(simulation.mySmartCharger.myHubLoadReader.getHubKeySet());
		makeSourceHub();
	}
	
	
	public HashMap<Integer, Schedule> getStochasticHubLoad(){
		return stochasticHubLoadDistribution;
	}
	
	public HashMap<Id, Schedule> getStochasticAgentVehicleSources(){
		return agentSource;
	}
	
	public HashMap<Integer, Schedule> getStochasticHubSources(){
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
	
	
	
	public void readStochasticLoad(Set<Integer> hubKeySet){
		
		
		for (Integer i: hubKeySet){
			
			// 3* 30min
			double secs= 3*30*60;
			
			double buffer= DecentralizedSmartCharger.SECONDSPERDAY-secs;
			
			double startSec= Math.random()*buffer;
			Schedule bullShitStochastic= new Schedule();
			PolynomialFunction p = new PolynomialFunction(new double[] {3500});
			
			bullShitStochastic.addTimeInterval(new LoadDistributionInterval(startSec, startSec+secs, p, true));
			
			stochasticHubLoadDistribution.put(i, bullShitStochastic);
		}
	
		
	}
	
	
	public void makeSourceHub(){
		
		Schedule bullShit= new Schedule();
		PolynomialFunction p = new PolynomialFunction(new double[] {3500});
		
		bullShit.addTimeInterval(new LoadDistributionInterval(50000.0, 62490.0, p, true));
		
		stochasticHubSource.put(1, bullShit);		
		
		
	}
	
	
}

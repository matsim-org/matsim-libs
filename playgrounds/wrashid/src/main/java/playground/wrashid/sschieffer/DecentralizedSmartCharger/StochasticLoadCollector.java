package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;

import playground.wrashid.lib.obj.LinkedListValueHashMap;

public class StochasticLoadCollector {
	
	private Controler controler;
	
	private LinkedListValueHashMap<Integer, Schedule> stochasticHubLoadDistribution=
		new  LinkedListValueHashMap<Integer, Schedule>();
	
	private LinkedListValueHashMap<Integer, Schedule> stochasticHubSource=
		new  LinkedListValueHashMap<Integer, Schedule>();
	
	private static LinkedListValueHashMap<Id, Schedule> agentSource= 
		new LinkedListValueHashMap<Id, Schedule>();
	
	
	
	public StochasticLoadCollector(Controler controler){
		this.controler=controler;
		makeAgentVehicleSource(controler);
		readStochasticLoad();
		makeSourceHub();
	}
	
	
	public LinkedListValueHashMap<Integer, Schedule> getStochasticHubLoad(){
		return stochasticHubLoadDistribution;
	}
	
	public LinkedListValueHashMap<Id, Schedule> getStochasticAgentVehicleSources(){
		return agentSource;
	}
	
	public LinkedListValueHashMap<Integer, Schedule> getStochasticHubSources(){
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
	
	
	
	public void readStochasticLoad(){
		
		Schedule bullShitStochastic= new Schedule();
		PolynomialFunction p = new PolynomialFunction(new double[] {3500});
		
		bullShitStochastic.addTimeInterval(new LoadDistributionInterval(0, 24*3600, p, true));
		for (int i=0; i<4; i++){
			stochasticHubLoadDistribution.put(i+1, bullShitStochastic);
		}
	
		
	}
	
	
	public void makeSourceHub(){
		
		Schedule bullShit= new Schedule();
		PolynomialFunction p = new PolynomialFunction(new double[] {3500});
		
		bullShit.addTimeInterval(new LoadDistributionInterval(50000.0, 62490.0, p, true));
		
		stochasticHubSource.put(1, bullShit);		
		
		
	}
	
	
}

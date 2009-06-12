package playground.ciarif.retailers;

import java.util.Vector;

import org.matsim.core.api.facilities.Facility;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.PlanElement;
import org.matsim.core.controler.Controler;

public class ComputeGravityModelParameters {
	
	private Vector Beta;
	private Vector Alfa;
	private Vector X;
	private Vector Prob;
	
	void computeProbability () {
		
		
		
	}
	void computeAverages () {
		
	}
	
	void computeDistance () {
		
	}
	
	void computeBetas () {
		
	}
	public void computeInitialParameters(RetailersControler retailersControler) {
		Controler controler = retailersControler;
		for (Facility f:controler.getFacilities().getFacilities().values()) {
			for (Person p:controler.getPopulation().getPersons().values()) {
				for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
					if (pe instanceof Activity) {
						Activity act = (Activity) pe;
						if (act.getType().equals("shop") && act.getFacility().getId().equals(f.getId())) {
							
						}
					}	
				}
			}
		}
		// TODO Auto-generated method stub
		
	}

}

package playground.ciarif.retailers;

import java.util.ArrayList;
import java.util.Vector;

import org.matsim.core.api.facilities.Facility;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.PlanElement;
import org.matsim.core.controler.Controler;

import cern.colt.matrix.impl.DenseDoubleMatrix2D;

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
	public void computeInitialParameters(Controler controler, DenseDoubleMatrix2D prob_zone_shop, ArrayList<Consumer> consumers ) {
		// TODO The prob_zone_shop Matrix and the one with probabilities which will be produced in this method
		// has the same number of columns. The idea is to go through all consumers and assign them the probability corresponding to 
		// the same column.
		
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
		
	}

}

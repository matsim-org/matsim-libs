package playground.ikaddoura.analysis.distance;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

public class Modus {
	
	public static final Logger logger = Logger.getLogger(Modus.class);
	private String modeName;
	private List<Double> distances;
	private SortedMap<Double,Integer> legsPerDistanceGroups;
	private double maximalDistance = 0.0;
	
	public Modus(String mode) {
		this.modeName = mode;
	}
	
	public double getMaximalDistance() {
		for(Double distance : distances){
			if (distance > maximalDistance){
				maximalDistance = distance;
			}
		}
		return maximalDistance;
	}

	public String getModeName() {
		return modeName;
	}

	public void setDistances(Population population) {
		List<Double> myList = new ArrayList<Double>();
		for (Person person : population.getPersons().values()){
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()){
				if (pE instanceof Leg){
					Leg leg = (Leg) pE;
					if (leg.getMode()==this.getModeName()){
						Double distance = leg.getRoute().getDistance();
						myList.add(distance);
					}
					else {
						// nicht das aktuell betrachtete Verkehrsmittel
					}
				}
				else {
					// nicht instanceof Leg
				}
			}
		}
		this.distances = myList;
	}

	public List<Double> getDistances() {
		return distances;
	}	
	
	public SortedMap<Double,Integer> getLegsPerDistanceGroups() {
		return legsPerDistanceGroups;
	}

	public void setLegsPerDistanceGroups(double basis, double maxDistance) {
		SortedMap<Double, Integer> legsPerDistanceGroups = new TreeMap<Double,Integer>();
		if (this.distances.isEmpty()){
			logger.error("Eine Liste aller Distanzen für den Modus "+this.getModeName()+" ist leer.");
		}
		
		if (basis <= 1){
			logger.error("Für die log. Darstellung muss eine Basis größer als 1 gewählt werden.");
		}
		
		else {	
			for (double n=basis; n <= maxDistance*basis; n = n*basis){ // je Distanzgruppe
				int counter = 0;
				for (Double distance: this.distances){
					if(distance <= n & distance > (n/basis)){ // wenn Distanz in Distanzgruppe
					counter = counter + 1; // setze Zähler eins höher
					}
					else {}
					legsPerDistanceGroups.put(n, counter);
				}
			this.legsPerDistanceGroups = legsPerDistanceGroups;
			}
		}		
	}
}
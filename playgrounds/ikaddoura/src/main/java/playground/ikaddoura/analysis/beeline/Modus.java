package playground.ikaddoura.analysis.beeline;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

public class Modus {
	
	public static final Logger logger = Logger.getLogger(Modus.class);
	private String modeName;
	private List<Double> luftlinien;
	private SortedMap<Double,Integer> legsPerLuftlinienGroups;
	private double maximalLuftlinie = 0.0;
	
	public Modus(String mode) {
		this.modeName = mode;
	}
	
	public double getMaximalLuftlinie() {
		for(Double luftlinie : luftlinien){
			if (luftlinie > maximalLuftlinie){
				maximalLuftlinie = luftlinie;
			}
		}
		return maximalLuftlinie;
	}

	public String getModeName() {
		return modeName;
	}

	public void setDistances(Population population) {
		List<Double> myList = new ArrayList<Double>();
		for (Person person : population.getPersons().values()){
			int planElementNr = 0;
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()){
				if (pE instanceof Leg){
					Leg leg = (Leg) pE;
					if (leg.getMode()==this.getModeName()){
						Activity act1 = (Activity) person.getSelectedPlan().getPlanElements().get(planElementNr-1); // vorherige Activity
						Activity act2 = (Activity) person.getSelectedPlan().getPlanElements().get(planElementNr+1); // folgende Activity
						double x1 = act1.getCoord().getX();
						double y1 = act1.getCoord().getY();
						double x2 = act2.getCoord().getX();
						double y2 = act2.getCoord().getY();
						Double luftlinie = Math.sqrt(Math.pow((x1-x2),2) + Math.pow((y1-y2),2));
						myList.add(luftlinie);
					}
					else {
						// nicht das aktuell betrachtete Verkehrsmittel
					}
				}
				else {
					// nicht instanceof Leg
				}
				planElementNr++;
			}
		}
		this.luftlinien = myList;
	}
	
	public List<Double> getLuftlinien() {
		return luftlinien;
	}
	
	public SortedMap<Double,Integer> getLegsPerLuftlinienGroups() {
		return legsPerLuftlinienGroups;
	}
	
	public void setLegsPerLuftlinienGroups(double basis, double maxDistance) {
		SortedMap<Double, Integer> legsPerLuftlinienGroups = new TreeMap<Double,Integer>();
		if (this.luftlinien.isEmpty()){
			logger.error("Eine Liste aller Distanzen für den Modus "+this.getModeName()+" ist leer.");
		}
		
		if (basis <= 1){
			logger.error("Für die log. Darstellung muss eine Basis größer als 1 gewählt werden.");
		}
		
		else {	
			for (double n=basis; n <= maxDistance*basis; n = n*basis){ // je Distanzgruppe
				int counter = 0;
				for (Double luftlinie: this.luftlinien){
					if(luftlinie <= n & luftlinie > (n/basis)){ // wenn Distanz in Distanzgruppe
					counter = counter + 1; // setze Zähler eins höher
					}
					else {}
					legsPerLuftlinienGroups.put(n, counter);
				}
			this.legsPerLuftlinienGroups = legsPerLuftlinienGroups;
			}
		}		
	}
	
}
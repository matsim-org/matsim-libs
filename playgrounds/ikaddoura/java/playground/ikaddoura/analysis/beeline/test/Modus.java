package playground.ikaddoura.analysis.beeline.test;

import java.util.ArrayList;
import java.util.List;

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
	private List<Double> distances;
	
	public Modus(String mode) {
		this.modeName = mode;
	}
	
	public String getModeName() {
		return modeName;
	}

	public void setLuftlinien(Population population) {
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

	
	public List<Double> getLuftlinien() {
		return luftlinien;
	}
	
	public List<Double> getDistances() {
		return distances;
	}

}
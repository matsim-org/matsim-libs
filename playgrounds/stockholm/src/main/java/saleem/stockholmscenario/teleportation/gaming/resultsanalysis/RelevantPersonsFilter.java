package saleem.stockholmscenario.teleportation.gaming.resultsanalysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkUtils;

public class RelevantPersonsFilter {
	private Coord origin;
	private double distance;
	public RelevantPersonsFilter(Coord origin, double distance){
		this.origin=origin;
		this.distance=distance;
	}
	public List<Person> getRelevantPersonsInArea(Population population){
		Collection<? extends Person> allpersons = population.getPersons().values();
		List<Person> relevantpersons = new ArrayList<Person>();
		for(Person person: allpersons){
			List<PlanElement> planelements = person.getSelectedPlan().getPlanElements();
			for (PlanElement planelement : planelements) {
				if (planelement instanceof Activity) {
					Activity activity = (Activity) planelement;
					Coord coord = activity.getCoord();
					if(inProximity(coord, origin)){
						relevantpersons.add(person);
						break;
					}
				}
			}
		}
		return relevantpersons;
	}
	public boolean inProximity(Coord coord, Coord origin){
		if(NetworkUtils.getEuclideanDistance(origin, coord)<distance){
			return true;
		}else{
			return false;
		}
	}
		
}

package playground.kai.conceptual.autosensingmargutls;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.population.Person;

class EffectiveMarginalUtilitiesContainer {
	private final Map<Person,Double> effectiveMarginalUtilityOfTravelTime = new HashMap<Person,Double>() ;
	private final Map<Person,Double> marginalUtilityOfDistance = new HashMap<Person,Double>() ;
	private final Map<Person,Double> marginalUtilityOfMoney = new HashMap<Person,Double>() ;
	
	private double effectiveMarginalUtilityOfTravelTimeMAX = Double.NEGATIVE_INFINITY ;
	private double effectiveMarginalUtilityOfDistanceMAX = Double.NEGATIVE_INFINITY ;

	public Map<Person,Double> getEffectiveMarginalUtilityOfTravelTime() {
		return effectiveMarginalUtilityOfTravelTime;
	}
	/**
	 * Notes:<ul>
	 * <li> One might want to pull this apart into distance cost rate and utl of money.  On the other hand, it is the _effective_
	 * value, so giving the disutility ("cost" in computer science terms) is really not so bad.  kai, nov'13
	 * </ul>
	 */
	public Map<Person,Double> getMarginalUtilityOfDistance() {
		return marginalUtilityOfDistance;
	}
	public Map<Person,Double> getMarginalUtilityOfMoney() {
		return marginalUtilityOfMoney;
	}
	public double getEffectiveMarginalUtilityOfTravelTimeMAX() {
		return effectiveMarginalUtilityOfTravelTimeMAX;
	}
	public void setEffectiveMarginalUtilityOfTravelTimeMAX(double effectiveMarginalUtilityOfTravelTimeMAX) {
		this.effectiveMarginalUtilityOfTravelTimeMAX = effectiveMarginalUtilityOfTravelTimeMAX;
	}
	public double getEffectiveMarginalUtilityOfDistanceMAX() {
		return effectiveMarginalUtilityOfDistanceMAX;
	}
	public void setEffectiveMarginalUtilityOfDistanceMAX(double effectiveMarginalUtilityOfDistanceMAX) {
		this.effectiveMarginalUtilityOfDistanceMAX = effectiveMarginalUtilityOfDistanceMAX;
	}
	
	
}
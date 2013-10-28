package playground.kai.conceptual.autosensingmargutls;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.population.Person;

class MarginalUtilitiesContainer {
	private final Map<Person,Double> effectiveMarginalUtilityOfTravelTime = new HashMap<Person,Double>() ;
	private final Map<Person,Double> marginalUtilityOfDistance = new HashMap<Person,Double>() ;
	private final Map<Person,Double> marginalUtilityOfMoney = new HashMap<Person,Double>() ;
	public Map<Person,Double> getEffectiveMarginalUtilityOfTravelTime() {
		return effectiveMarginalUtilityOfTravelTime;
	}
	/**
	 * Notes:<ul>
	 * <li> This should really be pulled apart into margUtlOfMoney and distanceCostRate. kai, oct'13
	 * </ul>
	 */
	public Map<Person,Double> getMarginalUtilityOfDistance() {
		return marginalUtilityOfDistance;
	}
	public Map<Person,Double> getMarginalUtilityOfMoney() {
		return marginalUtilityOfMoney;
	}
}
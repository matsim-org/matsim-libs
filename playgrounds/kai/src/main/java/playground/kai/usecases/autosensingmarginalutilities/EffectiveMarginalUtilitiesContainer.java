package playground.kai.usecases.autosensingmarginalutilities;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;


public class EffectiveMarginalUtilitiesContainer implements UtilityOfDistanceI, UtilityOfTtimeI {
	private final Map<Id,Double> effectiveMarginalUtilityOfTravelTime = new HashMap<Id,Double>() ;
	private final Map<Id,Double> effectiveMarginalUtilityOfDistance = new HashMap<Id,Double>() ;
	private final Map<Id,Double> marginalUtilityOfMoney = new HashMap<Id,Double>() ;
	
	private double effectiveMarginalUtilityOfTravelTimeMAX = Double.NEGATIVE_INFINITY ;
	private double effectiveMarginalUtilityOfDistanceMAX = Double.NEGATIVE_INFINITY ;

	@Override
	public double getEffectiveMarginalUtilityOfTtime( Id personId ) {
		return this.effectiveMarginalUtilityOfTravelTime.get( personId ) ;
	}
	public Double putEffectiveMarginalUtilityOfTtime( Id personId, double val ) {
		return this.effectiveMarginalUtilityOfTravelTime.put( personId, val ) ;
	}
	
	/**
	 * Notes:<ul>
	 * <li> One might want to pull this apart into distance cost rate and utl of money.  On the other hand, it is the _effective_
	 * value, so giving the disutility ("cost" in computer science terms) is really not so bad.  kai, nov'13
	 * </ul>
	 */
	public Double putMarginalUtilityOfDistance( Id personId, double val ) {
		return this.effectiveMarginalUtilityOfDistance.put( personId,  val ) ;
	}
	@Override
	public double getMarginalUtilityOfDistance( Id personId ) {
		return this.effectiveMarginalUtilityOfDistance.get(personId) ;
	}
	
	@Override
	public double getEffectiveMarginalUtilityOfTtimeMAX() {
		return effectiveMarginalUtilityOfTravelTimeMAX;
	}
	public void setMarginalUtilityOfTravelTimeMAX(double effectiveMarginalUtilityOfTravelTimeMAX) {
		this.effectiveMarginalUtilityOfTravelTimeMAX = effectiveMarginalUtilityOfTravelTimeMAX;
	}
	@Override
	public double getMarginalUtilityOfDistanceMAX() {
		return effectiveMarginalUtilityOfDistanceMAX;
	}
	public void setEffectiveMarginalUtilityOfDistanceMAX(double effectiveMarginalUtilityOfDistanceMAX) {
		this.effectiveMarginalUtilityOfDistanceMAX = effectiveMarginalUtilityOfDistanceMAX;
	}

	// yyyyyy this should deliberately not return the container but just the values so that a computation rather than 
	// a lookup can be put behind the interface.
	public Double putMarginalUtilityOfMoney( Id personId, double val ) {
		return this.marginalUtilityOfMoney.put( personId, val) ;
	}
	
	public double getMarginalUtilityOfMoney(Id personId) {
		return marginalUtilityOfMoney.get(personId) ;
	}
	
	
}
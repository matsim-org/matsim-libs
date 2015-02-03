package playground.artemc.heterogeneity.routing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import playground.artemc.heterogeneity.IncomeHeterogeneity;

import java.util.HashMap;

/**
 * Created by artemc on 30/1/15.
 */
public class TravelDisutilityIncludingIncomeAndToll implements TravelDisutility{


	private static final Logger log = Logger.getLogger(TravelDisutilityIncludingIncomeAndToll.class) ;
	private static int wrnCnt = 0 ;

	private final TravelDisutility normalTravelDisutility;
	private final IncomeHeterogeneity incomeHeterogeneity;

	protected final TravelTime timeCalculator;
	private final double marginalCostOfTime;
	private final double marginalCostOfDistance;

	private HashMap<Id<Person>, Double> incomeFactors;
	private Double factorMean;


	// === start Builder ===
	public static class Builder implements TravelDisutilityFactory{

		private final IncomeHeterogeneity incomeHeterogeneity;
		private TravelDisutilityFactory previousTravelDisutilityFactory;

		public Builder( TravelDisutilityFactory previousTravelDisutilityFactory, IncomeHeterogeneity incomeHeterogeneity ) {
			this.incomeHeterogeneity = incomeHeterogeneity;
			this.previousTravelDisutilityFactory = previousTravelDisutilityFactory ;
		}

		@Override
		public TravelDisutility createTravelDisutility(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
			if (incomeHeterogeneity.getType()!="homo"){
				return new TravelDisutilityIncludingIncomeAndToll(
						                                        previousTravelDisutilityFactory.createTravelDisutility(timeCalculator, cnScoringGroup), incomeHeterogeneity, timeCalculator, cnScoringGroup);
			}
			else{
				return previousTravelDisutilityFactory.createTravelDisutility(timeCalculator, cnScoringGroup);
			}
		}

	}
	// === end Builder ===

	private TravelDisutilityIncludingIncomeAndToll(final TravelDisutility normalTravelDisutility, final IncomeHeterogeneity incomeHeterogeneity, final TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup){
		this.incomeHeterogeneity = incomeHeterogeneity;
		this.normalTravelDisutility = normalTravelDisutility;
		this.timeCalculator = timeCalculator;
		this.incomeFactors = incomeHeterogeneity.getIncomeFactors();

		/*Calculate the mean in order to adjust the utility parameters*/
		Double factorSum=0.0;

		for(Double incomeFactor:this.incomeFactors.values()){
			factorSum = factorSum + incomeFactor;
		}
		this.factorMean = factorSum / (double) incomeFactors.size();

		/* Usually, the travel-utility should be negative (it's a disutility)
		 * but the cost should be positive. Thus negate the utility.
		 */
		this.marginalCostOfTime = (- cnScoringGroup.getTraveling_utils_hr() / 3600.0)  * factorMean + (cnScoringGroup.getPerforming_utils_hr() / 3600.0) * factorMean;

//		this.marginalUtlOfDistance = cnScoringGroup.getMarginalUtlOfDistanceCar();
		this.marginalCostOfDistance = - cnScoringGroup.getMonetaryDistanceCostRateCar() * cnScoringGroup.getMarginalUtilityOfMoney() ;
		if ( wrnCnt < 1 ) {
			wrnCnt++ ;
			if ( cnScoringGroup.getMonetaryDistanceCostRateCar() > 0. ) {
				Logger.getLogger(this.getClass()).warn("Monetary distance cost rate needs to be NEGATIVE to produce the normal" +
						                                       "behavior; just found positive.  Continuing anyway.  This behavior may be changed in the future.") ;
			}
		}
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);

//		if (this.marginalCostOfDistance == 0.0) {
//			return travelTime * this.marginalCostOfTime;
//		}
		// commenting this out since we think it is not (no longer?) necessary.  kai/benjamin, jun'11

		return this.marginalCostOfTime * (1.0/ this.incomeFactors.get(person.getId())) * travelTime + this.marginalCostOfDistance * link.getLength();
	}

	@Override
	public double getLinkMinimumTravelDisutility(final Link link) {

//		if (this.marginalCostOfDistance == 0.0) {
//			return (link.getLength() / link.getFreespeed()) * this.marginalCostOfTime;
//		}
		// commenting this out since we think it is not (no longer?) necessary.  kai/benjamin, jun'11

		return (link.getLength() / link.getFreespeed()) * this.marginalCostOfTime
				       + this.marginalCostOfDistance * link.getLength();
	}
}

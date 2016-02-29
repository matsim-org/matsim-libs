package playground.artemc.heterogeneity.routing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import java.util.Random;

/**
 * Created by artemc on 27/4/15.
 */
public class TimeDistanceAndHeterogeneityBasedTravelDisutility implements TravelDisutility {


	private static final Logger log = Logger.getLogger(TimeDistanceAndHeterogeneityBasedTravelDisutility.class ) ;
	private static int noramlisationWrnCnt = 0 ;

	private final TravelTime timeCalculator;
	private final double marginalCostOfTime;
	private final double marginalCostOfDistance;
	private final double marginalUtilityOfMoney;

	private final double normalization ;
	private final double sigma ;

	private Random random;

	private double logNormalRnd;

	private Person prevPerson;

	private static int wrnCnt = 0 ;
	private static int normalisationWrnCnt = 0 ;

	// === start Builder ===
	public static class Builder implements TravelDisutilityFactory {
		private double sigma = 0. ;
		private final PlanCalcScoreConfigGroup cnScoringGroup;
		public Builder(PlanCalcScoreConfigGroup cnScoringGroup){
			this.cnScoringGroup = cnScoringGroup;
		}
		@Override
		public TimeDistanceAndHeterogeneityBasedTravelDisutility createTravelDisutility(TravelTime timeCalculator) {
			return new TimeDistanceAndHeterogeneityBasedTravelDisutility(timeCalculator, cnScoringGroup, this.sigma);
		}
		public void setSigma( double val ) {
			this.sigma = val ;
		}
	}
	// === end Builder ===

	TimeDistanceAndHeterogeneityBasedTravelDisutility(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup, double sigma){
	// this should remain private; try using the Builder or ask. kai, sep'14

		this.timeCalculator = timeCalculator;

		/* Usually, the travel-utility should be negative (it's a disutility) but the cost should be positive. Thus negate the utility.*/
		this.marginalCostOfTime = (-cnScoringGroup.getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() / 3600.0)  + (cnScoringGroup.getPerforming_utils_hr() / 3600.0) ;
		this.marginalUtilityOfMoney = cnScoringGroup.getMarginalUtilityOfMoney() ;
		this.marginalCostOfDistance = -cnScoringGroup.getModes().get(TransportMode.car).getMonetaryDistanceRate() * cnScoringGroup.getMarginalUtilityOfMoney() ;

		PlanCalcScoreConfigGroup.ModeParams params = cnScoringGroup.getModes().get( TransportMode.car ) ;
		if ( params.getMarginalUtilityOfDistance() !=  0.0 ) {
			throw new RuntimeException( "marginal utility of distance not honored for travel disutility; aborting ... (should be easy to implement)") ;
		}

		if ( wrnCnt < 1 ) {
			wrnCnt++ ;
			if ( cnScoringGroup.getModes().get(TransportMode.car).getMonetaryDistanceRate() > 0. ) {
				Logger.getLogger(this.getClass()).warn("Monetary distance cost rate needs to be NEGATIVE to produce the normal " +
						                                       "behavior; just found positive.  Continuing anyway.  This behavior may be changed in the future.") ;
			}
		}

		this.sigma = sigma ;
		if ( sigma != 0. ) {
			this.random = MatsimRandom.getLocalInstance() ;
			this.normalization = 1./Math.exp( this.sigma*this.sigma/2 );
			if ( normalisationWrnCnt < 10 ) {
				normalisationWrnCnt++ ;
				log.info(" sigma: " + this.sigma + "; resulting normalization: " + normalization ) ;
			}
		} else {
			this.normalization = 1. ;
		}

	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle)
	{

		// randomize if applicable:
		if ( sigma != 0. ) {
			if ( person != prevPerson ) {
				prevPerson = person ;

				logNormalRnd = Math.exp( sigma * random.nextGaussian() ) ;
				logNormalRnd *= normalization ;
				// this should be a log-normal distribution with sigma as the "width" parameter.   Instead of figuring out the "location"
				// parameter mu, I rather just normalize (which should be the same, see next). kai, nov'13

				/* The argument is something like this:<ul>
				 * <li> exp( mu + sigma * Z) with Z = Gaussian generates lognormal with mu and sigma.
				 * <li> The mean of this is exp( mu + sigma^2/2 ) .
				 * <li> If we set mu=0, the expectation value is exp( sigma^2/2 ) .
				 * <li> So in order to set the expectation value to one (which is what we want), we need to divide by exp( sigma^2/2 ) .
				 * </ul>
				 * Should be tested. kai, jan'14 */
			}
		} else {
			logNormalRnd = 1. ;
		}
		// end randomize

		double travelTime = this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);
		double individualMarginalCostOfTime = this.marginalCostOfTime;

		if(person.getCustomAttributes().containsKey("incomeAlphaFactor")){
			individualMarginalCostOfTime  = this.marginalCostOfTime * (Double) person.getCustomAttributes().get("incomeAlphaFactor");
		}

		double timeAndDistanceBasedTravelDisutilityForLink =  individualMarginalCostOfTime  * travelTime + logNormalRnd *this.marginalCostOfDistance * link.getLength();

		return timeAndDistanceBasedTravelDisutilityForLink;
		// sign convention: these are all costs (= disutilities), so they are all normally positive.  tollCost is positive, marginalUtilityOfMoney as well.
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return (link.getLength() / link.getFreespeed()) * this.marginalCostOfTime
				       + this.marginalCostOfDistance * link.getLength();
	}


}

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
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.vehicles.Vehicle;

import java.util.Random;

/**
 * Created by artemc on 27/4/15.
 */
public class TimeDistanceTollAndHeterogeneityBasedTravelDisutility implements TravelDisutility {


	private static final Logger log = Logger.getLogger(TimeDistanceTollAndHeterogeneityBasedTravelDisutility.class ) ;
	private static int noramlisationWrnCnt = 0 ;

	protected final TravelTime timeCalculator;
	private final double marginalCostOfTime;
	private final double marginalCostOfDistance;
	private final double marginalUtilityOfMoney;

	private final RoadPricingScheme scheme;
	private final TollRouterBehaviour tollCostHandler;


	private final double normalization ;
	private final double sigma ;

	private Random random;

	private double logNormalRnd;

	private Person prevPerson;

	private static int utlOfMoneyWrnCnt = 0 ;
	private static int normalisationWrnCnt = 0 ;

	// === start Builder ===
	public static class Builder implements TravelDisutilityFactory {
		private final RoadPricingScheme scheme;
		private double sigma = 0. ;
		private final PlanCalcScoreConfigGroup cnScoringGroup;
		public Builder(RoadPricingScheme scheme, PlanCalcScoreConfigGroup cnScoringGroup ) {
			this.scheme = scheme ;
			this.cnScoringGroup = cnScoringGroup;
		}
		@Override
		public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
			return new TimeDistanceTollAndHeterogeneityBasedTravelDisutility(timeCalculator, cnScoringGroup, this.sigma, this.scheme);
		}
		public void setSigma( double val ) {
			this.sigma = val ;
		}
	}
	// === end Builder ===

	TimeDistanceTollAndHeterogeneityBasedTravelDisutility(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup, double sigma, final RoadPricingScheme scheme)
	// this should remain private; try using the Builder or ask. kai, sep'14
	{
		this.scheme = scheme;
		this.timeCalculator = timeCalculator;

		/* Usually, the travel-utility should be negative (it's a disutility) but the cost should be positive. Thus negate the utility.*/
		this.marginalCostOfTime = (-cnScoringGroup.getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() / 3600.0)  + (cnScoringGroup.getPerforming_utils_hr() / 3600.0) ;
		this.marginalUtilityOfMoney = cnScoringGroup.getMarginalUtilityOfMoney() ;
		this.marginalCostOfDistance = -cnScoringGroup.getModes().get(TransportMode.car).getMonetaryDistanceRate() * cnScoringGroup.getMarginalUtilityOfMoney() ;

		if (RoadPricingScheme.TOLL_TYPE_DISTANCE.equals(scheme.getType())) {
			this.tollCostHandler = new DistanceTollCostBehaviour();
		} else if (scheme.getType() == RoadPricingScheme.TOLL_TYPE_AREA) {
			this.tollCostHandler = new AreaTollCostBehaviour();
			Logger.getLogger(this.getClass()).warn("area pricing is more brittle than the other toll schemes; " +
					                                       "make sure you know what you are doing.  kai, apr'13 & sep'14") ;
		} else if (scheme.getType() == RoadPricingScheme.TOLL_TYPE_CORDON) {
			this.tollCostHandler = new CordonTollCostBehaviour();
		} else if (scheme.getType() == RoadPricingScheme.TOLL_TYPE_LINK) {
			this.tollCostHandler = new LinkTollCostBehaviour();
		} else {
			throw new IllegalArgumentException("RoadPricingScheme of type \"" + scheme.getType() + "\" is not supported.");
		}

		if ( utlOfMoneyWrnCnt < 1 && this.marginalUtilityOfMoney != 1. ) {
			utlOfMoneyWrnCnt ++ ;
			Logger.getLogger(this.getClass()).warn("There are no test cases for marginalUtilityOfMoney != 1.  Please write one " +
					                                       "and delete this message.  kai, apr'13 ") ;
		}

		if ( noramlisationWrnCnt < 1 ) {
			noramlisationWrnCnt++;
			if (cnScoringGroup.getModes().get(TransportMode.car).getMonetaryDistanceRate() > 0.) {
				Logger.getLogger(this.getClass()).warn("Monetary distance cost rate needs to be NEGATIVE to produce the normal" + "behavior; just found positive.  Continuing anyway.  This behavior may be changed in the future.");
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
		double tollCost = this.tollCostHandler.getTypicalTollCost(link, time );

		return timeAndDistanceBasedTravelDisutilityForLink + tollCost*this.marginalUtilityOfMoney*logNormalRnd ;
		// sign convention: these are all costs (= disutilities), so they are all normally positive.  tollCost is positive, marginalUtilityOfMoney as well.
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return (link.getLength() / link.getFreespeed()) * this.marginalCostOfTime
				       + this.marginalCostOfDistance * link.getLength();
	}

	private interface TollRouterBehaviour {
		double getTypicalTollCost(Link link, double time);
	}

	/*package*/ class DistanceTollCostBehaviour implements TollRouterBehaviour {
		@Override
		public double getTypicalTollCost(final Link link, final double time) {
			RoadPricingSchemeImpl.Cost cost_per_m = scheme.getTypicalLinkCostInfo(link.getId(), time );
			if (cost_per_m == null) {
				return 0.0;
			}
			return cost_per_m.amount * link.getLength();
		}
	}

	private static int wrnCnt2 = 0 ;

	/*package*/ class AreaTollCostBehaviour implements TollRouterBehaviour {
		@Override
		public double getTypicalTollCost(final Link link, final double time) {
			RoadPricingSchemeImpl.Cost cost = scheme.getTypicalLinkCostInfo(link.getId(), time );
			if (cost == null) {
				return 0.0;
			}
			/* just return some really high costs for tolled links, so that still a
			 * route could be found if there is no other possibility. */
			if ( wrnCnt2 < 1 ) {
				wrnCnt2 ++ ;
				Logger.getLogger(this.getClass()).warn("at least here, the area toll does not use the true toll value. " +
						                                       "This may work anyways, but without more explanation it is not obvious to me.  kai, mar'11") ;
			}
			return 1000;
		}
	}

	/*package*/ class CordonTollCostBehaviour implements TollRouterBehaviour {
		@Override
		public double getTypicalTollCost(final Link link, final double time) {
			RoadPricingSchemeImpl.Cost cost = scheme.getTypicalLinkCostInfo(link.getId(), time );
			if (cost == null) {
				return 0.0;
			}
			return cost.amount;
		}
	}

	/* package */ class LinkTollCostBehaviour implements TollRouterBehaviour {
		@Override
		public double getTypicalTollCost(final Link link, final double time) {
			RoadPricingSchemeImpl.Cost cost = scheme.getTypicalLinkCostInfo(link.getId(), time );
			if (cost == null) {
				return 0.0;
			}
			return cost.amount;
		}
	}

}

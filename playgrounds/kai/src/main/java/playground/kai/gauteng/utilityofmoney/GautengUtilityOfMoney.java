/**
 * 
 */
package playground.kai.gauteng.utilityofmoney;

import org.matsim.api.core.v01.Id;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

import playground.kai.gauteng.roadpricingscheme.SanralTollFactor;

/**This is a comment.
 * @author nagel
 *
 */
public class GautengUtilityOfMoney implements UtilityOfMoneyI {
	
	private PlanCalcScoreConfigGroup planCalcScore;

	public GautengUtilityOfMoney( final PlanCalcScoreConfigGroup cnScoringGroup ) {
		this.planCalcScore = cnScoringGroup ;
	}

	public double getUtilityOfMoney_normally_positive(final Id personId ) {
		double valueOfTime_hr = 100 ;
		switch( SanralTollFactor.typeOf(personId) ) {
		case carWithTag:
		case carWithoutTag:
			break ;
		case commercialClassBWithTag:
		case commercialClassCWithTag:
		case commercialClassBWithoutTag:
		case commercialClassCWithoutTag:
		case busWithTag:
		case busWithoutTag:
		case taxiWithTag:
		case taxiWithoutTag:
		case extWithTag:
		case extWithoutTag:
			valueOfTime_hr = 1000.;
			break ;
		}
		final double utilityOfTravelTime_hr = 
			this.planCalcScore.getPerforming_utils_hr() - this.planCalcScore.getTraveling_utils_hr() ;
		// "performing" is normally positive
		// "traveling" is normally negative
	
		double utilityOfMoney = utilityOfTravelTime_hr / valueOfTime_hr ;
		
		return utilityOfMoney ;
	}

}

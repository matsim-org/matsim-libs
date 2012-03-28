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
public class GautengUtilityOfMoney {

	public static double getUtilityOfMoney(final Id vehicleId, final PlanCalcScoreConfigGroup cnScoringGroup) {
		double valueOfTime_hr = 100 ;
		switch( SanralTollFactor.typeOf(vehicleId) ) {
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
			valueOfTime_hr = 100.;
			break ;
		}
		final double utilityOfTime_hr = - cnScoringGroup.getPerforming_utils_hr() + cnScoringGroup.getTraveling_utils_hr() ;
		// "performing" is normally positive, but needs to be counted negative
		// "traveling" is normally negative, and needs to be counted negative
	
		double utilityOfMoney = utilityOfTime_hr / valueOfTime_hr ;
		
		return utilityOfMoney ;
	}

}

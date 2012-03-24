/**
 * 
 */
package playground.kai.gauteng.utilityofmoney;

import org.matsim.api.core.v01.Id;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

import playground.kai.gauteng.roadpricingscheme.SanralTollFactor;

/**
 * @author nagel
 *
 */
public class GautengUtilityOfMoney {

	public static double getUtilityOfMoney(final Id vehicleId, final PlanCalcScoreConfigGroup cnScoringGroup) {
		double valueOfTime_hr = 100 ;
		switch( SanralTollFactor.typeOf(vehicleId) ) {
		case carWithTag:
			break ;
		case carWithoutTag:
			break ;
		case commercialClassBWithTag:
			valueOfTime_hr = 200.;
			break ;
		case commercialClassCWithTag:
			valueOfTime_hr = 200.;
			break ;
		case commercialClassBWithoutTag:
			valueOfTime_hr = 200.;
			break ;
		case commercialClassCWithoutTag:
			valueOfTime_hr = 200.;
			break ;
		case busWithTag:
			valueOfTime_hr = 200.;
			break ;
		case busWithoutTag:
			valueOfTime_hr = 200.;
			break ;
		case taxiWithTag:
			valueOfTime_hr = 200.;
			break ;
		case taxiWithoutTag:
			valueOfTime_hr = 200.;
			break ;
		case extWithTag:
			valueOfTime_hr = 200.;
			break ;
		case extWithoutTag:
			valueOfTime_hr = 200.;
			break ;
		}
		final double utilityOfTime_hr = - cnScoringGroup.getPerforming_utils_hr() + cnScoringGroup.getTraveling_utils_hr() ;
		// "performing" is normally positive, but needs to be counted negative
		// "traveling" is normally negative, and needs to be counted negative
	
		double utilityOfMoney = utilityOfTime_hr / valueOfTime_hr ;
		
		return utilityOfMoney ;
	}

}

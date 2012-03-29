/**
 * 
 */
package playground.southafrica.gauteng.utilityofmoney;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

import playground.southafrica.gauteng.roadpricingscheme.SanralTollFactor;
import playground.southafrica.gauteng.roadpricingscheme.SanralTollFactor.Type;

/**This is a comment.
 * @author nagel
 *
 */
public class GautengUtilityOfMoney implements UtilityOfMoneyI {
	
	private PlanCalcScoreConfigGroup planCalcScore;

	public GautengUtilityOfMoney( final PlanCalcScoreConfigGroup cnScoringGroup ) {
		this.planCalcScore = cnScoringGroup ;
		for ( Type vehType : Type.values() ) {
			Logger.getLogger(this.getClass()).info( " vehType: " + vehType.toString() 
					+ "; utility of travel time savings per hr: " + getUtilityOfTravelTime_hr()
					+ "; value of travel time savings per hr: " + getValueOfTime_hr(vehType)
					+ "; => utility of money: " + getUtilityOfMoneyFromValueOfTime( getValueOfTime_hr(vehType)) ) ;
		}
	}

	public double getUtilityOfMoney_normally_positive(final Id personId ) {
		Type vehicleType = SanralTollFactor.typeOf(personId);
		double valueOfTime_hr = getValueOfTime_hr(vehicleType);
		double utilityOfMoney = getUtilityOfMoneyFromValueOfTime(valueOfTime_hr);
		
		return utilityOfMoney ;
	}

	private double getUtilityOfMoneyFromValueOfTime(double valueOfTime_hr) {
		final double utilityOfTravelTime_hr = getUtilityOfTravelTime_hr();
	
		double utilityOfMoney = utilityOfTravelTime_hr / valueOfTime_hr ;
		return utilityOfMoney;
	}

	private double getUtilityOfTravelTime_hr() {
		final double utilityOfTravelTime_hr = 
			this.planCalcScore.getPerforming_utils_hr() - this.planCalcScore.getTraveling_utils_hr() ;
		// "performing" is normally positive
		// "traveling" is normally negative
		return utilityOfTravelTime_hr;
	}

	private double getValueOfTime_hr(Type vehicleType) {
		double valueOfTime_hr = 100 ;
		switch( vehicleType ) {
		case carWithTag:
		case carWithoutTag:
			break ;
		case commercialClassBWithTag:
		case commercialClassBWithoutTag:
		case busWithTag:
		case busWithoutTag:
			valueOfTime_hr = 300. ; 
			break;
		case commercialClassCWithTag:
		case commercialClassCWithoutTag:
			valueOfTime_hr = 600. ; 
			break ;
		case taxiWithTag:
		case taxiWithoutTag:
		case extWithTag:
		case extWithoutTag:
			valueOfTime_hr = 100.;
			break ;
		}
		return valueOfTime_hr;
	}

}

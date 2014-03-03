/**
 * 
 */
package playground.southafrica.gauteng.utilityofmoney;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.vehicles.VehicleType;

import playground.southafrica.gauteng.roadpricingscheme.TollFactorI;
import playground.southafrica.gauteng.roadpricingscheme.SanralTollVehicleType;

/**
 * Calculates the utility of money from a given Value of Time (VoT). 
 * @author nagel
 * @author jwjoubert
 */
public class GautengUtilityOfMoney implements UtilityOfMoneyI {
	
	private final Logger log = Logger.getLogger(GautengUtilityOfMoney.class);
	private PlanCalcScoreConfigGroup planCalcScore;
	private final double baseValueOfTime_h;
	private final double commercialMultiplier;
	private final Scenario sc;
	
	private final TollFactorI tollFactor ;

	/**
	 * Class to calculate the marginal utility of money (beta_money) for 
	 * different vehicle types given the value of time (VoT).  
	 * @param scenario TODO
	 * @param cnScoringGroup
	 * @param baseValueOfTime_h expressed in Currency/hr that is currently (March 
	 * 2012) used for private cars, taxis and external traffic. 
	 * @param valueOfTimeMultiplier to inflate the base VoT for heavy commercial 
	 * vehicles. A multiplier of <i>half</i> this value is used for busses and
	 * smaller commercial vehicles.
	 * @param tollFactor TODO
	 */
	public GautengUtilityOfMoney(Scenario scenario, final PlanCalcScoreConfigGroup cnScoringGroup, double baseValueOfTime_h, double valueOfTimeMultiplier, TollFactorI tollFactor ) {
		this.sc = scenario;
		this.planCalcScore = cnScoringGroup ;
		log.warn("Value of Time (VoT) used as base: " + baseValueOfTime_h) ;
		log.warn("Value of Time multiplier: " + valueOfTimeMultiplier) ;
		
		this.baseValueOfTime_h = baseValueOfTime_h ;
		this.commercialMultiplier = valueOfTimeMultiplier ;
		
		this.tollFactor = tollFactor ;

		/* Old */
		log.info("-----  Using SanralTollVehicleType  -----");
		for ( SanralTollVehicleType vehType : SanralTollVehicleType.values() ) {
			log.info( String.format("%30s: mUTTS: %5.2f/hr; mVTTS: %5.0f ZAR/hr; mUoM: %5.3f/ZAR", 
					vehType.toString(), getUtilityOfTravelTime_hr(), getValueOfTime_hr_OLD(vehType), 
					getUtilityOfMoneyFromValueOfTime( getValueOfTime_hr_OLD(vehType)) ) ) ;
		}
		// (I found (the previous verison of) these logging statements _above_ setting this.baseValueOfTime etc.  In consequence, I would think that they
		// were giving wrong information.  Originally, the values came from the config file only or were
		// hard-coded.  kai, nov'13)
		
		/* New (28/01/2014). Take a random person from each subpopulation */
		Map<String, Id> map = new TreeMap<String, Id>();
		for(Id id : sc.getPopulation().getPersons().keySet()){
			String subpopulation = (String)sc.getPopulation().getPersonAttributes().getAttribute(id.toString(), sc.getConfig().plans().getSubpopulationAttributeName());
			if( subpopulation!=null && !map.containsKey(subpopulation)){
				map.put(subpopulation, id);
			}
		}
		log.info("-----  Using subpopulation  -----");
		for(String sp : map.keySet()){
			log.info( String.format("%30s: mUTTS: %5.2f/hr; mVTTS: %5.0f ZAR/hr; mUoM: %5.3f/ZAR", 
					sp, getUtilityOfTravelTime_hr(), getValueOfTime_hr( map.get(sp) ), 
					getUtilityOfMoneyFromValueOfTime( getValueOfTime_hr( map.get(sp) )))
			);
		}
	}

	@Override
	public double getMarginalUtilityOfMoney(final Id personId ) {
		
		double valueOfTime_hr ;
		String subpopulation = (String)sc.getPopulation().getPersonAttributes().getAttribute(personId.toString(), sc.getConfig().plans().getSubpopulationAttributeName());
		if ( subpopulation==null ) { // OLD
			SanralTollVehicleType vehicleType = tollFactor.typeOf(personId);
			valueOfTime_hr = getValueOfTime_hr_OLD( vehicleType ) ; 
		} else { // NEW
			valueOfTime_hr = getValueOfTime_hr(personId); 
		}
		
		double utilityOfMoney = getUtilityOfMoneyFromValueOfTime(valueOfTime_hr);
		return utilityOfMoney ;
	}

	
	private double getValueOfTime_hr(final Id personId) {
		double valueOfTime_hr = baseValueOfTime_h;
		
		/* Get the subpopulation the person belongs to. */
		Object o = sc.getPopulation().getPersonAttributes().getAttribute(personId.toString(), sc.getConfig().plans().getSubpopulationAttributeName());
		String subpopulation = null;
		if(o != null && o instanceof String){
			subpopulation = (String)o;
		} else{
			throw new RuntimeException("Expected 'String' describing subpopulation, but it was '" + o.getClass().toString() + "'");
		}

		if(subpopulation.equalsIgnoreCase("car")){
			/* Do nothing, it remains the base value. */
		} else if(subpopulation.equalsIgnoreCase("commercial")){
			valueOfTime_hr = baseValueOfTime_h * commercialMultiplier;
		} else if(subpopulation.equalsIgnoreCase("bus")){
			valueOfTime_hr = baseValueOfTime_h * Math.sqrt( commercialMultiplier );
		} else if(subpopulation.equalsIgnoreCase("taxi")){
			/* Do nothing, it remains the base value. */
		} else if(subpopulation.equalsIgnoreCase("ext")){
			/* Do nothing, it remains the base value. */
		} else{
			log.warn("Not sure what should happen with Value-of-Time for subpopulation '" + subpopulation + "'. Returning base value of " + baseValueOfTime_h);
		}
		
		return valueOfTime_hr;
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

	@Deprecated
	private double getValueOfTime_hr_OLD(SanralTollVehicleType vehicleType) {
		double valueOfTime_hr = baseValueOfTime_h ;
		switch( vehicleType ) {
		case carWithTag:
		case carWithoutTag:
			break ;
		case commercialClassBWithTag:
		case commercialClassBWithoutTag:
		case busWithTag:
		case busWithoutTag:
			//			valueOfTime_hr = baseValueOfTime_h*0.5*commercialMultiplier ; 
			// yy if someone sets the commercial multiplier to less than two, this may not work as intended. kai, nov'13
			valueOfTime_hr = baseValueOfTime_h * Math.sqrt( commercialMultiplier ) ;
			break;
		case commercialClassCWithTag:
		case commercialClassCWithoutTag:
			valueOfTime_hr = baseValueOfTime_h*commercialMultiplier ; 
			break ;
		case commercialClassAWithTag:
		case commercialClassAWithoutTag:
		case taxiWithTag:
		case taxiWithoutTag:
		case extWithTag:
		case extWithoutTag:
			valueOfTime_hr = baseValueOfTime_h;
			break ;
		default:
			throw new RuntimeException("vehicle type not implemented") ;
		}
//		if ( wrncnt<1 ) {
//			wrncnt++ ;
//			Logger.getLogger(this.getClass()).warn("Johan, commercialClassAWith/WithoutTag were not explicitly given a value of time.  I now added them "
//					+ "under the vehicle types which are getting the base value of time ... which is what they must have gotten implicitly in previous runs. "
//					+ "Could you please modify if necessary, and in any case remove this warning when things are ok.  Thanks, kai, nov'13");
//		}
//		 I think we had talked about this in dec'13. kai
		
		return valueOfTime_hr;
	}

}

package playground.southafrica.population.demographics;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.households.Income;

import playground.southafrica.population.capeTownTravelSurvey.HouseholdEnums;
import playground.southafrica.population.census2011.containers.HousingType2011;
import playground.southafrica.population.census2011.containers.Income2011;
import playground.southafrica.population.nmbmTravelSurvey.containers.IncomeTravelSurvey2004;
import playground.southafrica.utilities.RandomVariateGenerator;
import playground.southafrica.utilities.SouthAfricaInflationCorrector;

/**
 * Class to convert a given household {@link Income} into a predefined income
 * category. The income is currently (June 2014) converted to a base year of 
 * 2011, the latest census information. The categories were defined using the 
 * University of South Africa's Bureau of Market Research<a>'s <i>'Household 
 * income and expenditure patterns' classification, 
 * <a href=http://www.unisa.ac.za/contents/faculties/ems/docs/Press429.pdf>Research Report 429</a> 
 *
 * @author jwjoubert
 * @see <a href=http://www.unisa.ac.za/contents/faculties/ems/docs/Press429.pdf>Research Report 429</a> 
 */
public enum SaDemographicsIncome {
	Poor, LowMiddle, EmergingMiddle, Middle, UpperMiddle, EmergingAffluent, Affluent, Unknown;
	
	private final static Logger LOG = Logger.getLogger(SaDemographicsIncome.class);
	
	/**
	 * Returns the income class based on the annual income categories given by
	 * University of South Africa's Bureau of Market Research.
	 * 
	 * @param value
	 * @return
	 */
	private static SaDemographicsIncome getIncomeClassFromValue(double value){
		if(value < 54345.00){
			return Poor;
		} else if(value < 151728.00){
			return LowMiddle;
		} else if(value < 363931.00){
			return EmergingMiddle;
		} else if(value < 631121.00){
			return Middle;
		} else if(value < 863907.00){
			return UpperMiddle;
		} else if(value < 1329845.00){
			return EmergingAffluent;
		} else{
			return Affluent;
		}
	}
	
	
	/**
	 * The household income given in the Census 2011 data is an annual value,
	 * and is based on 2011 currency values (South African Rand, ZAR).
	 * 
	 * @param income
	 * @return
	 */
	public static SaDemographicsIncome convertCensus2011Income(Income2011 income){
		switch (income) {
		case NotApplicable:
		case Unspecified:
			return Unknown;
		default:
			double value = generateCensusIncomeValue(income);
			return getIncomeClassFromValue(value);
		}
	}
	
	
	/**
	 * The household income given in the Nelson Mandela Bay Travel Survey of 
	 * 2004 is a monthly value based on 2004 currency values (South African
	 * Rand, ZAR). The value is therefore converted using accurate inflation
	 * values to a base year of 2011.
	 * 
	 * @param income
	 * @return
	 */
	public static SaDemographicsIncome convertNmbm2004Income(IncomeTravelSurvey2004 income){
		switch (income) {
		case Unspecified:
			return Unknown;
		default:
			double value = generateNmbm2004IncomeValue(income);
			double valueBaseYear = SouthAfricaInflationCorrector.convert(value, 2004, 2011);
			return getIncomeClassFromValue(valueBaseYear);
		}
	}
	
	
	public static SaDemographicsIncome convertCapeTown2013Income(String incomeString, String assetClass2){
		HouseholdEnums.MonthlyIncome income = HouseholdEnums.MonthlyIncome.parseFromDescription(incomeString);
		HouseholdEnums.AssetClass2 class2 = HouseholdEnums.AssetClass2.parseFromDescription(assetClass2);
		
		double value = 0.0;
		switch (income) {
		case REFUSE:
		case UNKNOWN:
		case NO_RESPONSE:
			switch (class2) {
			case UNKNOWN:
				return Unknown;
			default:
				value = generateCapeTown2013IncomeValueFromAssetClass(class2);
				break;
			}
			break;
		case NO_INCOME:
			value = 0.0;
			break;
		case CLASS2:
		case CLASS3:
		case CLASS4:
		case CLASS5:
		case CLASS6:
		case CLASS7:
		case CLASS8:
		case CLASS9:
		case CLASS10:
		case CLASS11:
		case CLASS12:
			value = generateCapeTown2013IncomeValueFromReportedIncome(income);
			break;
		}
		
		double valueBaseYear = SouthAfricaInflationCorrector.convert(value, 2013, 2011);
		return getIncomeClassFromValue(valueBaseYear);
	}
	
	private static double generateCapeTown2013IncomeValueFromAssetClass(HouseholdEnums.AssetClass2 assetClass){
		double value = 0.0;
		switch (assetClass) {
		case LOW:
			value = RandomVariateGenerator.getTriangular(0.0, 3200.0, 3200.0);
		case LOWMIDDLE:
			value = 3200.0 + Math.random()*(25600.0 - 3200.0);
		case HIGHMIDDLE:
			value = 25600.0 + Math.random()*(51200.0-25601);
		case HIGH:
			value = RandomVariateGenerator.getTriangular(51201.0, 51201.0, 3.0*51201.0);
		default:
			break;
		}
		return 12.0*value;
	}
	
	private static double generateCapeTown2013IncomeValueFromReportedIncome(HouseholdEnums.MonthlyIncome income){
		double value = 0.0;
		
		
		return value;
	}
	
	
	
	private static double generateCensusIncomeValue(Income2011 income){
		double value = 0.0;
		switch (income) {
		case No_Income:
			break;
		case Income_5K:
			value = RandomVariateGenerator.getTriangular(0.0, 5000.0, 5000.0);
		case Income_10K:
			value = 5000.0 + Math.random()*5000.0;
		case Income_20K:
			value = 10000.0 + Math.random()*10000.0;
		case Income_38K:
			value = 20000.0 + Math.random()*18000.0;
		case Income_77K:
			value = 38000.0 + Math.random()*39000.0;
		case Income_154K:
			value = 77000.0 + Math.random()*77000.0;
		case Income_308K:
			value = 154000.0 + Math.random()*154000.0;
		case Income_614K:
			value = 308000.0 + Math.random()*306000.0;
		case Income_1228K:
			value = 614000.0 + Math.random()*614000.0;
		case Income_2458K:
			value = 1228000.0 + Math.random()*1230000.0;
		case Income_2458K_Plus:
			value = RandomVariateGenerator.getTriangular(2458000.0, 2458000.0, 2*2458000.0);
		default:
			break;
		}
		return value;
	}
	
	
	private static double generateNmbm2004IncomeValue(IncomeTravelSurvey2004 income){
		double value = 0.0;
		switch (income) {
		case No_Income:
			break;
		case Income_500:
			value = RandomVariateGenerator.getTriangular(0.0, 500.0, 500.0);
		case Income_1500:
			value = 500.0 + Math.random()*1000.0;
		case Income_3500:
			value = 1500.0 + Math.random()*2000.0;
		case Income_6000:
			value = 3500.0 + Math.random()*2500.0;
		case Income_11000:
			value = 6000.0 + Math.random()*5000.0;
		case Income_30000:
			value = 11000.0 + Math.random()*19000.0;
		case Income_300000_Plus:
			/* Choose an arbitrary large upper limit. */
			value = RandomVariateGenerator.getTriangular(30000.0, 30000.0, 100000.0);
		default:
			break;
		}
		return value*12.0;
	}
	
	
	/**
	 * Make single step-change to income class. If 'Poor' or 'Affluent', then
	 * the change will be in the direction of the only neighbour. Otherwise,
	 * the step will be randomly to either neighbour in the class. 
	 * 
	 * @param age
	 * @return a neighbouring age class, or 'Middle' be default.
	 */
	public static SaDemographicsIncome getIncomePerturbation(SaDemographicsIncome income){
		switch (income) {
		case Poor:
			return LowMiddle;
		case LowMiddle:
			return MatsimRandom.getRandom().nextDouble() < 0.5 ? Poor : EmergingMiddle;
		case EmergingMiddle:
			return MatsimRandom.getRandom().nextDouble() < 0.5 ? LowMiddle : Middle;
		case Middle:
			return MatsimRandom.getRandom().nextDouble() < 0.5 ? EmergingMiddle : UpperMiddle;
		case UpperMiddle:
			return MatsimRandom.getRandom().nextDouble() < 0.5 ? Middle : EmergingAffluent;
		case EmergingAffluent:
			return MatsimRandom.getRandom().nextDouble() < 0.5 ? UpperMiddle : Affluent;
		case Affluent:
			return EmergingAffluent;
		case Unknown:
			return Middle;
		}
		LOG.warn("Unknown age class: " + income.toString() + "; returning 'Middle'");
		return Middle;
	}

}

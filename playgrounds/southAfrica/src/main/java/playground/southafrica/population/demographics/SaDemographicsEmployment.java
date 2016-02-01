package playground.southafrica.population.demographics;

import org.apache.log4j.Logger;
import org.matsim.core.population.PersonImpl;

import playground.southafrica.population.capeTownTravelSurvey.HouseholdEnums;
import playground.southafrica.population.capeTownTravelSurvey.PersonEnums;
import playground.southafrica.population.census2011.containers.Employment2011;
import playground.southafrica.population.nmbmTravelSurvey.NmbmSurveyParser;

public enum SaDemographicsEmployment {
	Working, Nonworking;
	
	private final static Logger LOG = Logger.getLogger(SaDemographicsEmployment.class);
	
	
	/**
	 * Converts the employment details from the South African Census 2011 
	 * population (either sample or synthetic) into a generic employment 
	 * status. the detail is taken from the person attributes.
	 * 
	 * @param census2011employment
	 * @return
	 */
	public static SaDemographicsEmployment convertCensus2011Employment(Employment2011 census2011employment){
		switch (census2011employment) {
		case Employed:
			return Working;
		case Discouraged:
		case Inactive:
		case NotApplicable:
		case Unemployed:
			return Nonworking;
		}
		LOG.warn("Unknown employment class from census 2011: " + census2011employment + "; returning 'Nonworking'");
		return Nonworking;
	}
	
	
	/**
	 * Converts the employment details from the South African Census 2011 
	 * population (either sample or synthetic) into a generic employment 
	 * status. The detail is taken from the {@link PersonImpl#isEmployed()}
	 * method.
	 * 
	 * @param employed
	 * @return
	 */
	public static SaDemographicsEmployment convertCensus2011Employment(boolean employed){
		if(employed){
			return Working;
		} else{
			return Nonworking;
		}
	}
	
	
	/**
	 * Converts the employment details from the Travel Diary conducted in 
	 * Nelson Mandela Bay in 2004 into a generic employment status. In this
	 * case the 'employed' status is true if the individual as a work-related
	 * activity in the activity chain. It was captured in {@link NmbmSurveyParser}.
	 * The detail is taken from the {@link PersonImpl#isEmployed()} method.
	 * 
	 * @param employed
	 * @return
	 */
	public static SaDemographicsEmployment convertNmbm2004Employment(boolean employed){
		if(employed){
			return Working;
		} else{
			return Nonworking;
		}
	}
	
	/**
	 * Converts the employment details from the Travel Diary conducted in the
	 * City of Cape Town in 2013 into a generic employment status. In this 
	 * case we consider the <code>employment</code> attribute that was captured
	 * in the <code>personAttributes.xml</code> file when parsing the Travel
	 * Survey.
	 * 
	 * @param employment
	 * @return
	 */
	public static SaDemographicsEmployment convertCapeTown2011Employment(String employment){
		PersonEnums.Employment emp = PersonEnums.Employment.parseFromDescription(employment);
		switch (emp) {
		case UNKNOWN:
		case UNEMPLOYED_LOOKING:
		case UNEMPLOYED_NOT_LOOKING:
		case PENSIONER:
		case STUDENT:
		case SCHOLAR:
		case HOUSEWIFE:
			return Nonworking;
		case FULLTIME:
		case PARTTIME:
		case SELFEMPLOYED:
		case CONTRACT:
			return Working;
		}
		return Nonworking;
	}
	
	
	/**
	 * Change the employment status. since there are only two employment 
	 * statuses, the compliment is returned.
	 * 
	 * @param employment
	 * @return
	 */
	public static SaDemographicsEmployment getEmploymentPerturbation(SaDemographicsEmployment employment){
		switch (employment) {
		case Working:
			return Nonworking;
		case Nonworking:
			return Working;
		}
		LOG.warn("Unknown employment class: " + employment.toString() + "; returning 'Nonworking'");
		return Nonworking;
	}

}

/* *********************************************************************** *
 * project: org.matsim.*
 * LivingQuarterjava
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.southafrica.population.nmbmTravelSurvey.containers;

import org.apache.log4j.Logger;
import org.matsim.households.Income;
import org.matsim.households.Income.IncomePeriod;
import org.matsim.households.IncomeImpl;


/**
 * The income groups as described in the Travel Survey questionnaire of 2004 
 * for Nelson Mandela Bay, Section 2, Question 5.
 *
 * @author jwjoubert
 */
public enum IncomeTravelSurvey2004 {
	No_Income, Income_500, Income_1500, Income_3500, Income_6000, 
	Income_11000, Income_30000, Income_300000_Plus, Unspecified;

	private final static Logger LOG = Logger.getLogger(IncomeTravelSurvey2004.class);

	/**
	 * Method accepting the given code as per Section 2, Question 5 in the 
	 * Travel Survey questionnaire.
	 *  
	 * @param code formal value as per final code list in census questionnaire
	 * @return a descriptive enum.
	 */
	public static IncomeTravelSurvey2004 parseIncomeFromSurveyCode(String code){
		int codeInt = 0;
		try{
			codeInt = Integer.parseInt(code);
		} catch(NumberFormatException e){
			LOG.error("Cannot parse interge code from " + code + "; returning 'Unspecified'");
		}

		switch (codeInt) {
		case 0:
			return Unspecified;
		case 1:
			return No_Income;
		case 2:
			return Income_500;
		case 3:
			return Income_1500;
		case 4:
			return Income_3500;
		case 5:
			return Income_6000;
		case 6:
			return Income_11000;
		case 7:
			return Income_30000;
		case 8:
			return Income_300000_Plus;
		} 
		LOG.error("Unknown census code: " + code + "!! Returning 'Unspecified'");
		return Unspecified;
	}


	/**
	 * Method to return an integer code for an income class. The integer codes 
	 * corresponds to the codes used in the census questionnaire, with
	 * a few exceptions: the code for 'Not applicable'.
	 * 
	 * @param income
	 * @return
	 */
	public static int getCode(IncomeTravelSurvey2004 income){
		switch (income) {
		case No_Income:
			return 1;
		case Income_500:
			return 2;
		case Income_1500:
			return 3;
		case Income_3500:
			return 4;
		case Income_6000:
			return 5;
		case Income_11000:
			return 6;
		case Income_30000:
			return 7;
		case Income_300000_Plus:
			return 8;
		case Unspecified:
			return 0;
		}
		LOG.error("Unknown income: " + income.toString() + "!! Returning code for 'Unspecified'");
		return 0;
	}
	

	/**
	 * Method to return an integer code from an income class. The integer codes 
	 * corresponds to the codes used in the census questionnaire, with a few 
	 * exceptions: the code for 'Not applicable'.
	 * 
	 * @param s
	 * @return
	 */
	public static int getCode(String s){
		return getCode(IncomeTravelSurvey2004.valueOf(s));
	}


	/**
	 * Convert income code to <b><i>monthly</i></b> income value in South 
	 * African Rand (ZAR). The <i>upper</i> upper income level is given.
	 * 
	 * @param income
	 * @return
	 */
	public static Income getIncome(IncomeTravelSurvey2004 income){
		Income inc = null;
		switch (income) {
		case No_Income:
			inc = new IncomeImpl(0.00, IncomePeriod.month); 
			break;
		case Income_500:
			inc = new IncomeImpl(500.00, IncomePeriod.month); 
			break;
		case Income_1500:
			inc = new IncomeImpl(1500.00, IncomePeriod.month); 
			break;
		case Income_3500:
			inc = new IncomeImpl(3500.00, IncomePeriod.month); 
			break;
		case Income_6000:
			inc = new IncomeImpl(6000.00, IncomePeriod.month); 
			break;
		case Income_11000:
			inc = new IncomeImpl(11000.00, IncomePeriod.month); 
			break;
		case Income_30000:
			inc = new IncomeImpl(30000.00, IncomePeriod.month); 
			break;
		case Income_300000_Plus: /* Twice the highest category value. */
			inc = new IncomeImpl(60000.00, IncomePeriod.month); 
			break;
		case Unspecified:
			break;
		}

		if(inc != null){
			inc.setCurrency("ZAR");
		}

		return inc;
	}


	public static IncomeTravelSurvey2004 getIncomeEnum(Income income){
		if(income == null){
			return Unspecified;
		}
		
		if(!income.getCurrency().equalsIgnoreCase("ZAR")){
			LOG.error("Unknown and invalid currency: " + income.getCurrency() 
					+ "!! Returning 'Unspecified'");
			return Unspecified;
		}
		if(income.getIncomePeriod() != IncomePeriod.month){
			LOG.error("Invalid income period: " + income.getIncomePeriod().toString()
					+ "!! Returning 'Unspecified'");
			return Unspecified;
		}

		double incomeValue = income.getIncome();
		if(incomeValue <= 0.00){
			return No_Income;
		} else if(incomeValue <= 500.00){
			return Income_500;
		} else if(incomeValue <= 1500.00){
			return Income_1500;
		} else if(incomeValue <= 3500.00){
			return Income_3500;
		} else if(incomeValue <= 6000.00){
			return Income_6000;
		} else if(incomeValue <= 11000.00){
			return Income_11000;
		} else if(incomeValue <= 30000.00){
			return Income_30000;
		} else if(incomeValue <= 60000.00){
			return Income_300000_Plus;
		} 
		
		LOG.error("Unknown income: " + incomeValue 
				+ "!! Returning 'Unspecified'");
		return Unspecified;
	}

}



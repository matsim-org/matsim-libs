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

package playground.southafrica.population.census2011.containers;

import org.apache.log4j.Logger;
import org.matsim.households.Income;
import org.matsim.households.IncomeImpl;
import org.matsim.households.Income.IncomePeriod;


/**
 * The income groups as described in the Census 2011 questionnaire,
 * question <b>P16_INCOME</b>.
 *
 * @author jwjoubert
 */
public enum Income2011 {
	No_Income, Income_5K, Income_10K, Income_20K, Income_38K, 
	Income_77K, Income_154K, Income_308K, Income_614K, Income_1228K, 
	Income_2458K, Income_2458K_Plus, Unspecified, NotApplicable;

	private final static Logger LOG = Logger.getLogger(Income2011.class);

	/**
	 * Method accepting the given code as per Question <b>P16_INCOME</b> 
	 * in the census questionnaire.
	 *  
	 * @param code formal value as per final code list in census questionnaire
	 * @return a descriptive Income2011 enum.
	 */
	public static Income2011 parseIncome2011FromCensusCode(String code){
		if(code.contains(".")){
			return NotApplicable;
		}
		code = code.replaceAll(" ", "");
		int codeInt = Integer.parseInt(code);

		switch (codeInt) {
		case 1:
			return No_Income;
		case 2:
			return Income_5K;
		case 3:
			return Income_10K;
		case 4:
			return Income_20K;
		case 5:
			return Income_38K;
		case 6:
			return Income_77K;
		case 7:
			return Income_154K;
		case 8:
			return Income_308K;
		case 9:
			return Income_614K;
		case 10:
			return Income_1228K;
		case 11:
			return Income_2458K;
		case 12:
			return Income_2458K_Plus;
		case 99:
			return Unspecified;
		} 
		LOG.error("Unknown census code: " + code + "!! Returning 'Unspecified'");
		return Unspecified;
	}


	/**
	 * Method to return the income class by parsing it from a string version 
	 * of the same  It is assumed the given String were originally created 
	 * through the <code>toString()</code> method. If not, and the input 
	 * string is the code from the census questionnaire, rather use
	 * the {@link Income2011#parseIncome2011FromCensusCode(String)} method.
	 * 
	 * @param Income2011
	 * @return
	 */
	public static Income2011 parseIncome2011FromString(String Income2011){
		return valueOf(Income2011);
	}


	/**
	 * Method to return an integer code for an income class. The integer codes 
	 * corresponds to the codes used in the census questionnaire, with
	 * a few exceptions: the code for 'Not applicable'.
	 * 
	 * @param income
	 * @return
	 */
	public static int getCode(Income2011 income){
		switch (income) {
		case No_Income:
			return 1;
		case Income_5K:
			return 2;
		case Income_10K:
			return 3;
		case Income_20K:
			return 4;
		case Income_38K:
			return 5;
		case Income_77K:
			return 6;
		case Income_154K:
			return 7;
		case Income_308K:
			return 8;
		case Income_614K:
			return 9;
		case Income_1228K:
			return 10;
		case Income_2458K:
			return 11;
		case Income_2458K_Plus:
			return 12;
		case Unspecified:
			return 99;
		case NotApplicable:
			return 13;
		}
		LOG.error("Unknown income: " + income.toString() + "!! Returning code for 'Unspecified'");
		return 99;
	}
	
	
	/**
	 * An additional class to aggregate the original income classes, if 
	 * necessary. For example, classes 2 and 3 can be combined.
	 * 
	 * @param income
	 * @return
	 */
	public static int getCodeForIpf(Income2011 income){
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
		return getCode(parseIncome2011FromString(s));
	}


	/**
	 * Method to return the housing Income2011 from a given code. The given integer 
	 * code is the one used internally by this class. It corresponds with the 
	 * original census questionnaire codes, but there are differences. If you 
	 * want to parse the dwelling Income2011 from the actual census code, rather use 
	 * the method {@link MainDwellingIncome20112011#parseIncomeFromSurveyCode(String)}.
	 * 
	 * @param code
	 * @return
	 */
	public static Income2011 getIncome2011(int code){
		switch (code) {
		case 1:
			return No_Income;
		case 2:
			return Income_5K;
		case 3:
			return Income_10K;
		case 4: 
			return Income_20K;
		case 5:
			return Income_38K;
		case 6:
			return Income_77K;
		case 7:
			return Income_154K;
		case 8:
			return Income_308K;
		case 9:
			return Income_614K;
		case 10:
			return Income_1228K;
		case 11:
			return Income_2458K;
		case 12:
			return Income_2458K_Plus;
		case 99:
			return Unspecified;
		case 13:
			return NotApplicable;
		}
		LOG.error("Unknown Income2011 code: " + code + "!! Returning 'NotApplicable'");
		return NotApplicable;
	}


	/**
	 * Convert income code to <b><i>annual</i></b> income value in South African
	 * Rand (ZAR). The <i>upper</i> upper income level is given.
	 * 
	 * @param Income2011
	 * @return
	 */
	public static Income getIncome(Income2011 Income2011){
		Income income = null;
		switch (Income2011) {
		case No_Income:
			income = new IncomeImpl(0.00, IncomePeriod.year); 
			break;
		case Income_5K:
			income = new IncomeImpl(4800.00, IncomePeriod.year); 
			break;
		case Income_10K:
			income = new IncomeImpl(9600.00, IncomePeriod.year); 
			break;
		case Income_20K:
			income = new IncomeImpl(19200.00, IncomePeriod.year); 
			break;
		case Income_38K:
			income = new IncomeImpl(38400.00, IncomePeriod.year); 
			break;
		case Income_77K:
			income = new IncomeImpl(76800.00, IncomePeriod.year); 
			break;
		case Income_154K:
			income = new IncomeImpl(153600.00, IncomePeriod.year); 
			break;
		case Income_308K:
			income = new IncomeImpl(307200.00, IncomePeriod.year); 
			break;
		case Income_614K:
			income = new IncomeImpl(614400.00, IncomePeriod.year); 
			break;
		case Income_1228K:
			income = new IncomeImpl(1228800.00, IncomePeriod.year); 
			break;
		case Income_2458K:
			income = new IncomeImpl(2457600.00, IncomePeriod.year); 
			break;
		case Income_2458K_Plus: /* Twice the category 11 value. */
			income = new IncomeImpl(4915200.00, IncomePeriod.year); 
			break;
		case Unspecified:
			break;
		case NotApplicable:
			break;
		}

		if(income != null){
			income.setCurrency("ZAR");
		}

		return income;
	}


	public static Income2011 getIncomeEnum(Income income){
		if(income == null){
			return Unspecified;
		}
		
		if(!income.getCurrency().equalsIgnoreCase("ZAR")){
			LOG.error("Unknown and invalid currency: " + income.getCurrency() 
					+ "!! Returning 'Unspecified'");
			return Unspecified;
		}
		if(income.getIncomePeriod() != IncomePeriod.year){
			LOG.error("Invalid income period: " + income.getIncomePeriod().toString()
					+ "!! Returning 'Unspecified'");
			return Unspecified;
		}

		double incomeValue = income.getIncome();
		if(incomeValue <= 0.00){
			return No_Income;
		} else if(incomeValue <= 4800.00){
			return Income_5K;
		} else if(incomeValue <= 9600.00){
			return Income_10K;
		} else if(incomeValue <= 19200.00){
			return Income_20K;
		} else if(incomeValue <= 38400.00){
			return Income_38K;
		} else if(incomeValue <= 76800.00){
			return Income_77K;
		} else if(incomeValue <= 153600.00){
			return Income_154K;
		} else if(incomeValue <= 307200.00){
			return Income_308K;
		} else if(incomeValue <= 614400.00){
			return Income_614K;
		} else if(incomeValue <= 1228800.00){
			return Income_1228K;
		} else if(incomeValue <= 2457600.00){
			return Income_2458K;
		} else if(incomeValue <= 4915200.00){
			return Income_2458K_Plus;
		} 
		
		LOG.error("Unknown income: " + incomeValue 
				+ "!! Returning 'Unspecified'");
		return Unspecified;
	}

}



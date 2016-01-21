/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdEnums.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.projects.capeTownDiary;

import java.util.Random;

import org.matsim.core.gbl.MatsimRandom;
import org.matsim.households.Income;
import org.matsim.households.Income.IncomePeriod;
import org.matsim.households.IncomeImpl;

/**
 * Class to capture the various variable classes in the household data provided
 * in the City of Cape Town 2013 travel survey.
 * 
 * @author jwjoubert
 */
public class HouseholdEnums {

	public static enum DwellingType{
		UNKNOWN (0, "Unknown"),
		HOUSE (1, "House"),
		TOWNHOUSE (2, "Townhouse"),
		FLAT (3, "Flat"),
		DUPLEX (4, "Duplex"),
		BACKYARD_FLAT (5, "Flat in backyard"),
		BACKYARD_SHACK (6, "Shack in backyard"),
		SHACK (7, "Informal dwelling");

		private final int code; /* Code used in survey. */
		private final String description;

		DwellingType(int code, String descr){
			this.code = code;
			this.description = descr;
		}

		int getCode(){ return this.code; }

		String getDescription(){ return this.description; }

		static DwellingType parseFromDescription(String descr){
			if(descr.equalsIgnoreCase("Unknown")){
				return UNKNOWN;
			} else if (descr.equalsIgnoreCase("House")){
				return HOUSE;
			} else if (descr.equalsIgnoreCase("Townhouse")){
				return TOWNHOUSE;
			} else if (descr.equalsIgnoreCase("Flat")){
				return FLAT;
			} else if (descr.equalsIgnoreCase("Duplex")){
				return DUPLEX;
			} else if (descr.equalsIgnoreCase("Flat in backyard")){
				return BACKYARD_FLAT;
			} else if (descr.equalsIgnoreCase("Shack in backyard")){
				return BACKYARD_SHACK;
			} else if (descr.equalsIgnoreCase("Informal dwelling")){
				return SHACK;
			} else{
				throw new RuntimeException("Cannot parse template from description: " + descr);
			}
		}

		static DwellingType parseFromCode(int code){
			switch (code) {
			case 0: return UNKNOWN;
			case 1: return HOUSE;
			case 2:	return TOWNHOUSE;
			case 3:	return FLAT;
			case 4: return DUPLEX;
			case 5: return BACKYARD_FLAT;
			case 6: return BACKYARD_SHACK;
			case 7: return SHACK;
			default:
				throw new RuntimeException("Cannot parse template from code: " + code);
			}
		}
	}


	public static enum MonthlyIncome{
		NO_INCOME (1, "No income"),
		CLASS2 (2, "R1-R400 per month"),
		CLASS3 (3, "R401-R800 per month"),
		CLASS4 (4, "R801-R1600 per month"),
		CLASS5 (5, "R1601-R3200 per month"),
		CLASS6 (6, "R3201-R6400 per month"),
		CLASS7 (7, "R6401-R12800 per month"),
		CLASS8 (8, "R12801-R25600 per month"),
		CLASS9 (9, "R25601-R51200 per month"),
		CLASS10 (10, "R51200-R102400 per month"),
		CLASS11 (11, "R102401-R204800 per month"),
		CLASS12 (12, "R204801 or more per month"),
		REFUSE (13, "Refuse"),
		UNKNOWN (14, "Unknown"),
		NO_RESPONSE (15, "No response");

		private final int code; /* Code used in survey. */
		private final String description;

		MonthlyIncome(int code, String descr){
			this.code = code;
			this.description = descr;
		}

		int getCode(){ return this.code; }

		String getDescription(){ return this.description; }

		static MonthlyIncome parseFromDescription(String descr){
			if(descr.equalsIgnoreCase("Unknown")){
				return UNKNOWN;
			} else if (descr.equalsIgnoreCase("No income")){
				return NO_INCOME;
			} else if (descr.equalsIgnoreCase("R1-R400 per month")){
				return CLASS2;
			} else if (descr.equalsIgnoreCase("R401-R800 per month")){
				return CLASS3;
			} else if (descr.equalsIgnoreCase("R801-R1600 per month")){
				return CLASS4;
			} else if (descr.equalsIgnoreCase("R1601-R3200 per month")){
				return CLASS5;
			} else if (descr.equalsIgnoreCase("R3201-R6400 per month")){
				return CLASS6;
			} else if (descr.equalsIgnoreCase("R6401-R12800 per month")){
				return CLASS7;
			} else if (descr.equalsIgnoreCase("R12801-R25600 per month")){
				return CLASS8;
			} else if (descr.equalsIgnoreCase("R25601-R51200 per month")){
				return CLASS9;
			} else if (descr.equalsIgnoreCase("R51200-R102400 per month")){
				return CLASS10;
			} else if (descr.equalsIgnoreCase("R102401-R204800 per month")){
				return CLASS11;
			} else if (descr.equalsIgnoreCase("R204801 or more per month")){
				return CLASS12;
			} else if (descr.equalsIgnoreCase("Refuse")){
				return REFUSE;
			} else if (descr.equalsIgnoreCase("No response")){
				return NO_RESPONSE;
			} else{
				throw new RuntimeException("Cannot parse template from description: " + descr);
			}
		}

		static MonthlyIncome parseFromCode(int code){
			switch (code) {
			case 1: return NO_INCOME;
			case 2:	return CLASS2;
			case 3:	return CLASS3;
			case 4: return CLASS4;
			case 5: return CLASS5;
			case 6: return CLASS6;
			case 7: return CLASS7;
			case 8: return CLASS8;
			case 9: return CLASS9;
			case 10: return CLASS10;
			case 11: return CLASS11;
			case 12: return CLASS12;
			case 13: return REFUSE;
			case 14: return UNKNOWN;
			case 15: return NO_RESPONSE;
			default:
				throw new RuntimeException("Cannot parse template from code: " + code);
			}
		}

		public Income getMatsimIncome(){
			Random r = MatsimRandom.getLocalInstance();
			int min = 0;
			int max = 0;
			switch (this) {
			case NO_INCOME: return new IncomeImpl(0.0, IncomePeriod.month);
			case CLASS2: min = 1; max = 400; break;
			case CLASS3: min = 401; max = 800; break;
			case CLASS4: min = 801; max = 1600; break;
			case CLASS5: min = 1601; max = 3200; break;
			case CLASS6: min = 3201; max = 6400; break;
			case CLASS7: min = 6401; max = 12800; break;
			case CLASS8: min = 12801; max = 25600; break;
			case CLASS9: min = 25601; max = 51200; break;
			case CLASS10: min = 51201; max = 102400; break;
			case CLASS11: min = 102401; max = 204800; break;
			case CLASS12: min = 204801; max = 409600; break; /* Manual maximum chosen arbitrarily. */
			case REFUSE: 
			case UNKNOWN: 
			case NO_RESPONSE: return null;
			}

			int value = min + r.nextInt(max - min);
			Income income = new IncomeImpl(value, IncomePeriod.month);
			income.setCurrency("ZAR");
			return income;
		}
	}
	
	
	
	public static enum CompletedDiary{
		UNKNOWN (0, "Unknown"),
		YES (1, "Yes"),
		NO (2, "No");
		
		private final int code; /* Code used in survey. */
		private final String description;
		
		CompletedDiary(int code, String descr){
			this.code = code;
			this.description = descr;
		}
		
		int getCode(){ return this.code; }
		
		String getDescription(){ return this.description; }
		
		static CompletedDiary parseFromDescription(String descr){
			if(descr.equalsIgnoreCase("Unknown")){
				return UNKNOWN;
			} else if(descr.equalsIgnoreCase("Yes")){
				return YES;
			} else if (descr.equalsIgnoreCase("No")){
				return NO;
			} else{
				throw new RuntimeException("Cannot parse travel to work/education from description: " + descr);
			}
		}
		
		static CompletedDiary parseFromCode(int code){
			switch (code) {
			case 0: return UNKNOWN;
			case 1: return YES;
			case 2:	return NO;
			default:
				throw new RuntimeException("Cannot parse travel to work/education from code: " + code);
			}
		}
	}
	
	
	
	public static enum CompletedStatedPreference{
		UNKNOWN (0, "Unknown"),
		YES (1, "Yes"),
		NO (2, "No");
		
		private final int code; /* Code used in survey. */
		private final String description;
		
		CompletedStatedPreference(int code, String descr){
			this.code = code;
			this.description = descr;
		}
		
		int getCode(){ return this.code; }
		
		String getDescription(){ return this.description; }
		
		static CompletedStatedPreference parseFromDescription(String descr){
			if(descr.equalsIgnoreCase("Unknown")){
				return UNKNOWN;
			} else if(descr.equalsIgnoreCase("Yes")){
				return YES;
			} else if (descr.equalsIgnoreCase("No")){
				return NO;
			} else{
				throw new RuntimeException("Cannot parse travel to work/education from description: " + descr);
			}
		}
		
		static CompletedStatedPreference parseFromCode(int code){
			switch (code) {
			case 0: return UNKNOWN;
			case 1: return YES;
			case 2:	return NO;
			default:
				throw new RuntimeException("Cannot parse travel to work/education from code: " + code);
			}
		}
	}
	
	

	/**
	 * It is important to accommodate the households who did not supply 
	 * household income information.  The assumption was made that the 
	 * distribution over the four income groups will be the same for those who 
	 * did not respond to the question on household income, as those who 
	 * responded to that question.
	 * 
	 * In this first method an asset index from Filmer & Pritchett (1998) is
	 * used. For details on its calculation, refer to the metadata accompanying
	 * the travel survey.
	 * 
	 * The following is taken from the metadata:
	 * <p>
	 * <i>''...Each household had a calculated asset value. Of the households 
	 * responding to the question on the income category (HH_income),   
	 * 39% households selected the low income group, 51%  the low middle 
	 * income group, 7% the high middle income group and 3% the high income 
	 * group. The entire group was then divided into 4 asset groups, according 
	 * to these percentages. After allocating each household in one of four 
	 * asset groups using the same ratios of those households with household 
	 * income, those 9789 households of which no household income information 
	 * was available, 35% were in the low asset group, 52% in the low middle 
	 * asset group, 9% in the high middle asset group and 4% in the high asset 
	 * group. These asset groups are used to classify the remaining households 
	 * without household income into these four groups, keeping the household 
	 * income groups of those who supplied their household incomes and combine 
	 * the two grouping methods, to create a new income group...''</i>
	 * </p>
	 * 
	 * @author jwjoubert
	 */
	public static enum AssetClass1{
		LOW (1, "Low income (R0 - R3200 monthly)"),
		LOWMIDDLE (2, "Low middle income (R3201 - R25600 monthly)"),
		HIGHMIDDLE (3, "High middle income (R25601 - R51200 monthly)"),
		HIGH (4, "High income (R51201 or more monthly)"),
		UNKNOWN (5, "Unknown");
		
		private final int code; /* Code used in survey. */
		private final String description;
		
		AssetClass1(int code, String descr){
			this.code = code;
			this.description = descr;
		}
		
		int getCode(){ return this.code; }
		
		String getDescription(){ return this.description; }
		
		static AssetClass1 parseFromDescription(String descr){
			if(descr.equalsIgnoreCase("Unknown")){
				return UNKNOWN;
			} else if (descr.equalsIgnoreCase("Low income (R0 - R3200 monthly)")){
				return LOW;
			} else if (descr.equalsIgnoreCase("Low middle income (R3201 - R25600 monthly)")){
				return LOWMIDDLE;
			} else if (descr.equalsIgnoreCase("High middle income (R25601 - R51200 monthly)")){
				return HIGHMIDDLE;
			} else if (descr.equalsIgnoreCase("High income (R51201 or more monthly)")){
				return HIGH;
			} else{
				throw new RuntimeException("Cannot parse template from description: " + descr);
			}
		}
		
		static AssetClass1 parseFromCode(int code){
			switch (code) {
			case 1: return LOW;
			case 2:	return LOWMIDDLE;
			case 3:	return HIGHMIDDLE;
			case 4: return HIGH;
			case 5: return UNKNOWN;
			default:
				throw new RuntimeException("Cannot parse template from code: " + code);
			}
		}
	}
	
	/**
	 * It is important to accommodate the households who did not supply 
	 * household income information.  The assumption was made that the 
	 * distribution over the four income groups will be the same for those who 
	 * did not respond to the question on household income, as those who 
	 * responded to that question.
	 * 
	 * In this second method an asset index from Filmer & Pritchett (1998) is
	 * again used. For details on its calculation, refer to the metadata 
	 * accompanying the travel survey.
	 * 
	 * The following is taken from the metadata:
	 * <p>
	 * <i>''...A second method was used to calculate income groups. Each 
	 * household had a calculated asset value. According to National census 
	 * data (Census 2011), the household income of the City of Cape Town is 
	 * distributed as follows: 47% households in the low income group, 39% in 
	 * the low middle income group, 9% in the high middle income group and 5% 
	 * in the high income group. The entire group was then divided into 4 asset 
	 * groups, according to these percentages. After allocating each household 
	 * in one of four asset groups using the same ratios of those households 
	 * with household income, those 9789 households of which no household 
	 * income information was available, 42% were in the low asset group, 
	 * 39% in the low middle asset group, 12% in the high middle asset group 
	 * and 7% in the high asset group. These asset groups are used to classify 
	 * the remaining households without household income into these four 
	 * groups, keeping the household income groups of those who supplied 
	 * their household incomes and combine the two grouping methods, to create 
	 * a new income group ...''</i>
	 * </p>
	 * 
	 * @author jwjoubert
	 */
	public static enum AssetClass2{
		LOW (1, "Low income (R0 - R3200 monthly)"),
		LOWMIDDLE (2, "Low middle income (R3201 - R25600 monthly)"),
		HIGHMIDDLE (3, "High middle income (R25601 - R51200 monthly)"),
		HIGH (4, "High income (R51201 or more monthly)"),
		UNKNOWN (5, "Unknown");
		
		private final int code; /* Code used in survey. */
		private final String description;
		
		AssetClass2(int code, String descr){
			this.code = code;
			this.description = descr;
		}
		
		int getCode(){ return this.code; }
		
		String getDescription(){ return this.description; }
		
		static AssetClass2 parseFromDescription(String descr){
			if(descr.equalsIgnoreCase("Unknown")){
				return UNKNOWN;
			} else if (descr.equalsIgnoreCase("Low income (R0 - R3200 monthly)")){
				return LOW;
			} else if (descr.equalsIgnoreCase("Low middle income (R3201 - R25600 monthly)")){
				return LOWMIDDLE;
			} else if (descr.equalsIgnoreCase("High middle income (R25601 - R51200 monthly)")){
				return HIGHMIDDLE;
			} else if (descr.equalsIgnoreCase("High income (R51201 or more monthly)")){
				return HIGH;
			} else{
				throw new RuntimeException("Cannot parse template from description: " + descr);
			}
		}
		
		static AssetClass2 parseFromCode(int code){
			switch (code) {
			case 1: return LOW;
			case 2:	return LOWMIDDLE;
			case 3:	return HIGHMIDDLE;
			case 4: return HIGH;
			case 5: return UNKNOWN;
			default:
				throw new RuntimeException("Cannot parse template from code: " + code);
			}
		}
	}


	public static enum Template{
		UNKNOWN (0, "Unknown"),
		A (1, ""),
		B (2, ""),
		C (3, ""),
		D (4, ""),
		E (5, ""),
		F (6, ""),
		G (7, "");

		private final int code; /* Code used in survey. */
		private final String description;

		Template(int code, String descr){
			this.code = code;
			this.description = descr;
		}

		int getCode(){ return this.code; }

		String getDescription(){ return this.description; }

		static Template parseFromDescription(String descr){
			if(descr.equalsIgnoreCase("Unknown")){
				return UNKNOWN;
			} else if (descr.equalsIgnoreCase("")){
				return A;
			} else if (descr.equalsIgnoreCase("")){
				return B;
			} else if (descr.equalsIgnoreCase("")){
				return C;
			} else if (descr.equalsIgnoreCase("")){
				return D;
			} else if (descr.equalsIgnoreCase("")){
				return E;
			} else if (descr.equalsIgnoreCase("")){
				return F;
			} else if (descr.equalsIgnoreCase("")){
				return G;
			} else{
				throw new RuntimeException("Cannot parse template from description: " + descr);
			}
		}

		static Template parseFromCode(int code){
			switch (code) {
			case 0: return UNKNOWN;
			case 1: return A;
			case 2:	return B;
			case 3:	return C;
			case 4: return D;
			case 5: return E;
			case 6: return F;
			case 7: return G;
			default:
				throw new RuntimeException("Cannot parse template from code: " + code);
			}
		}
	}

}

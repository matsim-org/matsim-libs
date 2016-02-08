/* *********************************************************************** *
 * project: org.matsim.*
 * PersonEnums.java
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

/**
 * 
 */
package playground.southafrica.population.capeTownTravelSurvey;

import org.apache.log4j.Logger;

/**
 * Class to capture the various variable classes in the persons data provided
 * in the City of Cape Town 2013 travel survey.
 * 
 * @author jwjoubert
 */
public class PersonEnums {
	final private static Logger LOG = Logger.getLogger(PersonEnums.class);

	/**
	 * This age group was not explicitly coded in the Travel Diary. Instead,
	 * the year of birth was provided. To be compatible with the Census 2011 
	 * data, we include it here so that we can deal with unknown birth years 
	 * as well.
	 *
	 * @author jwjoubert
	 */
	public static enum AgeGroup{
		UNKNOWN ("Unknown"),
		INFANT ("Infant"),
		CHILD ("Child"),
		YOUNG ("Young"),
		EARLYCAREER ("Early career"),
		LATECAREER ("Late career"),
		RETIRED ("Retired");
		
		private final String description;
		
		AgeGroup(String descr){
			this.description = descr;
		}
		
		public String getDescription(){ return this.description; }
		
		public static AgeGroup parseFromDescription(String descr){
			if(descr.equalsIgnoreCase("Unknown")){
				return UNKNOWN;
			} else if(descr.equalsIgnoreCase("Infant")){
				return INFANT;
			} else if(descr.equalsIgnoreCase("Child")){
				return CHILD;
			} else if(descr.equalsIgnoreCase("Young")){
				return YOUNG;
			} else if(descr.equalsIgnoreCase("Early career")){
				return EARLYCAREER;
			} else if(descr.equalsIgnoreCase("Late career")){
				return LATECAREER;
			} else if (descr.equalsIgnoreCase("Retired")){
				return RETIRED;
			} else{
				throw new RuntimeException("Cannot parse age from description: " + descr);
			}
		}
		
		public static AgeGroup parseFromBirthYear(String year){
			if(year.equalsIgnoreCase("Unknown")){
				return UNKNOWN;
			}
			try{
				double birthYear = Double.parseDouble(year);
				double age = Math.max(0.0, 2013.0 - birthYear);
				if(age <= 5){
					return INFANT;
				} else if(age <= 12){
					return CHILD;
				} else if (age <= 23){
					return YOUNG;
				} else if (age <= 45){
					return EARLYCAREER;
				} else if (age <= 68){
					return LATECAREER;
				} else{
					return RETIRED;
				}
			} catch (NumberFormatException e){
				LOG.error("Cannot parse birth year from " + year + ". Returning UNKNOWN.");
				return UNKNOWN;
			}
		}
	}
	
	
	public static enum Gender{
		UNKNOWN (0, "Unknown"),
		MALE (1, "Male"),
		FEMALE (2, "Female");
		
		private final int code; /* Code used in survey. */
		private final String description;
		
		Gender(int code, String descr){
			this.code = code;
			this.description = descr;
		}
		
		int getCode(){ return this.code; }
		
		String getDescription(){ return this.description; }
		
		static Gender parseFromDescription(String descr){
			if(descr.equalsIgnoreCase("Unknown")){
				return UNKNOWN;
			} else if(descr.equalsIgnoreCase("Male")){
				return MALE;
			} else if (descr.equalsIgnoreCase("Female")){
				return FEMALE;
			} else{
				throw new RuntimeException("Cannot parse gender from description: " + descr);
			}
		}
		
		static Gender parseFromCode(int code){
			switch (code) {
			case 0: return UNKNOWN;
			case 1: return MALE;
			case 2:	return FEMALE;
			default:
				throw new RuntimeException("Cannot parse gender from code: " + code);
			}
		}
	}

	
	public static enum Education{
		UNKNOWN (0, "Unknown"),
		NONE (1, "None"),
		PRIMARY (2, "Primary"),
		SECONDARY10 (3, "Secondary, Grade 10"),
		SECONDARY12 (4, "Secondary, Grade 12"),
		TERTIARY (5, "Tertiary");
		
		private final int code; /* Code used in survey. */
		private final String description;
		
		Education(int code, String descr){
			this.code = code;
			this.description = descr;
		}
		
		int getCode(){ return this.code; }
		
		String getDescription(){ return this.description; }
		
		static Education parseFromDescription(String descr){
			if(descr.equalsIgnoreCase("Unknown")){
				return UNKNOWN;
			} else if(descr.equalsIgnoreCase("None")){
				return NONE;
			} else if (descr.equalsIgnoreCase("Primary")){
				return PRIMARY;
			} else if (descr.equalsIgnoreCase("Secondary, Grade 10")){
				return SECONDARY10;
			} else if (descr.equalsIgnoreCase("Secondary, Grade 12")){
				return SECONDARY12;
			} else if (descr.equalsIgnoreCase("Tertiray")){
				return TERTIARY;
			} else{
				throw new RuntimeException("Cannot parse education from description: " + descr);
			}
		}
		
		static Education parseFromCode(int code){
			switch (code) {
			case 0: return UNKNOWN;
			case 1: return NONE;
			case 2:	return PRIMARY;
			case 3:	return SECONDARY10;
			case 4: return SECONDARY12;
			case 5: return TERTIARY;
			default:
				throw new RuntimeException("Cannot parse education from code: " + code);
			}
		}
	}

	
	public static enum Disability{
		UNKNOWN (0, "Unknown"),
		NONE (1, "None"),
		SIGHT (2, "Blind/poor sight"),
		HEARING (3, "Deaf/poor hearing"),
		SPEAK (4, "Cannot speak properly"),
		WALK (5, "Cannot walk without aid"),
		MENTAL (6, "Mental"),
		OTHER (7, "Other");
		
		private final int code; /* Code used in survey. */
		private final String description;
		
		Disability(int code, String descr){
			this.code = code;
			this.description = descr;
		}
		
		int getCode(){ return this.code; }
		
		String getDescription(){ return this.description; }

		static Disability parseFromDescription(String descr){
			if(descr.equalsIgnoreCase("Unknown")){
				return UNKNOWN;
			} else if(descr.equalsIgnoreCase("None")){
				return NONE;
			} else if (descr.equalsIgnoreCase("Blind/poor sight")){
				return SIGHT;
			} else if (descr.equalsIgnoreCase("Deaf/poor hearing")){
				return HEARING;
			} else if (descr.equalsIgnoreCase("Cannot speak properly")){
				return SPEAK;
			} else if (descr.equalsIgnoreCase("Cannot walk without aid")){
				return WALK;
			} else if (descr.equalsIgnoreCase("Mental")){
				return MENTAL;
			} else if (descr.equalsIgnoreCase("Other")){
				return OTHER;
			} else{
				throw new RuntimeException("Cannot parse disability from description: " + descr);
			}
		}
		
		static Disability parseFromCode(int code){
			switch (code) {
			case 0: return UNKNOWN;
			case 1: return NONE;
			case 2:	return SIGHT;
			case 3:	return HEARING;
			case 4: return SPEAK;
			case 5: return WALK;
			case 6: return MENTAL;
			case 7: return OTHER;
			default:
				throw new RuntimeException("Cannot parse disability from code: " + code);
			}
		}
	}
	
	
	public static enum LicenseCar{
		UNKNOWN (0, "Unknown"),
		YES (1, "Yes"),
		NO (2, "No");
		
		private final int code; /* Code used in survey. */
		private final String description;
		
		LicenseCar(int code, String descr){
			this.code = code;
			this.description = descr;
		}
		
		int getCode(){ return this.code; }
		
		String getDescription(){ return this.description; }
		
		static LicenseCar parseFromDescription(String descr){
			if(descr.equalsIgnoreCase("Unknown")){
				return UNKNOWN;
			} else if(descr.equalsIgnoreCase("Yes")){
				return YES;
			} else if (descr.equalsIgnoreCase("No")){
				return NO;
			} else{
				throw new RuntimeException("Cannot parse car license from description: " + descr);
			}
		}
		
		static LicenseCar parseFromCode(int code){
			switch (code) {
			case 0: return UNKNOWN;
			case 1:	return YES;
			case 2:	return NO;
			default:
				throw new RuntimeException("Cannot parse car license from code: " + code);
			}
		}
	}
	
	
	public static enum LicenseMotorcycle{
		UNKNOWN (0, "Unknown"),
		YES (1, "Yes"),
		NO (2, "No");
		
		private final int code; /* Code used in survey. */
		private final String description;
		
		LicenseMotorcycle(int code, String descr){
			this.code = code;
			this.description = descr;
		}
		
		int getCode(){ return this.code; }
		
		String getDescription(){ return this.description; }
		
		static LicenseMotorcycle parseFromDescription(String descr){
			if(descr.equalsIgnoreCase("Unknown")){
				return UNKNOWN;
			} else if(descr.equalsIgnoreCase("Yes")){
				return YES;
			} else if (descr.equalsIgnoreCase("No")){
				return NO;
			} else{
				throw new RuntimeException("Cannot parse motorcycle license from description: " + descr);
			}
		}
		
		static LicenseMotorcycle parseFromCode(int code){
			switch (code) {
			case 0: return UNKNOWN;
			case 1:	return YES;
			case 2:	return NO;
			default:
				throw new RuntimeException("Cannot parse motorcycle license from code: " + code);
			}
		}
	}
	
	
	public static enum LicenseHeavyVehicle{
		UNKNOWN (0, "Unknown"),
		YES (1, "Yes"),
		NO (2, "No");
		
		private final int code; /* Code used in survey. */
		private final String description;
		
		LicenseHeavyVehicle(int code, String descr){
			this.code = code;
			this.description = descr;
		}
		
		int getCode(){ return this.code; }
		
		String getDescription(){ return this.description; }
		
		static LicenseHeavyVehicle parseFromDescription(String descr){
			if(descr.equalsIgnoreCase("Unknown")){
				return UNKNOWN;
			} else if(descr.equalsIgnoreCase("Yes")){
				return YES;
			} else if (descr.equalsIgnoreCase("No")){
				return NO;
			} else{
				throw new RuntimeException("Cannot parse heavy vehicle license from description: " + descr);
			}
		}
		
		static LicenseHeavyVehicle parseFromCode(int code){
			switch (code) {
			case 0: return UNKNOWN;
			case 1:	return YES;
			case 2:	return NO;
			default:
				throw new RuntimeException("Cannot parse heavy vehicle license from code: " + code);
			}
		}
	}
	
	
	public static enum Employment{
		UNKNOWN (0, "Unknown"),
		FULLTIME (1, "Employed full time"),
		PARTTIME (2, "Employed part time"),
		SELFEMPLOYED (3, "Self employed"),
		UNEMPLOYED_NOT_LOOKING (4, "Unemployed: not looking"),
		UNEMPLOYED_LOOKING (5, "Unemployed: looking"),
		CONTRACT (6, "Contract/seasonal"),
		PENSIONER (7, "Pensioner"),
		STUDENT (8, "Tertiary student"),
		SCHOLAR (9, "Scholar/learner"),
		HOUSEWIFE (10, "Housewife");
		
		private final int code; /* Code used in survey. */
		private final String description;
		
		Employment(int code, String descr){
			this.code = code;
			this.description = descr;
		}
		
		int getCode(){ return this.code; }
		
		String getDescription(){ return this.description; }
		
		public static Employment parseFromDescription(String descr){
			if(descr.equalsIgnoreCase("Unknown")){
				return UNKNOWN;
			} else if(descr.equalsIgnoreCase("Employed full time")){
				return FULLTIME;
			} else if (descr.equalsIgnoreCase("Employed part time")){
				return PARTTIME;
			} else if (descr.equalsIgnoreCase("Self employed")){
				return SELFEMPLOYED;
			} else if (descr.equalsIgnoreCase("Unemployed: not looking")){
				return UNEMPLOYED_NOT_LOOKING;
			} else if (descr.equalsIgnoreCase("Unemployed: looking")){
				return UNEMPLOYED_LOOKING;
			} else if (descr.equalsIgnoreCase("Contract/seasonal")){
				return CONTRACT;
			} else if (descr.equalsIgnoreCase("Pensioner")){
				return PENSIONER;
			} else if (descr.equalsIgnoreCase("Tertiary student")){
				return STUDENT;
			} else if (descr.equalsIgnoreCase("Scholar/learner")){
				return SCHOLAR;
			} else if (descr.equalsIgnoreCase("Housewife")){
				return HOUSEWIFE;
			} else{
				throw new RuntimeException("Cannot parse employment from description: " + descr);
			}
		}
		
		static Employment parseFromCode(int code){
			switch (code) {
			case 0: return UNKNOWN;
			case 1: return FULLTIME;
			case 2:	return PARTTIME;
			case 3:	return SELFEMPLOYED;
			case 4: return UNEMPLOYED_NOT_LOOKING;
			case 5: return UNEMPLOYED_LOOKING;
			case 6: return CONTRACT;
			case 7: return PENSIONER;
			case 8: return STUDENT;
			case 9: return SCHOLAR;
			case 10: return HOUSEWIFE;
			default:
				throw new RuntimeException("Cannot parse employment from code: " + code);
			}
		}
	}
	
	
	public static enum TravelToPrimary{
		UNKNOWN (0, "Unknown"),
		YES (1, "Yes"),
		NO (2, "No");
		
		private final int code; /* Code used in survey. */
		private final String description;
		
		TravelToPrimary(int code, String descr){
			this.code = code;
			this.description = descr;
		}
		
		int getCode(){ return this.code; }
		
		String getDescription(){ return this.description; }
		
		static TravelToPrimary parseFromDescription(String descr){
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
		
		static TravelToPrimary parseFromCode(int code){
			switch (code) {
			case 0: return UNKNOWN;
			case 1: return YES;
			case 2:	return NO;
			default:
				throw new RuntimeException("Cannot parse travel to work/education from code: " + code);
			}
		}
	}
	
	
	public static enum WorkFromHome{
		UNKNOWN (0, "Unknown"),
		YES (1, "Yes"),
		NO (2, "No");
		
		private final int code; /* Code used in survey. */
		private final String description;
		
		WorkFromHome(int code, String descr){
			this.code = code;
			this.description = descr;
		}
		
		int getCode(){ return this.code; }
		
		String getDescription(){ return this.description; }
		
		static WorkFromHome parseFromDescription(String descr){
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
		
		static WorkFromHome parseFromCode(int code){
			switch (code) {
			case 0: return UNKNOWN;
			case 1: return YES;
			case 2:	return NO;
			default:
				throw new RuntimeException("Cannot parse travel to work/education from code: " + code);
			}
		}
	}
	
	
	public static enum TravelForWork{
		UNKNOWN (0, "Unknown"),
		YES (1, "Yes"),
		NO (2, "No");
		
		private final int code; /* Code used in survey. */
		private final String description;
		
		TravelForWork(int code, String descr){
			this.code = code;
			this.description = descr;
		}
		
		int getCode(){ return this.code; }
		
		String getDescription(){ return this.description; }
		
		static TravelForWork parseFromDescription(String descr){
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
		
		static TravelForWork parseFromCode(int code){
			switch (code) {
			case 0: return UNKNOWN;
			case 1: return YES;
			case 2:	return NO;
			default:
				throw new RuntimeException("Cannot parse travel to work/education from code: " + code);
			}
		}
	}
	
	
	public static enum ModeOfTravel{
		UNKNOWN (0, "Unknown"),
		WALK (1, "Walk"),
		CAR_DRIVER (2, "Car driver"),
		CAR_PASSENGER (3, "Car passenger"),
		TRAIN (4, "Train"),
		BUS (5, "Bus"),
		MINIBUS_TAXI (6, "Minibus/taxi"),
		BICYCLE (7, "Bicycle"),
		MOTORCYCLE_DRIVER (8, "Motorcycle driver"),
		MOTORCYCLE_PASSENGER (9, "Motorcycle passenger"),
		MYCITI (10, "MyCiti bus"),
		EMPLOYER_TRANSPORT (11, "Employer transport"),
		SCHOLAR_TRANSPORT (12, "Scholar transport"),
		OTHER (13, "Other");
		
		private final int code; /* Code used in survey. */
		private final String description;
		
		ModeOfTravel(int code, String descr){
			this.code = code;
			this.description = descr;
		}
		
		int getCode(){ return this.code; }
		
		String getDescription(){ return this.description; }
		
		static ModeOfTravel parseFromDescription(String descr){
			if(descr.equalsIgnoreCase("Unknown")){
				return UNKNOWN;
			} else if (descr.equalsIgnoreCase("Walk")){
				return WALK;
			} else if (descr.equalsIgnoreCase("Car driver")){
				return CAR_DRIVER;
			} else if (descr.equalsIgnoreCase("Car passenger")){
				return CAR_PASSENGER;
			} else if (descr.equalsIgnoreCase("Train")){
				return TRAIN;
			} else if (descr.equalsIgnoreCase("Bus")){
				return BUS;
			} else if (descr.equalsIgnoreCase("Minibus/taxi")){
				return MINIBUS_TAXI;
			} else if (descr.equalsIgnoreCase("Bicycle")){
				return BICYCLE;
			} else if (descr.equalsIgnoreCase("Motorcycle driver")){
				return MOTORCYCLE_DRIVER;
			} else if (descr.equalsIgnoreCase("Motorcycle passenger")){
				return MOTORCYCLE_PASSENGER;
			} else if (descr.equalsIgnoreCase("MyCiti bus")){
				return MYCITI;
			} else if (descr.equalsIgnoreCase("Employer transport")){
				return EMPLOYER_TRANSPORT;
			} else if (descr.equalsIgnoreCase("Scholar transport")){
				return SCHOLAR_TRANSPORT;
			} else if (descr.equalsIgnoreCase("Other")){
				return OTHER;
			} else{
				throw new RuntimeException("Cannot parse mode of travel from description: " + descr);
			}
		}
		
		static ModeOfTravel parseFromCode(int code){
			switch (code) {
			case 0: return UNKNOWN;
			case 1: return WALK;
			case 2:	return CAR_DRIVER;
			case 3:	return CAR_PASSENGER;
			case 4: return TRAIN;
			case 5: return BUS;
			case 6: return MINIBUS_TAXI;
			case 7: return BICYCLE;
			case 8: return MOTORCYCLE_DRIVER;
			case 9: return MOTORCYCLE_PASSENGER;
			case 10: return MYCITI;
			case 11: return EMPLOYER_TRANSPORT;
			case 12: return SCHOLAR_TRANSPORT;
			case 13: return OTHER;
			default:
				throw new RuntimeException("Cannot parse mode of travel from code: " + code);
			}
		}
	}
	
	
	public static enum PaymentMethod{
		UNKNOWN (0, "Unknown"),
		SINGLE_TICKET (1, "Single ticket"),
		RETURN_TICKET (2, "Return ticket"),
		DAILY_TICKET (3, "Daily ticket"),
		MULITPLE_TRIP (4, "Multiple trip"),
		WEEKLY_TICKET (5, "Weekly ticket"),
		MONTHLY_TICKET (6, "Monthly ticket"),
		OTHER (7, "Other");
		
		private final int code; /* Code used in survey. */
		private final String description;
		
		PaymentMethod(int code, String descr){
			this.code = code;
			this.description = descr;
		}
		
		int getCode(){ return this.code; }
		
		String getDescription(){ return this.description; }
		
		static PaymentMethod parseFromDescription(String descr){
			if(descr.equalsIgnoreCase("Unknown")){
				return UNKNOWN;
			} else if (descr.equalsIgnoreCase("Single ticket")){
				return SINGLE_TICKET;
			} else if (descr.equalsIgnoreCase("Return ticket")){
				return RETURN_TICKET;
			} else if (descr.equalsIgnoreCase("Daily ticket")){
				return DAILY_TICKET;
			} else if (descr.equalsIgnoreCase("Multiple trip")){
				return MULITPLE_TRIP;
			} else if (descr.equalsIgnoreCase("Weekly ticket")){
				return WEEKLY_TICKET;
			} else if (descr.equalsIgnoreCase("Monthly ticket")){
				return MONTHLY_TICKET;
			} else if (descr.equalsIgnoreCase("Other")){
				return OTHER;
			} else{
				throw new RuntimeException("Cannot parse template from description: " + descr);
			}
		}
		
		static PaymentMethod parseFromCode(int code){
			switch (code) {
			case 0: return UNKNOWN;
			case 1: return SINGLE_TICKET;
			case 2:	return RETURN_TICKET;
			case 3:	return DAILY_TICKET;
			case 4: return MULITPLE_TRIP;
			case 5: return WEEKLY_TICKET;
			case 6: return MONTHLY_TICKET;
			case 7: return OTHER;
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

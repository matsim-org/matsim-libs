/* *********************************************************************** *
 * project: org.matsim.*
 * DiaryEnums.java
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
package playground.jjoubert.projects.capeTownDiary;


/**
 * Class to capture the various variable classes in the diary data provided
 * in the City of Cape Town 2013 travel survey.
 * 
 * @author jwjoubert
 */
public class DiaryEnums {
	
	static enum TripPurpose{
		UNKNOWN (0, "Unknown"),
		HOME (1, "Home"),
		WORK (2, "Work"),
		SCHOOL (3, "School"),
		TERTIARY (4, "Tertiary education"),
		PICKUP_CHILDREN (5, "Pick up / drop off children"),
		PICKUP_OTHER (6, "Pick up / drop off other person"),
		TRANSFER (7, "Transfer"),
		ERRAND_WORK (8, "Errand at work"),
		SHOPPING (9, "Shopping"),
		RECREATION (10, "Recreation"),
		FUEL (11, "Fuel station"),
		MEDICARE (12, "Medicare"),
		SERVICE (13, "Post office/bank/municipality, etc."),
		VISIT (14, "Visit a person"),
		WATER (15, "Fetch water"),
		TEND_ANIMALS (16, "Tend to animals"),
		OTHER1 (17, "Other (1)"),
		OTHER2 (18, "Other (2)");
		
		private final int code; /* Code used in survey. */
		private final String description;
		
		TripPurpose(int code, String descr){
			this.code = code;
			this.description = descr;
		}
		
		int getCode(){ return this.code; }
		
		String getDescription(){ return this.description; }
		
		static TripPurpose parseFromDescription(String descr){
			if(descr.equalsIgnoreCase("Unknown")){
				return UNKNOWN;
			} else if (descr.equalsIgnoreCase("Home")){
				return HOME;
			} else if (descr.equalsIgnoreCase("Work")){
				return WORK;
			} else if (descr.equalsIgnoreCase("School")){
				return SCHOOL;
			} else if (descr.equalsIgnoreCase("Tertiary education")){
				return TERTIARY;
			} else if (descr.equalsIgnoreCase("Pick up / drop off children")){
				return PICKUP_CHILDREN;
			} else if (descr.equalsIgnoreCase("Pick up / drop off other person")){
				return PICKUP_OTHER;
			} else if (descr.equalsIgnoreCase("Transfer")){
				return TRANSFER;
			} else if (descr.equalsIgnoreCase("Errand at work")){
				return ERRAND_WORK;
			} else if (descr.equalsIgnoreCase("Shopping")){
				return SHOPPING;
			} else if (descr.equalsIgnoreCase("Recreation")){
				return RECREATION;
			} else if (descr.equalsIgnoreCase("Fuel station")){
				return FUEL;
			} else if (descr.equalsIgnoreCase("Medicare")){
				return MEDICARE;
			} else if (descr.equalsIgnoreCase("Post office/bank/municipality, etc.")){
				return SERVICE;
			} else if (descr.equalsIgnoreCase("Visit a person")){
				return VISIT;
			} else if (descr.equalsIgnoreCase("Fetch water")){
				return WATER;
			} else if (descr.equalsIgnoreCase("Tend to animals")){
				return TEND_ANIMALS;
			} else if (descr.equalsIgnoreCase("Other (1)")){
				return OTHER1;
			} else if (descr.equalsIgnoreCase("Other (2)")){
				return OTHER2;
			} else{
				throw new RuntimeException("Cannot parse trip purpose from description: " + descr);
			}
		}
		
		static TripPurpose parseFromCode(int code){
			switch (code) {
			case -999: return UNKNOWN;
			case 0: return UNKNOWN;
			case 1: return HOME;
			case 2:	return WORK;
			case 3:	return SCHOOL;
			case 4: return TERTIARY;
			case 5: return PICKUP_CHILDREN;
			case 6: return PICKUP_OTHER;
			case 7: return TRANSFER;
			case 8: return ERRAND_WORK;
			case 9: return SHOPPING;
			case 10: return RECREATION;
			case 11: return FUEL;
			case 12: return MEDICARE;
			case 13: return SERVICE;
			case 14: return VISIT;
			case 15: return WATER;
			case 16: return TEND_ANIMALS;
			case 17: return OTHER1;
			case 18: return OTHER2;
			default:
				throw new RuntimeException("Cannot parse trip purpose from code: " + code);
			}
		}
		
		public String getMatsimActivityCode(){
			switch (this) {
			case UNKNOWN:
			case PICKUP_OTHER:
			case TRANSFER:
			case FUEL:
			case SERVICE:
			case WATER:
			case TEND_ANIMALS:
			case OTHER1:
			case OTHER2: 
				return "o";
			case HOME: 
				return "h";
			case WORK: 
			case ERRAND_WORK:
				return "w";
			case SCHOOL:
				return "e1";
			case TERTIARY:
				return "e2";
			case PICKUP_CHILDREN:
				return "e3";
			case SHOPPING:
				return "s";
			case RECREATION:
				return "l";
			case VISIT:
				return "v";
			case MEDICARE:
				return "m";
			}			
			return "o";
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
			case -999:
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
		
		public String getMatsimMode(){
			switch (this) {
			case UNKNOWN:
			case BICYCLE:
			case EMPLOYER_TRANSPORT:
			case SCHOLAR_TRANSPORT:
			case OTHER:
				return "other";
			case WALK:
				return "walk";
			case CAR_DRIVER:
			case MOTORCYCLE_DRIVER:
				return "car";
			case CAR_PASSENGER:
			case MOTORCYCLE_PASSENGER:
				return "ride";
			case MINIBUS_TAXI:
				return "taxi";
			case MYCITI:
				return "brt";
			case BUS:
				return "bus";
			case TRAIN:
				return "rail";
			}
			return "other";
		}
	}
	
	
	static enum Template{
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

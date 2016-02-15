package org.matsim.contrib.accessibility;

public class FacilityTypes {
	public static final String HOME = "home";
	public static final String WORK = "work";
	public static final String EDUCATION = "education";
	public static final String LEISURE = "leisure";
	public static final String SHOPPING = "shopping";
	public static final String POLICE = "police";
	public static final String MEDICAL = "medical";
	public static final String OTHER = "other";
	public static final String IGNORE = "ignore";
	
	// added for smaller-scale analyses, i.e. Kibera slum in Nairobi, Kenya
	public static final String DRINKING_WATER = "drinking_water";
	public static final String TOILETS = "toilets";
	// using "hospital", "pharmacy", and "clinic", "medical" should then
	// be interpreted as "other medical"
	public static final String HOSPITAL = "hospital";
	public static final String PHARMACY = "pharmacy";
	public static final String CLINIC = "clinic";
}
package playground.dhosse.utils;

import playground.dhosse.gap.scenario.mid.MiDSurveyPerson;

public class EgapHashGenerator {
	
	public static String generateMiDPersonHash(MiDSurveyPerson person){
		
		int lowerBound = (int)(person.getAge()/10) * 10;
		int upperBound = lowerBound + 9;
		String ageClass = lowerBound + "_" + upperBound;
		
		return ("ageClass=" + ageClass + "_sex=" + person.getSex() + "_carAvail=" + person.getCarAvailable() + "_hasLicense=" + person.isHasLicense() + "_employed=" + person.isEmployed());
		
	}
	
	public static String generatePersonHash(int age, int sex, boolean carAvail, boolean hasLicense, boolean isEmployed){
		
		int lowerBound = (int)(age/10) * 10;
		int upperBound = lowerBound + 9;
		String ageClass = lowerBound + "_" + upperBound;
		
		return ("ageClass=" + ageClass + "_sex=" + sex + "_carAvail=" + carAvail + "_hasLicense=" + hasLicense + "_employed=" + isEmployed);
	
	}
	
	public static String generatePersonGroupHash(int age, int sex){
		
		int lowerBound = (int)(age/10) * 10;
		int upperBound = lowerBound + 9;
		String ageClass = lowerBound + "_" + upperBound;
		
		return ("ageClass=" + ageClass + "_sex=" + sex);
		
	}
	
	public static String generateAgeGroupHash(int age){
		
		int lowerBound = (int)(age/10) * 10;
		int upperBound = lowerBound + 9;
		return (lowerBound + "_" + upperBound);
		
	}
	
	public static String generateAgeGroupHash(int a0, int aX){
		
		return (a0 + "_" + aX);
		
	}

}

package playground.dhosse.scenarios.generic.population;

import playground.dhosse.scenarios.generic.population.io.mid.MiDSurveyPerson;

public class HashGenerator {

	public static String generateMiDPersonHash(MiDSurveyPerson person){
		
		int lowerBound = (int)(person.getAge()/10) * 10;
		int upperBound = lowerBound + 9;
		String ageClass = lowerBound + "_" + upperBound;
		
		return ("ageClass=" + ageClass + "_sex=" + person.getSex() + "_carAvail=" + person.getCarAvailable() +
				"_hasLicense=" + person.isHasLicense() + "_employed=" + person.isEmployed());
		
	}
	
	public static String generatePersonHash(int age, int sex, boolean carAvail, boolean hasLicense, boolean isEmployed){
		
		int lowerBound = (int)(age/10) * 10;
		int upperBound = lowerBound + 9;
		String ageClass = lowerBound + "_" + upperBound;
		
		return ("ageClass=" + ageClass + "_sex=" + sex + "_carAvail=" + carAvail +
				"_hasLicense=" + hasLicense + "_employed=" + isEmployed);
	
	}
	
	public static String generatePersonHash(String age, String sex, String carAvail, String isEmployed){
		
		int lowerBound = (int)(Integer.parseInt(age)/10) * 10;
		int upperBound = lowerBound + 9;
		String ageClass = lowerBound + "_" + upperBound;
		
		return ("ageClass=" + ageClass + "_sex=" + sex + "_carAvail=" + carAvail +
				"_employed=" + isEmployed);
	
	}
	
}

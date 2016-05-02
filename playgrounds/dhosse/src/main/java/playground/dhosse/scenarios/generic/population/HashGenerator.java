package playground.dhosse.scenarios.generic.population;

import playground.dhosse.scenarios.generic.population.io.mid.MiDPerson;

public class HashGenerator {
	
	private HashGenerator(){};

	public static String generateAgeGroupHash(MiDPerson person){
		
		if(person.getAge() < 18){
			return PersonUtils.CHILD;
		} else if(person.getAge() <= 65){
			return PersonUtils.ADULT;
		} else{
			return PersonUtils.PENSIONER;
		}
		
	}
	
	public static String generateMiDPersonHash(MiDPerson person){
		
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

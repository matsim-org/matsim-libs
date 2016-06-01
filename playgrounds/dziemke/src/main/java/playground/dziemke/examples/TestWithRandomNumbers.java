package playground.dziemke.examples;

import java.util.Random;

public class TestWithRandomNumbers {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		double inputValue = 5.532;
		double inputValueDividedByFive = inputValue / 5;
		System.out.println("Input value divided by 5 is = " + inputValueDividedByFive);
		// Math.floor returns next lower integer number (but as a double value)
		int outputValue = (int)Math.floor(inputValueDividedByFive);
		System.out.println("Output value is = " + outputValue);
		
		int counter = 0;
		for (int i=0; i <1000; i++) {
			Random random = new Random();
			double randomNumber = random.nextDouble();
			if (randomNumber < 0.5) {
				counter++;
			}
		}
		
		System.out.println("counter: " + counter);
		
		
		Random random2 = new Random();
		double randomNumber2 = random2.nextDouble();
		if (randomNumber2 < 0.5) {
			System.out.println("smaller");
		} else {
			System.out.println("larger");
		}
		
		
		System.out.println("age: " + getAge());	
	}
	
	
	private static int getAge() {
		int ageRange = getAgeRange();
		
        // Es ist wichtig darauf zu achten, dass nach Ausführung einer Anweisung der Schleifendurchlauf mit "break"
        // unterbrochen wird, da die folgenden Sprungmarken sonst ebenfalls geprüft und ggf. ausgeführt werden.
        // Trifft keine Übereinstimmung zu, kann optional mit der Sprungmarke default eine Standardanweisung ausgeführt werden. 
        switch (ageRange) {
            case 1:	return getAgeInRange(18, 19);
            case 2:	return getAgeInRange(20, 24);
            case 3:	return getAgeInRange(25, 29);
            case 4:	return getAgeInRange(30, 34);
            case 5:	return getAgeInRange(35, 39);
            case 6:	return getAgeInRange(40, 44);
            case 7:	return getAgeInRange(45, 59);
            case 8:	return getAgeInRange(60, 64);
            case 9:	return getAgeInRange(65, 90);
            default: 
            	System.err.println("No age range met.");
            	return -1;
        }
	}
	
	
	private static int getAgeRange() {
		Random r = new Random();
		// cf. p. 11 of statistic of 2012
		int populationInWorkingAge = 2932167;
		double randomNumber = r.nextDouble() * populationInWorkingAge;
		if (randomNumber < 54469) {return 1;}
		if (randomNumber < 54469+222434) {return 2;}
		if (randomNumber < 54469+222434+284440) {return 3;}
		if (randomNumber < 54469+222434+284440+277166) {return 4;}
		if (randomNumber < 54469+222434+284440+277166+228143) {return 5;}
		if (randomNumber < 54469+222434+284440+277166+228143+256192) {return 6;}
		if (randomNumber < 54469+222434+284440+277166+228143+256192+755482) {return 7;}
		if (randomNumber < 54469+222434+284440+277166+228143+256192+755482+198908) {return 8;}
		if (randomNumber < 54469+222434+284440+277166+228143+256192+755482+198908+654933) {return 9;}
		else {
			System.err.println("No age selected.");
			return -1;
		}
	}
	
	
	private static int getAgeInRange(int rangeMin, int rangeMax) {
		Random r = new Random();
		int randomAge = (int) (rangeMin + (rangeMax - rangeMin) * r.nextDouble());
		return randomAge;
	}
}

package playground.mmoyo.utils;

import java.util.Random;
import org.matsim.core.gbl.MatsimRandom;

public class TransposedNormalRandom {
	final double avg;
	final double dvStandard;
	final Random randomGenerator;
	
	public TransposedNormalRandom(double avg, double dvStandard, final Random randomGenerator){
		this.avg = avg;
		this.dvStandard = dvStandard;
		this.randomGenerator = randomGenerator; 
	}
	
	public double getTransposedRandomGaussian(){
		double random = randomGenerator.nextGaussian();
		double transpRandom = avg + (dvStandard*random);
		return Math.floor( transpRandom * 100) / 100;
	} 			

	private void getTransposedRandoms(int x){
		System.out.println("Transposed Randoms:");
		for (int i = 1; i <= x; i++){
			System.out.println(this.getTransposedRandomGaussian());
		}
	}

	public static void main(String[] args) {
		TransposedNormalRandom tnr = new TransposedNormalRandom(720.0 , 150.0, MatsimRandom.getLocalInstance());
		tnr.getTransposedRandoms(1000);

		tnr = new TransposedNormalRandom(720.0 , 150.0, MatsimRandom.getLocalInstance());
		tnr.getTransposedRandoms(1000);
	}
	
}

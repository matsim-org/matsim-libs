package pedCA.utility;

import java.util.Random;

public class RandomExtractor{
	
	public static Random generator = new Random(Constants.RANDOM_SEED);
	
	public static double nextDouble(){
		return generator.nextDouble();
	}
	
	public static int nextInt(int limit){
		return generator.nextInt(limit);
	}
	
}

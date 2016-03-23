package playground.dziemke.examples;

import org.junit.Assert;


public class TestOther {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		double value = 1.62345234523;
		
		value++;
				
		System.out.println(value);
		
		Assert.assertNotNull(value);
		
		
		System.out.println(Integer.MAX_VALUE);
		System.out.println(Integer.MIN_VALUE);
		System.out.println(Double.MAX_VALUE);
		System.out.println(Double.MIN_VALUE);
		
		
		Double cellSize = 1000.;
		System.out.println(cellSize.toString().split("\\.")[0]);
				
	}
}

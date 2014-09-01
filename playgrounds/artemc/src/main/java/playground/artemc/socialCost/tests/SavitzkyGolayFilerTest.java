package playground.artemc.socialCost.tests;

import java.math.BigDecimal;
import java.math.RoundingMode;

import playground.artemc.socialCost.SavitzkyGolayFilter;

public class SavitzkyGolayFilerTest {


	private static double[] data = {0.0,0.0,1.0,10.0,9.0,8.0,5.0,1.0,0.0};

	private static double[] smoothDataResult5 = {-0.086,-0.514,3.143,7.6,10.029,7.743,4.743,1.514,-0.086};
	private static double[] smoothDataResult7 = {-0.81,0.857,3.714,6.857,8.905,8.143,4.571,2.048,0.238};
	

	public static void main(String[] args) {

		boolean result = true;
		
		SavitzkyGolayFilter sgf5 = new SavitzkyGolayFilter(5, data, false);
		double[] smoothData5 = sgf5.appllyFilter();
		for(int i=0;i<data.length;i++){
			System.out.print(data[i]);

			if(i==data.length-1){
				System.out.print("\n");
			}
			else{
				System.out.print("\t");
			}
		}
		
		for(int i=0;i<smoothData5.length;i++){
			
			if(round(smoothData5[i],3)!=smoothDataResult5[i]){
				result = false;
			}
			
			System.out.print(round(smoothData5[i],3));
			if(i==smoothData5.length-1){
				System.out.print("\n");
			}
			else{
				System.out.print("\t");
			}
		}
		
		System.out.println();
		
		SavitzkyGolayFilter sgf7 = new SavitzkyGolayFilter(7, data, false);
		double[] smoothData7 = sgf7.appllyFilter();
		for(int i=0;i<data.length;i++){
			System.out.print(data[i]);

			if(i==data.length-1){
				System.out.print("\n");
			}
			else{
				System.out.print("\t");
			}
		}
		
		for(int i=0;i<smoothData7.length;i++){
			
			if(round(smoothData7[i],3)!=smoothDataResult7[i]){
				result = false;
			}
			
			System.out.print(round(smoothData7[i],3));

			if(i==smoothData7.length-1){
				System.out.print("\n");
			}
			else{
				System.out.print("\t");
			}
		}
		
		if(result){
		System.out.println("Success!");
		}
		else{
			System.out.println("Erorr!");
		}
	}
	
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}

}

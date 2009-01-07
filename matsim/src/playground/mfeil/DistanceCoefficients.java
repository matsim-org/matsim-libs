package playground.mfeil;

import java.util.ArrayList;

public class DistanceCoefficients {
	
	private double [] coef;
	private final ArrayList<String> namesOfCoef;
	
	public DistanceCoefficients (double [] coef, ArrayList<String> names){
		this.coef = coef;
		this.namesOfCoef = names;
		for (int i=0;i<this.namesOfCoef.size();i++){
			System.out.println(this.coef[i]);
			System.out.println(this.namesOfCoef.get(i));
		}
	}
	
	public double [] getCoef (){
		return this.coef;
	}
	
	public double getSingleCoef (int pos){
		return this.coef[pos];
	}
	
	public double getSingleCoef (String nameOfCoef){
		for (int i=0;i<this.coef.length;i++){
			if (nameOfCoef.equals(this.namesOfCoef.get(i))) {
				return this.coef[i];
			}
		}
		return 0;
	}
	
	public void setCoef (double []a){
		this.coef = a;
	}
	
	public void setSingleCoef (double a, int pos){
		this.coef[pos] = a;
	}
}

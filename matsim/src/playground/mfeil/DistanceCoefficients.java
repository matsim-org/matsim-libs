package playground.mfeil;

import java.util.ArrayList;

public class DistanceCoefficients {
	
	private double [] softCoef;
	private final ArrayList<String> namesOfSoftCoef;
	private final ArrayList<String> namesOfAllCoef;
	
	public DistanceCoefficients (double [] coef, ArrayList<String> softNames, ArrayList<String> allNames){
		this.softCoef = coef;
		this.namesOfSoftCoef = softNames;
		this.namesOfAllCoef = allNames;
		for (int i=0;i<this.namesOfSoftCoef.size();i++){
			System.out.println(this.softCoef[i]);
			System.out.println(this.namesOfSoftCoef.get(i));
		}
	}
	
	public double [] getCoef (){
		return this.softCoef;
	}
	
	public double getSingleCoef (int pos){
		return this.softCoef[pos];
	}
	
	public double getSingleCoef (String nameOfCoef){
		for (int i=0;i<this.softCoef.length;i++){
			if (nameOfCoef.equals(this.namesOfSoftCoef.get(i))) {
				return this.softCoef[i];
			}
		}
		return 0;
	}
	
	public ArrayList<String> getNamesOfCoef (){
		return this.namesOfAllCoef;
	}
	
	public void setCoef (double []a){
		this.softCoef = a;
	}
	
	public void setSingleCoef (double a, int pos){
		this.softCoef[pos] = a;
	}
}

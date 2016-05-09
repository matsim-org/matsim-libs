package playground.dhosse.scenarios.generic.population.io.mid;

import java.util.ArrayList;
import java.util.List;

public class MiDHousehold {
	
	private String id;
	
	private List<String> memberIds;
	private int nCars;
	private double hhIncome;
	
	public MiDHousehold(String id){
		
		this.id = id;
		this.memberIds = new ArrayList<String>();
		
	}

	public String getId(){
		
		return this.id;
		
	}
	
	public List<String> getMemberIds(){
		
		return this.memberIds;
		
	}
	
	public int getNCars(){
		
		return this.nCars;
		
	}
	
	public void setNCars(int n){
		
		this.nCars = n;
		
	}
	
	public double getIncome(){
		
		return this.hhIncome;
		
	}
	
	public void setIncome(double income){
		
		this.hhIncome = income;
		
	}
	
}

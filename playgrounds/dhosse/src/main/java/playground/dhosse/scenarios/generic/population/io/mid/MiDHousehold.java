package playground.dhosse.scenarios.generic.population.io.mid;

import java.util.ArrayList;
import java.util.List;

import org.matsim.households.Income;

public class MiDHousehold {
	
	private String id;
	
	private List<String> memberIds;
	private int nCars;
	private Income hhIncome;
	
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
	
	public Income getIncome(){
		
		return this.hhIncome;
		
	}
	
	public void setIncome(Income income){
		
		this.hhIncome = income;
		
	}
	
}

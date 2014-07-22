package playground.artemc.scenarioTools;

public class Household {

	private String hh_number;
	private Integer children;
	private Integer adults;
	private Integer pensioners;
	private Integer income;
	private Double stopDistance;
	private Integer unitsInBuilding;
	

	public Household(String id){
		this.hh_number = id;
		this.children = 0;
		this.adults = 0;
		this.pensioners = 0;
		this.income=0;	
		this.stopDistance = 0.0;	
		this.unitsInBuilding = 1;
	}

	
	public Integer getUnitsInBuilding() {
		return unitsInBuilding;
	}


	public void setUnitsInBuilding(Integer unitsInBuilding) {
		this.unitsInBuilding = unitsInBuilding;
	}


	public String getHh_number() {
		return hh_number;
	}


	public void setHh_number(String hh_number) {
		this.hh_number = hh_number;
	}


	public Integer getChildren() {
		return children;
	}


	public void setChildren(Integer children) {
		this.children = children;
	}


	public Integer getAdults() {
		return adults;
	}


	public void setAdults(Integer adults) {
		this.adults = adults;
	}


	public Integer getPensioners() {
		return pensioners;
	}


	public void setPensioners(Integer pensioners) {
		this.pensioners = pensioners;
	}


	public Integer getIncome() {
		return income;
	}


	public void setIncome(Integer income) {
		this.income = income;
	}
	
	public Double getStopDistance() {
		return stopDistance;
	}


	public void setStopDistance(Double stopDistance) {
		this.stopDistance = stopDistance;
	}
}

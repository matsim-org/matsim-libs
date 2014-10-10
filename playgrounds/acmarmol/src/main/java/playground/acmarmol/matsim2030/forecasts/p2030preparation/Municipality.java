package playground.acmarmol.matsim2030.forecasts.p2030preparation;

import org.matsim.api.core.v01.Id;

public class Municipality {

	private Id<Municipality> id;
	private boolean isOZ;
	private double rz_mz_ov;
	private double rz_mz_iv;
	private int gross_region;
	private double[] population;
	private int employment;
	private int RG_werk;
	private String name;
	
	
	public Municipality(Id<Municipality> id){
		this.id = id;
		this.population = new double[3];
				
	}
	
	
	public Id<Municipality> getId() {
		return id;
	}
	public void setId(Id<Municipality> id) {
		this.id = id;
	}
	public boolean isOZ() {
		return isOZ;
	}
	public void setOZ(boolean isOZ) {
		this.isOZ = isOZ;
	}
	public double getRz_mz_ov() {
		return rz_mz_ov;
	}
	public void setRz_mz_ov(double rz_mz_ov) {
		this.rz_mz_ov = rz_mz_ov;
	}
	public double getRz_mz_iv() {
		return rz_mz_iv;
	}
	public void setRz_mz_iv(double rz_mz_iv) {
		this.rz_mz_iv = rz_mz_iv;
	}
	public int getGross_region() {
		return gross_region;
	}
	public void setGross_region(int gross_region) {
		this.gross_region = gross_region;
	}
	public double[] getPopulation() {
		return population;
	}
	public void setPopulation(double[] population) {
		this.population = population;
	}
	public int getEmployment() {
		return employment;
	}
	public void setEmployment(int employment) {
		this.employment = employment;
	}
	public int getRG_werk() {
		return RG_werk;
	}
	public void setRG_werk(int rG_werk) {
		RG_werk = rG_werk;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}
	
	
	
	
	
}

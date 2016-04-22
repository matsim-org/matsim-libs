package playground.dhosse.scenarios.generic.utils;

import com.vividsolutions.jts.geom.Geometry;

public class AdministrativeUnit {
	
	private String id;
	
	private Integer regionType;
	private Integer districtType;
	private Integer municipalityType;
	
	private int nInhabitants;
	private double pChild;
	private double pAdult;
	private double pPensioner;
	
	private Geometry geometry;
	
	public AdministrativeUnit(String id){
		
		this.id = id;
		
	}
	
	public String getId(){
		
		return this.id;
		
	}

	public Integer getRegionType() {
	
		return this.regionType;
		
	}

	public void setRegionType(Integer regionType) {
		
		this.regionType = regionType;
		
	}

	public Integer getDistrictType() {
		
		return this.districtType;
		
	}

	public void setDistrictType(Integer districtType) {
		
		this.districtType = districtType;
		
	}

	public Integer getMunicipalityType() {
		
		return this.municipalityType;
		
	}

	public void setMunicipalityType(Integer municipalityType) {
		
		this.municipalityType = municipalityType;
		
	}

	public Geometry getGeometry() {
		return geometry;
	}

	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}
	
	public int getNumberOfInhabitants(){
		return this.nInhabitants;
	}

	public double getpChild() {
		return pChild;
	}

	public double getpAdult() {
		return pAdult;
	}

	public double getpPensioner() {
		return pPensioner;
	}

}
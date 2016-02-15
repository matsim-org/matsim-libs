package patryk.popgen2;

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Geometry;

public class Building {
	
	private Geometry geometry;
	private int buildingSize;
	
	private boolean singleFamilyBuilding = false;
	private boolean multiFamilyBuilding = false;
	private boolean workBuilding = false;

	private ArrayList<String> homeBuildingTypes;
	
	public Building(Geometry geometry, int buildingSize) {
		this.geometry = geometry;
		this.buildingSize = buildingSize;
		
		this.homeBuildingTypes = new ArrayList<>();
		this.homeBuildingTypes.add("Bostad; Flerfamiljshus");
		this.homeBuildingTypes.add("Bostad; Ospecificerad");
		this.homeBuildingTypes.add("Bostad; Småhus friliggande");
		this.homeBuildingTypes.add("Bostad; Småhus med flera lägenheter");
		this.homeBuildingTypes.add("Bostad; Småhus radhus");
		this.homeBuildingTypes.add("Bostad; Småhus kedjehus");
	}
	
	public void setBuildingType(String buildingType) {

		if (homeBuildingTypes.subList(0,1).contains(buildingType)) {
			multiFamilyBuilding = true;
		}
		else if (homeBuildingTypes.subList(2,5).contains(buildingType)) {
			singleFamilyBuilding = true;
		}
		else {
			workBuilding = true;
		}
	}
	
	
	public boolean isHomeBuilding() {
		if (singleFamilyBuilding == true || multiFamilyBuilding == true) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public boolean isVilla() {
		return singleFamilyBuilding;
	}
	
	public boolean isApartmentBuilding() {
		return multiFamilyBuilding;
	}
	
	public boolean isWorkBuilding() {
		return workBuilding;
	}
	
	public Geometry getGeometry() {
		return geometry;
	}
	
	public int getBuildingSize() {
		return buildingSize;
	}
}

package patryk.popgen2;

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Geometry;

public class Zone {
	
	private String zoneId;
	private Geometry geometry;
	private ArrayList<Building> buildings;
	private ArrayList<Building> homeBuildings;
	private ArrayList<Building> singleFamilyBuildings;
	private ArrayList<Building> multiFamilyBuildings;
	private ArrayList<Building> workBuildings;
	private ArrayList<Integer> multiFamilyBuildingSizes;
	private ArrayList<Integer> workBuildingSizes;
	
	public Zone(String zoneId) {
		this.zoneId = zoneId;
		this.buildings = new ArrayList<>();
		this.singleFamilyBuildings = new ArrayList<>();
		this.multiFamilyBuildings = new ArrayList<>();
		this.workBuildings = new ArrayList<>();
		this.multiFamilyBuildingSizes = new ArrayList<>();
		this.workBuildingSizes = new ArrayList<>();
		this.homeBuildings = new ArrayList<>();
	}
	
	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}
	
	public Geometry getGeometry() {
		return geometry;
	}
	
	public void addBuilding(Building building) {
		buildings.add(building);
		
		if(building.isVilla()) {
			singleFamilyBuildings.add(building);
			homeBuildings.add(building);
		}
		else if (building.isApartmentBuilding()) {
			multiFamilyBuildings.add(building);
			multiFamilyBuildingSizes.add(building.getBuildingSize());
			homeBuildings.add(building);
		}
		else if (building.isWorkBuilding()) {
			workBuildings.add(building);
			workBuildingSizes.add(building.getBuildingSize());
		}
		else {
			System.out.println("Undefined building type. Building not added to zone.");
		}
	}
	
	public ArrayList<Building> getHomeBuildings() {
		return homeBuildings;
	}
	
	public ArrayList<Building> getSingleFamilyBuildings() {
		return singleFamilyBuildings;
	}
	
	public ArrayList<Building> getMultiFamilyBuildings() {
		return multiFamilyBuildings;
	}
	
	public ArrayList<Building> getWorkBuildings() {
		return workBuildings;
	}
	
	public ArrayList<Integer> getMultiFamilyBuildingSizes() {
		return multiFamilyBuildingSizes;
	}
	
	public ArrayList<Integer> getWorkBuildingSizes() {
		return workBuildingSizes;
	}
	
	public String getId() {
		return zoneId;
	}

}

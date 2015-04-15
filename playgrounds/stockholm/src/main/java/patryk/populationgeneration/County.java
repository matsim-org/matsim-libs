package patryk.populationgeneration;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;

public class County {
	private final String key;
	private Geometry geometry;
	
	private ArrayList<Geometry> homebuildings;
	private ArrayList<Geometry> workbuildings;
	
	private List<String> homebuildingTypes;

	private ArrayList<Integer>  homeBuildingSizes;
	private ArrayList<Integer>  singleFamBuildingSizes;
	private ArrayList<Integer>  multiFamBuildingSizes;
	
	private ArrayList<Integer>  workBuildingSizes;
	
	private ArrayList<Geometry> singleFamilyBuildings;
	private ArrayList<Geometry> multiFamilyBuildings;
	
	public County(String key) {
		this.key = key;
		this.homebuildings = new ArrayList<>();
		this.workbuildings = new ArrayList<>();
		this.singleFamilyBuildings = new ArrayList<>();
		this.multiFamilyBuildings = new ArrayList<>();
		
		this.homebuildingTypes = new ArrayList<>();
		this.homebuildingTypes.add("Bostad; Flerfamiljshus");
		this.homebuildingTypes.add("Bostad; Ospecificerad");
		this.homebuildingTypes.add("Bostad; Sm�hus friliggande");
		this.homebuildingTypes.add("Bostad; Sm�hus med flera l�genheter");
		this.homebuildingTypes.add("Bostad; Sm�hus radhus");
		this.homebuildingTypes.add("Bostad; Sm�hus kedjehus");
		
		this.workBuildingSizes = new ArrayList<>();
		this.homeBuildingSizes = new ArrayList<>();
		this.singleFamBuildingSizes = new ArrayList<>();
		this.multiFamBuildingSizes = new ArrayList<>();
	}
	
	public String getKey() {
		return key;
	}

	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}
	
	public void addBuilding(Geometry geometry, String buildingtype) {
		if (homebuildingTypes.contains(buildingtype)){
			homebuildings.add(geometry);
			if (buildingtype.equals(homebuildingTypes.get(0))) {
				multiFamilyBuildings.add(geometry);
			}
			else {
				singleFamilyBuildings.add(geometry);
			}			
		}
		else {
			workbuildings.add(geometry);
		}
	}
	
	public void addArea(int area, String buildingtype) {
		if (!homebuildingTypes.contains(buildingtype)){
			workBuildingSizes.add(area);
		}
		else {
			homeBuildingSizes.add(area);
			if (buildingtype.equals(homebuildingTypes.get(0))) {
				multiFamBuildingSizes.add(area);
			}
			else {
				singleFamBuildingSizes.add(area);
			}
		}
	}
	
	public Geometry getGeometry() {
		return geometry;
	}
	
	public ArrayList<Geometry> getHomebuildings() {
		return homebuildings;
	}
	
	public ArrayList<Geometry> getWorkbuildings() {
		return workbuildings;
	}
	
	
	public ArrayList<Integer> getWorkBuildingSizes() {
		return workBuildingSizes;
	}
	
	
	public ArrayList<Integer> getHomeBuildingSizes() {
		return workBuildingSizes;
	}
	
	public ArrayList<Geometry> getMultiFamBuildings() {
		return multiFamilyBuildings;
	}
	
	public ArrayList<Geometry> getSingleFamBuildings() {
		return singleFamilyBuildings;
	}
	
	public ArrayList<Integer> getMultiFamBuildingSizes() {
		return multiFamBuildingSizes;
	}
	
	public ArrayList<Integer> getSingleFamBuildingSizes() {
		return singleFamBuildingSizes;
	}
	
}

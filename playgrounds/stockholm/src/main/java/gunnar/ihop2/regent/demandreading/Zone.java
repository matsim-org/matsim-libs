package gunnar.ihop2.regent.demandreading;

import java.util.ArrayList;
import java.util.List;

import patryk.popgen2.Building;

import com.vividsolutions.jts.geom.Geometry;

/**
 * 
 * @author Gunnar Flötteröd, pased on Patryk Larek
 *
 */
public class Zone {

	// -------------------- MEMBERS --------------------

	private final String zoneId;

	private final List<Building> buildings = new ArrayList<>();

	private final List<Building> homeBuildings = new ArrayList<>();

	private final List<Building> singleFamilyBuildings = new ArrayList<>();

	private final List<Building> multiFamilyBuildings = new ArrayList<>();

	private final List<Building> workBuildings = new ArrayList<>();

	private final List<Integer> multiFamilyBuildingSizes = new ArrayList<>();

	private final List<Integer> workBuildingSizes = new ArrayList<>();

	private Geometry geometry;

	// -------------------- CONSTRUCTION --------------------

	public Zone(final String zoneId) {
		this.zoneId = zoneId;
	}

	public void addBuilding(final Building building) {
		this.buildings.add(building);
		if (building.isSingleFamilyBuilding()) {
			this.singleFamilyBuildings.add(building);
			this.homeBuildings.add(building);
		} else if (building.isMultiFamilyBuilding()) {
			this.multiFamilyBuildings.add(building);
			this.multiFamilyBuildingSizes.add(building.getBuildingSize());
			this.homeBuildings.add(building);
		} else if (building.isWorkBuilding()) {
			this.workBuildings.add(building);
			this.workBuildingSizes.add(building.getBuildingSize());
		} else {
			throw new RuntimeException(
					"Undefined building type. Building not added to zone.");
		}
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public String getId() {
		return zoneId;
	}

	public void setGeometry(final Geometry geometry) {
		this.geometry = geometry;
	}

	public Geometry getGeometry() {
		return geometry;
	}

	public List<Building> getHomeBuildings() {
		return homeBuildings;
	}

	public List<Building> getSingleFamilyBuildings() {
		return singleFamilyBuildings;
	}

	public List<Building> getMultiFamilyBuildings() {
		return multiFamilyBuildings;
	}

	public List<Building> getWorkBuildings() {
		return workBuildings;
	}

	public List<Integer> getMultiFamilyBuildingSizes() {
		return multiFamilyBuildingSizes;
	}

	public List<Integer> getWorkBuildingSizes() {
		return workBuildingSizes;
	}
}

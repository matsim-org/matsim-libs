package org.matsim.contrib.carsharing.relocation.demand;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.config.CarsharingVehicleRelocationConfigGroup;
import org.matsim.contrib.carsharing.relocation.infrastructure.RelocationZone;
import org.matsim.contrib.carsharing.relocation.qsim.RelocationAgent;
import org.matsim.contrib.carsharing.relocation.qsim.RelocationAgentFactory;
import org.matsim.contrib.carsharing.relocation.qsim.RelocationInfo;
import org.matsim.contrib.carsharing.relocation.utils.RelocationAgentsReader;
import org.matsim.contrib.carsharing.relocation.utils.RelocationTimesReader;
import org.matsim.contrib.carsharing.relocation.utils.RelocationZonesReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.gis.PointFeatureFactory;

public class CarsharingVehicleRelocationContainer {
	private Scenario scenario;
	private Network network;
	private PointFeatureFactory pointFeatureFactory;

	public static final String ELEMENT_NAME = "carSharingRelocationZones";

	private RelocationAgentFactory relocationAgentFactory;

	private Map<String, List<RelocationZone>> relocationZones;

	private Map<String, List<Double>> relocationTimes;

	private Map<String, Map<Id<Person>, RelocationAgent>> relocationAgents;

	private Map<String, List<RelocationInfo>> relocations;

	private Map<String, Map<Double, Map<Id<RelocationZone>, Map<String, Double>>>> status = new HashMap<String, Map<Double, Map<Id<RelocationZone>, Map<String, Double>>>>();

	private Integer moduleEnableAfterIteration = null;

	private Integer demandEstimateIterations = null;

	public CarsharingVehicleRelocationContainer(Scenario sc, Network network) {
		this.scenario = sc;
		this.network = network;
		this.relocationAgentFactory = new RelocationAgentFactory(this.scenario, network);
		this.pointFeatureFactory = new PointFeatureFactory.Builder()
				.setName("point")
				.setCrs(DefaultGeographicCRS.WGS84)
				.create();

		this.relocationAgents = new TreeMap<String, Map<Id<Person>, RelocationAgent>>();

		this.relocations = new HashMap<String, List<RelocationInfo>>();

		final CarsharingVehicleRelocationConfigGroup confGroup = (CarsharingVehicleRelocationConfigGroup)
				this.scenario.getConfig().getModule( CarsharingVehicleRelocationConfigGroup.GROUP_NAME );

		this.moduleEnableAfterIteration = confGroup.moduleEnableAfterIteration();
		this.demandEstimateIterations = confGroup.demandEstimateIterations();
	}

	public void readRelocationZones() throws IOException {
		final CarsharingVehicleRelocationConfigGroup confGroup = (CarsharingVehicleRelocationConfigGroup)
				this.scenario.getConfig().getModule( CarsharingVehicleRelocationConfigGroup.GROUP_NAME );

		RelocationZonesReader reader = new RelocationZonesReader();
		reader.readFile(confGroup.getRelocationZones());
		this.relocationZones = reader.getRelocationZones();
	}

	public final void readRelocationTimes() throws IOException {
		final CarsharingVehicleRelocationConfigGroup confGroup = (CarsharingVehicleRelocationConfigGroup)
				this.scenario.getConfig().getModule( CarsharingVehicleRelocationConfigGroup.GROUP_NAME );

		RelocationTimesReader reader = new RelocationTimesReader();
		reader.readFile(confGroup.getRelocationTimes());
		this.relocationTimes = reader.getRelocationTimes();
	}

	public final void readRelocationAgents() throws IOException {
		final CarsharingVehicleRelocationConfigGroup confGroup = (CarsharingVehicleRelocationConfigGroup)
				this.scenario.getConfig().getModule( CarsharingVehicleRelocationConfigGroup.GROUP_NAME );

		RelocationAgentsReader reader = new RelocationAgentsReader();
		reader.readFile(confGroup.getRelocationAgents());


		for (Entry<String, Map<String, Map<String, Double>>> companyEntry : reader.getRelocationAgentBases().entrySet()) {
			String companyId = companyEntry.getKey();
			this.relocationAgents.put(companyId, new TreeMap<Id<Person>, RelocationAgent>());

			for (Entry<String, Map<String, Double>> baseEntry : companyEntry.getValue().entrySet()) {
				String baseId = baseEntry.getKey();
				HashMap<String, Double> agentBaseData = (HashMap<String, Double>) baseEntry.getValue();

				Coord coord = new Coord(agentBaseData.get("x"), agentBaseData.get("y"));
				Link link = (Link) NetworkUtils.getNearestLinkExactly(network, coord);

				int counter = 0;
				while (counter < agentBaseData.get("number")) {
					Id<Person> id = Id.createPersonId("RelocationAgent" + "_" + companyId + "_" + baseId + "_"  + counter);
					RelocationAgent agent = this.relocationAgentFactory.createRelocationAgent(id, companyId, link.getId());

					this.relocationAgents.get(companyId).put(id, agent);
					counter++;
				}
			}
		}
	}

	public Map<String, List<RelocationZone>> getRelocationZones() {
		return this.relocationZones;
	}

	public List<RelocationZone> getRelocationZones(String companyId) {
		if (this.getRelocationZones().keySet().contains(companyId)) {
			return this.getRelocationZones().get(companyId);
		}

		return null;
	}

	public Map<String, List<Double>> getRelocationTimes() {
		return this.relocationTimes;
	}

	public List<Double> getRelocationTimes(String companyId) {
		if (this.getRelocationTimes().keySet().contains(companyId)) {
			return this.getRelocationTimes().get(companyId);
		}

		return null;
	}

	public Map<String, Map<Id<Person>, RelocationAgent>> getRelocationAgents() {
		return this.relocationAgents;
	}

	public Map<Id<Person>, RelocationAgent> getRelocationAgents(String companyId) {
		if (this.getRelocationAgents().keySet().contains(companyId)) {
			return this.getRelocationAgents().get(companyId);
		}

		return null;
	}

	public Map<String, List<RelocationInfo>> getRelocations() {
		return this.relocations;
	}

	public List<RelocationInfo> getRelocations(String companyId) {
		if (this.getRelocations().keySet().contains(companyId)) {
			return this.getRelocations().get(companyId);
		}

		return null;
	}

	public void addRelocation(String companyId, RelocationInfo info) {
		if (this.getRelocations(companyId) == null) {
			this.getRelocations().put(companyId, new ArrayList<RelocationInfo>());
		}

		this.getRelocations(companyId).add(info);
	}

	public Integer moduleEnableAfterIteration() {
		return this.moduleEnableAfterIteration;
	}

	public Integer demandEstimateIterations() {
		return this.demandEstimateIterations;
	}

	public RelocationZone getRelocationZone(String companyId, Coord coord) {
		if (this.getRelocationZones().keySet().contains(companyId)) {
			SimpleFeature pointFeature = this.pointFeatureFactory.createPoint(coord, new Object[0], null);
			Point point = (Point) pointFeature.getAttribute("the_geom");

			for (RelocationZone relocationZone : this.getRelocationZones().get(companyId)) {
				MultiPolygon polygon = (MultiPolygon) relocationZone.getPolygon().getAttribute("the_geom");

				if (polygon.contains(point)) {
					return relocationZone;
				}
			}
		}

		return null;
	}

	public void addVehicles(String companyId, Link link, ArrayList<String> IDs) {
		RelocationZone relocationZone = this.getRelocationZone(companyId, link.getCoord());

		if (null != relocationZone) {
			relocationZone.addVehicles(link, IDs);
		}
	}

	public Map<String, Map<Double, Map<Id<RelocationZone>, Map<String, Double>>>> getStatus() {
		return this.status;
	}

	public void resetRelocationZones() {
		for (Entry<String, List<RelocationZone>> entry : this.getRelocationZones().entrySet()) {
			for (RelocationZone r : entry.getValue()) {
				r.reset();
			}
		}
	}

	public void resetRelocationZones(String companyId) {
		if (this.getRelocationZones().keySet().contains(companyId)) {
			for (RelocationZone r: this.getRelocationZones().get(companyId)) {
				r.reset();
			}
		}
	}

	public void resetRelocations() {
		this.relocations = new HashMap<String, List<RelocationInfo>>();
	}

	public void resetRelocationAgents() {
		for (Entry<String, Map<Id<Person>, RelocationAgent>> companyEntry : this.getRelocationAgents().entrySet()) {
			for (Entry<Id<Person>, RelocationAgent> agentEntry : companyEntry.getValue().entrySet()) {
				RelocationAgent agent = agentEntry.getValue();
				agent.reset();
			}
		}
	}

	public void reset() {
		this.resetRelocationZones();
		this.resetRelocations();
		this.resetRelocationAgents();
	}

	public void storeStatus(String companyId, double now) {
		Map<Id<RelocationZone>,Map<String,Double>> relocationZonesStatus = new HashMap<Id<RelocationZone>, Map<String, Double>>();

		for (RelocationZone relocationZone : this.getRelocationZones().get(companyId)) {
			Map<String,Double> zoneStatus = new HashMap<String, Double>();
			zoneStatus.put("vehicles", relocationZone.getNumberOfVehicles());
			zoneStatus.put("expectedRequests", relocationZone.getNumberOfExpectedRequests());
			zoneStatus.put("expectedReturns", relocationZone.getNumberOfExpectedReturns());
			zoneStatus.put("actualRequests", relocationZone.getNumberOfActualRequests());
			zoneStatus.put("actualReturns", relocationZone.getNumberOfActualReturns());
			relocationZonesStatus.put(relocationZone.getId(), zoneStatus);
		}

		if (this.status.get(companyId) == null) {
			this.status.put(companyId, new HashMap<Double, Map<Id<RelocationZone>, Map<String, Double>>>());
		}

		this.status.get(companyId).put(now, relocationZonesStatus);
	}

	protected Collection<RelocationZone> getAdjacentZones(String companyId, RelocationZone currentZone) {
		Collection<RelocationZone> relocationZones = new ArrayList<RelocationZone>(this.getRelocationZones().get(companyId));
		Collection<RelocationZone> adjacentZones = new ArrayList<RelocationZone>();
		relocationZones.remove(currentZone);
		MultiPolygon currentPolygon = (MultiPolygon) currentZone.getPolygon().getAttribute("the_geom");

		for (RelocationZone relocationZone : relocationZones) {
			MultiPolygon polygon = (MultiPolygon) relocationZone.getPolygon().getAttribute("the_geom");

			if (polygon.touches(currentPolygon)) {
				adjacentZones.add(relocationZone);
			}
		}

		return adjacentZones;
	}

	public Network getNetwork() {
		return this.network;
	}
}

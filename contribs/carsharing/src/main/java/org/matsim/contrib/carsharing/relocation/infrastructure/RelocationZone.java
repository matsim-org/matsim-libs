package org.matsim.contrib.carsharing.relocation.infrastructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;

public class RelocationZone implements Identifiable<RelocationZone> {

	private Id<RelocationZone> id;

	private SimpleFeature polygon;

	private Map<Link, ArrayList<String>> vehicles;

	private Double numberOfExpectedRequests;

	private Double numberOfExpectedReturns;

	private Double numberOfActualRequests;

	private Double numberOfActualReturns;

	private Comparator<Link> linkComparator = new Comparator<Link>() {
		@Override
		public int compare(Link l1, Link l2) {
			return l1.getId().toString().compareTo(l2.getId().toString());
		}
	};

	public RelocationZone(final Id<RelocationZone> id, SimpleFeature polygon) {
		this.id = id;
		this.polygon = polygon;
		this.vehicles = new TreeMap<Link, ArrayList<String>>(linkComparator);
	}

	@Override
	public Id<RelocationZone> getId() {
		return this.id;
	}

	public SimpleFeature getPolygon() {
		return this.polygon;
	}

	public Coord getCenter() {
		MultiPolygon polygon = (MultiPolygon) this.getPolygon().getAttribute("the_geom");
		Point centroid = polygon.getCentroid();
		Coord coord = new Coord(centroid.getX(), centroid.getY());

		return coord;
	}

	public Map<Link, ArrayList<String>> getVehicles() {
		return this.vehicles;
	}

	public List<String> getVehicleIds() {
		ArrayList<String> Ids = new ArrayList<String>();

		for (List<String> linkIds : this.getVehicles().values()) {
			Ids.addAll(linkIds);
		}

		return Ids;
	}

	public double getNumberOfVehicles() {
		return (double) this.getVehicles().size();
	}

	public void setNumberOfExpectedRequests(Double numberOfExpectedRequests) {
		this.numberOfExpectedRequests = numberOfExpectedRequests;
	}

	public Double getNumberOfExpectedRequests() {
		return this.numberOfExpectedRequests;
	}

	public void setNumberOfExpectedReturns(Double numberOfExpectedReturns) {
		this.numberOfExpectedReturns = numberOfExpectedReturns;
	}

	public Double getNumberOfExpectedReturns() {
		return this.numberOfExpectedReturns;
	}

	public void setNumberOfActualRequests(Double numberOfActualRequests) {
		this.numberOfActualRequests = numberOfActualRequests;
	}

	public Double getNumberOfActualRequests() {
		return this.numberOfActualRequests;
	}

	public void setNumberOfActualReturns(Double numberOfActualReturns) {
		this.numberOfActualReturns = numberOfActualReturns;
	}

	public Double getNumberOfActualReturns() {
		return this.numberOfActualReturns;
	}

	public double getNumberOfSurplusVehicles() {
		return this.getNumberOfVehicles() + this.getNumberOfExpectedReturns() - this.getNumberOfExpectedRequests();
	}

	public void addVehicles(Link link, ArrayList<String> IDs) {
		ArrayList<String> linkIDs = new ArrayList<String>();
		linkIDs.addAll(IDs);

		if (this.getVehicles().containsKey(link)) {
			linkIDs.addAll(this.getVehicles().get(link));
		}

		Collections.sort(linkIDs);

		this.getVehicles().put(link, linkIDs);
	}

	public void removeVehicles(Link link, ArrayList<String> IDs) {
		if (this.getVehicles().containsKey(link)) {
			ArrayList<String> linkIDs = this.getVehicles().get(link);

			for (String ID : IDs) {
				linkIDs.remove(ID);
			}
			Collections.sort(linkIDs);

			if (linkIDs.size() > 0) {
				this.getVehicles().put(link, linkIDs);
			} else {
				this.getVehicles().remove(link);
			}
		}
	}

	public void reset() {
		this.setNumberOfExpectedRequests(new Double(0));
		this.setNumberOfExpectedReturns(new Double(0));
		this.setNumberOfActualRequests(new Double(0));
		this.setNumberOfActualReturns(new Double(0));
		this.vehicles.clear();
	}

}

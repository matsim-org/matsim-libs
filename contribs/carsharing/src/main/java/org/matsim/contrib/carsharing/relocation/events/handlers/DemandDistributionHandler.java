package org.matsim.contrib.carsharing.relocation.events.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.carsharing.events.NoVehicleCarSharingEvent;
import org.matsim.contrib.carsharing.events.StartRentalEvent;
import org.matsim.contrib.carsharing.events.handlers.NoVehicleCarSharingEventHandler;
import org.matsim.contrib.carsharing.events.handlers.StartRentalEventHandler;
import org.matsim.contrib.carsharing.relocation.demand.CarsharingVehicleRelocationContainer;
import org.matsim.contrib.carsharing.relocation.infrastructure.RelocationZone;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.Matrix;

import com.google.inject.Inject;


public class DemandDistributionHandler implements StartRentalEventHandler, NoVehicleCarSharingEventHandler {

	@Inject Scenario scenario;

	@Inject CarsharingVehicleRelocationContainer carsharingVehicleRelocation;

	Map<String, Map<Double, Matrices>> ODMatrices = new HashMap<String, Map<Double, Matrices>>();

	@Override
	public void reset(int iteration) {
		this.ODMatrices = new HashMap<String, Map<Double, Matrices>>();
	}

	public void createODMatrices(String companyId, double time) {
		if (!this.ODMatrices.keySet().contains(companyId)) {
			this.ODMatrices.put(companyId, new TreeMap<Double, Matrices>());
		}

		this.ODMatrices.get(companyId).put(time, new Matrices());
		this.ODMatrices.get(companyId).get(time).createMatrix("rentals", "rentals of operator " + companyId + " starting at " + String.valueOf(time));
		this.ODMatrices.get(companyId).get(time).createMatrix("no_vehicle", "no_vehicles of operator " + companyId + " starting at " + String.valueOf(time));
	}

	@Override
	public void handleEvent(StartRentalEvent event) {
		Network network = this.scenario.getNetwork();

		Link originLink = network.getLinks().get(event.getOriginLinkId());
		Link destinationLink = network.getLinks().get(event.getDestinationLinkId());

		String companyId = event.getCompanyId();

		RelocationZone originZone = this.carsharingVehicleRelocation.getRelocationZone(companyId, originLink.getCoord());
		RelocationZone destinationZone = this.carsharingVehicleRelocation.getRelocationZone(companyId, destinationLink.getCoord());

		if ((originZone != null) && (destinationZone != null)) {
			this.addODRelation(companyId, "rentals", originZone, destinationZone);
		}
	}

	@Override
	public void handleEvent(NoVehicleCarSharingEvent event) {
		Network network = this.scenario.getNetwork();

		Link originLink = network.getLinks().get(event.getOriginLinkId());
		Link destinationLink = network.getLinks().get(event.getDestinationLinkId());

		for (String companyId : this.carsharingVehicleRelocation.getRelocationZones().keySet()) {
			RelocationZone originZone = this.carsharingVehicleRelocation.getRelocationZone(companyId, originLink.getCoord());
			RelocationZone destinationZone = this.carsharingVehicleRelocation.getRelocationZone(companyId, destinationLink.getCoord());
	
			if ((originZone != null) && (destinationZone != null)) {
				this.addODRelation(companyId, "no_vehicle", originZone, destinationZone);
			}
		}
	}

	public Map<String, Map<Double, Matrices>> getODMatrices() {
		return this.ODMatrices;
	}

	public Map<Double, Matrices> getODMatrices(String companyId) {
		Map<String, Map<Double, Matrices>> ODMatrices = this.getODMatrices();

		if ((null != ODMatrices) && (ODMatrices.keySet().contains(companyId))) {
			return ODMatrices.get(companyId);
		}

		return null;
	}

	public Matrices getODMatrices(String companyId, Double time) {
		Map<Double, Matrices> ODMatrices = this.getODMatrices(companyId);

		if ((null != ODMatrices) && (ODMatrices.keySet().contains(time))) {
			return ODMatrices.get(time);
		}

		return null;
	}

	public Matrix getODMatrix(String companyId, Double time, String eventType) {
		Matrices companyODMatrices = this.getODMatrices(companyId, time);

		if ((null != companyODMatrices) && (companyODMatrices.getMatrices().keySet().contains(eventType))) {
			return companyODMatrices.getMatrix(eventType);
		}

		return null;
	}

	protected void addODRelation(String companyId, String eventType, RelocationZone originZone, RelocationZone destinationZone) {
		if (!this.ODMatrices.keySet().contains(companyId)) {
			this.createODMatrices(companyId, 0);
		}

		double numRequests = originZone.getNumberOfActualRequests();
		originZone.setNumberOfActualRequests(numRequests + 1);

		if (eventType == "rentals") {
			double numReturns = destinationZone.getNumberOfActualReturns();
			destinationZone.setNumberOfActualReturns(numReturns + 1);
		}

		SortedSet<Double> keySet = (SortedSet<Double>) this.ODMatrices.get(companyId).keySet();
		Matrices companyODMatrices = this.ODMatrices.get(companyId).get(keySet.last());
		Matrix ODMatrix = companyODMatrices.getMatrix(eventType);

		if (null != ODMatrix) {
			String originId = originZone.getId().toString();
			String destinationId = destinationZone.getId().toString();

			double value = 1;
			Entry relation = ODMatrix.getEntry(originId, destinationId);

			if (null != relation) {
				value += relation.getValue();
			}

			ODMatrix.setEntry(originZone.getId().toString(), destinationZone.getId().toString(), value);
		}
	}
}

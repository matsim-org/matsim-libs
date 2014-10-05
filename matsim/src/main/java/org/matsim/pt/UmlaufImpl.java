package org.matsim.pt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.vehicles.Vehicle;

public class UmlaufImpl implements Umlauf {
	
	private Id<Umlauf> id;
	private Id<Vehicle> vehicleId;
	
	private ArrayList<UmlaufStueckI> umlaufStuecke = new ArrayList<UmlaufStueckI>();

	public UmlaufImpl(Id<Umlauf> id) {
		super();
		this.id = id;
	}

	@Override
	public List<UmlaufStueckI> getUmlaufStuecke() {
		return umlaufStuecke;
	}

	@Override
	public Id<Umlauf> getId() {
		return this.id;
	}
	
	@Override
	public void setVehicleId(final Id<Vehicle> vehicleId) {
		this.vehicleId = vehicleId;
		for (UmlaufStueckI umlaufStueck : umlaufStuecke) {
			if (umlaufStueck.isFahrt()) {
				umlaufStueck.getDeparture().setVehicleId(vehicleId);
			}
		}
	}

	@Override
	public Id<Vehicle> getVehicleId() {
		return this.vehicleId;
	}

	@Override
	public Id<TransitLine> getLineId() {
		return getLineId(getUmlaufStuecke());
	}

	private Id<TransitLine> getLineId(Collection<UmlaufStueckI> umlaufInConstruction) {
		Id<TransitLine> lineId = null;
		for (UmlaufStueckI umlaufStueck : umlaufInConstruction) {
			if (umlaufStueck.isFahrt()) {
				if (lineId == null) {
					lineId = umlaufStueck.getLine().getId();
				}
			}
		}
		return lineId;
	}
	
}

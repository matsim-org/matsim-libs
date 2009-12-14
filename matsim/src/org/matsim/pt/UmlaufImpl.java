package org.matsim.pt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.basic.v01.Id;

public class UmlaufImpl implements Umlauf {
	
	private Id id;
	private Id vehicleId;
	
	private ArrayList<UmlaufStueckI> umlaufStuecke = new ArrayList<UmlaufStueckI>();

	public UmlaufImpl(Id id) {
		super();
		this.id = id;
	}

	public List<UmlaufStueckI> getUmlaufStuecke() {
		return umlaufStuecke;
	}

	public Id getId() {
		return this.id;
	}
	
	public void setVehicleId(final Id vehicleId) {
		this.vehicleId = vehicleId;
		for (UmlaufStueckI umlaufStueck : umlaufStuecke) {
			if (umlaufStueck.isFahrt()) {
				umlaufStueck.getDeparture().setVehicleId(vehicleId);
			}
		}
	}

	public Id getVehicleId() {
		return this.vehicleId;
	}

	public Id getLineId() {
		return getLineId(getUmlaufStuecke());
	}

	private Id getLineId(Collection<UmlaufStueckI> umlaufInConstruction) {
		Id lineId = null;
		for (UmlaufStueckI umlaufStueck : umlaufInConstruction) {
			if (umlaufStueck.isFahrt()) {
				if (lineId == null) {
					lineId = umlaufStueck.getLine().getId();
				} else {
					if (!lineId.equals(umlaufStueck.getLine().getId())) {
						throw new IllegalStateException(
								"Expected an Umlauf which serves a single line. Got one which serves multiple lines.");
					}
				}
			}
		}
		return lineId;
	}
	
}

package playground.mzilske.pt.queuesim;

import java.util.ArrayList;
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
	
}

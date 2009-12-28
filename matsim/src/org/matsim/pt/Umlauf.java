package org.matsim.pt;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;


public interface Umlauf extends Identifiable {
	
	List<UmlaufStueckI> getUmlaufStuecke();
	
	public Id getVehicleId();
	
	public void setVehicleId(Id vehicleId);

	Id getLineId();

}

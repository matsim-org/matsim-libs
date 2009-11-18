package playground.mzilske.pt.queuesim;

import java.util.List;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.Identifiable;

public interface Umlauf extends Identifiable {
	
	List<UmlaufStueckI> getUmlaufStuecke();
	
	public Id getVehicleId();
	
	public void setVehicleId(Id vehicleId);

}

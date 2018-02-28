package demand.demandObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

public class DemandObjects {

	private Map<Id<DemandObject>, DemandObject> demandObjects = new HashMap<>();
	
	public DemandObjects(Collection<DemandObject> demandObjects) {
		makeMap(demandObjects);
	}
	
	public DemandObjects() {
		
	}

	public Map<Id<DemandObject>, DemandObject> getDemandObjects(){
		return demandObjects;
	}

	private void makeMap(Collection<DemandObject> demandObjects) {
		for(DemandObject d : demandObjects) {
			this.demandObjects.put(d.getId(), d);
		}
	}

	public void addDemandObject(DemandObject demandObject) {
		if(!demandObjects.containsKey(demandObject.getId())) {
			demandObjects.put(demandObject.getId(), demandObject);
		}
	}

}

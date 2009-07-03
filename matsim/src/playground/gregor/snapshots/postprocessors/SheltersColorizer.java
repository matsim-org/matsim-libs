package playground.gregor.snapshots.postprocessors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.core.events.LinkLeaveEvent;
import org.matsim.core.events.handler.LinkLeaveEventHandler;
import org.matsim.core.utils.misc.Counter;
import org.matsim.evacuation.base.Building;
import org.matsim.evacuation.base.BuildingsShapeReader;

public class SheltersColorizer implements LinkLeaveEventHandler {

	Map<String,ShelterInfo> shelters = new HashMap<String,ShelterInfo>();
	Map<String,ArrayList<Double>> occupancies = null;
	private final double snapPeriod;
	private final double scenarioSize;
	
	
	
	
	public SheltersColorizer(String buildings, double snapPeriod, double scenarioSize) {
		this.snapPeriod = snapPeriod;
		this.scenarioSize = scenarioSize;
		List<Building> bs = BuildingsShapeReader.readDataFile(buildings);
		for (Building b : bs) {
			if (b.isQuakeProof()) {
				Counter c= new Counter("shelter " + b.getId() + ":");
				ShelterInfo si = new ShelterInfo();
				si.b = b;
				si.c = c;
				this.shelters.put(b.getId().toString(), si);
			}
		}
	}
	
	
	public void handleEvent(LinkLeaveEvent event) {
		if (event.getLinkId().toString().contains("sl") && event.getLinkId().toString().contains("a")) {
			String id = event.getLinkId().toString().replace("sl","");
			id = id.replace("a", "");
			ShelterInfo si = this.shelters.get(id);
			si.c.incCounter();
						
			int snapshotIndex = (int) (event.getTime() / this.snapPeriod);
			if (si.lastSnap == -1) {
				si.lastSnap = snapshotIndex;
			}
			if (this.occupancies == null) {
				this.occupancies = new HashMap<String,ArrayList<Double>>();
			}
			
			ArrayList<Double> occupancy = this.occupancies.get(id);
			if (occupancy == null) {
				occupancy = new ArrayList<Double>();
				this.occupancies.put(id, occupancy);
			}
			
			double oldVal = 0;
			if (occupancy.size() > 0){
				oldVal = occupancy.get(occupancy.size()-1);
			}
			
			while (snapshotIndex > si.lastSnap ) {
				si.lastSnap++;
				if (snapshotIndex == si.lastSnap) {
					double d = si.c.getCounter()/(si.b.getShelterSpace() * this.scenarioSize);
					occupancy.add(d);
				} else {
					occupancy.add(oldVal);
				}
			}
		}
	}
	
	public Map<String,ArrayList<Double>> getOccMap() {
		return this.occupancies;
	}

	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	private static class ShelterInfo {
		Building b;
		Counter c;
		int lastSnap = -1;
	}
	
}

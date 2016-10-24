package playground.balac.utils.roadpricing;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;

public class LinkEnterHandler implements LinkEnterEventHandler {

	Set<String> links;
	Map<String, int[]> mapa = new HashMap<String, int[]>();
	public LinkEnterHandler(Set<String> links) {
		
		this.links = links;
		
	}
	@Override
	public void reset(int iteration) {

		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {

		double t = event.getTime();
		Id<Link> linkId = event.getLinkId();
		
		if (links.contains(linkId.toString())) {
		
			if (!mapa.containsKey(event.getVehicleId().toString())) {
				
				
				int[] arr = new int[3];
				mapa.put(event.getVehicleId().toString(),arr);
			}
			if (t >=5 * 3600 + 1800 && t <= 9 * 3600) {
				
				mapa.get(event.getVehicleId().toString())[0]++;
				
			}
			else if (t >=15 * 3600 + 1800 && t <= 19 * 3600) {
				
				mapa.get(event.getVehicleId().toString())[1]++;
			}
			else
				mapa.get(event.getVehicleId().toString())[2]++;

		
		}
	}
	public Map<String, int[]> getMapa() {
		return mapa;
	}
	
	

}

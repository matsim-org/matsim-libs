package playground.mzilske.cdr;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class SyntheticCellTowerDistribution {

	public static Zones naive(Network network) {
		Map<String, CellTower> cellTowerMap = new HashMap<String, CellTower>();
		for (Link link : network.getLinks().values()) {
			cellTowerMap.put(link.getId().toString(), new CellTower(link.getId().toString(), link.getCoord()));
		}
		Zones zones = new Zones(cellTowerMap);
		return zones;
	}
	
	

}

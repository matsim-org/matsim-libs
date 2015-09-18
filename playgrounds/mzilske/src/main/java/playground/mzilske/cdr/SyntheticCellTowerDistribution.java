package playground.mzilske.cdr;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import java.util.HashMap;
import java.util.Map;

class SyntheticCellTowerDistribution {

	public static Zones naive(Network network) {
		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		double GRID_SIZE = 100000.0; // m
		
		Map<String, CellTower> cellTowerMap = new HashMap<String, CellTower>();
		for (Node node : network.getNodes().values()) {
			minX = Math.min(minX, node.getCoord().getX());
			maxX = Math.max(maxX, node.getCoord().getX());
			minY = Math.min(minY, node.getCoord().getY());
			maxY = Math.max(maxY, node.getCoord().getY());
		}
		
		int idX = 0;
		for (double x=minX; x<maxX; x+=GRID_SIZE) {
			int idY = 0;
			for (double y=minY; y<maxY; y+=GRID_SIZE) {
				String id = idX + "_" + idY;
				cellTowerMap.put(id, new CellTower(id, new Coord(x + Math.random() * GRID_SIZE / 10, y + Math.random() * GRID_SIZE / 10)));
				++idY;
			}
			++idX;
		}

		
		Zones zones = new Zones(cellTowerMap);
		return zones;
	}
	
	

}

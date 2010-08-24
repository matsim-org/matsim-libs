package playground.ciarif.flexibletransports.router;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.algorithms.CalcBoundingBox;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;

public class CarSharingStations {
	private final QuadTree<CarSharingStation> stations;

	private static final Logger log = Logger.getLogger(CarSharingStations.class);

	public CarSharingStations(final Network network) {
		CalcBoundingBox bbox = new CalcBoundingBox();
		bbox.run(network);
		this.stations = new QuadTree<CarSharingStation>(bbox.getMinX(), bbox.getMinY(), bbox.getMaxX(), bbox.getMaxY());
	}

	public void readFile(final String filename) throws FileNotFoundException, IOException {
		final BufferedReader reader = IOUtils.getBufferedReader(filename);
		String line = reader.readLine(); // header
		while ((line = reader.readLine()) != null) {
			String[] parts = StringUtils.explode(line, '\t');
			if (parts.length == 7) {
				CoordImpl coord = new CoordImpl(Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
				CarSharingStation swissStop = new CarSharingStation(new IdImpl(parts[0]), coord);
				this.stations.put(coord.getX(), coord.getY(), swissStop);
			} else {
				log.warn("Could not parse line: " + line);
			}
		}
	}

	public CarSharingStation getClosestLocation(final Coord coord) {
		return this.stations.get(coord.getX(), coord.getY());
	}
	
	public CarSharingStation getCSStation(Id id) {
		
		CarSharingStation carStation = null;
		
		Iterator<CarSharingStation> it = stations.values().iterator();
		boolean notFound = true;
		while (notFound && it.hasNext()) {
			carStation = it.next();
			if (id.equals(carStation.getId())) {
				notFound = false;
			}
		}
		
		return carStation;
		
	}
}

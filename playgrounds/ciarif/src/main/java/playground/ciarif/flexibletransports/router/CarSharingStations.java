package playground.ciarif.flexibletransports.router;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.algorithms.CalcBoundingBox;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;
import playground.ciarif.flexibletransports.network.MyLinkUtils;

public class CarSharingStations
{
  private final QuadTree<CarSharingStation> stations;
  private Network network;
  private static final Logger log = Logger.getLogger(CarSharingStations.class);

  public CarSharingStations(Network network) {
    CalcBoundingBox bbox = new CalcBoundingBox();
    this.network = network;
    bbox.run(network);
    this.stations = new QuadTree(bbox.getMinX(), bbox.getMinY(), bbox.getMaxX(), bbox.getMaxY());
    log.info("Min X = " + bbox.getMinX());
    log.info("Min Y = " + bbox.getMinY());
    log.info("Max X = " + bbox.getMaxX());
    log.info("Max Y = " + bbox.getMaxY());
  }

  public void readFile(String filename) throws FileNotFoundException, IOException {
    BufferedReader reader = IOUtils.getBufferedReader(filename);
    String line = reader.readLine();
    while ((line = reader.readLine()) != null) {
      String[] parts = StringUtils.explode(line, '\t');
      if (parts.length == 7) {
        CoordImpl coord = new CoordImpl(Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
        LinkImpl stationLink = MyLinkUtils.getClosestLink(this.network, coord);
        CarSharingStation csStation = new CarSharingStation(new IdImpl(parts[0]), coord, stationLink);
        this.stations.put(coord.getX(), coord.getY(), csStation);
        log.info("The station " + csStation.getId() + " has been added");
      } else {
        log.warn("Could not parse line: " + line);
      }
    }
  }

  public CarSharingStation getClosestLocation(Coord coord)
  {
    return ((CarSharingStation)this.stations.get(coord.getX(), coord.getY()));
  }

  public CarSharingStation getCSStation(Id id)
  {
    CarSharingStation carStation = null;

    Iterator it = this.stations.values().iterator();
    boolean notFound = true;
    while ((notFound) && (it.hasNext())) {
      carStation = (CarSharingStation)it.next();
      if (id.equals(carStation.getId())) {
        notFound = false;
      }
    }

    return carStation;
  }
}

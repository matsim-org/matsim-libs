package playground.ciarif.flexibletransports.router;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.algorithms.CalcBoundingBox;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;

import playground.ciarif.data.FacilitiesPortfolio;
import playground.ciarif.flexibletransports.network.MyLinkUtils;

public class CarSharingStations implements FacilitiesPortfolio
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
        Coord coord = new Coord(Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
        int cars = Integer.parseInt(parts[4]);
        LinkImpl stationLink = MyLinkUtils.getClosestLink(this.network, coord);
        CarSharingStation csStation = new CarSharingStation(Id.create(parts[0], CarSharingStation.class), coord, stationLink, cars);
        this.stations.put(coord.getX(), coord.getY(), csStation);
        log.info("The station " + csStation.getId() + " has been added");
      } else {
        log.warn("Could not parse line: " + line);
      }
    }
  }

  public CarSharingStation getClosestLocation(Coord coord)
  {
    return (this.stations.getClosest(coord.getX(), coord.getY()));
  }
  
  public Vector<CarSharingStation> getClosestStations (Coord coord, int number, double distance) {
	  
	  Vector<CarSharingStation> orderedClosestStations = new Vector<CarSharingStation> ();
	  //orderedClosestStations.add(0, this.stations.get(coord.getX(),coord.getY()));
	  Collection<CarSharingStation> allClosestStations = this.stations.getDisk(coord.getX(), coord.getY(), distance);
	  //log.info("number of all cs stations = " + allClosestStations.size());
	  for (CarSharingStation station:allClosestStations){
		  int i = 0;
		  if (orderedClosestStations.isEmpty()) {
			  
			  orderedClosestStations.add(0,station);
			  //log.info("The station n. " + station.getId() + " has been added at position " + orderedClosestStations.indexOf(station));
		  
		  }
		  else {
			  if (!orderedClosestStations.contains(station)){
			  	while (CoordUtils.calcDistance(coord, station.getCoord()) > (CoordUtils.calcDistance(coord, orderedClosestStations.get(i).getCoord()))  ) {
					  
						  //log.info("station " + station.getId() + " is " + CoordUtils.calcDistance(coord, station.getCoord()) + " distant, further away than station " + orderedClosestStations.get(i).getId() + " which is " + CoordUtils.calcDistance(coord, orderedClosestStations.get(i).getCoord()) + "distant");
						  i++;
						  //log.info("i = " + i);
						  if (i==orderedClosestStations.size()) { break;}
				}
				  	  
				
				  orderedClosestStations.add(i,station);
				  //log.info("The station n. " + station.getId() + " has been added at position " + i);
				
			  }
		  }
		  
	  }
	  if (orderedClosestStations.size()<=number) {
		  //log.info("final number of cs stations = " + orderedClosestStations.size());
		  return orderedClosestStations;
	  }
	  else {
		  List<CarSharingStation> oCS = orderedClosestStations.subList(0,number);
		  Vector <CarSharingStation> finalOrderedClosestStations = new Vector <CarSharingStation> ();
		  finalOrderedClosestStations.addAll(oCS);
		  //log.info("final number of cs stations = " + finalOrderedClosestStations.size());
		  return finalOrderedClosestStations;
	  }
	  
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

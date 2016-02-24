package playground.balac.carsharing.router;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.algorithms.CalcBoundingBox;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;

import playground.balac.carsharing.preprocess.membership.FacilitiesPortfolio;
import playground.balac.carsharing.preprocess.membership.MyLinkUtils;

public class CarSharingStations
  implements FacilitiesPortfolio
{
  private QuadTree<CarSharingStation> stations;
  private ArrayList<CarSharingStation> stationsArr = new ArrayList<CarSharingStation>();
  private Network network;
  private static final Logger log = Logger.getLogger(CarSharingStations.class);

  public QuadTree<CarSharingStation> getQuadStations() {
	  return stations;
  }
  public CarSharingStations(Network network) {
    CalcBoundingBox bbox = new CalcBoundingBox();
    this.network = network;
    bbox.run(network);
    this.stations = new QuadTree<CarSharingStation>(bbox.getMinX(), bbox.getMinY(), bbox.getMaxX(), bbox.getMaxY());
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
	        int cars = Integer.parseInt(parts[6]);
	        LinkImpl stationLink = MyLinkUtils.getClosestLink(this.network, coord);
	        CarSharingStation csStation = new CarSharingStation(Id.create(parts[0], CarSharingStation.class), coord, stationLink, cars);
	        this.stations.put(coord.getX(), coord.getY(), csStation);
	        stationsArr.add(csStation);
	        log.info("The station " + csStation.getId() + " has been added");
	      } else {
	        log.warn("Could not parse line: " + line);
	      }
	    }
	  }
  
  public void readFile(String filename, String filename_new) throws FileNotFoundException, IOException {
	    BufferedReader reader = IOUtils.getBufferedReader(filename);
	    String line = reader.readLine();
	    BufferedReader reader1 = IOUtils.getBufferedReader(filename_new);
	    String line1 = null;
	    HashMap<String, String> mapa = new HashMap<String, String>();
	    while ((line = reader.readLine()) != null) {
		      String[] parts = StringUtils.explode(line, '\t');
		      mapa.put(parts[0], parts[6]);
	    	
	    	
	    }
	    
	    while ((line1 = reader1.readLine()) != null) {
	      String[] parts = StringUtils.explode(line1, '\t');
	      if (parts.length == 4) {
            Coord coord = new Coord(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
	        

	        int cars = Integer.parseInt(mapa.get(parts[0]));
	        
	        LinkImpl stationLink =(LinkImpl) network.getLinks().get(Id.create(parts[3], Link.class));
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
    return this.stations.getClosest(coord.getX(), coord.getY());
  }

  public Vector<CarSharingStation> findClosestStations(Coord coord, int number, double distance)
  {
    Vector orderedClosestStations = new Vector();

    Collection<CarSharingStation> allClosestStations = this.stations.getDisk(coord.getX(), coord.getY(), distance);

    for (CarSharingStation station : allClosestStations) {
      int i = 0;
      if (orderedClosestStations.isEmpty())
      {
        orderedClosestStations.add(0, station);
      }
      else if (!orderedClosestStations.contains(station)) {
        while (CoordUtils.calcEuclideanDistance(coord, station.getCoord()) > CoordUtils.calcEuclideanDistance(coord, ((CarSharingStation)orderedClosestStations.get(i)).getCoord()))
        {
          i++;

          if (i == orderedClosestStations.size()) {
            break;
          }
        }
        orderedClosestStations.add(i, station);
      }

    }

    if (orderedClosestStations.size() <= number)
    {
      return orderedClosestStations;
    }

    List oCS = orderedClosestStations.subList(0, number);
    Vector finalOrderedClosestStations = new Vector();
    finalOrderedClosestStations.addAll(oCS);

    return finalOrderedClosestStations;
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
  
  public ArrayList<CarSharingStation> getStationsArr() {
	    return this.stationsArr;
	  }

  public Collection<CarSharingStation> getStations() {
    return this.stations.values();
  }
}
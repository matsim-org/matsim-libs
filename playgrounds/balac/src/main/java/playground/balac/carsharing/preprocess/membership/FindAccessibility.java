package playground.balac.carsharing.preprocess.membership;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;

import playground.balac.carsharing.preprocess.membership.IO.Persons_CS_Stations_Distances_Writer;


public class FindAccessibility
{
  private String outpath;
  private String personsfilePath;
  private String stationsfilePath;
  private QuadTree<Station> stations;
  private TreeMap<Id<Person>, PersonWithClosestStations> personsWithClosestStations = new TreeMap<>();
  private TreeMap<Id<Station>, Station> stations_origin = new TreeMap<>();
  private WGS84toCH1903LV03 coordTranformer = new WGS84toCH1903LV03();

  private static final Logger log = Logger.getLogger(MembershipMain.class);

  public static void main(String[] args) {
    Gbl.startMeasurement();
    FindAccessibility fCS = new FindAccessibility();
    fCS.run(args[0]);
    Gbl.printElapsedTime();
  }

  public void run(String inputFile) {
    init(inputFile);
    findAccessibility();
    writePersons();
  }

  private void readInputFile(String pathsFile) {
    try {
      FileReader fileReader = new FileReader(pathsFile);
      BufferedReader bufferedReader = new BufferedReader(fileReader);
      this.stationsfilePath = bufferedReader.readLine();
      log.info("stations file = " + this.stationsfilePath);
      this.personsfilePath = bufferedReader.readLine();
      log.info("persons file = " + this.personsfilePath);
      this.outpath = bufferedReader.readLine();
      bufferedReader.close();
      fileReader.close();
    }
    catch (IOException e) {
    }
  }

  private void init(String inputFile) {
    String pathsFile = inputFile;
    readInputFile(pathsFile);

    log.info("reading the stations ...");
    try {
      readStationsFile(this.stationsfilePath);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    log.info("reading the persons ...");
    try {
      readPersonsFile(this.personsfilePath);
    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void readPersonsFile(String personsfilePath2) throws FileNotFoundException, IOException
  {
    BufferedReader reader = IOUtils.getBufferedReader(this.personsfilePath);
    String line = reader.readLine();

    while ((line = reader.readLine()) != null) {
      PersonWithClosestStations pwcs = new PersonWithClosestStations();
      String[] parts = StringUtils.explode(line, '\t');
      Id<Person> id = Id.create(Integer.parseInt(parts[0]), Person.class);
      log.info("Id = " + id);
      pwcs.setId(id);
      Coord coordHome = new Coord(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
      Coord coordWork = new Coord(Double.parseDouble(parts[3]), Double.parseDouble(parts[4]));
      pwcs.setCoordHome(this.coordTranformer.transform(coordHome));
      pwcs.setCoordWork(this.coordTranformer.transform(coordWork));
      this.personsWithClosestStations.put(pwcs.getId(), pwcs);
    }
  }

  private void readStationsFile(String stationsfilePath2) throws FileNotFoundException, IOException
  {
    BufferedReader reader = IOUtils.getBufferedReader(this.stationsfilePath);
    String line = reader.readLine();

    while ((line = reader.readLine()) != null) {
      Station station = new Station();
      String[] parts = StringUtils.explode(line, '\t');

      Id<Station> id = Id.create(Integer.parseInt(parts[0]), Station.class);
      Coord coord = new Coord(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
      int cars = Integer.parseInt(parts[3]);
      station.setCoord(this.coordTranformer.transform(coord));
      station.setCars(cars);
      station.setId(id);
      this.stations_origin.put(station.getId(), station);
    }

    double minx = (1.0D / 0.0D);
    double miny = (1.0D / 0.0D);
    double maxx = (-1.0D / 0.0D);
    double maxy = (-1.0D / 0.0D);

    for (Station station2 : this.stations_origin.values()) {
      if (station2.getCoord().getX() < minx) minx = station2.getCoord().getX();
      if (station2.getCoord().getY() < miny) miny = station2.getCoord().getY();
      if (station2.getCoord().getX() > maxx) maxx = station2.getCoord().getX();
      if (station2.getCoord().getY() > maxy) maxy = station2.getCoord().getY();
    }
    minx -= 1.0D; miny -= 1.0D; maxx += 1.0D; maxy += 1.0D;
    this.stations = new QuadTree<>(minx, miny, maxx, maxy);
    for (Station station3 : this.stations_origin.values()) {
      this.stations.put(station3.getCoord().getX(), station3.getCoord().getY(), station3);
    }
    log.info("StationsQuadTree has been created");
  }

  private void writePersons()
  {
    Persons_CS_Stations_Distances_Writer pcsWriter = new Persons_CS_Stations_Distances_Writer(this.outpath);
    for (PersonWithClosestStations person : this.personsWithClosestStations.values())
      pcsWriter.write(person);
  }

  private void findAccessibility()
  {
    for (PersonWithClosestStations pwcs : this.personsWithClosestStations.values())
    {
      double accessibilityHome = 0.0D;
      double accessibilityWork = 0.0D;
      Vector orderedClosestStationsWork = new Vector();
      Vector orderedClosestStationsHome = new Vector();

      Collection<Station> allClosestStationsWork = this.stations.getDisk(pwcs.getCoordWork().getX(), pwcs.getCoordWork().getY(), 5000.0D);
      Collection<Station> allClosestStationsHome = this.stations.getDisk(pwcs.getCoordHome().getX(), pwcs.getCoordHome().getY(), 5000.0D);

      for (Station stationWork : allClosestStationsWork) {
        int i = 0;
        if (orderedClosestStationsWork.isEmpty())
        {
          orderedClosestStationsWork.add(0, stationWork);
        }
        else if (!orderedClosestStationsWork.contains(stationWork)) {
          while (CoordUtils.calcEuclideanDistance(pwcs.getCoordWork(), stationWork.getCoord()) > CoordUtils.calcEuclideanDistance(pwcs.getCoordWork(), ((Station)orderedClosestStationsWork.get(i)).getCoord()))
          {
            i++;

            if (i == orderedClosestStationsWork.size()) {
              break;
            }
          }
          orderedClosestStationsWork.add(i, stationWork);
        }

      }

      if (orderedClosestStationsWork.size() == 3)
      {
        pwcs.setOrderedClosestStationsWork(orderedClosestStationsWork);
      }
      else if (orderedClosestStationsWork.size() < 3) {
        while (orderedClosestStationsWork.size() < 3) {
          Station nullStationWork = new Station();
          nullStationWork.setId(Id.create(0L, Station.class));
          nullStationWork.setCoord(new Coord(0.0D, 0.0D));
          orderedClosestStationsWork.add(orderedClosestStationsWork.size(), nullStationWork);
        }

        pwcs.setOrderedClosestStationsWork(orderedClosestStationsWork);
      }
      else {
        List oCS = orderedClosestStationsWork.subList(0, 3);
        Vector finalOrderedClosestStationsWork = new Vector();
        finalOrderedClosestStationsWork.addAll(oCS);
        pwcs.setOrderedClosestStationsWork(finalOrderedClosestStationsWork);
      }

      for (Object finalOrderedClosestStationsWork = allClosestStationsHome.iterator(); ((Iterator)finalOrderedClosestStationsWork).hasNext(); ) { Station stationHome = (Station)((Iterator)finalOrderedClosestStationsWork).next();
        int i = 0;
        if (orderedClosestStationsHome.isEmpty())
        {
          orderedClosestStationsHome.add(0, stationHome);
        }
        else if (!orderedClosestStationsHome.contains(stationHome)) {
          while (CoordUtils.calcEuclideanDistance(pwcs.getCoordHome(), stationHome.getCoord()) > CoordUtils.calcEuclideanDistance(pwcs.getCoordHome(), ((Station)orderedClosestStationsHome.get(i)).getCoord()))
          {
            i++;

            if (i == orderedClosestStationsHome.size()) {
              break;
            }
          }
          orderedClosestStationsHome.add(i, stationHome);
        }

      }

      if (orderedClosestStationsHome.size() == 3)
      {
        pwcs.setOrderedClosestStationsHome(orderedClosestStationsHome);
      }
      else if (orderedClosestStationsHome.size() < 3) {
        while (orderedClosestStationsHome.size() < 3) {
          Station nullStationHome = new Station();
          nullStationHome.setId(Id.create(0L, Station.class));
          nullStationHome.setCoord(new Coord(0.0D, 0.0D));
          orderedClosestStationsHome.add(orderedClosestStationsHome.size(), nullStationHome);
        }
        pwcs.setOrderedClosestStationsHome(orderedClosestStationsHome);
      }
      else
      {
        List oCS = orderedClosestStationsHome.subList(0, 3);
        Vector finalOrderedClosestStationsHome = new Vector();
        finalOrderedClosestStationsHome.addAll(oCS);
        pwcs.setOrderedClosestStationsHome(finalOrderedClosestStationsHome);
      }

      for (Object finalOrderedClosestStationsHome = pwcs.getOrderedClosestStationsHome().iterator(); ((Iterator)finalOrderedClosestStationsHome).hasNext(); ) { Station stationHome = (Station)((Iterator)finalOrderedClosestStationsHome).next();
        accessibilityHome += stationHome.getCars() * Math.exp(-2.0D * CoordUtils.calcEuclideanDistance(stationHome.getCoord(), pwcs.getCoordHome()) / 1000.0D);
      }

      for (Object finalOrderedClosestStationsHome = pwcs.getOrderedClosestStationsWork().iterator(); ((Iterator)finalOrderedClosestStationsHome).hasNext(); ) { Station stationWork = (Station)((Iterator)finalOrderedClosestStationsHome).next();
        accessibilityWork += stationWork.getCars() * Math.exp(-2.0D * CoordUtils.calcEuclideanDistance(stationWork.getCoord(), pwcs.getCoordWork()) / 1000.0D);
      }
      pwcs.setAccessibilityHome(Double.valueOf(accessibilityHome));
      pwcs.setAccessibilityWork(Double.valueOf(accessibilityWork));
    }
  }
}
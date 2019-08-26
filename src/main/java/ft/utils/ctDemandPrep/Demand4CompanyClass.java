package ft.utils.ctDemandPrep;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.opencsv.CSVReader;

import vwExamples.utils.DemandFromCSV.Trip;

public class Demand4CompanyClass {
	List<String> csvDemandFile;
	String epsgForDemandFile;
	Map<String, Geometry> zoneMap;
	List<String[]> companyLocationsList;
	public Map<String, ArrayList<Company>> companyClass2CompanyMap;
	public Map<String, ArrayList<Company>> zone2CompanyMap;

	public Demand4CompanyClass(List<String> csvDemandFile, String epsgForDemandFile, Map<String, Geometry> zoneMap) {
		this.csvDemandFile = csvDemandFile;
		this.epsgForDemandFile = epsgForDemandFile;
		this.zoneMap = zoneMap;
		this.companyLocationsList = new ArrayList<String[]>();
		this.companyClass2CompanyMap = new HashMap<String, ArrayList<Company>>();
		this.zone2CompanyMap = new HashMap<String, ArrayList<Company>>();
	}

	public Demand4CompanyClass() {

	}

	public String getZone(Coord coord, Map<String, Geometry> zoneMap) {
		// Function assumes Shapes are in the same coordinate system like MATSim
		// simulation

		SortedSet<String> keys = new TreeSet<String>(zoneMap.keySet());

		for (String zone : keys) {
			Geometry geometry = zoneMap.get(zone);
			if (geometry.intersects(MGC.coord2Point(coord))) {
				// System.out.println("Coordinate in "+ zone);
				return zone;
			}
		}

		return null;
	}

	public String getZoneFancy(Coord coord, Map<String, Geometry> zoneMap) {
		// Function assumes Shapes are in the same coordinate system like MATSim
		// simulation

		SortedSet<String> keys = new TreeSet<String>(zoneMap.keySet());
		double maxRadius = 50.0;
		double radius = 0.0;

		while (radius <= maxRadius) {
			// System.out.println("Working with radius: "+ radius );
			for (String zone : keys) {
				Geometry geometry = zoneMap.get(zone);

				if (radius == 0) {
					if (geometry.intersects(MGC.coord2Point(coord))) {
						// System.out.println("Coordinate in "+ zone +" with radius "+ radius);

						return zone;
					}

				} else {
					if (geometry.intersects(MGC.coord2Point(coord).buffer(radius))) {
						// System.out.println("Coordinate in "+ zone +" with radius "+ radius);

						return zone;
					}

				}

			}

			radius = radius + 25.0;

		}

		return null;
	}

	public String getCompanyClass() {
		File f = new File(csvDemandFile.get(0));
		String[] splitted = f.getName().split("-");
		String companyClass = null;

		if (splitted.length > 0) {
			companyClass = splitted[0];
		}

		return companyClass;
	}

	public void readDemandCSV() {
		// List<String[]> lines = new ArrayList<String[]>();
		// request_time,origin_lon,origin_lat,destination_lon,destination_lat,adult_passengers,earliest_departure_time
		// 3.83,9.748710662806856,52.37641117305442,9.724524282164197,52.387478853652404,1,3.83

		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
				"EPSG:25832");

		CSVReader reader = null;
		try {
			for (String demandFile : csvDemandFile) {

				reader = new CSVReader(new FileReader(demandFile));
				companyLocationsList = reader.readAll();
				for (int i = 1; i < companyLocationsList.size(); i++) {
					String[] lineContents = companyLocationsList.get(i);
					double lon = Double.parseDouble(lineContents[3]); // origin_lon,
					double lat = Double.parseDouble(lineContents[2]); // origin_lat,

					Coord coord = ct.transform(new Coord(lon, lat));

					String companyClass = getCompanyClass();

					String zone = getZone(coord, zoneMap);

					Company company = new Company(coord, zone, companyClass);

					if (!companyClass2CompanyMap.containsKey(companyClass)) {
						companyClass2CompanyMap.put(companyClass, new ArrayList<Company>());
						companyClass2CompanyMap.get(companyClass).add(company);
					} else {
						companyClass2CompanyMap.get(companyClass).add(company);
					}

					if (!zone2CompanyMap.containsKey(zone)) {
						zone2CompanyMap.put(zone, new ArrayList<Company>());
						zone2CompanyMap.get(zone).add(company);
					} else {
						zone2CompanyMap.get(zone).add(company);
					}
				}

			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

}

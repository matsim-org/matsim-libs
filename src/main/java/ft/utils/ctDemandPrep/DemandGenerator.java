package ft.utils.ctDemandPrep;

import org.apache.commons.lang3.mutable.MutableInt;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class DemandGenerator {
	String inputpath;
	String outputpath;
	public File[] files;
	public Map<String, List<String>> filesPerCompanyClass;
	public Map<String, Geometry> zoneMap;
	Map<String, String> nameMap;
	public Map<String, List<String>> neighbourMap;
	Map<String, String> areaMap;
	Map<String, String> useMap;
	Map<String, CommercialZone> commercialZoneMap;
	Set<String> zones;
	String ShapeFile;
	public Map<String, Demand4CompanyClass> demand4CompanyClass2List;
	Map<String, Map<String, MutableInt>> zone2CompanyClassCounterMap;
	TreeSet<String> zoneKeys;

	public DemandGenerator(String inputpath, String ShapeFile, String outputpath) {
		this.outputpath = outputpath;
		this.inputpath = inputpath;
		this.files = new File(inputpath).listFiles();
		this.filesPerCompanyClass = new HashMap<String, List<String>>();
		this.zoneMap = new HashMap<String, Geometry>();
		this.neighbourMap = new HashMap<String, List<String>>();
		this.nameMap = new HashMap<String, String>();
		this.areaMap = new HashMap<String, String>();
		this.useMap = new HashMap<String, String>();
		this.zones = new HashSet<String>();
		this.commercialZoneMap = new HashMap<String, CommercialZone>();

		this.ShapeFile = ShapeFile;
		this.demand4CompanyClass2List = new HashMap<String, Demand4CompanyClass>();
		// Zone --> CompanyClass --> Counter
		this.zone2CompanyClassCounterMap = new HashMap<String, Map<String, MutableInt>>();

		readShape(ShapeFile, "Zelle");

	}

	public static void main(String[] args) {

		run();

	}

	public static void run() {

		String companyFolder = "D:\\Thiel\\Programme\\WVModell\\00_Eingangsdaten\\Unternehmen\\";
		String zoneSHP = "D:\\Thiel\\Programme\\WVModell\\00_Eingangsdaten\\Zellen\\FNP_Merged\\baseShapeH.shp";
		String outputpath = "D:\\Thiel\\Programme\\WVModell\\00_Eingangsdaten\\";

		DemandGenerator demand = new DemandGenerator(companyFolder, zoneSHP, outputpath);
		demand.getFilesperCompanyClass();
		for (String keys:demand.filesPerCompanyClass.keySet()) {
			//String dummyDemandFile = demand.getFile(i);

			Demand4CompanyClass d = new Demand4CompanyClass(demand.filesPerCompanyClass.get(keys), null, demand.zoneMap);

			d.readDemandCSV();

			demand.demand4CompanyClass2List.put(d.getCompanyClass(), d);

		}

		demand.getCompanyClassesPerZone();
		demand.findNeighbourZones();
		demand.writeNeighbourZonesCSV();
		demand.writeTravelTimes2ZonesCSV();

	}

	public void getFilesperCompanyClass() {
		for (int i = 0; i < this.files.length; i++) {
			File f = new File(files[i].toString());
			String[] splitted = f.getName().split("-");
			String companyClass = null;
			if (splitted.length > 0) {
				companyClass = splitted[0];
			}

			if (!filesPerCompanyClass.containsKey(companyClass)) {
				filesPerCompanyClass.put(companyClass, new ArrayList<String>());
                filesPerCompanyClass.get(companyClass).add(f.getAbsolutePath());
			} else {
                filesPerCompanyClass.get(companyClass).add(f.getAbsolutePath());
			}

		}
	}

	public String getFile(int i) {

		return this.files[i].toString();

	}

	public void readShape(String shapeFile, String featureKeyInShapeFile) {
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
		for (SimpleFeature feature : features) {
			String id = feature.getAttribute(featureKeyInShapeFile).toString();
			String name = feature.getAttribute("Stadtteil").toString();
			String use = feature.getAttribute("Nutzung").toString();
			String area = null;
			if (use.contains("A")) {
				area = "0.000000";
			} else {
				area = feature.getAttribute("Flaeche").toString();
			}
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			zones.add(id);
			zoneMap.put(id, geometry);
			nameMap.put(id, name);
			areaMap.put(id, area);
			useMap.put(id, use);
			neighbourMap.put(id, null);
		}

		zoneKeys = new TreeSet<String>(zoneMap.keySet());
	}

	public void findNeighbourZonesOld() {

		for (String zone : zoneMap.keySet()) {
			ArrayList<String> neighbourzones = new ArrayList<String>();
			Geometry geometry = zoneMap.get(zone);
			for (String neighbourzone : zoneMap.keySet()) {
				Geometry neighbourgeometry = zoneMap.get(neighbourzone);
				if (zone != neighbourzone && geometry.intersects(neighbourgeometry)) {
					// System.out.println("Coordinate in "+ zone);
					neighbourzones.add(neighbourzone);
					// System.out.println("gefunden" + neighbourzone);
				}
			}
			neighbourMap.put(zone, neighbourzones);
		}
		// System.out.println("halt Stop!");
	}

	public void findNeighbourZones() {
		int maxBuffer = 150;
		int bufferInc = 10;

		for (String zone : zoneKeys) {
			int buffer = 0;
			ArrayList<String> neighbourzones = new ArrayList<String>();

			while (neighbourzones.isEmpty() && (buffer < maxBuffer)) {
				Geometry geometry = zoneMap.get(zone).buffer(buffer);
				for (String neighbourzone : zoneKeys) {

					Geometry neighbourgeometry = zoneMap.get(neighbourzone);
					if (zone != neighbourzone && geometry.intersects(neighbourgeometry)) {
						// System.out.println("Coordinate in "+ zone);
						neighbourzones.add(neighbourzone);
						// System.out.println("gefunden" + neighbourzone);
					}
				}
				buffer = buffer + bufferInc;
			}
			neighbourMap.put(zone, neighbourzones);
		}
		// System.out.println("halt Stop!");
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

	public void initializeZone2CompanyClassCounterMap() {

		Set<String> uniqueClasses = new HashSet<String>(Arrays.asList("allCompanies", "A", "B", "C", "D", "E", "F", "G",
                "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "KEP"));

		// for (Demand4CompanyClass demandPerFile : demand4CompanyClass2List) {
		// uniqueClasses.add(demandPerFile.getCompanyClass());
		//
		// }

		// Initialize
		for (String zone : this.zones) {

			Map<String, MutableInt> companyClassesCounterMap = new HashMap<String, MutableInt>();

			for (String uniqueClass : uniqueClasses) {
				companyClassesCounterMap.put(uniqueClass, new MutableInt(0));

			}

			zone2CompanyClassCounterMap.put(zone, companyClassesCounterMap);

		}

	}

	public void getCompanyClassesPerZone() {
		initializeZone2CompanyClassCounterMap();

		for (Demand4CompanyClass demandPerFile : demand4CompanyClass2List.values()) {
			String companyClass = demandPerFile.getCompanyClass();

			// Loop over all companies

			for (Entry<String, ArrayList<Company>> companiesPerClassList : demandPerFile.companyClass2CompanyMap
					.entrySet()) {
				ArrayList<Company> compList = companiesPerClassList.getValue();

				for (Company company : compList) {
					String zone = company.zone;

					if (zone != null) {
						zone2CompanyClassCounterMap.get(zone).get(companyClass).increment();
						zone2CompanyClassCounterMap.get(zone).get("allCompanies").increment();
					}

				}

			}

		}
		shape2CommercialZoneMap();
		writeCompanies2ZoneCSV();

	}

	public void shape2CommercialZoneMap() {

		for (String zone : zones) {

			CommercialZone cz = new CommercialZone(zone, zoneMap.get(zone), nameMap.get(zone), areaMap.get(zone),
					useMap.get(zone));
			commercialZoneMap.put(zone, cz);
		}

	}

	public void writeCompanies2ZoneCSV() {
		String header = "Zelle;Stadtteil;Flaeche;Nutzung;Unternehmen;A;B;C;D;E;F;G;H;I;J;K;L;M;N;O;P;Q;R;S;T;U";

		BufferedWriter bw = IOUtils.getBufferedWriter(outputpath + "Zellen.csv");
		try {
			bw.write(header);
			for (Entry<String, Map<String, MutableInt>> entry : zone2CompanyClassCounterMap.entrySet()) {
				String zone = entry.getKey();

				bw.newLine();
				bw.write(zone + ";" + commercialZoneMap.get(zone).getName() + ";"
						+ commercialZoneMap.get(zone).getArea() + ";" + commercialZoneMap.get(zone).getUse() + ";"
						+ entry.getValue().get("allCompanies") + ";" + entry.getValue().get("A") + ";"
						+ entry.getValue().get("B") + ";" + entry.getValue().get("C") + ";" + entry.getValue().get("D")
						+ ";" + entry.getValue().get("E") + ";" + entry.getValue().get("F") + ";"
						+ entry.getValue().get("G") + ";" + entry.getValue().get("H") + ";" + entry.getValue().get("I")
						+ ";" + entry.getValue().get("J") + ";" + entry.getValue().get("K") + ";"
						+ entry.getValue().get("L") + ";" + entry.getValue().get("M") + ";" + entry.getValue().get("N")
						+ ";" + entry.getValue().get("O") + ";" + entry.getValue().get("P") + ";"
						+ entry.getValue().get("Q") + ";" + entry.getValue().get("R") + ";" + entry.getValue().get("S")
						+ ";" + entry.getValue().get("T") + ";" + entry.getValue().get("U"));
			}
			bw.flush();
			bw.close();
			System.out.println("Write ZellenCSV done!");
		} catch (IOException entry) {
			// TODO Auto-generated catch block
			entry.printStackTrace();
		}
	}

	public void writeNeighbourZonesCSV() {
		String header = "Zelle;Nachbarzelle";
		BufferedWriter bw = IOUtils.getBufferedWriter(outputpath + "Nachbarzellen.csv");
		try {
			bw.write(header);
			bw.newLine();
			for (Entry<String, List<String>> entry : neighbourMap.entrySet()) {
				String zone = entry.getKey();

				if (neighbourMap.get(zone) != null) {
					for (String neighbour : neighbourMap.get(zone)) {
						bw.write(zone + ";" + neighbour);
						bw.newLine();
					}
				}

			}
			bw.flush();
			bw.close();
			System.out.println("Write NeighbourCSV done!");
		} catch (IOException entry) {
			// TODO Auto-generated catch block
			entry.printStackTrace();
		}

	}

	public void writeTravelTimes2ZonesCSV() {
		String header = "Von;Nach;Reisezeit";
		BufferedWriter bw = IOUtils.getBufferedWriter(outputpath + "Reisezeiten.csv");
		try {
			bw.write(header);
			bw.newLine();
			for (Entry<String, Geometry> entry : zoneMap.entrySet()) {
				String zone = entry.getKey();
				for (Entry<String, Geometry> other : zoneMap.entrySet()) {
					String otherZone = other.getKey();

					if (zone != otherZone) {

						double cDistance = zoneMap.get(zone).getCentroid()
								.distance(zoneMap.get(otherZone).getCentroid());
						double tt = (cDistance / (36 / 3.6)) / 60;

						bw.write(zone + ";" + otherZone + ";" + tt);
						bw.newLine();
					} else {
						int tt = 1;
						bw.write(zone + ";" + otherZone + ";" + tt);
						bw.newLine();
					}
				}

			}
			bw.flush();
			bw.close();
			System.out.println("Write TravelTimeCSV done!");
		} catch (IOException entry) {
			// TODO Auto-generated catch block
			entry.printStackTrace();
		}

	}
}
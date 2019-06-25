package ft.utils.ctDemandPrep;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableInt;
import org.locationtech.jts.geom.Geometry;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

public class DemandGenerator {
	String inputpath;
	String outputpath;
	File[] files;
	Map<String, Geometry> zoneMap;
	Map<String, String> nameMap;
	Map<String, String> areaMap;
	Map<String, String> useMap;
	Map<String, CommercialZone> commercialZoneMap;
	Set<String> zones;
	String ShapeFile;
	List<Demand4CompanyClass> demand4CompanyClass2List;
	Map<String, Map<String, MutableInt>> zone2CompanyClassCounterMap;

	DemandGenerator(String inputpath, String ShapeFile, String outputpath) {
		this.outputpath = outputpath;
		this.inputpath = inputpath;
		this.files = new File(inputpath).listFiles();
		this.zoneMap = new HashMap<String, Geometry>();
		this.nameMap = new HashMap<String, String>();
		this.areaMap = new HashMap<String, String>();
		this.useMap = new HashMap<String, String>();
		this.zones = new HashSet<String>();
		this.commercialZoneMap = new HashMap<String, CommercialZone>();

		this.ShapeFile = ShapeFile;
		this.demand4CompanyClass2List = new ArrayList<Demand4CompanyClass>();
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
		String outputCSV = "D:\\Thiel\\Programme\\WVModell\\00_Eingangsdaten\\zellen.csv";

		DemandGenerator demand = new DemandGenerator(companyFolder, zoneSHP, outputCSV);
		for (int i = 0; i < demand.files.length; i++) {
			String dummyDemandFile = demand.getFile(i);

			Demand4CompanyClass d = new Demand4CompanyClass(dummyDemandFile, null, demand.zoneMap);

			d.readDemandCSV();

			demand.demand4CompanyClass2List.add(d);

		}

		demand.getCompanyClassesPerZone();

	}

	public String getFile(int i) {

		return this.files[i].toString();

	}

	public void readShape(String shapeFile, String featureKeyInShapeFile) {
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
		for (SimpleFeature feature : features) {
			String id = feature.getAttribute(featureKeyInShapeFile).toString();
			String name = feature.getAttribute("Stadtteil").toString();
			String area = feature.getAttribute("Flaeche").toString();
			String use = feature.getAttribute("Nutzung").toString();
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			zones.add(id);
			zoneMap.put(id, geometry);
			nameMap.put(id, name);
			areaMap.put(id, area);
			useMap.put(id, use);
		}
	}

	public void initializeZone2CompanyClassCounterMap() {

		Set<String> uniqueClasses = new HashSet<String>(Arrays.asList("allCompanies", "A", "B", "C", "D", "E", "F", "G",
				"H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U"));

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

		for (Demand4CompanyClass demandPerFile : demand4CompanyClass2List) {
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
		String header = "Zelle;Stadtteil,Flaeche,Nutzung,Unternehmen;A;B;C;D;E;F;G;H;I;J;K;L;M;N;O;P;Q;R;S;T;U";

		BufferedWriter bw = IOUtils.getBufferedWriter(outputpath);
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
			System.out.println("Write CSV done!");
		} catch (IOException entry) {
			// TODO Auto-generated catch block
			entry.printStackTrace();
		}
	}

}

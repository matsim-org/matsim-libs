package playground.anhorni.locationchoice.preprocess.facilities.assembleFacilitiesVariousSources;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import playground.anhorni.choiceSetGeneration.helper.ZHFacility;

public class CreateShops {
	private final static Logger log = Logger.getLogger(CreateShops.class);

	public static void main(String[] args) {
		
		log.info("reading BZ ...");
		BZReader reader = new BZReader();
		List<Hectare> hectares = reader.readBZGrocery("input/facilities/BZ01_UNT_P_DSVIEW.TXT");
				
		log.info("filtering BZ ...");
		ZHFilter filter = new ZHFilter("input/cs/gem.shp");
		hectares = filter.filterHectares(hectares);
		
		log.info("reading Nelson facilities");
		ZHFacilitiesReader facilitiesReader = new ZHFacilitiesReader();
		List<ZHFacility> zhfacilities = facilitiesReader.readFile("input/facilities/zhFacilities3.dat");
		
		log.info("reading datapuls facilities");
		DatapulsReader datapulsreader = new DatapulsReader();
		List<ZHFacilityComposed> datapulsFacilities = datapulsreader.readDatapuls("input/facilities/POIs.txt");
		
		log.info("filtering datapuls facilities");
		datapulsFacilities = filter.filterFacilities(datapulsFacilities);
		GroceryFilter groceryFilter = new GroceryFilter();
		datapulsFacilities = groceryFilter.filterFacilities(datapulsFacilities);
		log.info("Number of datapuls facilities: " + datapulsFacilities.size());
			
		log.info("create Konrad facilities");
		ReadKonradFacilities konradReader = new ReadKonradFacilities();
		List<ZHFacilityComposed> konradFacilities = konradReader.readFacilities("input/facilities/facilities_shopsOf2005.xml");
		
		log.info("filter Konrad facilities");
		konradFacilities = filter.filterFacilities(konradFacilities);
		
		log.info("complete Konrad shops");
		TreeMap<String, ZHFacilityComposed> konradFacilitiesMap = createTree(konradFacilities);
		ReadCoop readCoop = new ReadCoop();
		readCoop.completeWithCoop("input/facilities/coop-zh.csv", konradFacilitiesMap);
		
		/*
		konradFacilities.clear();
		konradFacilities.addAll(konradFacilitiesMap.values());
		*/
		
		log.info("compare facilities ...");
		CompareFacilities comparator = new CompareFacilities();
		comparator.compare(konradFacilities, createTreeReduced(datapulsFacilities));
		
		
		
		/*
		log.info("creating shp file ...");
		SHPWriter writer = new SHPWriter();
		writer.write(hectares);
		writer.writeNelsonFacilities(zhfacilities);
		writer.writeDatapulsFacilities(datapulsFacilities);
		writer.finish();
		*/
		
		
		log.info("write shops dataset");
		ShopsWriter shopsWriter = new ShopsWriter();
		shopsWriter.write(konradFacilities);		
	}
	
	private static TreeMap<String, ZHFacilityComposed>  createTree(List<ZHFacilityComposed> facilities) {
		TreeMap<String, ZHFacilityComposed> facilitiesMap = new TreeMap<String, ZHFacilityComposed>();
		Iterator<ZHFacilityComposed> facilities_it = facilities.iterator();
		while (facilities_it.hasNext()) {
			ZHFacilityComposed facility = facilities_it.next();
		
			String key = facility.getDesc()+ facility.getPLZ()+ facility.getStreet();
			facilitiesMap.put(key, facility);
		}
		return facilitiesMap;
	}
	
	private static TreeMap<String, ZHFacilityComposed>  createTreeReduced(List<ZHFacilityComposed> facilities) {
		TreeMap<String, ZHFacilityComposed> facilitiesMap = new TreeMap<String, ZHFacilityComposed>();
		Iterator<ZHFacilityComposed> facilities_it = facilities.iterator();
		while (facilities_it.hasNext()) {
			ZHFacilityComposed facility = facilities_it.next();		
			String key = facility.getPLZ()+ facility.getStreet();
			facilitiesMap.put(key, facility);
		}
		return facilitiesMap;
	}
}

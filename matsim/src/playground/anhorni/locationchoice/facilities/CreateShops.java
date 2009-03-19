package playground.anhorni.locationchoice.facilities;

import java.util.List;

import org.apache.log4j.Logger;

import playground.anhorni.locationchoice.cs.helper.ZHFacility;

public class CreateShops {
	private final static Logger log = Logger.getLogger(CreateShops.class);

	public static void main(String[] args) {
		
		log.info("reading BZ ...");
		BZReader reader = new BZReader();
		//List<Hectare> hectares = reader.readBZ("input/facilities/BZ01_UNT_P_DSVIEW.TXT");
				
		log.info("filtering BZ ...");
		ZHFilter filter = new ZHFilter("input/cs/gem.shp");
		//hectares = filter.filterHectares(hectares);
		
		log.info("reading Nelson facilities");
		ZHFacilitiesReader facilitiesReader = new ZHFacilitiesReader();
		//List<ZHFacility> zhfacilities = facilitiesReader.readFile("input/facilities/zhFacilities3.dat");
		
		log.info("reading datapuls facilities");
		DatapulsReader datapulsreader = new DatapulsReader();
		List<ZHFacilityComposed> datapulsFacilities = datapulsreader.readDatapuls("input/facilities/POIs.txt");
		
		log.info("filtering datapuls facilities");
		datapulsFacilities = filter.filterFacilities(datapulsFacilities);
		GroceryFilter groceryFilter = new GroceryFilter();
		datapulsFacilities = groceryFilter.filterFacilities(datapulsFacilities);
		log.info("Number of datapuls facilities: " + datapulsFacilities.size());
		
		log.info("complete datapuls shops");
		
		
		log.info("create Konrad facilities");
		ReadKonradFacilities konradReader = new ReadKonradFacilities();
		List<ZHFacilityComposed> konradFacilities = konradReader.readFacilities("input/facilities/facilities_shopsOf2005.xml");
		
		log.info("filter Konrad facilities");
		konradFacilities = filter.filterFacilities(konradFacilities);
		
		log.info("compare facilities ...");
		CompareFacilities comparator = new CompareFacilities();
		comparator.compare(konradFacilities, datapulsFacilities);
		
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
}

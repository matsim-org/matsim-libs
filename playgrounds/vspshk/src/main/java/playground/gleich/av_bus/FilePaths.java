package playground.gleich.av_bus;

public class FilePaths {
	
	public final static String PATH_BASE_DIRECTORY = "../../../../../shared-svn/studies/gleich/av-bus_berlinNW/"; // 3 Levels to Workspace -> runs-svn (partly checked-out -> looks up local copy)
//	public final static String PATH_BASE_DIRECTORY = "C:/Users/gleich/av_bus berlin/";
//	public final static String PATH_BASE_DIRECTORY = "dat/Uni/av_bus berlin/";
	
	/** 10pct Scenario Unmodified */
	public final static String PATH_NETWORK_BERLIN__10PCT = PATH_BASE_DIRECTORY + "data/input/Berlin10pct/network.final10pct.xml.gz";
	public final static String PATH_POPULATION_BERLIN__10PCT_UNFILTERED = PATH_BASE_DIRECTORY + 
			"data/input/Berlin10pct/population.10pct.unfiltered.base.xml.gz";
	public final static String PATH_TRANSIT_SCHEDULE_BERLIN__10PCT = PATH_BASE_DIRECTORY + "data/input/Berlin10pct/transitSchedule.xml.gz";
	public final static String PATH_TRANSIT_VEHICLES_BERLIN__10PCT = PATH_BASE_DIRECTORY + "data/input/Berlin10pct/transitVehicles.final.xml";
	/** 10pct Scenario Modified */
	public final static String PATH_POPULATION_BERLIN__10PCT_FILTERED_TEST = PATH_BASE_DIRECTORY + 
			"data/input/Berlin10pct/mod/population.10pct.filtered.test.xml.gz";
	public final static String PATH_NETWORK_IN_STUDY_AREA_BERLIN__10PCT = PATH_BASE_DIRECTORY + 
			"data/input/Berlin10pct/mod/network.10pct.only_links_enclosed_in_area.test.txt";
	
	/** 100pct Scenario Unmodified */
	public final static String PATH_NETWORK_BERLIN_100PCT = PATH_BASE_DIRECTORY + "data/input/Berlin100pct/network.final.xml.gz";
	public final static String PATH_POPULATION_BERLIN_100PCT_UNFILTERED = PATH_BASE_DIRECTORY + 
			"data/input/Berlin100pct/population.100pct.unfiltered.base.xml.gz";
	public final static String PATH_TRANSIT_SCHEDULE_BERLIN_100PCT = PATH_BASE_DIRECTORY + "data/input/Berlin100pct/transitSchedule.xml.gz";
	public final static String PATH_TRANSIT_VEHICLES_BERLIN_100PCT = PATH_BASE_DIRECTORY + "data/input/Berlin100pct/transitVehicles.final.xml.gz";

	/** Study area */
	public final static String PATH_STUDY_AREA_SHP = PATH_BASE_DIRECTORY + "data/input/Untersuchungsraum shp/study_area.shp";
	public final static String STUDY_AREA_SHP_KEY = "id"; // name of the key column
	public final static String STUDY_AREA_SHP_ELEMENT = "1"; // key for the element containing the study area

}

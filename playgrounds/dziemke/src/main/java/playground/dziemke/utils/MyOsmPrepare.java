package playground.dziemke.utils;


public class MyOsmPrepare {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// information on which tags/filters to use taken from "shared-svn\studies\countries\de\osm_berlinbrandenburg\readme.txt":
		// berlinbrandenburg_filtered.osm - filtered by OsmPrepare (osmosis) with tags
		// tagKeyValues.put("highway", new HashSet<String>(Arrays.asList("motorway","motorway_link","trunk","trunk_link","primary","primary_link","secondary","tertiary","minor","unclassified","residential","living_street")));
		// tagKeyValues.put("route", new HashSet<String>(Arrays.asList("ferry", "subway", "light_rail", "tram", "train", "bus", "trolleybus")));
		
		// compare the following, for instance, to: "playground.andreas.osmBB.PTCountsOsm2Matsim"
		
		String inputFile = "D:/Workspace/shared-svn/studies/countries/de/osm_berlinbrandenburg/workingset/berlinbrandenburg.osm/berlinbrandenburg.osm";
		String outputFile = "D:/Workspace/shared-svn/studies/countries/de/osm_berlinbrandenburg/workingset/further/berlinbrandenburg-filtered.osm";
		String[] streetFilter = new String[]{"motorway","motorway_link","trunk","trunk_link","primary","primary_link","secondary","tertiary","minor","unclassified","residential","living_street"};
		String[] transitFilter = new String[]{"ferry", "subway", "light_rail", "tram", "train", "bus", "trolleybus"};
		
//		OsmPrepare osmPrepare = new OsmPrepare(inputFile, outputFile, streetFilter, transitFilter);
//		osmPrepare.prepareOsm();
	}

}

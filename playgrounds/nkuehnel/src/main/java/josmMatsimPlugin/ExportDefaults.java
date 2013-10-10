package josmMatsimPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores the default values for highways that are used when exporting a network from OSM-data.
 * @author nkuehnel
 * 
 */
public class ExportDefaults
{
	protected static Map<String, OsmHighwayDefaults> defaults = new HashMap<String, OsmHighwayDefaults>();
	
	
	protected static class OsmHighwayDefaults
	{

		public final int hierarchy;
		public final double lanes;
		public final double freespeed;
		public final double freespeedFactor;
		public final double laneCapacity;
		public final boolean oneway;

		public OsmHighwayDefaults(final int hierarchy, final double lanes,
				final double freespeed, final double freespeedFactor,
				final double laneCapacity, final boolean oneway)
		{
			this.hierarchy = hierarchy;
			this.lanes = lanes;
			this.freespeed = freespeed;
			this.freespeedFactor = freespeedFactor;
			this.laneCapacity = laneCapacity;
			this.oneway = oneway;
		}
	}
	
	protected static void initialize()
	{
		defaults.put("motorway", new OsmHighwayDefaults(1, 2, 120./3.6,	 1.0, 2000, true));
		defaults.put("motorway_link", new OsmHighwayDefaults(2, 1, 80./3.6,	 1.0, 1500, true));
		defaults.put("trunk", new OsmHighwayDefaults(2, 1, 80./3.6,	 1.0, 2000, false));
		defaults.put("trunk_link", new OsmHighwayDefaults(2, 1, 50./3.6, 1.0, 1500, false));
		defaults.put("primary", new OsmHighwayDefaults(3, 1, 80./3.6, 1.0, 1500, false));
		defaults.put("primary_link", new OsmHighwayDefaults(3, 1, 60./3.6, 1.0, 1500, false));
		defaults.put("secondary", new OsmHighwayDefaults(4, 1, 60./3.6, 1.0, 1000, false));
		defaults.put("tertiary", new OsmHighwayDefaults(5, 1, 45./3.6, 1.0, 600, false));
		defaults.put("minor", new OsmHighwayDefaults(6, 1, 45./3.6, 1.0, 600, false));
		defaults.put("unclassified", new OsmHighwayDefaults(6, 1, 45./3.6, 1.0, 600, false));
		defaults.put("residential", new OsmHighwayDefaults(6, 1, 30./3.6, 1.0, 600, false));
		defaults.put("living_street", new OsmHighwayDefaults(6, 1, 15./3.6, 1.0, 300, false));
	}


}

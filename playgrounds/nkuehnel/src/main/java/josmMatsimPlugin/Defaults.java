package josmMatsimPlugin;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * Stores the default values for highways that are used when exporting a network
 * from OSM-data.
 * 
 * @author nkuehnel
 * 
 */
public class Defaults {
    protected static Map<String, OsmHighwayDefaults> defaults = new HashMap<String, OsmHighwayDefaults>();
    protected static boolean cleanNet = true;
    protected static boolean keepPaths = false;
    protected static String exportPath = System.getProperty("user.home")
	    + "\\josm_matsim_export";
    protected static String importPath = System.getProperty("user.home");
    protected static String targetSystem = "WGS84";
    protected static String originSystem = "WGS84";
    protected static String[] coordSystems = { TransformationFactory.WGS84,
	    TransformationFactory.ATLANTIS, TransformationFactory.CH1903_LV03,
	    TransformationFactory.GK4, TransformationFactory.WGS84_UTM47S,
	    TransformationFactory.WGS84_UTM48N,
	    TransformationFactory.WGS84_UTM35S,
	    TransformationFactory.WGS84_UTM36S,
	    TransformationFactory.WGS84_Albers,
	    TransformationFactory.WGS84_SA_Albers,
	    TransformationFactory.WGS84_UTM33N, TransformationFactory.DHDN_GK4,
	    TransformationFactory.WGS84_UTM29N,
	    TransformationFactory.CH1903_LV03_GT,
	    TransformationFactory.WGS84_SVY21,
	    TransformationFactory.NAD83_UTM17N, TransformationFactory.WGS84_TM };

    protected static class OsmHighwayDefaults {

	public final int hierarchy;
	public final double lanes;
	public final double freespeed;
	public final double freespeedFactor;
	public final double laneCapacity;
	public final boolean oneway;

	public OsmHighwayDefaults(final int hierarchy, final double lanes,
		final double freespeed, final double freespeedFactor,
		final double laneCapacity, final boolean oneway) {
	    this.hierarchy = hierarchy;
	    this.lanes = lanes;
	    this.freespeed = freespeed;
	    this.freespeedFactor = freespeedFactor;
	    this.laneCapacity = laneCapacity;
	    this.oneway = oneway;
	}
    }

    protected static void initializeExportDefaults() {
	defaults.put("motorway", new OsmHighwayDefaults(1, 2, 120. / 3.6, 1.0,
		2000, true));
	defaults.put("motorway_link", new OsmHighwayDefaults(2, 1, 80. / 3.6,
		1.0, 1500, true));
	defaults.put("trunk", new OsmHighwayDefaults(2, 1, 80. / 3.6, 1.0,
		2000, false));
	defaults.put("trunk_link", new OsmHighwayDefaults(2, 1, 50. / 3.6, 1.0,
		1500, false));
	defaults.put("primary", new OsmHighwayDefaults(3, 1, 80. / 3.6, 1.0,
		1500, false));
	defaults.put("primary_link", new OsmHighwayDefaults(3, 1, 60. / 3.6,
		1.0, 1500, false));
	defaults.put("secondary", new OsmHighwayDefaults(4, 1, 60. / 3.6, 1.0,
		1000, false));
	defaults.put("tertiary", new OsmHighwayDefaults(5, 1, 45. / 3.6, 1.0,
		600, false));
	defaults.put("minor", new OsmHighwayDefaults(6, 1, 45. / 3.6, 1.0, 600,
		false));
	defaults.put("unclassified", new OsmHighwayDefaults(6, 1, 45. / 3.6,
		1.0, 600, false));
	defaults.put("residential", new OsmHighwayDefaults(6, 1, 30. / 3.6,
		1.0, 600, false));
	defaults.put("living_street", new OsmHighwayDefaults(6, 1, 15. / 3.6,
		1.0, 300, false));
    }

    protected static double calculateWGS84Length(Coord coord, Coord coord2) {
	double lon1 = coord.getX();
	double lat1 = coord.getY();

	double lon2 = coord2.getX();
	double lat2 = coord2.getY();

	double lat = (lat1 + lat2) / 2 * 0.01745;
	double dx = 111.3 * Math.cos(lat) * (lon1 - lon2);
	double dy = 111.3 * (lat1 - lat2);

	return Math.sqrt(dx * dx + dy * dy) * 1000;
    }

    protected static void handleInput(Map<String, JComponent> input) {
	for (int i = 0; i < 12; i++) {
	    Map<String, String> values = new HashMap<String, String>();
	    for (int j = 0; j < 5; j++) {
		String value = ((JTextField) input.get(i + "_" + j)).getText();
		values.put(i + "_" + j, value);
	    }

	    int hierarchy = Integer.parseInt(values.get(i + "_0"));
	    double lanes = Double.parseDouble(values.get(i + "_1"));
	    double freespeed = Double.parseDouble(values.get(i + "_2"));
	    double freespeedFactor = Double.parseDouble(values.get(i + "_3"));
	    double laneCapacity = Double.parseDouble(values.get(i + "_4"));
	    defaults.put(
		    OsmExportDefaultsDialog.types[i],
		    new OsmHighwayDefaults(hierarchy, lanes, freespeed,
			    freespeedFactor, laneCapacity, ((JCheckBox) input
				    .get(i + "_5")).isSelected()));
	}
    }
}

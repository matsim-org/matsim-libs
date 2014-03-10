package josmMatsimPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import josmMatsimPlugin.OsmConvertDefaults.OsmHighwayDefaults;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;

/**
 * @author nkuehnel
 */
public class Converter_v2 implements PreferenceChangedListener {
	private final static Logger log = Logger.getLogger(Converter.class);

	private final static String TAG_LANES = "lanes";
	private final static String TAG_HIGHWAY = "highway";
	private final static String TAG_MAXSPEED = "maxspeed";
	private final static String TAG_JUNCTION = "junction";
	private final static String TAG_ONEWAY = "oneway";
	private final static String TAG_CAPACITY = "capacity";
	private final static String TAG_MODES = "modes";

	private static final List<String> TRANSPORT_MODES = Arrays.asList(
			TransportMode.bike, TransportMode.car, TransportMode.other,
			TransportMode.pt, TransportMode.ride, TransportMode.transit_walk,
			TransportMode.walk);

	Map<String, OsmHighwayDefaults> highwayDefaults = new HashMap<String, OsmHighwayDefaults>();
	private final Network network;
	private boolean scaleMaxSpeed = false;
	private static boolean keepPaths = Main.pref.getBoolean(
			"matsim_convertDefaults_keepPaths", false);

	private DataSet dataSet;

	public Converter_v2(DataSet dataSet, Network network) {
		this.dataSet = dataSet;
		this.network = network;
		this.highwayDefaults = OsmConvertDefaults.getDefaults();
	}

	public Converter_v2(Network network) {
		this.highwayDefaults = OsmConvertDefaults.getDefaults();
		this.network = network;
	}

	public void setScaleMaxSpeed(final boolean scaleMaxSpeed) {
		this.scaleMaxSpeed = scaleMaxSpeed;
	}

	public Network convert() {
		List<Way> ways2Convert = new ArrayList<Way>();
		List<org.openstreetmap.josm.data.osm.Node> nodes2Convert = new ArrayList<org.openstreetmap.josm.data.osm.Node>();

		for (Way way : dataSet.getWays()) {
			if (way.getKeys().containsKey("highway")) {
				ways2Convert.add(way);
				for (org.openstreetmap.josm.data.osm.Node node: way.getNodes()) {
					if (!nodes2Convert.contains(node)) {
						nodes2Convert.add(node);
					}
				}
			}
		}
		
		for (org.openstreetmap.josm.data.osm.Node node: nodes2Convert) {
			network.addNode(convertNode(node));
		}
		for (Way way: ways2Convert) {
			for (Link link: convertWay(way)) {
				network.addLink(link);
			}
		}
		return network;
	}


	private boolean shouldWrite(OsmPrimitive osm) {
		return !osm.isNewOrUndeleted() || !osm.isDeleted();
	}


	@Override
	public void preferenceChanged(PreferenceChangeEvent e) {
		if (e.getKey().equalsIgnoreCase("matsim_convertDefaults_keepPaths")) {
			keepPaths = (Boolean) e.getNewValue().getValue();
			System.out.println(keepPaths);
		}

	}

	private Double parseDoubleIfPossible(String string) {
		try {
			return Double.parseDouble(string);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private Node matsim4osm(org.openstreetmap.josm.data.osm.Node firstNode) {
		String id = Long.toString(firstNode.getUniqueId());
		Node node = network.getNodes().get(new IdImpl(id));
		return node;
	}

	public static Double linkLength(Link link, String coordSystem) {
		Double length;
		if (coordSystem.equals(TransformationFactory.WGS84)) {
			length = OsmConvertDefaults.calculateWGS84Length(link.getFromNode()
					.getCoord(), link.getToNode().getCoord());
		} else {
			length = CoordUtils.calcDistance(link.getFromNode().getCoord(),
					link.getToNode().getCoord());
		}
		return length;
	}

	public Node convertNode(org.openstreetmap.josm.data.osm.Node n) {

		if (!shouldWrite(n)) {
			return null;
		}
		if (n.isIncomplete())
			return null;
		Long id = n.getUniqueId();
		double lat = n.getCoor().lat();
		double lon = n.getCoor().lon();

		Node node = this.network.getFactory().createNode(new IdImpl(id),
				new CoordImpl(lon, lat));

		return node;
	}

	public List<Link> convertWay(Way w) {

		// TODO: KeepPaths option

		List<Link> links = new ArrayList<Link>();
		Map<String, String> keys = w.getKeys();

		if (!shouldWrite(w)) {
			return Collections.emptyList();
		}
		if (w.isIncomplete()) {
			return Collections.emptyList();
		}
		if (w.getNodesCount() != 2)
			return Collections.emptyList();

		Double lanes = 0.;
		Double capacity = 0.;
		Double freespeed = 0.;
		Double freespeedFactor;
		Double length;
		boolean oneway = false;
		boolean onewayReverse = false;
		Set<String> modes = new HashSet<String>();

		Node fromNode = matsim4osm(w.firstNode());
		Node toNode = matsim4osm(w.lastNode());

		if (fromNode == null || toNode == null) {
			return Collections.emptyList();
		}

		if (keys.containsKey("highway")) {
			String highway = keys.get("highway");
			if (this.highwayDefaults.containsKey(highway)) {

				OsmHighwayDefaults defaults = this.highwayDefaults.get(highway);

				lanes = defaults.lanes;
				freespeed = defaults.freespeed;
				freespeedFactor = defaults.freespeedFactor;
				oneway = defaults.oneway;
				onewayReverse = false;
				double laneCapacity = defaults.laneCapacity;

				if (highway.equalsIgnoreCase("trunk")
						|| highway.equalsIgnoreCase("primary")
						|| highway.equalsIgnoreCase("secondary")) {
					if (oneway && lanes == 1.0) {
						lanes = 2.0;
					}
				}

				if (keys.containsKey(TAG_MAXSPEED)) {
					String maxspeedTag = keys.get(TAG_MAXSPEED);
					freespeed = parseDoubleIfPossible(maxspeedTag) / 3.6;
				}

				if (keys.containsKey(TAG_LANES)) {
					String lanesTag = keys.get(TAG_LANES);
					double tmp = parseDoubleIfPossible(lanesTag);
					if (tmp > 0) {
						lanes = tmp;
					}
				}

				if (!keys.containsKey(TAG_CAPACITY)) {
					capacity = lanes * laneCapacity;
				} else {
					capacity = parseDoubleIfPossible(keys.get(TAG_CAPACITY));
				}

				if (keys.containsKey(TAG_MODES)) {
					String tempArray[] = keys.get(TAG_MODES).split(";");
					for (int i = 0; i < tempArray.length; i++) {
						String mode = tempArray[i];
						if (TRANSPORT_MODES.contains(mode)) {
							modes.add(tempArray[i]);
						}
					}
				}

				if (this.scaleMaxSpeed) {
					freespeed = freespeed * freespeedFactor;
				}
			}
		} else {
			if (!keys.containsKey(TAG_CAPACITY))
				return Collections.emptyList();
			if (!keys.containsKey(TAG_MAXSPEED))
				return Collections.emptyList();
			if (!keys.containsKey(TAG_LANES))
				return Collections.emptyList();
			if (!keys.containsKey(TAG_MODES))
				return Collections.emptyList();

			capacity = parseDoubleIfPossible(keys.get(TAG_CAPACITY));
			if (capacity == null) {
				return Collections.emptyList();
			}
			freespeed = parseDoubleIfPossible(keys.get(TAG_MAXSPEED));
			if (freespeed == null) {
				return Collections.emptyList();
			}
			lanes = parseDoubleIfPossible(keys.get(TAG_LANES));
			if (lanes == null) {
				return Collections.emptyList();
			}
			modes = new HashSet<String>();
			String tempArray[] = keys.get(TAG_MODES).split(";");
			for (int i = 0; i < tempArray.length; i++) {
				String mode = tempArray[i];
				if (TRANSPORT_MODES.contains(mode)) {
					modes.add(tempArray[i]);
				}
			}
			if (modes.size() == 0) {
				return Collections.emptyList();
			}
		}

		if (keys.containsKey(TAG_ONEWAY)) {
			String onewayTag = keys.get((TAG_ONEWAY));
			if ("yes".equals(onewayTag)) {
				oneway = true;
			} else if ("true".equals(onewayTag)) {
				oneway = true;
			} else if ("1".equals(onewayTag)) {
				oneway = true;
			} else if ("-1".equals(onewayTag)) {
				onewayReverse = true;
				oneway = false;
			} else if ("no".equals(onewayTag)) {
				oneway = false; // may be used to overwrite defaults
			}
		}

		if ("roundabout".equals(keys.get(TAG_JUNCTION))) {
			// if "junction" is not set in tags, get() returns null and equals()
			// evaluates to false
			oneway = true;
		}

		String id = Long.toString(w.getUniqueId());
		if (!onewayReverse) {
			Link link = network.getFactory().createLink(new IdImpl(id),
					fromNode, toNode);
			if (keys.containsKey(ImportTask.WAY_TAG_ID)) {
				((LinkImpl) link).setOrigId(keys.get(ImportTask.WAY_TAG_ID));
			} else {
				((LinkImpl) link).setOrigId(id.toString());
			}

			if (keys.containsKey("length")) {
				length = parseDoubleIfPossible(keys.get("length"));
			} else {
				length = w.getLength();
			}
			link.setLength(length);
			link.setFreespeed(freespeed);
			link.setCapacity(capacity);
			link.setNumberOfLanes(lanes);
			link.setAllowedModes(modes);
			links.add(link);
		}
		if (!oneway) {
			Link link = network.getFactory().createLink(new IdImpl(id + "-r"),
					toNode, fromNode);

			if (keys.containsKey(ImportTask.WAY_TAG_ID)) {
				((LinkImpl) link).setOrigId(keys.get(ImportTask.WAY_TAG_ID)
						+ "-r");
			} else {
				((LinkImpl) link).setOrigId(id.toString() + "-r");
			}

			if (keys.containsKey("length")) {
				length = parseDoubleIfPossible(keys.get("length"));
			} else {
				length = w.getLength();
			}
			link.setLength(length);
			link.setFreespeed(freespeed);
			link.setCapacity(capacity);
			link.setNumberOfLanes(lanes);
			link.setAllowedModes(modes);
			links.add(link);
		}
		return links;
	}
}

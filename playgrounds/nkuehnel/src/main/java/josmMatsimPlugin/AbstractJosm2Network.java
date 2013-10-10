package josmMatsimPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.misc.Counter;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * abstract class for converting data to network-xml
 * 
 * @author nkuehnel
 */

public abstract class AbstractJosm2Network implements PrimitiveVisitor, MatsimWriter
{
	protected Network network;
	CoordinateTransformation transform;

	protected final Map<Long, OsmNode> nodes = new HashMap<Long, OsmNode>();
	protected final Map<Long, OsmWay> ways = new HashMap<Long, OsmWay>();

	final Counter nodeCounter = new Counter("node ");
	final Counter wayCounter = new Counter("way ");
	protected static long maxNodeId=0;

	protected final Set<String> unknownMaxspeedTags = new HashSet<String>();
	protected final Set<String> unknownLanesTags = new HashSet<String>();
	

	protected final static String TAG_LANES = "lanes";
	protected final static String TAG_HIGHWAY = "highway";
	protected final static String TAG_MAXSPEED = "maxspeed";
	protected final static String TAG_JUNCTION = "junction";
	
	
	protected final static String TAG_ID = "id";
	private final static String TAG_FROM = "from";
	private final static String TAG_TO = "to";
	protected final static String TAG_LENGTH = "length";
	protected final static String TAG_FREESPEED = "freespeed";
	protected final static String TAG_CAPACITY = "capacity";
	protected final static String TAG_PERMLANES = "permlanes";
	protected final static String TAG_ORIGID = "origid";
	protected final static String TAG_ONEWAY = "oneway";
	protected final static String TAG_MODES = "modes";
	
	
	
	
	private final static String[] ALL_TAGS = new String[]
			{ TAG_LANES, TAG_HIGHWAY, TAG_MAXSPEED, TAG_JUNCTION, TAG_ONEWAY, TAG_ID, TAG_FROM, TAG_TO, TAG_LENGTH, TAG_FREESPEED, TAG_CAPACITY, TAG_PERMLANES, TAG_ORIGID, TAG_MODES };
	
	protected final static String[] ALL_MATSIMTAGS = new String[]
			{ TAG_ID, TAG_FROM, TAG_TO, TAG_LENGTH, TAG_FREESPEED, TAG_CAPACITY, TAG_PERMLANES};

	
	protected boolean keepPaths = false;
	protected long id = 0;

	protected AbstractJosm2Network(final Network network,
			final CoordinateTransformation transformation)
	{
		this.network = network;
		this.transform = transformation;
	}
	
	
	
	public void writeLayer(OsmDataLayer layer)
	{
		writeContent(layer.data);
	}

	/**
	 * Writes the contents of the given dataset (nodes, then ways)
	 * 
	 * @param ds
	 *            The dataset to write
	 */
	private void writeContent(DataSet ds)
	{
		writeNodes(ds.getNodes());
		writeWays(ds.getWays());
		convert();
	}

	public void writeNodes(Collection<Node> nodes)
	{
		for (Node n : sortById(nodes))
		{
			if (shouldWrite(n))
			{
				visit(n);
			}
		}
	}

	protected void writeWays(Collection<Way> ways)
	{
		for (Way w : sortById(ways))
		{
			if (shouldWrite(w))
			{
				visit(w);
			}
		}
	}
	
	protected abstract boolean shouldWrite(OsmPrimitive osm);
	
	public void visit(INode n)
	{
		if (n.isIncomplete())
			return;
		Long id = n.getUniqueId();
		double lat = n.getCoor().lat();
		double lon = n.getCoor().lon();
		this.nodes.put(id, new OsmNode(id, this.transform.transform(new CoordImpl(lon, lat))));
		this.nodeCounter.incCounter();
	}

	@Override
	public void visit(IWay w)
	{
		if (w.isIncomplete())
			return;
		Long id = w.getUniqueId();
		OsmWay way = new OsmWay(id);
		for (int i = 0; i < w.getNodesCount(); ++i)
		{
			Node node = ((Way)w).getNode(i);
			way.nodes.add(node.getUniqueId());
		}
		if (way.nodes.isEmpty())
		{
			return;
		}
		for (String tag : ALL_TAGS)
		{
			for (String ref : w.getKeys().keySet())
			{
				if (tag.equals(ref))
				{
					way.tags.put(ref, w.getKeys().get(ref));
					break;
				}
			}
		}
		this.ways.put(id, way);
	}

	@Override
	public void visit(IRelation e)
	{
		//placeholder
	}

	protected static final Comparator<OsmPrimitive> byIdComparator = new Comparator<OsmPrimitive>()
	{
		@Override
		public int compare(OsmPrimitive o1, OsmPrimitive o2)
		{
			return (o1.getUniqueId() < o2.getUniqueId() ? -1 : (o1
					.getUniqueId() == o2.getUniqueId() ? 0 : 1));
		}
	};

	protected <T extends OsmPrimitive> Collection<T> sortById(
			Collection<T> primitives)
	{
		List<T> result = new ArrayList<T>(primitives.size());
		result.addAll(primitives);
		Collections.sort(result, byIdComparator);
		return result;
	}

	public abstract void convert();
	

	static class OsmNode
	{
		public long id;
		public boolean used = false;
		public int ways = 0;
		public final Coord coord;

		public OsmNode(final long id, final Coord coord)
		{
			this.id = id;
			this.coord = coord;
		}
	}

	static class OsmWay
	{
		public final long id;
		public final List<Long> nodes = new ArrayList<Long>(4);
		public final Map<String, String> tags = new HashMap<String, String>();
		public int hierarchy = -1;

		public OsmWay(final long id)
		{
			this.id = id;
		}
	}


	

	public abstract void createLink(final Network network, final OsmWay way,
			final OsmNode fromNode, final OsmNode toNode, double length);
	
	
	
	
	@Override
	public void write(String filename)
	{
		new org.matsim.core.network.NetworkWriter(network)
				.writeFileV1(filename);
	}
}

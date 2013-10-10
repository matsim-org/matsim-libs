package josmMatsimPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.xml.sax.Attributes;

/**
 * Parses an XML-network file and stores the nodes and ways to be used in ExportTask
 * @author nkuehnel
 * 
 */
public class JosmNetworkReader extends MatsimXmlParser
{
	private Map<Long, Node> nodes = new HashMap<Long,Node>();
	private List<Way> ways = new ArrayList<Way>();
	private CoordinateTransformation ct;
	
	
	public JosmNetworkReader(CoordinateTransformation ct)
	{
		super();
		this.ct=ct;
	}
	
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context)
	{
		if (name.equals("node"))
			createNode(atts);
		else if (name.equals("link"))
			createLink(atts);
	}

	@Override
	public void endTag(String name, String content, Stack<String> context)
	{
		// TODO Auto-generated method stub
	}
	
	private void createNode(Attributes atts)
	{
		LatLon coor;
		if(ImportTask.originSystem.equals("WGS84"))
		{
			coor = new LatLon(Double.parseDouble(atts.getValue(2)), Double.parseDouble(atts.getValue(1)));
		}
		else
		{
			Coord tmp =ct.transform(new CoordImpl(Double.parseDouble(atts.getValue(1)), Double.parseDouble(atts.getValue(2))));
			coor = new LatLon(tmp.getY(), tmp.getX());
		}
		Node node = new Node(coor);
		node.setOsmId(Long.parseLong(atts.getValue(0)), 1);
		nodes.put(node.getId(),node);
	}
	
	private void createLink (Attributes atts)
	{
		Way way = new Way(Long.parseLong(atts.getValue(0)), 1);
		way.addNode(nodes.get(Long.parseLong(atts.getValue(1))));
		way.addNode(nodes.get(Long.parseLong(atts.getValue(2))));
		for (int i=0; i<atts.getLength(); i++)
		{
			way.put(atts.getLocalName(i), atts.getValue(i));
		}
		ways.add(way);
	}
	
	public Map<Long, Node> getNodes()
	{
		return this.nodes;
	}
	
	public List<Way> getWays()
	{
		return this.ways;
	}
	

}

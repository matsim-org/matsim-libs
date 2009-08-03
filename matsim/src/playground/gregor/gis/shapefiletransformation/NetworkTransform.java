package playground.gregor.gis.shapefiletransformation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.Time;

import playground.gregor.MY_STATIC_STUFF;

public class NetworkTransform {

	private final String net;
	private final String outFile;
	private final String nodesFile;

	public NetworkTransform(String net, String outFile, String nodesFile) {
		this.net = net;
		this.outFile = outFile;
		this.nodesFile = nodesFile;
	}
	private void run() throws IOException {
		Map<String,Coord> map = new HashMap<String, Coord>(); 
		FeatureSource fts = ShapeFileReader.readDataFile(this.nodesFile);
		Iterator it = fts.getFeatures().iterator();
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			Integer id = (Integer) ft.getAttribute("ID");
			map.put(id.toString(), MGC.coordinate2Coord(ft.getDefaultGeometry().getCentroid().getCoordinate()));
			
		}
		ScenarioImpl sc = new ScenarioImpl();
		NetworkLayer net = sc.getNetwork();
		new MatsimNetworkReader(net).readFile(this.net);
		
		NetworkLayer nl = new NetworkLayer();
		for (NodeImpl node : net.getNodes().values()) {
			Coord c = map.get(node.getId().toString());
			if (node.getId().toString().contains("en")) {
				c = node.getCoord();
			}
			
			if (c == null){
				c = node.getCoord();
			}
			nl.createNode(node.getId(), c);
		}
		for (LinkImpl link : net.getLinks().values()) {
			nl.createLink(link.getId(), nl.getNode(link.getFromNode().getId()), nl.getNode(link.getToNode().getId()), link.getLength(), link.getFreespeed(Time.UNDEFINED_TIME), link.getCapacity(Time.UNDEFINED_TIME), link.getNumberOfLanes(Time.UNDEFINED_TIME));
		}
		nl.setEffectiveCellSize(net.getEffectiveCellSize());
		nl.setEffectiveLaneWidth(net.getEffectiveLaneWidth());
		nl.setCapacityPeriod(net.getCapacityPeriod());
		
		new NetworkWriter(nl,this.outFile).write();
		
	}
	public static void main(String [] args) {
		
		String nodesFile = MY_STATIC_STUFF.CVS_GIS + "/network_v20080618/nodes.shp";
		List<String> list = new ArrayList<String>();
		list.add("/home/laemmel/devel/outputs/output/output_network.xml.gz");
//		list.add("/home/laemmel/devel/inputs/networks/padang_net.xml");
//		list.add("/home/laemmel/devel/inputs/networks/padang_net_KOGAMI.xml");
//		list.add("/home/laemmel/devel/inputs/networks/padang_net_car_v20090604.xml");
//		list.add("/home/laemmel/devel/inputs/networks/padang_net_dynStorageCap.xml");
//		list.add("/home/laemmel/devel/inputs/networks/padang_net_dynStorageCap_evac.xml");
//		list.add("/home/laemmel/devel/inputs/networks/padang_net_evac.xml");
//		list.add("/home/laemmel/devel/inputs/networks/padang_net_evac_2.xml");
//		list.add("/home/laemmel/devel/inputs/networks/padang_net_evac_cutout.xml");
//		list.add("/home/laemmel/devel/inputs/networks/padang_net_evac_v20080522.xml");
//		list.add("/home/laemmel/devel/inputs/networks/padang_net_evac_v20080608.xml");
//		list.add("/home/laemmel/devel/inputs/networks/padang_net_evac_v20080618.xml");
//		list.add("/home/laemmel/devel/inputs/networks/padang_net_evac_v20080618_10p_5s.xml");
//		list.add("/home/laemmel/devel/inputs/networks/padang_net_new.xml");
//		list.add("/home/laemmel/devel/inputs/networks/padang_net_new_26112007.xml");
//		list.add("/home/laemmel/devel/inputs/networks/padang_net_v20080522.xml");
//		list.add("/home/laemmel/devel/inputs/networks/padang_net_v20080608.xml");
//		list.add("/home/laemmel/devel/inputs/networks/padang_net_v20080618.xml");


		for (String net : list) {
			try {
				new NetworkTransform(net,net,nodesFile).run();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	

}

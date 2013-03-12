package playground.dhosse.bachelorarbeit;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.geotools.data.shapefile.shp.ShapefileWriter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.MultiLineString;


public class CreateNetwork {
	
	public static void main(String[] args) throws IOException {
		
	    String osm = "C:/Users/Daniel/Dropbox/bsc/input/berlin.osm";
//		String osm = "C:/Users/Daniel/Desktop/INSPIRE/Berlin_Detailnetz.osm";
	    Config config = ConfigUtils.createConfig();
	    Scenario sc = ScenarioUtils.createScenario(config);
	    Network net = sc.getNetwork();
	  	String crs = "PROJCS[\"ETRS89_UTM_Zone_33\"," +
	  				"GEOGCS[\"GCS_ETRS89\",DATUM[\"D_ETRS89\",SPHEROID[\"GRS_1980\"," +
	  				"6378137.0,298.257222101]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\"," +
	  				"0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"]," +
	  				"PARAMETER[\"False_Easting\",3500000.0],PARAMETER[\"False_Northing\"," +
	  				"0.0],PARAMETER[\"Central_Meridian\",15.0],PARAMETER[\"Scale_Factor\"," +
	  				"0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]";
	  	CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, crs);

	  	OsmNetworkReader onr = new OsmNetworkReader(net,ct);
	    onr.parse(osm);
	    
	    new NetworkWriter(net).write("C:/Users/Daniel/Dropbox/bsc/input/berlin_osm_main.xml");
	    
	    Links2ESRIShape.main(new String[]{"C:/Users/Daniel/Dropbox/bsc/input/berlin_osm.xml",
				"C:/Users/Daniel/Dropbox/bsc/output/berlin_osm_line.shp",
				"C:/Users/Daniel/Dropbox/bsc/output/berlin_osm_poly.shp", TransformationFactory.WGS84});
	    
//		Config config = ConfigUtils.createConfig();
//		Scenario sc = ScenarioUtils.createScenario(config);
//		Network net = sc.getNetwork();
//		
//		ShapeFileReader reader = new ShapeFileReader();
//		Collection<SimpleFeature> features = reader.readFileAndInitialize("C:/Users/Daniel/Dropbox/bsc/input/detailnetz/detailnetz.shp");
//		
//		int nodeCounter=1,linkCounter=1;
//		
//		Node fromNode = null;
//		Node toNode = null;
//		
//		for(SimpleFeature feature : features){
//			
//			double länge = (Double) feature.getAttribute("LAENGE");
//			String kategorie = (String) feature.getAttribute("STRKLASSE1");
//			String klasse = (String) feature.getAttribute("STRKLASSE");
//			String direction = (String) feature.getAttribute("VRICHT");
//			String origid = (String) feature.getAttribute("OKSTRA_ID");
//			
//			MultiLineString ls = (MultiLineString) feature.getDefaultGeometry();
//			
//			Coord fromCoord = new CoordImpl(ls.getCoordinates()[0].x,ls.getCoordinates()[0].y);
//			Coord toCoord = new CoordImpl(ls.getCoordinates()[ls.getCoordinates().length-1].x,ls.getCoordinates()[ls.getCoordinates().length-1].y);
//			
//			fromNode = nodeAlreadyExistsAt(fromCoord,net.getNodes());
//			
//			toNode = nodeAlreadyExistsAt(toCoord,net.getNodes());
//			
//			if(fromNode==null){
//				fromNode = net.getFactory().createNode(new IdImpl(nodeCounter),
//						new CoordImpl(fromCoord));
//				nodeCounter++;
//				net.addNode(fromNode);
//			}
//			
//			if(toNode==null){
//				toNode = net.getFactory().createNode(new IdImpl(nodeCounter), 
//						new CoordImpl(toCoord));
//				nodeCounter++;
//				net.addNode(toNode);
//			}
//			
//			Link l = net.getFactory().createLink(new IdImpl(linkCounter), fromNode, toNode);
//			l.setLength(länge);
//			if(kategorie.equals("0")||kategorie.equals("V")||kategorie.equals("-")){
//				l.setFreespeed(10./3.6);
//				l.setNumberOfLanes(1);
//				l.setCapacity(300);
//			} else if(kategorie.equals("IV")||kategorie.equals("III")){
//				l.setFreespeed(30/3.6);
//				l.setNumberOfLanes(1);
//				l.setCapacity(600);
//			} else /*if((kategorie.equals("I")||kategorie.equals("II")))*/{
//				if(klasse.equals("A")){
//					l.setFreespeed(80/3.6);
//					l.setNumberOfLanes(3);
//					l.setCapacity(4500);
//				} else {
//					l.setFreespeed(50/3.6);
//					l.setNumberOfLanes(2);
//					l.setCapacity(1500);
//				}
//			} 
//			if(l instanceof LinkImpl)
//				((LinkImpl)l).setOrigId(origid);
//
//			net.addLink(l);
//			linkCounter++;
//			
//			if(direction.equals("B")){
//				Link l2 = net.getFactory().createLink(new IdImpl(linkCounter), toNode, fromNode);
//				l2.setLength(l.getLength());
//				l2.setFreespeed(l.getFreespeed());
//				l2.setNumberOfLanes(l.getNumberOfLanes());
//				l2.setCapacity(l.getFreespeed());
//				
//				if(l2 instanceof LinkImpl){
//					((LinkImpl)l2).setOrigId(origid);
//				}
//				
//				net.addLink(l2);
//				linkCounter++;
//			}
//			
//			toNode = null;
//			fromNode = null;
//			
//		}
//		
//		System.out.println("network contains " + net.getNodes().size() + " nodes and " + net.getLinks().size() + " links");
//		
//		String crs = "PROJCS[\"ETRS89_UTM_Zone_33\"," +
//  				"GEOGCS[\"GCS_ETRS89\",DATUM[\"D_ETRS89\",SPHEROID[\"GRS_1980\"," +
//  				"6378137.0,298.257222101]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\"," +
//  				"0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"]," +
//  				"PARAMETER[\"False_Easting\",3500000.0],PARAMETER[\"False_Northing\"," +
//  				"0.0],PARAMETER[\"Central_Meridian\",15.0],PARAMETER[\"Scale_Factor\"," +
//  				"0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]";
//		
//		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_UTM33N, crs);
//		
//		for(Node node : net.getNodes().values())
//			((NodeImpl)node).setCoord(ct.transform(node.getCoord()));
//		
//		new NetworkWriter(net).write("C:/Users/Daniel/Dropbox/bsc/input/berlin_fis.xml");
//		
//		Links2ESRIShape.main(new String[]{"C:/Users/Daniel/Dropbox/bsc/input/berlin_fis.xml",
//				"C:/Users/Daniel/Dropbox/bsc/output/berlin_fis_line.shp",
//				"C:/Users/Daniel/Dropbox/bsc/output/berlin_fis_poly.shp", TransformationFactory.WGS84});
		
	   }
	
//	private static Node nodeAlreadyExistsAt(Coord coordinate,
//			Map<Id, ? extends Node> nodes) {
//		
//		for(Node node : nodes.values()){
//			
//			if(node.getCoord().getX()==coordinate.getX()&&node.getCoord().getY()==coordinate.getY()){
//				return node;
//			}
//			
//		}
//		
//		return null;
//	}

}

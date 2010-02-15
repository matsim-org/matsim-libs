package playground.pieter.utils.gis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;

import com.vividsolutions.jts.geom.Point;

public class FindDistanceToNearestLinkWithCap {
	private NetworkLayer network;
	private String netFile;
	private String outFile;
	private String pointsFile;
	private Collection<Feature> points;
	private HashMap<Double, CoordImpl> coordMap;
	private HashMap<Double, Double> distanceMap;

	public FindDistanceToNearestLinkWithCap(String netFile, String outFile, String pointsFile, int idAttributeNo) {
		ScenarioImpl scenario = new ScenarioImpl();
		this.network = scenario.getNetwork();
		this.netFile = netFile;
		this.outFile = outFile;
		this.pointsFile =pointsFile;
		this.points = null;
		this.coordMap = new HashMap<Double, CoordImpl>();
		this.distanceMap = new HashMap<Double, Double>();
		new MatsimNetworkReader(scenario).readFile(this.netFile);
		readPoints(idAttributeNo);
	}

	public void removeLinksWithCap(double cap, boolean invert){
		Map<Id, LinkImpl> linkMap =  network.getLinks();
		Iterator<LinkImpl> linkIterator = linkMap.values().iterator();
		ArrayList<String> removeList = new ArrayList<String>();
		while(linkIterator.hasNext()){
			if(invert){
				LinkImpl currLink = linkIterator.next();
				if(currLink.getCapacity(3600) != cap){
					removeList.add(currLink.getId().toString());
				}
			}else{
				LinkImpl currLink = linkIterator.next();
				if(currLink.getCapacity(3600) == cap){
					removeList.add(currLink.getId().toString());
				}
			}
		}
//		Now remove all the marked links
		Iterator<String> listIterator = removeList.iterator();
		while(listIterator.hasNext()){
			LinkImpl link = this.network.getLinks().get(new IdImpl(listIterator.next()));
			this.network.removeLink(link);
		}
		removeList.clear();
		//quadtree finds nearest node first, so isolated nodes must be removed
		Iterator<NodeImpl> nodeIterator = this.network.getNodes().values().iterator();
		while(nodeIterator.hasNext()){
			NodeImpl currNodeImpl = nodeIterator.next();
			if(currNodeImpl.getIncidentLinks().isEmpty())
				removeList.add(currNodeImpl.getId().toString());
		}
		listIterator = removeList.iterator();
		while(listIterator.hasNext()){
			NodeImpl node = this.network.getNodes().get(new IdImpl(listIterator.next()));
			this.network.removeNode(node);
		}
	}

	public void readPoints(int idAttributeNo) {
		FeatureSource n;
		try {
			n = ShapeFileReader.readDataFile(this.pointsFile); //loads the shapefile into featuresource
			org.geotools.feature.FeatureIterator ftIterator = n.getFeatures().features();
			while (ftIterator.hasNext()) {
				Feature feature = ftIterator.next();
				Point thisPoint = (Point) feature.getAttribute(0);
				double thisId = (Double) feature.getAttribute(idAttributeNo);
				CoordImpl thisCoord = new CoordImpl(thisPoint.getX(), thisPoint.getY());
				this.coordMap.put(thisId, thisCoord);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public void mapDistances(){
		Iterator<Entry<Double,CoordImpl>> coordIterator = this.coordMap.entrySet().iterator();
		while(coordIterator.hasNext()){
			Entry<Double, CoordImpl> currEntry = coordIterator.next();
			double nearestLinkDistance = this.network.getNearestLinkDistance(currEntry.getValue());
			this.distanceMap.put(currEntry.getKey(), nearestLinkDistance);
		}
	}

	public void writeDistances() throws Exception{
			BufferedWriter output = IOUtils.getBufferedWriter(this.outFile);
			Iterator<Entry<Double,Double>> entryIterator = this.distanceMap.entrySet().iterator();
			while (entryIterator.hasNext()){
				Entry<Double, Double> currEntry = entryIterator.next();
				output.write(String.format("%f \t %f \n", currEntry.getKey(), currEntry.getValue()));
			}
			output.close();
			System.out.println("DONE!!!");
	}


	public static void main(String[] args) throws Exception{
		String netFile = "southafrica/IPDM_ETH_EmmeMOD/fullnetworkCLEAN_nozones.xml";
		String shapeFile = "southafrica/IPDM_ETH_Emme/EthDOT/GISOUT/NHTSEAcentroids.shp";
//		String shapeFile = "D:\\eclipseWorkSpace\\matsim\\southafrica\\IPDM_ETH_Emme\\EthDOT\\GISOUT\\ETAzonecentroids.shp";
		String outFile = "D:\\temp\\EAdist2road.txt";
		int idAttributeNo = 1;
		FindDistanceToNearestLinkWithCap test = new FindDistanceToNearestLinkWithCap(netFile, outFile, shapeFile, idAttributeNo);
//		test.removeLinksWithCap(1000, false);
//		test.removeLinksWithCap(6000, false);
		test.mapDistances();
		test.writeDistances();
	}
}

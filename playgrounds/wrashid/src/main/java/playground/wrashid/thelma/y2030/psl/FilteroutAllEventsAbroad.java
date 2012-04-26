package playground.wrashid.thelma.y2030.psl;

import java.awt.Polygon;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.JTS;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.StringMatrix;
import playground.wrashid.lib.tools.kml.BasicPointVisualizer;
import playground.wrashid.lib.tools.kml.Color;

import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;

public class FilteroutAllEventsAbroad {


	public static void main(String[] args) {
		Polygon swissBorders = getSwissBorders(1);
		System.out.println("-1");
		Network network = GeneralLib.readNetwork("H:/data/cvs/ivt/studies/switzerland/networks/teleatlas-ivtcheu/network.xml.gz");
		StringMatrix outputNetwork=new StringMatrix();
		ArrayList<String> strArr=new ArrayList<String>();
		strArr.add("linkId");
		strArr.add("x");
		strArr.add("y");
		outputNetwork.addRow(strArr);
		
		// select links outside switzerland
		HashMap<Id,Id> foreignLinks=new HashMap<Id,Id>();
		//HashMap<Id,Id> swissLinks=new HashMap<Id,Id>();
		
		for (Id linkId:network.getLinks().keySet()){
			Link link=network.getLinks().get(linkId);
			Coord coord = link.getCoord();
			if (!swissBorders.contains(coord.getX(), coord.getY())){
				foreignLinks.put(linkId,null);
			} else {
				//swissLinks.put(linkId,null);
				/*
				strArr=new ArrayList<String>();
				strArr.add(linkId.toString());
				strArr.add(Double.toString(coord.getX()));
				strArr.add(Double.toString(coord.getY()));
				outputNetwork.addRow(strArr);
			*/
			}
		}
		
		
		
		StringMatrix parkingTimesMatrix = GeneralLib.readStringMatrix("A:/for marina/3. april 2012_1/parkingTimesAndEnergyConsumptionCH.txt");
		// detect agents, which perform activities outside Switzerland
		HashMap<Id,Id> foreignActAgents=new HashMap<Id,Id>();
		for (int i=1;i<parkingTimesMatrix.getNumberOfRows();i++){
			
			Id agentId=new IdImpl(parkingTimesMatrix.getString(i, 0));
			Id linkId=new IdImpl(parkingTimesMatrix.getString(i, 3));
				
				if (foreignLinks.containsKey(linkId)){
					foreignActAgents.put(agentId,null);
				}
		}
		
		// remove agents from abroad from parking times file
		HashMap<Id,Id> usedLinks=new HashMap<Id,Id>();
        int i=1;
		while (i<parkingTimesMatrix.getNumberOfRows()){
			Id agentId=new IdImpl(parkingTimesMatrix.getString(i, 0));
			Id linkId=new IdImpl(parkingTimesMatrix.getString(i, 3));
			
			if (foreignActAgents.containsKey(agentId)){
				parkingTimesMatrix.deleteRow(i);
			} else {
				i++;
				usedLinks.put(linkId, null);
			}
		}
		parkingTimesMatrix.writeMatrix("c:/temp/parkingTimesAndEnergyConsumption.txt");
		
		
		// remove links from network, which are not used
		for (Id linkId:usedLinks.keySet()){
			Link link=network.getLinks().get(linkId);
			Coord coord = link.getCoord();
			strArr=new ArrayList<String>();
			strArr.add(linkId.toString());
			strArr.add(Double.toString(coord.getX()));
			strArr.add(Double.toString(coord.getY()));
			outputNetwork.addRow(strArr);
		}
		
		outputNetwork.writeMatrix("c:/temp/network.txt");
		
		
		BasicPointVisualizer basicPointVisualizer=new BasicPointVisualizer();
		/*
		
		
		
		
		HashMap<Id,Id> linkIds=new HashMap<Id, Id>();
		System.out.println("0");
		for (int i=1;i<parkingTimesMatrix.getNumberOfRows();i++){
			
			if (!parkingTimesMatrix.getString(i, 4).equalsIgnoreCase("tta")){
				
				Id linkId=new IdImpl(parkingTimesMatrix.getString(i, 3));
				
				if (!linkIds.containsKey(linkId)){
					linkIds.put(linkId,null);
				}
				
				if (i%10000==0){
					System.out.println(i + "/" + parkingTimesMatrix.getNumberOfRows());
				}
				
			}
			
		}
		
		System.out.println("1");
		*/
		for (Id linkId:usedLinks.keySet()){
			Link link=network.getLinks().get(linkId);
			Coord coord = link.getCoord();
			basicPointVisualizer.addPointCoordinate(coord, "" ,Color.GREEN);
		}
		
		
		System.out.println("2");

		
		basicPointVisualizer.write("c:/temp/abd.kml");
		

	}
	
	public static Polygon getSwissBorders(double scalingFactor){
		File file = new File("P:/Daten/GIS_Daten/ArcView/Vector200/staat_CH_polyline.shp");

		Polygon polygon=null;
		 LinkedList<MultiLine> multiLines=null;
		try {
		  Map connect = new HashMap();
		  connect.put("url", file.toURL());

		  DataStore dataStore = DataStoreFinder.getDataStore(connect);
		  String[] typeNames = dataStore.getTypeNames();
		  String typeName = typeNames[0];

		  System.out.println("Reading content " + typeName);

		  FeatureSource featureSource = dataStore.getFeatureSource(typeName);
		  FeatureCollection collection = featureSource.getFeatures();
		  FeatureIterator iterator = collection.features();
			 LinkedList<Geometry> list=null;
	         // Polyline pl=new FilteroutAllEventsAbroad()
			  
			 //Polygonizer poligonizer=new Polygonizer();
			  polygon=new Polygon();
			 
			 
			 //Point p = MGC.coord2Point(coord);

			 multiLines=new LinkedList<MultiLine>();
			  
			  try {
			    while (iterator.hasNext()) {
			      Feature feature = iterator.next();
			      Geometry sourceGeometry = feature.getDefaultGeometry();
			     
			     // System.out.println(p.within(sourceGeometry));
			      //System.out.println(sourceGeometry.getGeometryType());
			     // System.out.println(sourceGeometry.getCoordinate());
			     // System.out.println("*");
			    //  list=new LinkedList<Geometry>();
			      
			      /*
			      for (Coordinate c:sourceGeometry.getCoordinates()){
			    	  //System.out.println(c);
			    	  //poligonizer.add(c);
			    	 // CoordImpl coord2 = new CoordImpl(c.x,c.y);
					
			    	  //list.add(MGC.coord2Point(coord2));
			          
			    	 // polygon.addPoint((int) Math.round(c.x*0.001),(int) Math.round(c.y*0.001));
			    	  polygon.addPoint((int) Math.round(c.x),(int) Math.round(c.y));
			    	  
			    	  System.out.println(c);
			      }
			     */
			      
			      multiLines.add(new MultiLine(sourceGeometry));
			      
			    //Coordinate[] coordinates = sourceGeometry.getCoordinates();
				//System.out.println("f: " + coordinates[0]);
				//System.out.println("l: " + coordinates[coordinates.length-1]);
			     
			    }
			  } finally {
			    iterator.close();
			  }
			 //System.out.println(list.size());
			//  poligonizer.add(list);
			  
			// System.out.println(poligonizer.getPolygons().size());
			System.out.println();

		} catch (Throwable e) {}
		
		LinkedList<MultiLine> sortedMultiLine=new LinkedList<MultiLine>();
		
		sortedMultiLine.add(multiLines.poll());
		
		int i=0;
		while (!multiLines.isEmpty()){
			
			if (i==multiLines.size()){
				System.out.println("sorted:");
				
				for (MultiLine ml:sortedMultiLine){
					System.out.println("f: " + ml.firstCoordinate());
					System.out.println("l: " + ml.lastCoordinate());
            	} 
				
				System.out.println("unsorted:");
				
				for (MultiLine ml:multiLines){
					System.out.println("f: " + ml.firstCoordinate());
					System.out.println("l: " + ml.lastCoordinate());
            	} 
				
				
				System.out.println("not found:" + sortedMultiLine.getLast().lastCoordinate());
				
				// note: although some links still remain in the network, which don't fit with the others, but
				// they are close to each other (network looks ok visually).
				break;
			}
			
			
			
			Coordinate unsortedFirstCoordinate = multiLines.get(i).firstCoordinate();
			Coordinate unsortedLastCoordinate = multiLines.get(i).lastCoordinate();
			Coordinate sortedLastCoordinate = sortedMultiLine.getLast().lastCoordinate();
			
            
			
			if (sortedLastCoordinate.x==unsortedFirstCoordinate.x && sortedLastCoordinate.y==unsortedFirstCoordinate.y){
				sortedMultiLine.add(multiLines.get(i));
				multiLines.remove(i);
				i=0;
			} else if(sortedLastCoordinate.x==unsortedLastCoordinate.x && sortedLastCoordinate.y==unsortedLastCoordinate.y){
				multiLines.get(i).changeDirection();
				sortedMultiLine.add(multiLines.get(i));
				multiLines.remove(i);
				i=0;
			} else {
				i++;
			}
			MultiLine last = sortedMultiLine.getLast();
			
            if (last.lastCoordinate().x==647210.001141 && last.lastCoordinate().y==105720.001173){
            	LinkedList<MultiLine> tmpList=new LinkedList<MultiLine>();
            	
            	
            	while (!sortedMultiLine.isEmpty()){
            		tmpList.add(sortedMultiLine.pollLast());
            	}
            	
            	sortedMultiLine.addAll(tmpList);
            	
            	for (MultiLine ml:sortedMultiLine){
            		ml.changeDirection();
            	}
            	
			}
		} 
		
		for (MultiLine ml:sortedMultiLine){
			Coordinate[] coordinates = ml.multiLine.getCoordinates();
			if (ml.isChangeDirection()){
				for (i=coordinates.length-1;i>=0;i--){
					polygon.addPoint((int)Math.round(coordinates[i].x*scalingFactor), (int)Math.round(coordinates[i].y*scalingFactor));
				}
			} else {
				for (i=0;i<coordinates.length;i++){
					polygon.addPoint((int)Math.round(coordinates[i].x*scalingFactor), (int)Math.round(coordinates[i].y*scalingFactor));
				}
			}
			
			
		}
		
		
		return polygon;
	}
	
	
	
	
	public static boolean isInSwitzerland(Polygon swissBorders, Coord coord){
		return swissBorders.contains(coord.getX(), coord.getY());
	}
	
	private static class MultiLine{
		
		
		private final Geometry multiLine;
		private boolean changeDirection=false;

		public MultiLine(Geometry multiLine){
			this.multiLine = multiLine;
		}
		
		public Coordinate firstCoordinate(){
			if (isChangeDirection()){
				return getMultiLine().getCoordinates()[getMultiLine().getCoordinates().length-1];
			} else {
				return getMultiLine().getCoordinates()[0];
			}
			
		}
		
		public Coordinate lastCoordinate(){
			if (!isChangeDirection()){
				return getMultiLine().getCoordinates()[getMultiLine().getCoordinates().length-1];
			} else {
				return getMultiLine().getCoordinates()[0];
			}
		}
		
		public void changeDirection(){
			changeDirection=!this.isChangeDirection();
		}

		public Geometry getMultiLine() {
			return multiLine;
		}

		

		public boolean isChangeDirection() {
			return changeDirection;
		}
		
	}
	
}

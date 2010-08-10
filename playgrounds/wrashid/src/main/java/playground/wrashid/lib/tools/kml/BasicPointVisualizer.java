package playground.wrashid.lib.tools.kml;

import java.util.ArrayList;
import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;

import playground.wrashid.lib.GeneralLib;

public class BasicPointVisualizer {

	LinkedList<String> labels=new LinkedList<String>();
	LinkedList<Coord> coordinates=new LinkedList<Coord>();
	
	public void addPointCoordinate(Coord coord, String label){
		coordinates.add(coord);
		labels.add(label);
	}
	
	public void write(String pathName){
		ArrayList<String> fileContents=new ArrayList<String>();
		
		fileContents.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		fileContents.add("<kml xmlns=\"http://www.opengis.net/kml/2.2\">");
		fileContents.add("<Folder>");
		

		for (int i=0;i<coordinates.size();i++){
			addPlaceMark(coordinates.get(i),fileContents, labels.get(i));
		}
		
		fileContents.add("</Folder></kml>");
		
		GeneralLib.writeList(fileContents, pathName);
	}

	private void addPlaceMark(Coord coord, ArrayList<String> fileContents, String label) {
		CoordinateTransformation ct = new CH1903LV03toWGS84();
		Coord transformedCoord=ct.transform(coord);
		
		fileContents.add("<Placemark>");
		fileContents.add("<name>");
		fileContents.add(label);
		fileContents.add("</name>");
		
		fileContents.add("<Point><coordinates>");
		fileContents.add(transformedCoord.getX()+","+transformedCoord.getY());
		fileContents.add("</coordinates></Point>");
		
		fileContents.add("</Placemark>");
	}
	
}

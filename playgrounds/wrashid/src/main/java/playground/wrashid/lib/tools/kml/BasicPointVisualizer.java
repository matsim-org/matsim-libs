package playground.wrashid.lib.tools.kml;

import java.util.ArrayList;
import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;

import playground.wrashid.lib.GeneralLib;

public class BasicPointVisualizer {

	// TODO: make object out of these three lists (and just one list of that object...
	LinkedList<Color> color=new LinkedList<Color>();
	LinkedList<String> labels=new LinkedList<String>();
	LinkedList<Coord> coordinates=new LinkedList<Coord>();
	
	
	public void addPointCoordinate(Coord coord, String label, Color pointColor){
		coordinates.add(coord);
		labels.add(label);
		color.add(pointColor);
	}
	
	// TODO: performance of this method is horrible and needs to be improved...
	public void write(String pathName){
		ArrayList<String> fileContents=new ArrayList<String>();
		
		fileContents.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		fileContents.add("<kml xmlns=\"http://www.opengis.net/kml/2.2\">");
		fileContents.add("<Folder>");
		
		defineIconColors(fileContents);
		
		int oneMB=1024*1024*8;
		StringBuffer stringBuffer=new StringBuffer(10*oneMB);
		
		for (int i=0;i<coordinates.size();i++){
			stringBuffer=addPlaceMark(coordinates.get(i),fileContents, labels.get(i), color.get(i),stringBuffer);
		}
		
		fileContents.add(stringBuffer.toString());
		
		fileContents.add("</Folder></kml>");
		
		GeneralLib.writeList(fileContents, pathName);
	}

	private void defineIconColors(ArrayList<String> fileContents) {
		fileContents.add("<Style id=\"red\"><IconStyle><color>ff0000ff</color></IconStyle></Style>");
		fileContents.add("<Style id=\"green\"><IconStyle><color>ff00ff00</color></IconStyle></Style>");
		fileContents.add("<Style id=\"blue\"><IconStyle><color>f0ff0000</color></IconStyle></Style>");
		fileContents.add("<Style id=\"black\"><IconStyle><color>ff000000</color></IconStyle></Style>");
		fileContents.add("<Style id=\"yellow\"><IconStyle><color>ff00ffff</color></IconStyle></Style>");
	}

	
	
	private StringBuffer addPlaceMark(Coord coord, ArrayList<String> fileContents, String label, Color pointColor, StringBuffer stringBuffer) {
		CoordinateTransformation ct = new CH1903LV03toWGS84();
		Coord transformedCoord=ct.transform(coord);
		
		stringBuffer.append("<Placemark>");
		
		insertStyleUrl(pointColor,fileContents,stringBuffer);
				
		stringBuffer.append("<name>");
		stringBuffer.append(label);
		stringBuffer.append("</name>");
		
		stringBuffer.append("<Point><coordinates>");
		stringBuffer.append(transformedCoord.getX());
		stringBuffer.append(",");		
		stringBuffer.append(transformedCoord.getY());
		stringBuffer.append("</coordinates></Point></Placemark>");
		
		return stringBuffer;
	}

	private void insertStyleUrl(Color pointColor, ArrayList<String> fileContents, StringBuffer stringBuffer) {
		if (pointColor==Color.RED){
			stringBuffer.append("<styleUrl>red</styleUrl>");
		} else if (pointColor==Color.GREEN){
			stringBuffer.append("<styleUrl>green</styleUrl>");
		} else if (pointColor==Color.BLUE){
			stringBuffer.append("<styleUrl>blue</styleUrl>");
		} else if (pointColor==Color.BLACK){
			stringBuffer.append("<styleUrl>black</styleUrl>");
		} else if (pointColor==Color.YELLOW){
			stringBuffer.append("<styleUrl>yellow</styleUrl>");
		} else {
			throw new Error("color not defined");
		}
	}
	
}

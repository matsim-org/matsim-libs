package playground.droeder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.droeder.gis.DaShapeWriter;

public class TMC2009ToShape {
	
	private String inFile;
	private Messages messages;
	private Map<String, SortedMap<Integer, Coord>> lineShapes;
	private Map<String, SortedMap<String, String>> shapeAttribs;
	private Map<String, Coord> pointShapes;
	
	
	public static void main(String[] args){
		String dir = "D:/VSP/output/Meldungen2009/";
		String inFile = dir + "Meldungen2009.txt";
		String lineOutfile = dir + "Meldungen2009Lines.shp";
		String pointOutfile = dir + "Meldungen2009points.shp";
		TMC2009ToShape reader = new TMC2009ToShape(inFile);
		reader.run(lineOutfile, pointOutfile);
	}

	public TMC2009ToShape(String inFile){
		this.inFile = inFile;
		this.shapeAttribs = new HashMap<String, SortedMap<String,String>>();
		this.lineShapes = new HashMap<String, SortedMap<Integer,Coord>>();
		this.pointShapes = new HashMap<String, Coord>();
 	}
	
	public void run(String lineStringOutFile, String pointOutFile){
		this.readFromTxt();
		this.prepareCoords();
		this.prepareDataForShape();
		this.writeToShape(lineStringOutFile, pointOutFile);
	}
	
	private void prepareCoords(){
		String[] coords;
		String[] coords2;
		for(String[] v : this.messages.getMessages()){
			
			coords = v[8].split("\\)");
			coords2 = new String[coords.length-1];
			
			//handle first coord different
			coords2[0] = coords[0].substring(2);
			// handle other coord. last entry is no coord
			for(int i = 1; i<coords.length-1;i++){
				coords2[i] = coords[i].substring(2);
			}
			
			for(String s : coords2){
				String[] temp = s.split(";");
				this.messages.addCoord(v[0], new CoordImpl(temp[0].replace(",", "."), temp[1].replace(",", ".")));
			}
			
		}
	}
	
	private void prepareDataForShape(){
		SortedMap<Integer, Coord> tempCoords;
		SortedMap<String, String> tempAttribs;
		
		for(String[] m : this.messages.getMessages()){
			tempCoords = new TreeMap<Integer, Coord>();
			int i = 0;
			for(Coord c : this.messages.getCoord(m[0])){
				tempCoords.put(i, c);
				i++;
			}
			if(tempCoords.size() == 1){
				this.pointShapes.put(m[0], tempCoords.get(0));
			}else{
				this.lineShapes.put(m[0], tempCoords);
			}
			
			tempAttribs = new TreeMap<String, String>();
			for(int ii = 1; ii < m.length - 1; ii++){
				tempAttribs.put(this.messages.getKeys()[ii], m[ii]);
			}
			this.shapeAttribs.put(m[0], tempAttribs);
			
		}
		
		
	}

	private void writeToShape(String lineOutfile, String pointOutFile) {
		new DaShapeWriter().writeDefaultLineString2Shape(lineOutfile, "TMC2009", this.lineShapes, this.shapeAttribs);
		new DaShapeWriter().writeDefaultPoints2Shape(pointOutFile, "TMC2009", this.pointShapes, this.shapeAttribs);
	}

	private void readFromTxt() {
		boolean first = true;
		String line;
		try {
			BufferedReader reader = IOUtils.getBufferedReader(this.inFile);
			line = reader.readLine();
			do{
				if(!(line == null)){
					String[] columns = line.split("\t");
					if(first == true){
						this.messages = new Messages(columns);
						first = false;
					}else{
//						if(columns[5].equals("Berlin")){
						this.messages.addNewMessage(columns);
					}
					
					line = reader.readLine();
				}
			}while(!(line == null));
			reader.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class Messages{
	
	private String[] keys;
	private List<String[]> values;
	private Map<String, List<Coord>> coords;
	
	public Messages(String[] keys){
		this.keys = keys;
		this.values = new ArrayList<String[]>();
		this.coords = new HashMap<String, List<Coord>>();
	}
	
	public void addNewMessage(String[] value){
		this.values.add(value);
	}
	
	public List<String[]> getMessages(){
		return this.values;
	}
	
	public String[] getKeys(){
		return this.keys;
	}
	
	public void addCoord(String id, Coord coord){
		List<Coord> temp;
		if(!this.coords.containsKey(id)){
			temp = new ArrayList<Coord>();
			temp.add(coord);
			this.coords.put(id, temp);
		}else{
			this.coords.get(id).add(coord);
		}
	}
	
	public Map<String, List<Coord>> getCoords(){
		return this.coords;
	}
	
	public List<Coord> getCoord(String id){
		return this.coords.get(id);
	}
	
}

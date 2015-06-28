package playground.dziemke.ikea;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;


public class ConvertTazToCoord {

// path of nodeCoord2zone_mapping
private String dataFile ="./input/nodeCoord2zone_mapping.csv";

//Coordinates expected to be in 'WGS84' as Hasselt used this coordinate system for their network creation aswell. (-> CreateNetwork.java)
private CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,  "EPSG:32631");

private HashMap<Integer,List<Coord>>CoordMap = new HashMap<Integer, List<Coord>>(4000);

// set indices
private int index_tazId = 0;
private int index_x = 1;
private int index_y = 2;

private Random random = new Random();

public void convertCoordinates() throws IOException{

// load data-file
BufferedReader bufferedReader = new BufferedReader(new FileReader(this.dataFile));

// skip header
	String line = bufferedReader.readLine();
// read file
	while((line=bufferedReader.readLine()) != null){
		String parts[] = line.split(",");

// read Coordinates
		Coord CoordReadFromFile = new CoordImpl(
				Double.parseDouble(parts[index_x]),
				Double.parseDouble(parts[index_y]));
	// Transform Coordinates
		CoordReadFromFile=ct.transform(CoordReadFromFile);
	// declare TazID as key for HashMap
		int key=Integer.parseInt(parts[index_tazId].trim());

		// populate CoordMap
		if(CoordMap.containsKey(key)){
			List<Coord> CoordsInTaz = CoordMap.get(key);
			CoordsInTaz.add(CoordReadFromFile);
			CoordMap.put(key, CoordsInTaz);
		}
		else{
			List<Coord> CoordsInTaz = new ArrayList<Coord>();
			CoordsInTaz.add(CoordReadFromFile);
			CoordMap.put(key, CoordsInTaz);
		}
	}
}

public Coord randomCoordinates(int tazId){
	List<Coord> CoordListForTazId = CoordMap.get(tazId);
	Coord SelectedCoordinates = CoordListForTazId.get((random.nextInt(CoordListForTazId.size())));
	return SelectedCoordinates;
}

}

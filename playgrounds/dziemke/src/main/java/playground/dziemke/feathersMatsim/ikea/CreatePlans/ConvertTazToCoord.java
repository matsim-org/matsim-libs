package playground.dziemke.feathersMatsim.ikea.CreatePlans;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class ConvertTazToCoord {

	// path of nodeCoord2zone_mapping
	private String dataFile ="C:/Users/jeffw_000/Desktop/Dropbox/Uni/Master/Masterarbeit/MT/workspace new/ikeaStudy/input/nodeCoord2zone_mappingRectifiedTAZ.csv";

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
			String parts[] = line.split(";");

			// read Coordinates
			Coord CoordReadFromFile = new Coord(
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
		//apparently no nodes in TAZ 1841. subzone 1841 seems to mainly consist of docks from the harbor of Antwerpen. The TAZ is substituted by neighbouring TAZ 1845
		if(tazId==1841){tazId=1845;}
		//the same goes for subzone 1821 et cet.
		if(tazId==1821){tazId=1820;}
		if(tazId==1819){tazId=1820;}
		if(tazId==1817){tazId=1820;}
		if(tazId==1836){tazId=1837;}
		if(tazId==1528){tazId=1526;}
		List<Coord> CoordListForTazId = CoordMap.get(tazId);
		Coord SelectedCoordinates = CoordListForTazId.get((random.nextInt(CoordListForTazId.size())));
		return SelectedCoordinates;
	}

	public Coord findBestRandomCoordinates (int tazId, Coord originCoord, double FEATHERSdistance, int mode){
		//apparently no nodes in TAZ 1841. subzone 1841 seems to mainly consist of docks from the harbor of Antwerpen. The TAZ is substituted by neighbouring TAZ 1845
				if(tazId==1841){tazId=1845;}
				//the same goes for subzone 1821 etc.
				if(tazId==1821){tazId=1820;}
				if(tazId==1819){tazId=1820;}
				if(tazId==1817){tazId=1820;}
				if(tazId==1836){tazId=1837;}
				if(tazId==1528){tazId=1526;}
		Coord bestSuitedCoordinates=null;
		double beelineFactor=1.3;
		double differenceMin=9999999.99;
		double difference=99999999.99;
		List<Coord> CoordListForTazId = CoordMap.get(tazId);
		Iterator<Coord> ListIterator = CoordListForTazId.iterator();
		/////////////////////////////////////////////////////////////////////////////
		int counter = 0;
		while(ListIterator.hasNext()){
			Coord destinationCoord=ListIterator.next();	
			double distance=Math.sqrt(
					Math.pow(destinationCoord.getX()-originCoord.getX(),2)
					+Math.pow(destinationCoord.getY()-originCoord.getY(), 2)
					);
			/////////////////////////////////////////////////////////////////////////////
			counter++;
		// for car trips: multiply with beelineFactor	
			if(mode==1||mode==6){distance=distance*beelineFactor;}
			
			difference=Math.abs(distance-FEATHERSdistance);
			if(difference<differenceMin){
				System.out.println("------------------------------"+counter+"----------------------"+difference);
				differenceMin=difference;
				bestSuitedCoordinates=destinationCoord;
			}

		}

		return bestSuitedCoordinates;
	}

	public Boolean homeInIkeaTAZ(Coord coord){
		Boolean isResident=false;
		if(CoordMap.get(1954).contains(coord)){
			System.out.println("Agent is a resident. Slagboom opens.");
			isResident=true;
		}
		return isResident;
	}

}

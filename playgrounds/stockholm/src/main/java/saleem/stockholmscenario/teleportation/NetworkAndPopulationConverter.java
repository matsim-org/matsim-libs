package saleem.stockholmscenario.teleportation;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import saleem.stockholmscenario.utils.StockholmTransformationFactory;

public class NetworkAndPopulationConverter {
	public static void main(String[] args) {
		CoordinateTransformation ct = StockholmTransformationFactory.getCoordinateTransformation(StockholmTransformationFactory.WGS84_SWEREF99, StockholmTransformationFactory.WGS84);
		Coord coord = new CoordImpl(8269530.886710928, 1980468.5454298481 );
		 coord = ct.transform(coord);
		 System.out.println(coord.getX() + "........." + coord.getY());
		//String path = "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\only-pt_100-percent.xml";
		//String path1 = "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\only-pt_100-percent_rt90.xml";//UTM + Scaling done
		//XMLReaderWriter xml = new XMLReaderWriter();
		//xml.writeDocumentInXML(xml.modifyPopulation(xml.readFile(path)), path1);//Read, modify and save Network
	}
}

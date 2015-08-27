package saleem.stockholmscenario.teleportation;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class NetworkAndPopulationConverter {
	public static void main(String[] args) {
		//The commented transformation is correct. Commented for committing to avoid compile errors. Have to stay this way till the code is committed to TransformationFactory and MGC. Use the commented line when doing transformation.
		//CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_SWEREF99, TransformationFactory.WGS84);
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84);
		Coord coord = new CoordImpl(8269530.886710928, 1980468.5454298481 );
		 coord = ct.transform(coord);
		 System.out.println(coord.getX() + "........." + coord.getY());
		//String path = "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\only-pt_100-percent.xml";
		//String path1 = "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\only-pt_100-percent_rt90.xml";//UTM + Scaling done
		//XMLReaderWriter xml = new XMLReaderWriter();
		//xml.writeDocumentInXML(xml.modifyPopulation(xml.readFile(path)), path1);//Read, modify and save Network
	}
}

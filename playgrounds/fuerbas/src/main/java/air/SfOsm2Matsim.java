package air;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.xml.sax.SAXException;

public class SfOsm2Matsim {

	/**
	 * @param args
	 */
	public static void main(String[] args) {


		OsmAerowayParser osmReader = new OsmAerowayParser(TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
						TransformationFactory.WGS84));

		String input = args[0];				// OSM Input File

		try {
			osmReader.parse(input);
			osmReader.writeToFile(args[1]);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}

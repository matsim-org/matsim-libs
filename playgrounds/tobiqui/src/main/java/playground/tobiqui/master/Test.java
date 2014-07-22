package playground.tobiqui.master;
import java.io.IOException;

import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * 
 */

/**
 * @author tquick
 *
 */
public class Test {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		TqMatsimPlansParser plansReader = new TqMatsimPlansParser(TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
				"EPSG:3395"));
		String input = "D:/MA/workspace/master/output/siouxfalls-2014/ITERS/it.3000/3000.plans.xml.gz";				// XML plans input file
		String output = "D:/MA/workspace/master/output/siouxfalls-2014/3000_Siouxfalls_PT_SUMO.rou.xml";			// XML SUMO routes file
		plansReader.parse(input);
		
//		System.out.println(plansReader.routes.toString());
//		System.out.println(plansReader.persons.toString());

		
		TqSumoRoutesWriter routesWriter = new TqSumoRoutesWriter(plansReader, output);
		routesWriter.writeFile();
		
		System.out.println("selected: " + plansReader.selectedCount);
		System.out.println("selected: " + plansReader.carCount);
		
		
	}

}


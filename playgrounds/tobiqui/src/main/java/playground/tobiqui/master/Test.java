package playground.tobiqui.master;

import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.io.IOException;

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
		String input = "/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/regimes/uncongested/output-berlin/2kW.15.output_plans.xml.gz";
		String output = "/Users/michaelzilske/wurst/sumo.xml";
		plansReader.parse(input);
		
//		System.out.println(plansReader.routes.toString());
//		System.out.println(plansReader.persons.toString());

		
		TqSumoRoutesWriter routesWriter = new TqSumoRoutesWriter(plansReader, output);
		routesWriter.writeFile();
		
		System.out.println("selected: " + plansReader.selectedCount);
		System.out.println("selected: " + plansReader.carCount);
		
		
	}

}


package gunnar.ihop2.regent.demandreading;

import org.matsim.utils.objectattributes.ObjectAttributeUtils2;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class PopulationSampler {

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		final String prefix = "./test/regentmatsim/exchange/trips";
		final String fromFile = prefix + ".xml";
		final double fraction = 0.01;
		final String toFile = prefix + "_" + +fraction + ".xml";

		final ObjectAttributes all = new ObjectAttributes();
		final ObjectAttributesXmlReader reader = new ObjectAttributesXmlReader(
				all);
		reader.readFile(fromFile);

		final ObjectAttributes subset = ObjectAttributeUtils2
				.newFractionalSubset(all, fraction);
		System.out.println("size of all is "
				+ ObjectAttributeUtils2.allObjectKeys(all).size());
		System.out.println("size of subset is "
				+ ObjectAttributeUtils2.allObjectKeys(subset).size());

		final ObjectAttributesXmlWriter writer = new ObjectAttributesXmlWriter(
				subset);
		writer.writeFile(toFile);

		System.out.println("... DONE");
	}
}

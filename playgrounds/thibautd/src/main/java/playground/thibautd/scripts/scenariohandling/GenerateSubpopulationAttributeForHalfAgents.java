package playground.thibautd.scripts.scenariohandling;

import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class GenerateSubpopulationAttributeForHalfAgents {
	private static final String ATT = "subpopulation";
	private static final String MY_SUBPOP = "mySubpop";

	public static void main(final String[] args) {
		final String outputFile = args[ 0 ];
		final ObjectAttributes atts = new ObjectAttributes();

		for ( int i = 1; i < args.length; i++ ) {
			atts.putAttribute(
					args[ i ],
					ATT,
					MY_SUBPOP );
		}

		new ObjectAttributesXmlWriter( atts ).writeFile( outputFile );
	}
}

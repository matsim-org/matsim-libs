package org.matsim.codeexamples.converter.networkToShapefile;

import org.matsim.api.core.v01.network.Network;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;

/**
 * Short class that demonstrates how to use the somewhat hidden {@link Links2ESRIShape}.  Please feel free to actually make it run,
 * and write a test around it.
 */
class RunConvertNetworkToShapefileExample{

	public static void main( String[] args ){

		Network network = null ;

		String outputFilename = null ;

		String coordSystem = null ;

		Links2ESRIShape converter = new Links2ESRIShape( network, outputFilename, coordSystem );

		converter.write();

	}

}

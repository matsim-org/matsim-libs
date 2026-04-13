package org.matsim.contrib.accidents;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;

import java.util.ArrayList;
import java.util.List;

import static org.matsim.contrib.accidents.AccidentCostComputationBVWP.*;

class AccidentUtils{
	private static final Logger log = LogManager.getLogger( AccidentUtils.class );

	private static final String BVWP_ROAD_TYPE_ATTRIBUTE_NAME = "bvwpRoadType";

	static void setRoadTypeForAccidents( Link link, RoadType roadType ) {
		link.getAttributes().putAttribute( BVWP_ROAD_TYPE_ATTRIBUTE_NAME, roadType );
		// yy I am not sure if the AttributeConverters auto magic is able to write records.  kai, mar'26
	}
	static RoadType getRoadTypeForAccidents( Link link ) {
		RoadType result = (RoadType) link.getAttributes().getAttribute( BVWP_ROAD_TYPE_ATTRIBUTE_NAME );
		if ( result != null ) {
			return result;
		}
		String bvwpRoadTypeString = getRoadTypeString( link );
		if (bvwpRoadTypeString == null) {
			throw new RuntimeException("Required link attribute " + AccidentUtils.BVWP_ROAD_TYPE_ARRAY_ATTRIBUTE_NAME + " is null."
										   + " Please pre-process your network and specify the link attributes that are required to compute accident costs. Aborting...");
		}

		ArrayList<Integer> bvwpRoadType = new ArrayList<>();
		bvwpRoadType.add(0, Integer.valueOf(bvwpRoadTypeString.split(",")[0]));
		bvwpRoadType.add(1, Integer.valueOf(bvwpRoadTypeString.split(",")[1]));
		bvwpRoadType.add(2, Integer.valueOf(bvwpRoadTypeString.split(",")[2]));

		return AccidentUtils.getRoadTypeFromIntArray( bvwpRoadType );

	}




	/* package */ static final String BVWP_ROAD_TYPE_ARRAY_ATTRIBUTE_NAME = "bvwpRoadTypeArray";
	static void setRoadTypeArrayForAccidents( Link link, ArrayList<Integer> bvwpRoadTypeArray ){
		link.getAttributes().putAttribute( BVWP_ROAD_TYPE_ARRAY_ATTRIBUTE_NAME, bvwpRoadTypeArray.get(0 ) + "," + bvwpRoadTypeArray.get(1 ) + "," + bvwpRoadTypeArray.get(2 ) );
	}
	static String getRoadTypeString( Link link ){
		return (String) link.getAttributes().getAttribute( BVWP_ROAD_TYPE_ARRAY_ATTRIBUTE_NAME );
	}


	static RoadType getRoadTypeFromIntArray( List<Integer> roadTypeAsArray ){
		InfraType infraType = AccidentCostComputation.InfraType.values()[roadTypeAsArray.get(0 )];
		LocationContext locationContext = AccidentCostComputation.LocationContext.values()[roadTypeAsArray.get(1 )];
		int nLanes = roadTypeAsArray.get(2 );
		RoadType roadType = new RoadType( infraType, locationContext, nLanes  );
		return roadType;
	}
}

package org.matsim.contrib.accidents;

import org.matsim.api.core.v01.network.Link;

import java.util.List;

// I can't say if this interface is useful.  For the time being, RoadType uses the German names, and thus seems to be geared towards the German approach.  On the other hand,
// it seems that even within other contexts, this should not be vastly different, so maybe we should make the enum keys anglo?  Alternatively, could use String constants instead of
// enums.  kai, mar'26
// (The main reason for doing this now is that I want to bind the class so I can get, say, config.)

public interface AccidentCostComputation{
	// do not change the sequence of these since the lookup downstream is an int
	// ( This might be "Alignment" but tunnel is not an alignment!)
	enum InfraType{gradeSeparated, atGrade, tunnel}

	// do not change the sequence of these since the lookup downstream is an int
	// (Germany puts the "onlyMotorVehs" or not into the location context.  kai, mar'26)
	enum LocationContext{outsideBuiltUpOnlyMotorVehs, builtUpOnlyMotorVehs, outsideBuiltUp, BuiltUp}
	double computeAccidentCosts( double demand, Link link, AccidentCostComputationBVWP.RoadType roadType );


	@Deprecated
	default double computeAccidentCosts( double demand, Link link, List<Integer> roadTypeAsArray ) {
		final AccidentCostComputationBVWP.RoadType roadType = AccidentUtils.getRoadTypeFromIntArray( roadTypeAsArray );
		return computeAccidentCosts( demand, link, roadType );
	}

	record RoadType(InfraType infraType, LocationContext locationContext, int nLanes ) {
	}
}

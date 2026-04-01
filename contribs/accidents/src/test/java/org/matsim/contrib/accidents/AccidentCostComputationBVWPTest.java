package org.matsim.contrib.accidents;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.util.Assert;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;

import static org.matsim.contrib.accidents.AccidentCostComputationBVWP.*;

class AccidentCostComputationBVWPTest{
	private static final Logger log = LogManager.getLogger( AccidentCostComputationBVWPTest.class );

	// Die BVWP Unfallkosten Eu pro Tsd FZ-km
	// Wenn wir also 10km fahren, mit 100 FZen, dann sollten wir das genau bekommen.

	@Test
	void computeAccidentCosts(){
		Network network = NetworkUtils.createNetwork();

		Id<Link> id = Id.createLinkId( "dummyLink" );
		Node from = NetworkUtils.createNode( Id.createNodeId( "from" ) );
		Node to = NetworkUtils.createNode( Id.createNodeId( "to" ) );
		double length = 10000 ;
		double freespeed = Double.NaN;
		double capacity = Double.NaN;
		double lanes = Double.NaN;
		Link link = NetworkUtils.createLink(id, from, to, network, length, freespeed, capacity, lanes );
		double demand = 100;

		{
			Config config = ConfigUtils.createConfig();
			AccidentCostComputation accidentCostComputation = new AccidentCostComputationBVWP( config );
			{
				RoadType roadType = new RoadType( AccidentCostComputation.InfraType.atGrade, AccidentCostComputation.LocationContext.BuiltUp, 1 );
				double result = accidentCostComputation.computeAccidentCosts( demand, link, roadType );
				Assert.equals( 101.2, result );
			}
			{
				RoadType roadType = new RoadType( AccidentCostComputation.InfraType.atGrade, AccidentCostComputation.LocationContext.BuiltUp, 2 );
				double result = accidentCostComputation.computeAccidentCosts( demand, link, roadType );
				Assert.equals( 101.53, result );
			}
			{
				RoadType roadType = new RoadType( AccidentCostComputation.InfraType.atGrade, AccidentCostComputation.LocationContext.outsideBuiltUp, 1 );
				double result = accidentCostComputation.computeAccidentCosts( demand, link, roadType );
				Assert.equals( 61.785, result );
			}
			{
				RoadType roadType = new RoadType( AccidentCostComputation.InfraType.gradeSeparated, AccidentCostComputation.LocationContext.outsideBuiltUpOnlyMotorVehs, 2 );
				double result = accidentCostComputation.computeAccidentCosts( demand, link, roadType );
				Assert.equals( 23.165, result );
			}
		}
		{
			Config config1 = ConfigUtils.createConfig();
			ConfigUtils.addOrGetModule( config1, AccidentsConfigGroup.class ).setErrorHandling( AccidentsConfigGroup.ErrorHandling.returnZeroAccidentCost );
			AccidentCostComputation accidentCostComputation1 = new AccidentCostComputationBVWP( config1 );
			RoadType roadType = new RoadType( AccidentCostComputation.InfraType.gradeSeparated, AccidentCostComputation.LocationContext.BuiltUp, 2 );
			double result = accidentCostComputation1.computeAccidentCosts( demand, link, roadType );
			Assert.equals( 0., result );
		}

	}
}

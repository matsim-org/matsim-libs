package org.matsim.contrib.accidents;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

/**
 *
 * Computes the accident costs depending on the vehicle-km and the cost rate given for each road type
 *
* @author mmayobre
*/
class AccidentCostComputationBVWP implements AccidentCostComputation {
	private static final Logger log = LogManager.getLogger(AccidentCostComputationBVWP.class);

	private final AccidentsConfigGroup accidentsConfig;

	@Inject AccidentCostComputationBVWP( Config config ) {
		this.accidentsConfig = ConfigUtils.addOrGetModule( config, AccidentsConfigGroup.class );
		log.warn("errorHandling={}", this.accidentsConfig.getErrorHandling() );
	}

	/**
	 *
	 * Provides the accident costs in EUR based on a simplified version of Fig. 13 in the German 'BVWP Methodenhandbuch 2030'
	 *
	 * BE AWARE THAT THIS DOES NOT DIFFERENTIATE RAILWAYS AND ROADWAYS AT THE MOMENT!
	 * //TODO incorporate the global average cost rate for railways from the BVWP Methodenhandbuch! But we need a good guess for the distinction of railways and roads (in the pt network) before that!
	 * tschlenther, sep '21
	 *
	 * @param demand
	 * @param link
	 * @param roadType
	 * @return accident costs in EUR
	 */
	@Override public double computeAccidentCosts( double demand, Link link, RoadType roadType ){

		// in EUR per 1000 vehicle-km
		double costRateTable[][][] = {
			// yyyy The following needs to be made more robust in order to be useful for production:
			// * Replace integer numbers by constants or even better enums
			// * use english terminology

			// yy Something like "column 0" is, I think, just 0.

			// yy costRateTable[][][] has positions 0, 1, 2; starting the comments with "position 1" is misleading (albeit correct).

			// yyyyyy I am sceptical if the "1 lane", ... comments are correct.  If nLanes is indeed the last index, then the number of lanes translates into the horizontal position.
			// yyyyyy --> But then the tables are wrong.  The "planfrei" table should only have entries for "Kfz-Strasse", meaning rows 0 and 1 (instead of columns 0 and 1).
			// --> The lookup (around line 101) is actually the other way around: The number of lanes, coming in as the last index, is used as the middle index for lookup.

			// yyyyyy TODO: Write very simple text that looks up values.  For this, presumably introduce constants or maybe even enums with position index to be able to program with robust names rather than non-robust integers.

			/*	position 1
			 * 		column 0: 'Außerhalb von bebauten Gebiet, Kfz-Straße'
			 * 		column 1: 'Innerhalb von bebauten Gebiet, Kfz-Straße'
			 * 		column 2: 'Außerhalb von bebauten Gebiet'
			 * 		column 3: 'Innerhalb von bebauten Gebiet'
			 *
			 * 	position 2: number of lanes
			 */

			// 'planfrei': positon 0 --> 0
			{
				{ 0      , 0      , 0      , 0      }, // 1 lane
				{ 23.165 , 23.165 , 0      , 0      }, // 2 lane
				{ 23.79  , 23.79  , 0      , 0      }, // 3 lane
				{ 23.79  , 23.79  , 0      , 0      }  // 4 lane
			},
			// 'plangleich': positon 0 --> 1
			{
				{ 61.785 , 101.2  , 61.785 , 101.2  }, // 1 lane
				{ 31.63  , 101.53 , 31.63  , 101.53 }, // 2 lane
				{ 37.84  , 82.62  , 34.735 , 101.53 }, // 3 lane
				{ 0      , 0      , 0      , 101.53 }  // 4 lane
			},
			// tunnel: positon 0 --> 2
			{
				{ 9.56   , 15.09    , 9.56  , 15.09 }, // 1 lane
				{ 11.735 , 14.67425 , 0     , 17.57 }, // 2 lane
				{ 11.735 , 11.735   , 0     , 17.57 }, // 3 lane
				{ 9.11   , 9.11     , 0     , 17.57 }  // 4 lane
			}
		};

//		double costRate = costRateTable[roadType.get(0)][roadType.get(2)-1][roadType.get(1)];
		double costRate = costRateTable[roadType.infraType().ordinal()][(int) roadType.nLanes() -1][roadType.locationContext().ordinal()];
		// nLanes in MATSim can be fractional, since in some contexts it encodes width.  Not much we can do here, but not clear at which level we should clarify this.
		// --> the nLanes that arrives here have already been converted to int.

		if (costRate == 0) {
			log.warn("Accident cost rate for link " + link.getId().toString() + " is 0. (roadtype: " + roadType.toString() + ")" );
			log.warn("errorHandling={}", accidentsConfig.getErrorHandling() );
			switch( accidentsConfig.getErrorHandling() ) {
				case abort -> {
					throw new RuntimeException("accident cost rate does not exist for this link; see log message above; aborting");
				}
				case returnZeroAccidentCost -> {
					// doing nothing
				}
				case returnFallbackAccidentCost -> {
					throw new RuntimeException("this is currently not implemented; see comment by TS above");
				}
				default -> throw new IllegalStateException("Unexpected value: " + accidentsConfig.getErrorHandling());
			}

		}

		double vehicleKm = demand * (link.getLength() / 1000.);

		double accidentCosts = costRate * (vehicleKm / 1000.);

		return accidentCosts;
	}

}

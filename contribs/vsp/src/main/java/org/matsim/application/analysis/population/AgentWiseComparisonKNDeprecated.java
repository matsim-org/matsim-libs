package org.matsim.application.analysis.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.DefaultAnalysisMainModeIdentifier;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvWriteOptions;

import java.util.*;

import static org.matsim.application.analysis.population.AgentWiseComparisonKNUtils.formatTable;
import static org.matsim.application.analysis.population.HeadersKN.*;

class AgentWiseComparisonKNDeprecated extends AgentWiseComparisonKN{
	private static final Logger log = LogManager.getLogger( AgentWiseComparisonKNDeprecated.class );

	@NotNull private Table generateTripsTableFromPopulation( Population population, Config config, boolean isBaseTable ){

		Table table = Table.create( StringColumn.create( PERSON_ID )
			, IntColumn.create( TRIP_IDX )
			// per agent:
//			, DoubleColumn.create( UTL_OF_MONEY)
			, DoubleColumn.create( SCORE )
			, DoubleColumn.create( MONEY)
			// per trip:
//			, DoubleColumn.create( HeadersKN.MUTTS_H )
			, DoubleColumn.create( TTIME)
			, DoubleColumn.create( ASCS)
			, DoubleColumn.create( ADDTL_TRAV_SCORE )
			, StringColumn.create( MODE )
			, StringColumn.create( ACT_AT_END)
								  );

		if ( isBaseTable ) {
			table.addColumns( DoubleColumn.create( MUTTS_H ), DoubleColumn.create( UTL_OF_MONEY ), DoubleColumn.create( MUSL_h ) );
		}
		// (This has the advantage that one does not need to run the VTTS code on the policy cases.)

		MainModeIdentifier mainModeIdentifier = new DefaultAnalysisMainModeIdentifier();
		for( Person person : population.getPersons().values() ){
			// yyyyyy much/all of the following needs to be differentiated by subpopulation !!! yyyyyy

			double sumMoney = 0.;
			Double moneyFromEvents = (Double) person.getAttributes().getAttribute( KN_MONEY );
			if ( moneyFromEvents!=null ) {
				sumMoney += moneyFromEvents ;
			};
			Map<String,Double> dailyMoneyByMode = new TreeMap<>();

			final List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips( person.getSelectedPlan() );
			for( TripStructureUtils.Trip trip : trips ){

				// need to repeat these for every trip:
				// (but I am no longer looking at trip tables)
				table.stringColumn( PERSON_ID ).append( person.getId().toString() );
				table.doubleColumn( SCORE).append( person.getSelectedPlan().getScore() );

				AgentWiseComparisonKNUtils.processMUoM( isBaseTable, person, table );

				// per trip:
				table.intColumn( TRIP_IDX ).append( trips.indexOf( trip ) );
				table.stringColumn( MODE ).append( AgentWiseComparisonKNUtils.shortenModeString( mainModeIdentifier.identifyMainMode( trip.getTripElements() ) ) );
				table.stringColumn( ACT_AT_END ).append( trip.getDestinationActivity().getType() );
				if ( isBaseTable ){
					Double mutts_h = AgentWiseComparisonKNUtils.getMUTTS_h( trip.getDestinationActivity() );
					if( mutts_h != null ){
						table.doubleColumn( MUTTS_H ).append( mutts_h );
					} else{
						throw new RuntimeException( "find default value" );
					}
					Double musl_h = AgentWiseComparisonKNUtils.getMUSL_h( trip.getDestinationActivity() );
					if( musl_h != null ){
						table.doubleColumn( MUSL_h ).append( musl_h );
					} else{
						throw new RuntimeException( "find default value" );
					}
				}

				double tripTtime = 0.;
				double sumAscs = 0.;
				double addtlTravelScore = 0.;
				boolean haveAddedFirstPtAscOfTrip = false;
				for( Leg leg : trip.getLegsOnly() ){
					final ScoringConfigGroup.ScoringParameterSet subpopScoringParams = config.scoring().getScoringParameters( PopulationUtils.getSubpopulation( person ) );
					final ScoringConfigGroup.ModeParams modeParams = subpopScoringParams.getModes().get( leg.getMode() );

					// ttime:
					tripTtime += leg.getTravelTime().seconds();
					addtlTravelScore += leg.getTravelTime().seconds()/3600. * modeParams.getMarginalUtilityOfTraveling() ;

					// money:
					sumMoney += leg.getRoute().getDistance() * modeParams.getMonetaryDistanceRate();

					dailyMoneyByMode.put( leg.getMode(), modeParams.getDailyMonetaryConstant() );
					// we only want this once!

					// ascs:
					sumAscs += modeParams.getConstant();
					if ( TransportMode.pt.equals( leg.getMode() ) ) {
						if ( haveAddedFirstPtAscOfTrip ){
							//deduct this again:
							sumAscs -= modeParams.getConstant();
							// instead, add the (dis)utility of line switch:
							addtlTravelScore += subpopScoringParams.getUtilityOfLineSwitch();
//							addtlTravelScore += config.scoring().getUtilityOfLineSwitch();
							// yyyyyy the above actually adds zero.  ????
//							addtlTravelScore ++;
							if ( AgentWiseComparisonKNUtils.isTestPerson( person.getId() ) ) {
								log.warn( "personId={}; addtlTravelScore={};", person.getId(), addtlTravelScore );
							}
						} else {
							haveAddedFirstPtAscOfTrip = true;
						}
//						if( isTestPerson( person.getId() ) ){
//							log.warn( "personId={}; mode={}; ASC={}; sumAscs={}", person.getId(), leg.getMode(), modeParams.getConstant(), sumAscs );
//						}
					}
				}
				table.doubleColumn( TTIME ).append( tripTtime/3600. );
				table.doubleColumn( ASCS ).append( sumAscs );
				table.doubleColumn( ADDTL_TRAV_SCORE ).append( addtlTravelScore );
			}

			// money:
			// (could try to do this per trip; dailyConstant paid when using mode for first time; refund in last trip, or one extra "pseudo" trip)
			double dailyMoney = 0.;
			for( Double value : dailyMoneyByMode.values() ){
				dailyMoney += value;
			}
			for( TripStructureUtils.Trip trip : trips ){
				// (we have the money only at the end)
				table.doubleColumn( MONEY ).append( sumMoney + dailyMoney );
			}

			// end person loop:
		}
		{
			final Table filteredTable = table.where( table.stringColumn( PERSON_ID ).isEqualTo( "960148" ) );
			formatTable( filteredTable, 2 );
			log.info( "print filteredTable:" );
			System.out.println( filteredTable );
		}
		{
			log.info( "print summary on full table:");
			System.out.println( table.summary() );
			final Table filteredTable = table.where( table.doubleColumn( MUSL_h ).isBetweenExclusive( 0.,  16.3096909) );
			log.info( "print summary on table after 0 and max(mUSL) are removed:");
			System.out.println( filteredTable.summary() );
		}


		return table;
	}
	private void tripWiseDifferenceTable( Table tableBase, Table tablePolicy ){
		// yy There might be a more direct way to do the table join; I didn't find it.  (I now think that the main issue is that
		// under some circumstances the join column name of the second table needs to be given and I had some syntax where this was
		// not necessary and the error message was not helpful.)
		{
			final StringColumn columnToAdd = tablePolicy.stringColumn( PERSON_ID ).concatenate( "-" ).concatenate( tablePolicy.intColumn( TRIP_IDX ).asStringColumn() ).setName( "abc" );
			tablePolicy.addColumns( columnToAdd );
		}

		// Compute overlapping columns (excluding join keys)
		Set<String> leftCols = new HashSet<>( tableBase.columnNames());
		Set<String> rightCols = new HashSet<>( tablePolicy.columnNames());

//		leftCols.remove( PERSON_ID);
//		leftCols.remove( TRIP_IDX);
//		rightCols.remove( PERSON_ID);
//		rightCols.remove( TRIP_IDX);

		Set<String> duplicates = new HashSet<>(leftCols);
		duplicates.retainAll(rightCols);

		// Rename duplicates in right-hand table
		for (String dup : duplicates) {
			tablePolicy.column( dup ).setName( keyTwoOf(  dup ) );
		}

//		Table filteredTableBase = tableBase.where( tableBase.stringColumn( MODE_SEQ ).containsString("car").andNot( tableBase.stringColumn( MODE_SEQ ).containsString( "eCar" ) ) );
		Table filteredTableBase = tableBase;

//		Table filteredTablePolicy = tablePolicy.where( tablePolicy.stringColumn( keyTwoOf(HeadersKN.MODE_SEQ) ).containsString( "drt" ) );
//		Table filteredTablePolicy = tablePolicy.where( tablePolicy.stringColumn( HeadersKN.keyTwoOf(HeadersKN.MODE_SEQ ) ).containsString( "eCar" ) );
		Table filteredTablePolicy = tablePolicy;

		System.out.println( filteredTableBase.summary() );
		System.out.println( filteredTablePolicy.summary() );

		Table joinedTable = filteredTableBase.joinOn( "abc" ).inner( filteredTablePolicy, "abc_r");

		joinedTable.addColumns( AgentWiseComparisonKNUtils.deltaColumn( joinedTable, TTIME ) );
		{
			// The effect of the direct marginal utl of travelling (beta_trav) is already included in the (dis)utl computation of the corresponding legs.  Here we only need the effect on the activity times, thus MUSL:
			final DoubleColumn newColumn = joinedTable.doubleColumn( deltaOf( TTIME ) ).multiply( joinedTable.doubleColumn(
				MUSL_h ) ).multiply( -1 ).setName( deltaOf(  WEIGHTED_TTIME ) );
			joinedTable.addColumns( newColumn );
		}
		joinedTable.addColumns( AgentWiseComparisonKNUtils.deltaColumn( joinedTable, MONEY ) );
//		{
//			final DoubleColumn newColumn = joinedTable.doubleColumn( deltaOf( MONEY ) ).multiply( joinedTable.doubleColumn( UTL_OF_MONEY ) ).setName( deltaOf( WEIGHTED_MONEY) );
//			joinedTable.addColumns( newColumn );
//		}
		joinedTable.addColumns( AgentWiseComparisonKNUtils.deltaColumn( joinedTable, ADDTL_TRAV_SCORE ) );


		Table deltaTable = Table.create( joinedTable.column( PERSON_ID), joinedTable.column( TRIP_IDX )
			, joinedTable.column( SCORE )
//			, joinedTable.column( keyTwoOf( SCORE ) )
			, joinedTable.column( TTIME )
			, joinedTable.column( deltaOf( TTIME ) )
//			, joinedTable.column( MUTTS_H )
			, joinedTable.column( MUSL_h )
			, joinedTable.column( deltaOf( MONEY ) )
			, joinedTable.column( UTL_OF_MONEY)
			, joinedTable.column( ASCS )
			//
			, AgentWiseComparisonKNUtils.deltaColumn( joinedTable, SCORE )
			, joinedTable.column( deltaOf( WEIGHTED_TTIME ) )
//			, joinedTable.column( deltaOf( WEIGHTED_MONEY ) )
			, AgentWiseComparisonKNUtils.deltaColumn( joinedTable, ASCS )
			, joinedTable.column( deltaOf( ADDTL_TRAV_SCORE ) )
			//
			, joinedTable.column( MODE )
			, joinedTable.column( keyTwoOf( MODE ) )
			, joinedTable.column( ACT_AT_END )
									   );

		deltaTable.write().usingOptions( CsvWriteOptions.builder( "deltaTable.tsv" ).separator( '\t' ).build() );

		log.warn("###");
		log.warn(
			"D_SCORE_MEAN=" + deltaTable.doubleColumn( deltaOf( SCORE ) ).mean()
				+"; d_w_ttime_mean=" + deltaTable.doubleColumn( deltaOf( WEIGHTED_TTIME ) ).mean()
//				+ "; d_w_money=" + deltaTable.where( deltaTable.numberColumn( TRIP_IDX ).isEqualTo( 0 ) ).doubleColumn( deltaOf( WEIGHTED_MONEY) ).mean()
//				+ "; d_w_money=" + deltaTable.where( deltaTable.numberColumn( TRIP_IDX ).isEqualTo( 0 ) ).doubleColumn( deltaOf( WEIGHTED_MONEY) ).mean()
				+ "; d_ascs=" + deltaTable.doubleColumn( deltaOf( ASCS ) ).mean()
				+ "; d_addtlTScore=" + deltaTable.doubleColumn( deltaOf( ADDTL_TRAV_SCORE ) ).mean()
				+ "; d_ttime_mean=" + deltaTable.doubleColumn( deltaOf( TTIME) ).mean() / 3600 * 6
				);
		log.warn("###");

		Table sortedTable = deltaTable.sortOn( deltaOf( SCORE), TRIP_IDX );
		log.info( "sorted table coming here ..." );
		formatTable( sortedTable, 2 );
		System.out.println( sortedTable );
	}
}

package org.matsim.application.analysis.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.GeometryUtils;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.matsim.application.ApplicationUtils.globFile;
import static org.matsim.application.analysis.population.HeadersKN.*;
import static org.matsim.core.population.PersonUtils.getMarginalUtilityOfMoney;
import static org.matsim.core.router.TripStructureUtils.StageActivityHandling.*;

class AgentWiseComparisonKNUtils{
	private static Logger log = LogManager.getLogger( AgentWiseComparisonKNUtils.class );
	private static int wrnCnt = 0;

	static double MUTTS_AV = 9.16;
	// yyyy I am at this point unsure if we need to modify this by mode, or if this optimizes away. kai, dec'25
	// yyyyyy convert "forbidden" non-final static variable to class variable. kai, dec'25

	static class MyMoneyEventsHandler implements PersonMoneyEventHandler{
		private final Population population;
		public MyMoneyEventsHandler( Population population ){
			this.population = population;
		}
		@Override public void handleEvent( PersonMoneyEvent event ){
			Person person = population.getPersons().get( event.getPersonId() );
			Double moneyAttrib = (Double) person.getAttributes().getAttribute( AgentWiseComparisonKN.KN_MONEY );
			if ( moneyAttrib == null ) {
				person.getAttributes().putAttribute( AgentWiseComparisonKN.KN_MONEY, event.getAmount() );
			} else {
				person.getAttributes().putAttribute( AgentWiseComparisonKN.KN_MONEY, moneyAttrib + event.getAmount() );
			}
		}
	}

	static class MyStuckEventsHandler implements PersonStuckEventHandler{
		private final Population population;
		public MyStuckEventsHandler( Population population ){
			this.population = population;
		}
		@Override public void handleEvent( PersonStuckEvent event ){
			Person result = population.removePerson( event.getPersonId() );
		}
	}

	static String shortenModeString( String string ) {
		return string.replace( "electric_car", "eCar" ).replace( "electric_ride", "eRide" );
	}
	static DoubleColumn deltaColumn( Table joinedTable, String key ){
		return joinedTable.doubleColumn( keyTwoOf( key ) ).subtract( joinedTable.doubleColumn( key ) ).setName( deltaOf( key ) );
	}
	static void handleEventsfile( Path path, String pattern, Population population ){
		String baseEventsFile = globFile( path, pattern ).toString();

		double popSizeBefore = population.getPersons().size();

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler( new MyMoneyEventsHandler( population ) );
		events.addHandler( new MyStuckEventsHandler( population ) );
		events.initProcessing();

		MatsimEventsReader baseReader = new MatsimEventsReader( events );
		baseReader.readFile( baseEventsFile );
		events.finishProcessing();

		log.warn("popSize before={}; popSize after={}; ", popSizeBefore, population.getPersons().size() );
	}
	static void cleanPopulation( Population basePopulation ){
		long popSizeBefore = basePopulation.getPersons().size();
		List<Id<Person>> personsToRemove = new ArrayList<>();
		for( Person person : basePopulation.getPersons().values() ){
			Id<Person> personId = person.getId();
//			if ( personId.toString().contains("goods") || personId.toString().contains("commercial") || personId.toString().contains("freight")  ) {
			if ( !"person".equals( PopulationUtils.getSubpopulation( person ) ) ) {
				personsToRemove.add( person.getId() );
			}
		}
		for( Id<Person> personId : personsToRemove ){
			basePopulation.removePerson( personId );
		}
		log.warn( "popSizeBefore={}; popSizeAfter={}", popSizeBefore, basePopulation.getPersons().size() );
	}

	static void tagPersonsToAnalyse( Population basePopulation, List<PreparedGeometry> geometries, Scenario scenario ){
		if ( geometries==null || geometries.isEmpty() ) {
			return;
		}
		for( Person person : basePopulation.getPersons().values() ){
			boolean toAnalyse = false;
			for( Activity act : TripStructureUtils.getActivities( person.getSelectedPlan(), StagesAsNormalActivities ) ){
				Coord coord = PopulationUtils.decideOnCoordForActivity( act, scenario );
				Point point = GeometryUtils.createGeotoolsPoint( coord );
				for( PreparedGeometry geometry : geometries ){
					if( geometry.contains( point ) ){
						toAnalyse = true;
					}
				}
			}
			if ( toAnalyse ){
				setAnalysisPopulation( person, "true" );
			} else {
				setAnalysisPopulation( person, "false" );
			}
		}
//		Population newPop = PopulationUtils.createPopulation( ConfigUtils.createConfig() );
//		for( Person person : basePopulation.getPersons().values() ){
//			if ( "true".equals( getAnalysisPopulation( person ) ) ) {
//				newPop.addPerson( person );
//			}
//		}
//		PopulationUtils.writePopulation( newPop, "gartenfeld.plans.xml.gz" );
//		log.warn("exiting here");
//		System.exit(-1);
	}
	static void setAnalysisPopulation( Person person, String analysisPopulation ){
		person.getAttributes().putAttribute( "analysisPopulation", analysisPopulation );
	}
	static String getAnalysisPopulation( Person person ) {
		return (String) person.getAttributes().getAttribute( "analysisPopulation" );
	}

	static void processMUoM( boolean isBaseTable, Person person, Table table ){
		if ( isBaseTable ){
			final Double marginalUtilityOfMoney = getMarginalUtilityOfMoney( person );
			if ( marginalUtilityOfMoney != null ){
				table.doubleColumn( UTL_OF_MONEY ).append( marginalUtilityOfMoney );
			} else {
				table.doubleColumn( UTL_OF_MONEY ).append( 1. );
				if ( wrnCnt<10 ){
					log.warn( "marginalUtlOfMoney is null; personId={}", person.getId() );
					wrnCnt++;
					if( wrnCnt == 10 ){
						log.warn( Gbl.FUTURE_SUPPRESSED );
					}
				}
			}
		}
	}
	static void formatTable( Table deltaTable, int nDigits ){
		NumberFormat format = NumberFormat.getNumberInstance( Locale.US );
		format.setMaximumFractionDigits( nDigits );
		format.setMinimumFractionDigits( 0 );
		for( Column<?> column : deltaTable.columns() ){
			if ( column instanceof DoubleColumn ) {
				((DoubleColumn) column).setPrintFormatter( format, "n/a" );
			}
		}
	}
	static boolean isTestPerson( Id<Person> personId ){
		return switch( personId.toString() ){
			case
//				"766222", "459926", "1279437", "1055071", "1083364", "1450752", "114301" , "203311",
				 // lausitz:
//				 "1012515",
				 "960148",
					// dresden:
					"1084690","34371","843219","588488",
				// berlin:
				 "berlin_c5d528ea",
				 // example:
				 "person1"
					-> true;
			default -> false;
		};
	}
	static void writeSpecialPopulationForVia( Path inputPath, Population policyPopulation ){
		List<Person> personsToRemove = new ArrayList<>();
		for( Person person : policyPopulation.getPersons().values() ){
			boolean toRemove = true;
			for( Leg leg : TripStructureUtils.getLegs( person.getSelectedPlan() ) ){
				if ( TransportMode.drt.equals( leg.getMode() ) ) {
					toRemove = false;
				}
			}
			if ( toRemove ) {
				personsToRemove.add( person );
			}
		}
		for( Person person : personsToRemove ){
			policyPopulation.removePerson( person.getId() );
		}
		PopulationUtils.writePopulation( policyPopulation, inputPath + "/output_plans_with_drt.xml.gz" );
	}
	static void printSpecificPerson( Table table, String personId ){
		final Table filteredTable = table.where( table.stringColumn( PERSON_ID ).isEqualTo( personId ) );
		formatTable( filteredTable, 2 );
		log.info( "print table for specific person:" );
		System.out.println( filteredTable );
	}
	static void writeRuleOfHalfSummaryTable( Path inputPath, Config config, Table deltaTable ){
		final double factor = 1./ config.qsim().getFlowCapFactor();

		double homTtimeBenefitRemainers = deltaTable.doubleColumn( W1_TTIME_DIFF_REM ).sum() * factor ;
		double hetTtimeBenefitRemainers = deltaTable.doubleColumn( W2_TTIME_DIFF_REM ).sum() * factor ;

		double homTtimeBenfitSwitchers = deltaTable.doubleColumn( W1_TTIME_DIFF_SWI ).sum() * factor / 2 ;
		double hetTtimeBenfitSwitchers = deltaTable.doubleColumn( W2_TTIME_DIFF_SWI ).sum() * factor / 2 ;

		double ixBenefitRemainers = deltaTable.doubleColumn( IX_DIFF_REMAINING ).sum() * factor * (-1);
		double ixBenefitSwitchers = deltaTable.doubleColumn( IX_DIFF_SWITCHING ).sum() * factor / 2 * (-1);

		double roh = homTtimeBenefitRemainers + ixBenefitRemainers + homTtimeBenfitSwitchers + ixBenefitSwitchers;

//		double ttimeBenefitRemainersUniformMutts = deltaTable.doubleColumn( deltaOf( TTIME)  ).sum() * factor * (-9.16) ; // 9.16 = mean mUTTS bln
		// yy das geht so nicht, weil es nicht zwischen remainers und switchers unterscheidet

		final StringBuilder score_cmt = new StringBuilder( "are the overall (hom.) \"anglo\" benefits (potentially negative). This has the following contributions:" );

		final StringBuilder hom_ttime_uniform_rem_cmt = new StringBuilder( "... are the travel time benefits trips-w-same-mode (hom. mUTTS)." );
		final StringBuilder het_ttime_rem_cmt = new StringBuilder( "... are the travel time benefits trips-w-same-mode (het. mUTTS)." );

		final StringBuilder hom_ttime_swi_cmt = new StringBuilder( "... are the roh travel time benefits trips-w-other-mode (hom. mUTTS)." );
		final StringBuilder het_ttime_swi_cmt = new StringBuilder( "... are the roh travel time benefits trips-w-other-mode (het. mUTTS)." );

		final StringBuilder u_lineswitches_rem_cmt = new StringBuilder("... are the iX benefits trips-w-same-mode.");
		final StringBuilder u_lineswitches_swi_cmt = new StringBuilder("... are the roh iX benefits trips-w-other-mode.");

		final int maxLen = score_cmt.length();alignLeft( het_ttime_rem_cmt, maxLen );alignLeft( u_lineswitches_rem_cmt, maxLen );
		alignLeft( hom_ttime_swi_cmt, maxLen ); alignLeft( het_ttime_swi_cmt, maxLen ); alignLeft( u_lineswitches_swi_cmt, maxLen );
		alignLeft( hom_ttime_uniform_rem_cmt, maxLen );

		// ---

		Table summaryTable = Table.create( DoubleColumn.create( "value" ), StringColumn.create( "comment" ) );

		summaryTable.doubleColumn( "value" ).append( roh );
		summaryTable.stringColumn( "comment" ).append( score_cmt.toString() );

//		summaryTable.doubleColumn( "value" ).append( hetTtimeBenefitRemainers );
//		summaryTable.stringColumn( "comment" ).append( het_ttime_rem_cmt.toString() );

		summaryTable.doubleColumn( "value" ).append( homTtimeBenefitRemainers );
		summaryTable.stringColumn( "comment" ).append( hom_ttime_uniform_rem_cmt.toString() );

		summaryTable.doubleColumn( "value" ).append( ixBenefitRemainers );
		summaryTable.stringColumn( "comment" ).append( u_lineswitches_rem_cmt.toString() );

		summaryTable.doubleColumn( "value" ).append( homTtimeBenfitSwitchers );
		summaryTable.stringColumn( "comment" ).append( hom_ttime_swi_cmt.toString() );

//		summaryTable.doubleColumn( "value" ).append( hetTtimeBenfitSwitchers );
//		summaryTable.stringColumn( "comment" ).append( het_ttime_swi_cmt.toString() );

		summaryTable.doubleColumn( "value" ).append( ixBenefitSwitchers );
		summaryTable.stringColumn( "comment" ).append( u_lineswitches_swi_cmt.toString() );

//			summaryTable.doubleColumn( "value" ).append( ascs_sum );
//			summaryTable.stringColumn( "comment" ).append( asc_cmt.toString() );
//
//			summaryTable.doubleColumn( "value" ).append( acts_score + u_trav_score + ascs_sum + line_switch_score + money_score );
//			summaryTable.stringColumn( "comment" ).append( alt_sum_cmt.toString() );

		formatTable( summaryTable, 0 );

		System.out.println();
		log.info( inputPath );
		log.info( "Popsize={} rescaled to 100% by multiplying with {}.", deltaTable.rowCount(), factor );
		System.out.println( summaryTable + System.lineSeparator() );
		System.out.println();
	}
	static void alignLeft( StringBuilder weighted_ttime_cmt, int maxLen ){
		weighted_ttime_cmt.append( " ".repeat( maxLen - weighted_ttime_cmt.length() ) );
	}
	static void writeMatsimScoresSummaryTable( Path inputPath, Config config, Table deltaTable ){
		final double factor = 1./ config.qsim().getFlowCapFactor();
		final double score_sum = deltaTable.doubleColumn( deltaOf( SCORE ) ).sum() * factor;
		final double money_score = deltaTable.doubleColumn( deltaOf(MONEY_SCORE ) ).sum() * factor;
		final double ascs_sum = deltaTable.doubleColumn( deltaOf( ASCS ) ).sum() * factor;
		final double u_trav_score = deltaTable.doubleColumn( deltaOf( U_TRAV_DIRECT ) ).sum() * factor;
		final double line_switch_score = deltaTable.doubleColumn( deltaOf( U_LINESWITCHES ) ).sum() * factor;
		final double acts_score = deltaTable.doubleColumn( deltaOf( ACTS_SCORE ) ).sum() * factor;

		final StringBuilder score_cmt = new StringBuilder( "is the overall benefit (potentially negative) in score space. This has the following contributions:" );
		final StringBuilder weighted_ttime_cmt = new StringBuilder( "... is the travel time benefit." );
		final StringBuilder weighted_money_cmt = new StringBuilder( "... is the monetary benefit (re-weighted by indiv. mUoM)." );
		final StringBuilder asc_cmt = new StringBuilder( "... are the ASC benefits." );
		final StringBuilder u_trav_direct_cmt = new StringBuilder( "... are the direct travel score benefits (=less bike, less ride)." );
		final StringBuilder u_lineswitches_cmt = new StringBuilder("... are the line switching benefits.");
		final StringBuilder sum_cmt = new StringBuilder( "is the sum of these contributions." );
		final StringBuilder acts_score_cmt = new StringBuilder( "... are the activities score (= pure travel time) benefits." );
		final StringBuilder alt_sum_cmt = new StringBuilder("is the sum of these contributions.") ;

		final int maxLen = score_cmt.length();
		alignLeft( weighted_ttime_cmt, maxLen );
		alignLeft( weighted_money_cmt, maxLen );
		alignLeft( asc_cmt, maxLen );
		alignLeft( u_trav_direct_cmt, maxLen );
		alignLeft( u_lineswitches_cmt, maxLen );
		alignLeft( sum_cmt, maxLen );
		alignLeft( acts_score_cmt, maxLen );
		alignLeft( alt_sum_cmt, maxLen );

		// ---

		Table summaryTable = Table.create( DoubleColumn.create( "value" ), StringColumn.create( "comment" ) );

		summaryTable.doubleColumn( "value" ).append( score_sum );
		summaryTable.stringColumn( "comment" ).append( score_cmt.toString() );

		summaryTable.doubleColumn( "value" ).append( acts_score );
		summaryTable.stringColumn( "comment" ).append( acts_score_cmt.toString() );

		summaryTable.doubleColumn( "value" ).append( u_trav_score );
		summaryTable.stringColumn( "comment" ).append( u_trav_direct_cmt.toString() );

		summaryTable.doubleColumn( "value" ).append( line_switch_score );
		summaryTable.stringColumn( "comment" ).append( u_lineswitches_cmt.toString() );

		summaryTable.doubleColumn( "value" ).append( money_score );
		summaryTable.stringColumn( "comment" ).append( weighted_money_cmt.toString() );

		summaryTable.doubleColumn( "value" ).append( ascs_sum );
		summaryTable.stringColumn( "comment" ).append( asc_cmt.toString() );

		summaryTable.doubleColumn( "value" ).append( acts_score + u_trav_score + ascs_sum + line_switch_score + money_score );
		summaryTable.stringColumn( "comment" ).append( alt_sum_cmt.toString() );

		formatTable( summaryTable, 0 );

		System.out.println();
		log.info( inputPath );
		log.info( "Popsize={} rescaled to 100% by multiplying with {}.", deltaTable.rowCount(), factor );
		System.out.println( summaryTable + System.lineSeparator() );
		System.out.println();
	}
	static void writeAscTable( Config config ){
		Table summaryTable = Table.create( DoubleColumn.create( "ASC" ), StringColumn.create( "mode" ));

		for( ScoringConfigGroup.ModeParams modeParams : config.scoring().getModes().values() ){
			summaryTable.doubleColumn( "ASC" ).append( modeParams.getConstant() );
			summaryTable.stringColumn( "mode" ).append( modeParams.getMode() ) ;
		}

		formatTable(  summaryTable, 2 );

		System.out.println( System.lineSeparator() + summaryTable + System.lineSeparator() );
	}
	static @NotNull Population readAndCleanPopulation( Path path, List<String> eventsFilePatterns ){
		String basePopulationFilename;
		try {
			basePopulationFilename = globFile( path, "*vtts_experienced_plans.xml.gz" ).toString();
		} catch ( IllegalStateException ee ) {
			try{
				basePopulationFilename = globFile( path, "*postproc_experienced_plans.xml.gz" ).toString();
			} catch ( IllegalStateException e2 ) {
				basePopulationFilename = globFile( path, "*_experienced_plans.xml.gz" ).toString();
			}
		}

		Population basePopulation = PopulationUtils.readPopulation( basePopulationFilename );

		cleanPopulation( basePopulation );

		for (String pattern : eventsFilePatterns) {
			handleEventsfile( path, pattern, basePopulation );
			// (most of the time, this should be a filtered events file, and we only use money and stuck info)
		}

		return basePopulation;
	}
	static void computeAndSetMarginalUtilitiesOfMoney( Population basePopulation ){
		double sumIncome = 0.;
		double incomeCnt = 0.;
		for( Person person : basePopulation.getPersons().values() ){
			Double income = PersonUtils.getIncome( person );
			if ( income != null ) {
				sumIncome += income;
				incomeCnt ++;
			}
		}
		final double avIncome = sumIncome / incomeCnt;
		for( Person person : basePopulation.getPersons().values() ){
			Double income = PersonUtils.getIncome( person );
			if ( income != null ) {
				PersonUtils.setMarginalUtilityOfMoney( person, avIncome / income );
			} else {
				PersonUtils.setMarginalUtilityOfMoney( person, 1. );
			}
		}
	}
}

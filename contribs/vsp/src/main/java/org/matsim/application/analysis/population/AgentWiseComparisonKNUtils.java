package org.matsim.application.analysis.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.matsim.application.ApplicationUtils.globFile;
import static org.matsim.application.analysis.population.HeadersKN.*;

class AgentWiseComparisonKNUtils{
	private static Logger log = LogManager.getLogger( AgentWiseComparisonKNUtils.class );
	private static int wrnCnt = 0;

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

	public static Double getMUTTS_h( Activity activity ) {
		return (Double) activity.getAttributes().getAttribute( "mUTTS_h (incoming trip)" );
	}
	public static Double getVTTS_h( Activity activity ) {
		return (Double) activity.getAttributes().getAttribute( "VTTS_h (incoming trip)" );
	}
	public static Double getMarginalUtilityOfMoney( Person person ) {
		return (Double) person.getAttributes().getAttribute( "marginalUtilityOfMoney" );
	}
	public static Double getMUSL_h( Activity activity ) {
		return (Double) activity.getAttributes().getAttribute( "marginal_utility_of_starting_later_h" );
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

		log.info("popSize before={}; popSize after={}; ", popSizeBefore, population.getPersons().size() );
	}
	static void cleanPopulation( Population basePopulation ){
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
				 "berlin_c5d528ea"
					-> true;
			default -> false;
		};
	}
}

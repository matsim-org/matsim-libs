package playground.thibautd.initialdemandgeneration.activitychainsextractor;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;

class MzWeg implements Identifiable {
	/**
	 * reproduces all the possible answers of the MZ
	 */
	public enum Purpose {
		work,
		commercialActivity,
		educ,
		leisure,
		shop,
		transitTransfer,
		useService,
		servePassengerRide,
		accompany,
		unknown}; 

	// /////////////////////////////////////////////////////////////////////////
	// static fields
	private static boolean structureIsKnown = false;

	private static final String PERSON_NAME = "INTNR";
	private static int personIndex = -1;

	private static final String WEGNR_NAME = "WEG";
	private static int wegnrIndex = -1;

	private static final String DEPARTURE_TIME_NAME = "WVON";
	private static int departureTimeIndex = -1;

	private static final String ARRIVAL_TIME_NAME = "WBIS";
	private static int arrivalTimeIndex = -1;

	private static final String DISTANCE_NAME = "WEGDIST";
	private static int distanceIndex = -1;

	private static final String DURATION_NAME = "WDAUER2";
	private static int durationIndex = -1;

	private static final String PURPOSE_NAME = "WZWECK2";
	private static int purposeIndex = -1;

	private static final String START_ORT_NAME = "W61201";
	private static int startOrtIndex = -1;

	private static final String START_STREET_NAME = "W61202";
	private static int startStreetIndex = -1;

	private static final String START_STREET_NR_NAME = "W61203";
	private static int startStreetNrIndex = -1;

	private static final String END_ORT_NAME = "W61601";
	private static int endOrtIndex = -1;

	private static final String END_STREET_NAME = "W61602";
	private static int endStreetIndex = -1;

	private static final String END_STREET_NR_NAME = "W61603";
	private static int endStreetNrIndex = -1;

	// /////////////////////////////////////////////////////////////////////////
	// attributes
	private final Id personId;
	private final Id wegId;
	private final double departureTime;
	private final double arrivalTime;
	private final double distance;
	private final double duration;
	private final MzAdress startAdress;
	private final MzAdress endAdress;
	private final Purpose purpose;

	// /////////////////////////////////////////////////////////////////////////
	// other fields
	private final Map<Id, MzEtappe> etappen = new HashMap<Id, MzEtappe>();

	public static void notifyStructure(final String headLine) {
		String[] names = headLine.split("\t");

		for (int i=0; i < names.length; i++) {
			if (names[ i ].equals( PERSON_NAME )) {
				personIndex = i;
			}
			if (names[ i ].equals( WEGNR_NAME )) {
				wegnrIndex = i;
			}
			if (names[ i ].equals( DEPARTURE_TIME_NAME )) {
				departureTimeIndex = i;
			}
			if (names[ i ].equals( ARRIVAL_TIME_NAME )) {
				arrivalTimeIndex = i;
			}
			if (names[ i ].equals( DISTANCE_NAME )) {
				distanceIndex = i;
			}
			if (names[ i ].equals( DURATION_NAME )) {
				durationIndex = i;
			}
			if (names[ i ].equals( PURPOSE_NAME )) {
				purposeIndex = i;
			}
			if (names[ i ].equals( START_ORT_NAME )) {
				startOrtIndex = i;
			}
			if (names[ i ].equals( START_STREET_NAME )) {
				startStreetIndex = i;
			}
			if (names[ i ].equals( START_STREET_NR_NAME )) {
				startStreetNrIndex = i;
			}
			if (names[ i ].equals( END_ORT_NAME )) {
				endOrtIndex = i;
			}
			if (names[ i ].equals( END_STREET_NAME )) {
				endStreetIndex = i;
			}
			if (names[ i ].equals( END_STREET_NR_NAME )) {
				endStreetNrIndex = i;
			}
		}

		structureIsKnown = true;
	}

	// /////////////////////////////////////////////////////////////////////////
	// constructor
	// /////////////////////////////////////////////////////////////////////////
	public MzWeg ( final String line ) {
		if (!structureIsKnown) throw new IllegalStateException( "structure of file unknown" );
		String[] lineArray = line.split( "\t" );

		this.personId = new IdImpl( lineArray[ personIndex ].trim() );
		this.wegId = new IdImpl( lineArray[ wegnrIndex ].trim() );
		this.departureTime = time( lineArray[ departureTimeIndex ] );
		this.arrivalTime = time( lineArray[ arrivalTimeIndex ] );
		this.distance = distance( lineArray[ distanceIndex ] );
		this.duration = time( lineArray[ durationIndex ] );
		this.startAdress = MzAdress.createAdress(
				lineArray[ startOrtIndex ],
				lineArray[ startStreetIndex ],
				lineArray[ startStreetNrIndex ] );
		this.endAdress = MzAdress.createAdress(
				lineArray[ endOrtIndex ],
				lineArray[ endStreetIndex ],
				lineArray[ endStreetNrIndex ] );
		this.purpose = purpose( lineArray[ purposeIndex ] );
	}

	private double time( final String value ) {
		// min -> secs
		return Double.parseDouble( value ) * 60d;
	}

	private double distance( final String value ) {
		// km -> m
		return Double.parseDouble( value ) * 1000d;
	}

	private Purpose purpose( final String value ) {
		int i = Integer.parseInt( value.trim() );

		switch ( i ) {
			case 0: // Umsteigen / Verkehrsmittelwechsel
				return Purpose.transitTransfer;
			case 1: // Arbeit
				return Purpose.work;
			case 2: // Ausbildung
				return Purpose.educ;
			case 3: // Einkauf / Besorgungen
				return Purpose.shop;
			case 4: // Geschäftliche Tätigkkeit
				return Purpose.commercialActivity;
			case 5: // Dienstfahrt
				return Purpose.useService;
			case 6: // Freizeit
				return Purpose.leisure;
			case 7: // Serviceweg
				return Purpose.servePassengerRide;
			case 8: // Begleitweg
				return Purpose.accompany;
			case 9: // keine Angabe
			default:
				return Purpose.unknown;
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// getters
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public Id getId() {
		return wegId;
	}

	public Id getPersonId() {
		return personId;
	}

	public MzAdress getDepartureAdress() {
		return startAdress;
	}

	public MzAdress getArrivalAdress() {
		return endAdress;
	}

	public Purpose getPurpose() {
		return purpose;
	}

	public double getDepartureTime() {
		return departureTime;
	}

	public double getArrivalTime() {
		return arrivalTime;
	}

	public double getDuration() {
		return duration;
	}

	public Leg getLeg() {
		LegImpl leg = new LegImpl( getMainMode() );

		leg.setDepartureTime( departureTime );
		// TODO: check consistency tt / arrival time
		leg.setArrivalTime( arrivalTime );
		leg.setTravelTime( duration );

		NetworkRoute route = new LinkNetworkRouteImpl(null, null);
		leg.setRoute(route);
		route.setDistance(distance);
		route.setTravelTime(leg.getTravelTime());

		return leg;
	}

	// ////////////////////// helpers for the getters //////////////////////////
	private String getMainMode() {
		double longestDistance = Double.NEGATIVE_INFINITY;
		String mode = null;

		// main mode is the mode of the longest etap
		for (MzEtappe etappe : etappen.values()) {
			if (etappe.getDistance() > longestDistance) {
				longestDistance = etappe.getDistance();
				mode = etappe.getMode();
			}
		}

		return mode;
	}

	// /////////////////////////////////////////////////////////////////////////
	// creation methods
	// /////////////////////////////////////////////////////////////////////////
	public void addEtappe(final MzEtappe etappe) {
		if (etappen.put( etappe.getId() , etappe ) != null) {
			throw new RuntimeException( "same etappe created twice" );
		}	
	}
}
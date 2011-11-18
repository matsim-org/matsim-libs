package playground.thibautd.initialdemandgeneration.activitychainsextractor;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;

class MzEtappe implements Identifiable {
	// /////////////////////////////////////////////////////////////////////////
	// static fields
	private static boolean structureIsKnown = false;

	private static final String PERSON_NAME = "INTNR";
	private static int personIndex = -1;

	private static final String WEGNR_NAME = "WEG";
	private static int wgnrIndex = -1;

	private static final String ETAPPENR_NAME = "ETAPPE";
	private static int etappenrIndex = -1;

	private static final String DISTANCE_NAME = "F61500";
	private static int distanceIndex = -1;

	private static final String MODE_NAME = "F61300";
	private static int modeIndex = -1;

	// /////////////////////////////////////////////////////////////////////////
	// attributes
	private final Id personId;
	private final Id wegId;
	private final Id id;
	private final double distance;
	private final String mode;

	public static void notifyStructure(final String headLine) {
		if (GlobalMzInformation.getMzYear() == 2000) {
			String[] names = headLine.split("\t");

			for (int i=0; i < names.length; i++) {
				if (names[ i ].equals( PERSON_NAME )) {
					personIndex = i;
				}
				if (names[ i ].equals( WEGNR_NAME )) {
					wgnrIndex = i;
				}
				if (names[ i ].equals( ETAPPENR_NAME )) {
					etappenrIndex = i;
				}
				if (names[ i ].equals( DISTANCE_NAME )) {
					distanceIndex = i;
				}
				if (names[ i ].equals( MODE_NAME )) {
					modeIndex = i;
				}
			}
		}

		structureIsKnown = true;
	}

	// /////////////////////////////////////////////////////////////////////////
	// constructor
	// /////////////////////////////////////////////////////////////////////////
	public MzEtappe(final String line) {
		if (GlobalMzInformation.getMzYear() == 2000) {
			if (!structureIsKnown) throw new IllegalStateException( "structure of file unknown" );
			String[] lineArray = line.split( "\t" );

			this.personId = new IdImpl( lineArray[ personIndex ].trim() );
			this.wegId = new IdImpl( lineArray[ wgnrIndex ].trim() );
			this.id = new IdImpl( lineArray[ etappenrIndex ].trim() );
			this.distance = distance( lineArray[ distanceIndex ].trim() );
			this.mode = mode( lineArray[ modeIndex ].trim() );
		}
		else if (GlobalMzInformation.getMzYear() == 1994) {
			this.personId = null;
			this.wegId = null;
			this.id = null;
			this.distance = 0;
			this.mode = null;
		}
		else {
			throw new IllegalStateException( "unhandled mz year "+GlobalMzInformation.getMzYear() );
		}
	}

	private String mode( final String value ) {
		int i = Integer.parseInt( value );

		switch (i) {
			case 1: // Zu Fuss
				return TransportMode.walk;
			case 2: // Velo
				return TransportMode.bike;
			case 6: // Auto als Fahrer
				return TransportMode.car;
			case 7: // Auto als Mitfahrer
				return TransportMode.ride;
			case 8: // Bahn
			case 9: // Postauto
			case 10: // Bus
			case 11: // Tram
			case 12: // Taxi
			case 13: // Reisecar
			case 14: // Lastwagen
			case 15: // Schiff
			case 16: // Flugzeug
			case 17: // Zahnradbahn, Seilbahn, Standseilbahn, Sessellift, Skilift
				return TransportMode.pt;
			case 23: // Kleinmotorrad
			case 3: // Mofa (Motorfahrrad)
			case 4: // Motorrad als Fahrer
			case 5: // Motorrad als Mitfahrer
			default: return "unknown";
		}
	}

	private double distance( final String value ) {
		// km -> m
		return Double.parseDouble( value ) * 1000d;
	}

	// /////////////////////////////////////////////////////////////////////////
	// getters
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public Id getId() {
		return id;
	}

	public String getMode() {
		return mode;
	}

	public double getDistance() {
		return distance;
	}

	public Id getPersonId() {
		return personId;
	}

	public Id getWegId() {
		return wegId;
	}
}

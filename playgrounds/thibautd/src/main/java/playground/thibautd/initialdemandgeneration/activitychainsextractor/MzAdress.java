package playground.thibautd.initialdemandgeneration.activitychainsextractor;

import org.apache.log4j.Logger;

class MzAdress {
	private static final Logger log =
		Logger.getLogger(MzAdress.class);

	private final String adress;

	private	static boolean warnXy = true;
	private static int nCreatedAdresses = 0;
	private static int nCreatedFullAdresses = 0;

	private MzAdress(
			final String ortCode,
			final String street,
			final String streetNr) {
		this( (street.toLowerCase().trim()+
				" "+streetNr.toLowerCase().trim()+
				" "+ortCode.trim()).trim() );

		nCreatedAdresses++;
		if (!adress.equals( ortCode.trim() )) nCreatedFullAdresses++;
	}

	private MzAdress(final String adress) {
		this.adress = adress;
	}

	public static void printStatistics() {
		log.info( "number of created adresses: "+nCreatedAdresses );
		log.info( "number of created full adresses: "+nCreatedFullAdresses );
		log.info( "=> proportion of adresses only consisting in ZIP Code: "+
				(100 * ( ((double) nCreatedAdresses - (double) nCreatedFullAdresses)
						 / (double) nCreatedAdresses ))+
				"%");
	}

	@Override
	public int hashCode() {
		return adress.hashCode();
	}

	@Override
	public boolean equals(final Object other) {
		return other instanceof MzAdress &&
			((MzAdress) other).adress.equals( adress );
	}

	static MzAdress createAdress(
			final String ortCode,
			final String street,
			final String streetNr) {
		return new MzAdress(ortCode , street , streetNr);
	}

	static MzAdress createAdress(
			final int x,
			final int y) {
		if ( warnXy ) {
			log.warn( "You are using X-Y coordinates as adresses. Double check the results, as it is not sure that the same place will always have the same X-Y pair associated..." );
			warnXy = false;
		}
		return new MzAdress( "("+x+";"+y+")" );
	}
}

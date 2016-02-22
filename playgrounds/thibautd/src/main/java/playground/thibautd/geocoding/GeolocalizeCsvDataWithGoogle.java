/* *********************************************************************** *
 * project: org.matsim.*
 * GeolocalizeCsvData.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.thibautd.geocoding;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import playground.ivt.utils.ArgParser;
import playground.ivt.utils.ArgParser.Args;
import playground.thibautd.utils.CsvUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author thibautd
 */
public class GeolocalizeCsvDataWithGoogle {
	public static final String STREET = "street";
	public static final String NUMBER = "number";
	public static final String ZIP_CODE = "zip";
	public static final String CITY = "municipality";
	public static final String COUNTRY = "country";
	public static final String STATUS = "geoloc_status";
	public static final String LONG = "longitude";
	public static final String LAT = "latitude";
	public static final String LOCTYPE = "google_loctype";
	public static final String GOOGLE_ADDRESS = "google_address";
	public static final String GOOGLE_STATUS = "google_status";

	public static final String LOC_ID = "locationID";

	private static final char SEP = ',';
	private static final char QUOTE = '"';

	public static void main(final String[] args) {
		final ArgParser argParser = new ArgParser();
		argParser.setDefaultValue( "-i" , null );
		argParser.setDefaultValue( "-o" , null );
		argParser.setDefaultValue( "-r" , null );
		argParser.setDefaultValue( "-k" , null );
		
		main( argParser.parseArgs( args ) );
	}

	private static void main(final Args argParser) {
		final String inFile = argParser.getValue( "-i" );
		final String outFile = argParser.getValue( "-o" );
		final String rejectFile = argParser.getValue( "-r" );
		final String key = argParser.getValue( "-k" );
		
		final Iterator<Address> addressProvider = new CsvParser( inFile );
		final CsvWriter successWriter = new CsvWriter( outFile );
		final CsvWriter rejectWriter = new CsvWriter( rejectFile );

		final GeolocalizingParser<GoogleAPIResult> parser =
			new GeolocalizingParser<GoogleAPIResult>(
					new GoogleGeolocalizer( key ) );

		parser.parse(
				addressProvider,
				new GeolocalizingParser.GeolocalizationListenner<GoogleAPIResult>() {
					@Override
					public void handleResult(
							final Address address,
							final GoogleAPIResult result) {
						successWriter.write( address , result , null );
					}
				},
				new GeolocalizingParser.NonlocalizedAddressListenner() {
					@Override
					public void handleNonlocalizedAddress(
							final Address address,
							final Status cause) {
						rejectWriter.write( address , null , cause );
					}
				} );

		successWriter.close();
		rejectWriter.close();
	}

	private static class CsvWriter {
		private final BufferedWriter writer;

		public CsvWriter(final String file) {
			this.writer = IOUtils.getBufferedWriter( file );
			final String firstLine =
				CsvUtils.buildCsvLine(
					SEP,
					QUOTE, 
					LOC_ID,
					STREET,
					NUMBER,
					ZIP_CODE,
					CITY,
					COUNTRY,
					STATUS,
					LONG,
					LAT,
					LOCTYPE,
					GOOGLE_ADDRESS,
					GOOGLE_STATUS );
			try {
				this.writer.write( firstLine );
			}
			catch (IOException e) {
				throw new UncheckedIOException( e );
			}
		}

		public void write(
				final Address address,
				final GoogleAPIResult result,
				final Status rejectCause ) {
			if ( result == null ) {
				write( address , null , null , rejectCause );
			}
			else {
				for ( int i=0; i < result.getNumberResults(); i++ ) {
					write( address , result.getGoogleStatus() , result.getResults( i ) , rejectCause );
				}
			}
		}

		private void write(
				final Address address,
				final GoogleAPIResult.GoogleStatus status,
				final GoogleAPIResult.Result result,
				final Status rejectCause ) {
			// Why do I always have to go so dirty when writing files?
			final String[] fields = new String[ 12 ];

			for ( int i = 0; i < fields.length; i++ ) fields[ i ] = "";

			if ( address != null ) {
				fields[ 0 ] = address.getId() != null ? address.getId() : "";
				fields[ 1 ] = address.getStreet() != null ? address.getStreet() : "";
				fields[ 2 ] = address.getNumber() != null ? address.getNumber() : "";
				fields[ 3 ] = address.getZipcode() != null ? address.getZipcode() : "";
				fields[ 4 ] = address.getMunicipality() != null ? address.getMunicipality() : "";
				fields[ 5 ] = address.getCountry() != null ? address.getCountry() : "";
			}

			if ( rejectCause != null ) {
				fields[ 6 ] = rejectCause.toString();
			}

			if ( result != null ) {
				fields[ 7 ] = result.getLongitude().toString();
				fields[ 8 ] = result.getLatitude().toString();
				fields[ 9 ] = result.getLocationType().toString();
				fields[ 10 ] = result.getFormattedAddress();
				fields[ 11 ] = status.toString();
			}

			final String line =
				CsvUtils.buildCsvLine(
					SEP,
					QUOTE, 
					fields );

			try {
				this.writer.newLine();
				this.writer.write( line );
			}
			catch (IOException e) {
				throw new UncheckedIOException( e );
			}
		}

		public void close() {
			try {
				writer.close();
			}
			catch (IOException e) {
				throw new UncheckedIOException( e );
			}
		}
	}

	private static class CsvParser implements Iterator<Address> {

		final BufferedReader reader;

		private String line = null;
		private int streetIndex = -1;
		private int numberIndex = -1;
		private int zipIndex = -1;
		private int municipalityIndex = -1;
		private int countryIndex = -1;
		private int idIndex = -1;
		
		public CsvParser(final String file) {
			this.reader = IOUtils.getBufferedReader( file );

			try {
				final String[] firstLine =  CsvUtils.parseCsvLine( SEP , QUOTE , reader.readLine() );

				for ( int i = 0; i < firstLine.length; i++ ) {
					if ( STREET.equals( firstLine[ i ] ) ) {
						if ( streetIndex >= 0 ) throw new RuntimeException();
						streetIndex = i;
					}
					if ( NUMBER.equals( firstLine[ i ] ) ) {
						if ( numberIndex >= 0 ) throw new RuntimeException();
						numberIndex = i;
					}
					if ( ZIP_CODE.equals( firstLine[ i ] ) ) {
						if ( zipIndex >= 0 ) throw new RuntimeException();
						zipIndex = i;
					}
					if ( CITY.equals( firstLine[ i ] ) ) {
						if ( municipalityIndex >= 0 ) throw new RuntimeException();
						municipalityIndex = i;
					}
					if ( COUNTRY.equals( firstLine[ i ] ) ) {
						if ( countryIndex >= 0 ) throw new RuntimeException();
						countryIndex = i;
					}
					if ( LOC_ID.equals( firstLine[ i ] ) ) {
						if ( idIndex >= 0 ) throw new RuntimeException();
						idIndex = i;
					}
				}
			}
			catch (IOException e) {
				throw new UncheckedIOException( e );
			}
		}

		@Override
		public boolean hasNext() {
			if ( line == null ) {
				try {
					line = reader.readLine();
				}
				catch (IOException e) {
					throw new UncheckedIOException( e );
				}
			}
			return line != null;
		}

		@Override
		public Address next() {
			try {
				if ( line == null ) line = reader.readLine();
			}
			catch (IOException e) {
				throw new UncheckedIOException( e );
			}
			if ( line == null ) throw new NoSuchElementException();

			final String[] fields = CsvUtils.parseCsvLine( SEP , QUOTE , line );

			line = null;

			final Address address = new Address();
			address.setId(
					readField(
						fields,
						idIndex ) );
			address.setStreet(
					readField(
						fields,
						streetIndex ) );
			address.setNumber(
					readField(
						fields,
						numberIndex ) );
			address.setZipcode(
					readField(
						fields,
						zipIndex ) );
			address.setMunicipality(
					readField(
						fields,
						municipalityIndex ) );
			address.setCountry(
					readField(
						fields,
						countryIndex ) );

			return address;
		}

		private static String readField(
				final String[] fields,
				final int index ) {
			try {
				return
					index == -1 ||
							fields[ index ].isEmpty() ||
					fields[ index ].equals( "NULL" ) ||
					fields[ index ].equals( "NA" ) ?
						null :
						fields[ index ].trim();
			}
			catch (ArrayIndexOutOfBoundsException e) {
				throw new RuntimeException( fields.length+" fields in "+Arrays.toString( fields ) , e );
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}


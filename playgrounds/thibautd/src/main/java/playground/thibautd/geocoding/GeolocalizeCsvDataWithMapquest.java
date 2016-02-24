/* *********************************************************************** *
 * project: org.matsim.*
 * GeolocalizeCsvDataWithMapquest.java
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

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import playground.ivt.utils.ArgParser;
import playground.thibautd.geocoding.MapquestResult.Result;
import playground.thibautd.utils.CsvUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * @author thibautd
 */
public class GeolocalizeCsvDataWithMapquest {
	public static final String STREET = "street";
	public static final String NUMBER = "number";
	public static final String ZIP_CODE = "zip";
	public static final String CITY = "municipality";
	public static final String COUNTRY = "country";
	public static final String STATUS = "geoloc_status";
	public static final String LONG = "longitude_wgs84";
	public static final String LAT = "latitude_wgs84";
	public static final String X = "easting_CH03";
	public static final String Y = "northing_CH03";
	public static final String QUALITY = "geocode_quality";
	public static final String QUALITY_CODE = "geocode_quality_code";
	public static final String MAPQUEST_STREET = "mapquest_street";
	public static final String MAPQUEST_ZIP_CODE = "mapquest_zip";
	public static final String MAPQUEST_CITY = "mapquest_municipality";
	public static final String MAPQUEST_COUNTRY = "mapquest_country";
	public static final String MAPQUEST_STATUS = "mapquest_status";

	public static final String LOC_ID = "locationID";

	private static final char SEP = ',';
	private static final char QUOTE = '"';

	public static void main(final String[] args) {
		final ArgParser parser = new ArgParser();
		
		parser.setDefaultValue( "-i" , null );
		parser.setDefaultValue( "-o" , null );
		parser.setDefaultValue( "-r" , null );
		parser.setDefaultValue( "-k" , null );
		
		main( parser.parseArgs( args ) );
	}

	private static void main(final ArgParser.Args args) {
		final String inFile = args.getValue( "-i" );
		final String outFile = args.getValue( "-o" );
		final String rejectFile = args.getValue( "-r" );
		final String key = args.getValue( "-k" );
		
		final Iterator<Address> addressProvider = new CsvParser( inFile );
		final CsvWriter successWriter = new CsvWriter( outFile );
		final CsvWriter rejectWriter = new CsvWriter( rejectFile );

		final GeolocalizingParser<MapquestResult> parser =
			new GeolocalizingParser<MapquestResult>(
					new MapquestGeolocalizer( key ) );

		parser.parse(
				addressProvider,
				new GeolocalizingParser.GeolocalizationListenner<MapquestResult>() {
					@Override
					public void handleResult(
							final Address address,
							final MapquestResult result) {
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
					X,
					Y,
					QUALITY,
					QUALITY_CODE,
					MAPQUEST_STREET,
					MAPQUEST_ZIP_CODE,
					MAPQUEST_CITY,
					MAPQUEST_COUNTRY,
					MAPQUEST_STATUS );
			try {
				this.writer.write( firstLine );
			}
			catch (IOException e) {
				throw new UncheckedIOException( e );
			}
		}

		public void write(
				final Address address,
				final MapquestResult result,
				final Status rejectCause ) {
			if ( result == null ) {
				write( address , null , null , rejectCause );
			}
			else {
				for ( MapquestResult.Result r : filterResults( address , result ) ) {
					write( address , result.getMapquestStatus() , r , rejectCause );
				}
			}
		}

		private void write(
				final Address address,
				final MapquestResult.MapquestStatus status,
				final MapquestResult.Result result,
				final Status rejectCause ) {
			// Why do I always have to go so dirty when writing files?
			final String[] fields = new String[ 18 ];

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

				final Coord wgsCoord = new Coord(result.getLongitude(), result.getLatitude());
				final Coord chCoord = new WGS84toCH1903LV03().transform( wgsCoord );
				fields[ 9 ] = ""+chCoord.getX();
				fields[ 10 ] = ""+chCoord.getY();
				fields[ 11 ] = result.getGeocodeQuality().toString();
				fields[ 12 ] = result.getGeocodeQualityCode();
				fields[ 13 ] = result.getStreet();
				fields[ 14 ] = result.getZip();
				fields[ 15 ] = result.getCity();
				fields[ 16 ] = result.getCountry();
				fields[ 17 ] = status.toString();
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

	private static List<MapquestResult.Result> filterResults(
			final Address address,
			final MapquestResult results ) {
		final List<MapquestResult.Result> filtered = new ArrayList<MapquestResult.Result>();
		for ( int i=0; i < results.getNumberResults(); i++ ) {
			final MapquestResult.Result result = results.getResults( i );
			filtered.add( result );
		}

		filterBadAddresses( address , filtered );
		onlyKeepCenterOfStreetsOrAddresses( filtered );

		return filtered;
	}

	private static void onlyKeepCenterOfStreetsOrAddresses(
			final List<Result> filtered) {
		final Map<String, List<Result>> resultsPerStreet = new HashMap<String, List<Result>>();
		for ( Result result : filtered ) {
			switch ( result.getGeocodeQuality() ) {
				// sometimes several results for one given address... Just keep one.
				case ADDRESS:
				case CITY:
				case COUNTRY:
				case STATE:
				// when no street number is available, one gets matches all along the street.
				case STREET:
					final String street = result.getFormattedAddress();
					final List<Result> inStreet =
						MapUtils.getList(
								street,
								resultsPerStreet );
					inStreet.add( result );
					break;
				case UNKNOWN:
				case INTERSECTION:
				case COUNTY:
				case POINT:
				case ZIP:
				case ZIP_EXTENDED:
				default:
					break;
			}
		}

		for ( List<Result> inStreet : resultsPerStreet.values() ) {
			final Coord coord = getCenter( inStreet );
			final Result toKeep = getClosest( coord , inStreet );
			inStreet.remove( toKeep );
			filtered.removeAll( inStreet );
		}
	}

	private static Result getClosest(
			final Coord center,
			final List<Result> inStreet) {
		double min = Double.POSITIVE_INFINITY;
		Result closest = null;

		for ( Result r : inStreet ) {
			final Coord c = r.getCH03Coord();
			final double dist = CoordUtils.calcEuclideanDistance( c , center );
			if ( dist < min ) {
				min = dist;
				closest = r;
			}
		}

		return closest;
	}

	private static Coord getCenter(final List<Result> inStreet) {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		for ( Result r : inStreet ) {
			final Coord c = r.getCH03Coord();
			minX = Math.min( minX , c.getX() );
			minY = Math.min( minY , c.getY() );
			maxX = Math.max( maxX , c.getX() );
			maxY = Math.max( maxY , c.getY() );
		}

		return new Coord((minX + maxX) / 2, (minY + maxY) / 2);
	}

	private static void filterBadAddresses(
			final Address address,
			final List<MapquestResult.Result> filtered) {
		boolean wasZipMatch = false;
		boolean wasCityMatch = false;
		boolean wasStreetMatch = false;

		for ( MapquestResult.Result result : filtered ) {

			if ( address.getZipcode() != null && address.getZipcode().equals( result.getZip() ) ) {
				wasZipMatch = true;
			}
			if ( address.getMunicipality() != null && address.getMunicipality().equals( result.getCity() ) ) {
				wasCityMatch = true;
			}
			if ( address.getStreet() != null && result.getStreet().matches( ".*"+address.getStreet() ) ) {
				wasStreetMatch = true;
			}
		}

		final Iterator<MapquestResult.Result> it = filtered.iterator();
		while ( it.hasNext() ) {
			final MapquestResult.Result result = it.next();

			final boolean zip = wasZipMatch && !address.getZipcode().equals( result.getZip() );
			final boolean city = wasCityMatch && !address.getMunicipality().equals( result.getCity() );
			final boolean street = wasStreetMatch && !result.getStreet().matches( ".*"+address.getStreet() );

			if ( zip || city || street ) it.remove();
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
			final String country =
				readField(
					fields,
					countryIndex );
			address.setCountry( country != null ? country : "CH" );

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
					fields[ index ].equals( "NA" )?
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


/* *********************************************************************** *
 * project: org.matsim.*
 * SampleCpMatches.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.scripts;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author thibautd
 */
public class SampleCpMatches {
	private static final Logger log =
		Logger.getLogger(SampleCpMatches.class);

	public static void main(final String[] args) {
		Matcher matcher = new Matcher( args[0] );
		String outPrefix = args[1];
		int nDraws = Integer.parseInt( args[2] );
		long firstSeed = 1988;
		if (args.length > 3) {
			firstSeed = Long.parseLong( args[3] );
		}

		Random seedSource = new Random( firstSeed );
		for (int i=0; i < nDraws; i++) {
			long seed = seedSource.nextLong();
			matcher.run(
					outPrefix + "." + seed + ".dat.gz",
					seed);
		}
	}

	private static class Matcher {
		private final Map<String, Set<String>> driversPerPassenger = new TreeMap<String, Set<String>>();

		public Matcher(final String passengerRecords) {
			BufferedReader reader = IOUtils.getBufferedReader( passengerRecords );
			try {
				Counter count = new Counter( "reading line # " );
				reader.readLine(); // skip title
				for( String line = reader.readLine();
						line != null;
						line = reader.readLine() ) {
					count.incCounter();
					String[] arr = line.split( "[ \t]+" );

					String d = arr[0];//.intern();
					String p = arr[1];//.intern();

					getEntry( driversPerPassenger , p ).add( d );
				}
				count.printCounter();
			}
			catch (IOException e) {
				throw new UncheckedIOException( e );
			}
			finally {
				try {
					reader.close();
				} catch (IOException e) {
					throw new UncheckedIOException( e );
				}
			}
		}

		private Set<String> getEntry(
				final Map<String, Set<String>> map,
				final String key) {
			Set<String> e = map.get( key );

			if (e == null) {
				e = new TreeSet<String>();
				map.put( key , e );
			}

			return e;
		}

		public void run( final String outfile , final long seed ) {
			Random rand = new Random( seed );
			List<String> passengers = new ArrayList<String>(driversPerPassenger.keySet());
			Collections.shuffle( passengers , rand );
			Set<String> matched = new TreeSet<String>();

			BufferedWriter writer = IOUtils.getBufferedWriter( outfile );
			log.info( "writing file "+outfile );
			log.info( "using seed "+seed );
			Counter count = new Counter( "match # " );
			try {
				writer.write( "driverRecordId\tpassengerRecordId" );
				for (String passenger : passengers) {
					if ( matched.contains( passenger ) ) continue;

					Set<String> drivers = new TreeSet<String>(driversPerPassenger.get( passenger ));
					drivers.removeAll( matched );
					int size = drivers.size();

					if (size == 0) continue;

					count.incCounter();
					int c = rand.nextInt( size );
					String driver = drivers.toArray( new String[0] )[c];

					// match
					writer.newLine();
					writer.write( driver + "\t" + passenger );
					matched.add( driver );
					matched.add( passenger );
				}
				count.printCounter();
			}
			catch (IOException ex) {
				throw new UncheckedIOException( ex );
			}
			finally {
				try {
					writer.close();
				}
				catch (IOException e) {
					throw new UncheckedIOException( e );
				}
			}
		}
	}
}


/* *********************************************************************** *
 * project: org.matsim.*
 * BenchmarkRandomSamplingMethods.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import playground.thibautd.utils.MoreIOUtils;

/**
 * @author thibautd
 */
public class BenchmarkRandomSamplingMethods {
	private static final int N_DRAWS = 1_000_000;
	private static final Random random = new Random();

	public static void main(final String[] args) {
		final String outdir = args[ 0 ];

		MoreIOUtils.createDirectory( outdir );

		final double[] numbersUniqueDraw = new double[ N_DRAWS ];
		final double[] numbersForLoop =  new double[ N_DRAWS ];
		final double[] numbersByteBuffer = new double[ N_DRAWS ];
		final double[] numbersByteThrown = new double[ N_DRAWS ];
		final double[] numbersLongLoop = new double[ N_DRAWS ];

		final long startUnique = System.currentTimeMillis();
		sampleUniqueDraw( numbersUniqueDraw );
		final long startFor = System.currentTimeMillis();
		sampleForLoop( numbersForLoop );
		final long startBuffer = System.currentTimeMillis();
		sampleByteBuffer( numbersByteBuffer );
		final long startByte = System.currentTimeMillis();
		sampleBytes( numbersByteThrown );
		final long startLong = System.currentTimeMillis();
		sampleLongForLoop( numbersLongLoop );
		final long end = System.currentTimeMillis();

		try ( BufferedWriter writer = IOUtils.getBufferedWriter( outdir+"/times.txt" ) ) {
			writer.write( "unique draw ms:\t"+(startFor - startUnique) );
			writer.newLine();
			writer.write( "5 double draws ms:\t"+(startBuffer - startFor) );
			writer.newLine();
			writer.write( "buffered draws ms:\t"+(startByte - startBuffer) );
			writer.newLine();
			writer.write( "thrown byte draws ms:\t"+(startLong - startByte) );
			writer.newLine();
			writer.write( "thrown long draw ms:\t"+(end - startLong) );
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}

		try ( BufferedWriter writer = IOUtils.getBufferedWriter( outdir+"/draws.dat" ) ) {
			writer.write( "seed\tuniqueDraw\tforDoubleDraw\tthrownByteDraw\tforLongDraw" );
			for ( int i=0; i < N_DRAWS; i++ ) {
				writer.newLine();
				writer.write( i+"\t"+numbersUniqueDraw[ i ] );
				writer.write( "\t"+numbersForLoop[ i ] );
				writer.write( "\t"+numbersByteThrown[ i ] );
				writer.write( "\t"+numbersLongLoop[ i ] );
			}
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	private static void sampleUniqueDraw( final double[] draws ) {
		for ( int i=0; i < N_DRAWS; i++ ) {
			random.setSeed( i );
			draws[ i ] = random.nextDouble();
		}
	}

	private static void sampleForLoop( final double[] draws ) {
		for ( int i=0; i < N_DRAWS; i++ ) {
			random.setSeed( i );
			for ( int j=0; j < 5; j++ ) random.nextDouble();
			draws[ i ] = random.nextDouble();
		}
	}

	// long implies less operations per draw
	private static void sampleLongForLoop( final double[] draws ) {
		for ( int i=0; i < N_DRAWS; i++ ) {
			random.setSeed( i );
			for ( int j=0; j < 5; j++ ) random.nextLong();
			draws[ i ] = random.nextDouble();
		}
	}

	private static final ByteBuffer buffer = ByteBuffer.allocate( 48 );
	private static final byte[] bytes = new byte[ 48 ];
	private static void sampleByteBuffer( final double[] draws ) {
		for ( int i=0; i < N_DRAWS; i++ ) {
			random.setSeed( i );
			random.nextBytes( bytes );
			buffer.put( bytes );
			buffer.flip();
			draws[ i ] = buffer.getDouble( 40 );
			buffer.clear();
		}
	}

	private static final byte[] bytesToThrowAway = new byte[ 40 ];
	private static void sampleBytes( final double[] draws ) {
		for ( int i=0; i < N_DRAWS; i++ ) {
			random.setSeed( i );
			random.nextBytes( bytesToThrowAway );
			draws[ i ] = random.nextDouble();
		}
	}
}


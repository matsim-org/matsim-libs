/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.framework;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

/**
 * Not that nice, but best way I found to be able to pass csv writers as listenner without maintaining each one separately
 * @author thibautd
 */
public class AutocloserModule extends AbstractModule implements AutoCloseable {
	private final List<AutoCloseable> closeables = new ArrayList<>();

	@Override
	protected void configure() {
		bind( Closer.class ).toInstance( new Closer( closeables ) );
	}

	@Override
	public void close() throws Exception {
		for ( AutoCloseable c : closeables ) {
			c.close();
		}
	}

	@Singleton
	public static class Closer {
		private final List<AutoCloseable> closeables;

		public Closer( final List<AutoCloseable> closeables ) {
			this.closeables = closeables;
		}

		public void add( final AutoCloseable closeable ) {
			closeables.add( closeable );
		}
	}
}

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
package playground.ivt.maxess.nestedlogitaccessibility.framework;

import java.util.ArrayList;
import java.util.List;

/**
 * @author thibautd
 */
public class Nest<N extends Enum<N>> {
	private final N name;
	private final double mu_n;
	// if need exists, could easily be made generic (with alternatives type as a class parameter)
	private final List<Alternative<N>> alternatives;

	public static class Builder<N extends Enum<N>> {
		private N name = null;
		private double mu_n = 1;
		private final List<Alternative<N>> alternatives = new ArrayList<>( );

		public Builder<N> setName( N name ) {
			this.name = name;
			return this;
		}

		public Builder<N> setMu( final double mu ) {
			this.mu_n = mu;
			return this;
		}

		public Builder<N> addAlternative( final Alternative<N> a ) {
			alternatives.add( a );
			return this;
		}

		public Builder<N> addAlternatives( final Iterable<Alternative<N>> as ) {
			for ( Alternative<N> a : as ) {
				alternatives.add( a );
			}
			return this;
		}

		public Nest<N> build() {
			return new Nest<N>( name , mu_n , alternatives );
		}
	}

	public Nest( N name, double mu_n, List<Alternative<N>> alternatives ) {
		this.name = name;
		this.mu_n = mu_n;
		this.alternatives = alternatives;
	}

	public N getNestId() {
		return name;
	}

	public List<Alternative<N>> getAlternatives() {
		return alternatives;
	}

	public double getMu_n() {
		return mu_n;
	}
}

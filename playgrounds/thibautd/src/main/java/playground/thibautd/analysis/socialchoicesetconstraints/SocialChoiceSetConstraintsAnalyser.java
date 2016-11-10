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
package playground.thibautd.analysis.socialchoicesetconstraints;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.thibautd.utils.spatialcollections.VPTree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author thibautd
 */
public class SocialChoiceSetConstraintsAnalyser {
	private final VPTree<Coord,ActivityFacility> facilities;
	private final Utils.AllCliques cliques;
	private final SocialChoiceSetConstraintsConfigGroup configGroup;
	private final Population population;

	public SocialChoiceSetConstraintsAnalyser(
			final SocialChoiceSetConstraintsConfigGroup configGroup,
			final ActivityFacilities facilities, final Population population ) {
		this.configGroup = configGroup;
		this.facilities = Utils.createTree( facilities );
		this.cliques = Utils.readMaximalCliques( configGroup.getInputCliquesCsvFile() );
		this.population = population;
	}

	public void analyze( final Consumer<IndividualResultRecord> callback ) {
		for ( int size = 1; size < cliques.getMaxSize(); size++ ) {
			final Collection<Set<Id<Person>>> cliquesOfSize = cliques.getCliquesOfSize( size );

			for ( Set<Id<Person>> clique : cliquesOfSize ) {
				final Collection<FacilitiesPerDistance> fpd = new ArrayList<>();
				for ( double distance : configGroup.getDistances() ) {
					final int sizeOfIntersection =
							facilities.getBallsIntersection(
									clique.stream()
											.map( id -> population.getPersons().get( id ) )
											.map( SocialChoiceSetConstraintsAnalyser::calcCoord )
											.collect( Collectors.toList() ),
									distance,
									v -> true ).size();
					fpd.add( new FacilitiesPerDistance( sizeOfIntersection , distance ) );
				}
				callback.accept( new IndividualResultRecord( size , fpd ) );
			}
		}
	}

	private static Coord calcCoord( final Person p ) {
		return p.getSelectedPlan().getPlanElements().stream()
				.filter( pe -> pe instanceof Activity )
				.map( pe -> (Activity) pe )
				.findFirst()
				.get()
				.getCoord();
	}

	public static class IndividualResultRecord {
		public final int cliqueSize;
		public final Collection<FacilitiesPerDistance> nFacilitiesPerDistance;

		private IndividualResultRecord(
				final int cliqueSize,
				final Collection<FacilitiesPerDistance> nFacilitiesPerDistance ) {
			this.cliqueSize = cliqueSize;
			this.nFacilitiesPerDistance = nFacilitiesPerDistance;
		}
	}

	public static class FacilitiesPerDistance {
		public final int nFacilities;
		public final double distance;

		private FacilitiesPerDistance( final int nFacilities, final double distance ) {
			this.nFacilities = nFacilities;
			this.distance = distance;
		}
	}
}

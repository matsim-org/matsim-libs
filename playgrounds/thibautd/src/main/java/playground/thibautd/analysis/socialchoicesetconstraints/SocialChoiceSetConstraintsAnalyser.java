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

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.thibautd.utils.CsvUtils;
import playground.thibautd.utils.CsvWriter;
import playground.thibautd.utils.spatialcollections.VPTree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author thibautd
 */
public class SocialChoiceSetConstraintsAnalyser {
	private static final Logger log = Logger.getLogger( SocialChoiceSetConstraintsAnalyser.class );

	private final Random random = new Random( 123 );

	private final VPTree<Coord,ActivityFacility> facilities;
	private final Utils.AllCliques cliques;
	private final SocialChoiceSetConstraintsConfigGroup configGroup;
	private final Population population;

	@Inject
	public SocialChoiceSetConstraintsAnalyser(
			final SocialChoiceSetConstraintsConfigGroup configGroup,
			final ActivityFacilities facilities,
			final Population population ) {
		this.configGroup = configGroup;
		this.facilities = Utils.createTree( facilities );
		this.cliques = Utils.readMaximalCliques( configGroup.getInputCliquesCsvFile() );
		this.population = population;
	}

	public SocialChoiceSetConstraintsAnalyser( final Scenario scenario ) {
		this( (SocialChoiceSetConstraintsConfigGroup) scenario.getConfig().getModule( SocialChoiceSetConstraintsConfigGroup.GROUP_NAME ),
				scenario.getActivityFacilities(),
				scenario.getPopulation() );
	}

	public void analyzeToFile( final String path ) {
		log.info( "analyse social constraints on choice set to file "+path );
		try ( final CsvWriter writer =
					  new CsvWriter(
					  		'\t' , '"' ,
							  new CsvUtils.TitleLine( "cliqueSize" , "distance" , "nFacilities" ) ,
							  path ) ) {
			analyze( r -> {
				for ( FacilitiesPerDistance fs : r.nFacilitiesPerDistance ) {
					writer.nextLine();
					writer.setField( "cliqueSize" , ""+r.cliqueSize );
					writer.setField( "distance" , ""+fs.distance );
					writer.setField( "nFacilities" , ""+fs.nFacilities );
				}
			} );
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	public void analyze( final Consumer<IndividualResultRecord> callback ) {
		log.info( "analyse for clique sizes in [1;"+cliques.getMaxSize()+"]" );
		for ( int size = 1; size < cliques.getMaxSize(); size++ ) {
			log.info( "look at size "+size+"..." );

			final Collection<Set<Id<Person>>> cliquesOfSize =
					cliques.getNCliquesOfSize(
							random,
							configGroup.getSampleSize(),
							size );

			log.info( "look at size "+size+": "+cliquesOfSize.size()+" cliques" );

			final Counter counter =
					new Counter(
							"analyse clique size "+size+" / "+cliques.getMaxSize()+": clique # ",
							" / "+cliquesOfSize.size() );
			for ( Set<Id<Person>> clique : cliquesOfSize ) {
				counter.incCounter();
				final List<Coord> coords = clique.stream()
						.map( id -> population.getPersons().get( id ) )
						.map( SocialChoiceSetConstraintsAnalyser::calcCoord )
						.collect( Collectors.toList() );

				final VPTree<Coord,ActivityFacility> tree =
						facilities.getSubtreeContainingBalls(
								coords,
								configGroup.getMaxDistanceKm() * 1000 );

				final Collection<FacilitiesPerDistance> fpd = new ArrayList<>( configGroup.getNDistances() );
				boolean intersectionEmpty = false;
				for ( double distance : configGroup.getDecreasingDistances_m() ) {
					final int sizeOfIntersection =
							intersectionEmpty ? 0 :
							tree.getBallsIntersection(
									coords,
									distance,
									v -> true ).size();
					fpd.add( new FacilitiesPerDistance( sizeOfIntersection , distance ) );
					if ( sizeOfIntersection == 0 ) intersectionEmpty = true;
				}
				callback.accept( new IndividualResultRecord( size , fpd ) );
			}
			counter.printCounter();
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

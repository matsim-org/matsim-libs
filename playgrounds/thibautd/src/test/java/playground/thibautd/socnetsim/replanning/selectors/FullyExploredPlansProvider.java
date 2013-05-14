/* *********************************************************************** *
 * project: org.matsim.*
 * FullyExploredPlansProvider.java
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
package playground.thibautd.socnetsim.replanning.selectors;

import java.io.File;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

import org.apache.log4j.Logger;

import org.junit.runners.model.FrameworkMethod;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Counter;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.socnetsim.cliques.population.CliquesWriter;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.population.JointPlansXmlReader;
import playground.thibautd.socnetsim.population.JointPlansXmlWriter;
import playground.thibautd.socnetsim.replanning.grouping.FixedGroupsIdentifierFileParser;
import playground.thibautd.socnetsim.replanning.grouping.GroupIdentifier;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.selectors.highestweightselection.FullExplorationSelector;

/**
 * @author thibautd
 */
public class FullyExploredPlansProvider {
	private static final Logger log =
		Logger.getLogger(FullyExploredPlansProvider.class);

	// XXX: this is hideous, but better than other solutions I could imagine...
	// I want:
	// - to use the standard way of getting the input test path provided by MatsimTestUtils
	// - outside of a test.
	private static final String CACHE_DIRECTORY;
	static {
		final MatsimTestUtils utils = new MatsimTestUtils();
		utils.starting( new FrameworkMethod( FullyExploredPlansProvider.class.getMethods()[0] ) );
		CACHE_DIRECTORY = utils.getClassInputDirectory();
	}

	private FullyExploredPlansProvider() {};

	public static SelectedInformation getGroupsAndSelected(
			final IncompatiblePlansIdentifierFactory incompFactory,
			final boolean isBlocking) {
		final SelectedInformation fromFile = readGroupsAndSelected( incompFactory , isBlocking );
		if ( fromFile != null ) {
			log.info( "plans succesfully read" );
			return fromFile;
		}

		log.info( "plans reading FAILED" );
		return createGroupsAndSelected( incompFactory , isBlocking );
	}

	private static SelectedInformation readGroupsAndSelected(
			final IncompatiblePlansIdentifierFactory incompFactory,
			final boolean isBlocking) {
		log.info( "attempt to read test plans" );
		final StoragePaths paths = getStoragePaths( incompFactory , isBlocking );

		if ( !paths.allPathsExist() ) return null;

		log.info( "read cliques from "+paths.cliquesFilePath );
		final GroupIdentifier cliques = FixedGroupsIdentifierFileParser.readCliquesFile( paths.cliquesFilePath );

		log.info( "read plans from "+paths.plansFilePath );
		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimPopulationReader( scenario ).readFile( paths.plansFilePath );

		log.info( "read joint plans from "+paths.jointPlansFilePath );
		final JointPlans jointPlans = JointPlansXmlReader.readJointPlans( scenario.getPopulation() , paths.jointPlansFilePath );

		final SelectedInformation information = new SelectedInformation( jointPlans );
		for ( ReplanningGroup clique : cliques.identifyGroups( scenario.getPopulation() ) ) {
			final List<JointPlan> selectedJointPlans = new ArrayList<JointPlan>();
			final List<Plan> selectedPlans = new ArrayList<Plan>();

			for ( Person person : clique.getPersons() ) {
				final Plan selectedPlan = person.getSelectedPlan();
				// this is used as a "flag" for no plan selected,
				// as PersonImpl is too sentimental to have no plan
				// selected.
				if ( selectedPlan.getScore() == null ) {
					person.getPlans().remove( selectedPlan );
					continue;
				}
				final JointPlan jp = jointPlans.getJointPlan( selectedPlan );
				assert jp == null || consistentSelectionStatus( jp ) : jp;

				if ( jp != null && !selectedJointPlans.contains( jp ) ) {
					selectedJointPlans.add( jp );
				}

				if ( jp == null ) {
					selectedPlans.add( selectedPlan );
				}
			}

			final GroupPlans gps =
					new GroupPlans(
						selectedJointPlans,
						selectedPlans );
			final int nPlans = gps.getAllIndividualPlans().size();

			assert nPlans == 0 || nPlans == clique.getPersons().size() : nPlans +" != "+ clique.getPersons().size();

			information.addInformation(
					clique,
					nPlans == 0 ? null : gps);
		}

		return information;
	}

	private static boolean consistentSelectionStatus(final Iterable<JointPlan> jps) {
		for ( JointPlan jp : jps ) {
			if ( !consistentSelectionStatus( jp ) ) return false;
		}
		return true;
	}

	private static boolean consistentSelectionStatus(final JointPlan jp) {
		boolean foundSelected = false;
		boolean foundNonSelected = false;

		for ( Plan p : jp.getIndividualPlans().values() ) {
			if ( p.isSelected() ) foundSelected = true;
			else foundNonSelected = true;
		}
		return !(foundSelected && foundNonSelected);
	}

	private static StoragePaths getStoragePaths(
			final IncompatiblePlansIdentifierFactory incompFactory,
			final boolean isBlocking) {
		final String baseDir = CACHE_DIRECTORY +"/"+ incompFactory.getClass().getName() +"/"+ isBlocking+"/";
		return new StoragePaths( baseDir );
	}

	private static SelectedInformation createGroupsAndSelected(
			final IncompatiblePlansIdentifierFactory incompFactory,
			final boolean isBlocking) {
		log.info( "creating test plans" );
		final SelectedInformation selecteds = new SelectedInformation();
		final GroupLevelPlanSelector fullSelector =
			new FullExplorationSelector(
					isBlocking,
					incompFactory,
					new FullExplorationSelector.WeightCalculator() {
						@Override
						public double getWeight(
								final Plan indivPlan,
								final ReplanningGroup replanningGroup) {
							final Double score = indivPlan.getScore();
							return score == null ? Double.POSITIVE_INFINITY : score;
						}
					});

		final int nTries = 20000;
		log.info( nTries+" random test plans" );
		final Counter counter = new Counter( "Create test instance # " );
		final Random random = new Random( 1234 );
		final Iterator<Id> ids =
				new Iterator<Id> () {
						@Override
						public boolean hasNext() {
							return true;
						}

						int currentId=0;
						@Override
						public Id next() {
							return new IdImpl( currentId++ );
						}

						@Override
						public void remove() {
							throw new UnsupportedOperationException();
						}
				};
		for ( int i=0; i < nTries; i++ ) {
			counter.incCounter();
			final ReplanningGroup clique = createNextTestClique(
					ids,
					selecteds.getJointPlans(),
					random );
			final GroupPlans fullResult = fullSelector.selectPlans(
						selecteds.getJointPlans(),
						clique );
			selecteds.addInformation(
						clique,
						fullResult );
		}
		counter.printCounter();

		return selecteds;
	}

	private static ReplanningGroup createNextTestClique(
			final Iterator<Id> idsIterator,
			final JointPlans jointPlans,
			final Random random) {
		// attempt to get a high diversity of joint structures.
		final int nMembers = 1 + random.nextInt( 20 );
		final int nPlans = 1 + random.nextInt( 10 );
		// this is the max number of attempts to create a joint plan
		final int maxJointPlans = 1000;
		final double pJoin = random.nextDouble();
		final ReplanningGroup group = new ReplanningGroup();
		final PopulationFactory factory = ScenarioUtils.createScenario( ConfigUtils.createConfig() ).getPopulation().getFactory();

		final Map<Id, Queue<Plan>> plansPerPerson = new LinkedHashMap<Id, Queue<Plan>>();

		// create plans
		for (int j=0; j < nMembers; j++) {
			final Id id = idsIterator.next();
			final Person person = factory.createPerson( id );
			group.addPerson( person );
			for (int k=0; k < nPlans; k++) {
				final Plan plan = factory.createPlan();
				plan.setPerson( person );
				person.addPlan( plan );
				plan.setScore( random.nextDouble() * 1000 );
			}
			plansPerPerson.put( id , new LinkedList<Plan>( person.getPlans() ) );
		}

		// join plans randomly
		final int nJointPlans = random.nextInt( maxJointPlans );
		for (int p=0; p < nJointPlans; p++) {
			final Map<Id, Plan> jointPlan = new LinkedHashMap<Id, Plan>();
			for (Queue<Plan> plans : plansPerPerson.values()) {
				if ( random.nextDouble() > pJoin ) continue;
				final Plan plan = plans.poll();
				if (plan != null) jointPlan.put( plan.getPerson().getId() , plan );
			}
			if (jointPlan.size() <= 1) continue;
			jointPlans.addJointPlan( jointPlans.getFactory().createJointPlan( jointPlan ) );
		}

		return group;
	}

	public static class SelectedInformation {
		private final JointPlans jointPlans;
		private final ArrayList<Tuple<ReplanningGroup, GroupPlans>> plans =
			new ArrayList<Tuple<ReplanningGroup, GroupPlans>>();

		public SelectedInformation() {
			this( new JointPlans() );
		}

		public SelectedInformation(final JointPlans jointPlans) {
			this.jointPlans = jointPlans;
		}

		public JointPlans getJointPlans() {
			return jointPlans;
		}

		public Iterable<Tuple<ReplanningGroup, GroupPlans>> getGroupInfos() {
			return plans;
		}

		private void addInformation(
				final ReplanningGroup group,
				final GroupPlans selected) {
			plans.add( new Tuple<ReplanningGroup, GroupPlans>( group , selected ) );
		}
	}

	/**
	 * to generate test data.
	 * mvn -e exec:java -Dexec.mainClass="playground.thibautd.socnetsim.replanning.selectors.FullyExploredPlansProvider" -Dexec.classpathScope="test"
	 */
	public static void main(final String[] args) {
		generateInputData(
				new EmptyIncompatiblePlansIdentifierFactory(),
				true );

		generateInputData(
				new EmptyIncompatiblePlansIdentifierFactory(),
				false );

		generateInputData(
				new FewGroupsIncompatibilityFactory(),
				true );

		generateInputData(
				new FewGroupsIncompatibilityFactory(),
				false );
	}

	private static void generateInputData(
			final IncompatiblePlansIdentifierFactory factory,
			final boolean blocking) {
		final SelectedInformation toDump = createGroupsAndSelected( factory , blocking );
		final StoragePaths paths = getStoragePaths( factory , blocking );
		new File( paths.directoryPath ).mkdirs();
		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );

		int id = 0;
		final CliquesWriter cliquesWriter = new CliquesWriter();
		cliquesWriter.openAndStartFile( paths.cliquesFilePath );
		for ( Tuple<ReplanningGroup, GroupPlans> info : toDump.getGroupInfos() ) {
			cliquesWriter.writeClique(
					new IdImpl( id++ ),
					info.getFirst().getPersons() );

			for ( Person p : info.getFirst().getPersons() ) {
				scenario.getPopulation().addPerson( p );
				((PersonImpl) p).setSelectedPlan( null );
			}

			if ( info.getSecond() != null ) {
				final Collection<Plan> plans = info.getSecond().getAllIndividualPlans();
				assert plans.size() == info.getFirst().getPersons().size();
				for ( Plan p : plans ) {
					assert p.getPerson().getSelectedPlan() == null;
					((PersonImpl) p.getPerson()).setSelectedPlan( p );
				}
			}
			else {
				for ( Person p : info.getFirst().getPersons() ) {
					// XXX THIS IS UGLY!!!
					// we HAVE to do something like this because this stupid PersonImpl
					// doesn't accept to have no plan selected...
					final Plan dummyPlan = scenario.getPopulation().getFactory().createPlan();
					dummyPlan.setScore( null );
					p.addPlan( dummyPlan );
					((PersonImpl) p).setSelectedPlan( dummyPlan );
				}
			}

			assert consistentSelectionStatus( info.getSecond().getJointPlans() );
			assert !inJointPlans( info.getSecond().getIndividualPlans() , toDump.getJointPlans() );
		}
		cliquesWriter.finishAndCloseFile();

		new PopulationWriter(
				scenario.getPopulation(),
				scenario.getNetwork() ).write(
					paths.plansFilePath );

		JointPlansXmlWriter.write(
				scenario.getPopulation(),
				toDump.getJointPlans(),
				paths.jointPlansFilePath );
	}

	private static boolean inJointPlans(
			final Collection<Plan> individualPlans,
			final JointPlans jointPlans) {
		for ( Plan p : individualPlans ) {
			if ( jointPlans.getJointPlan( p ) != null ) return true;
		}
		return false;
	}
}

class StoragePaths {
	public final String directoryPath, plansFilePath, jointPlansFilePath, cliquesFilePath;

	public StoragePaths(
			final String directoryPath) {
		this.directoryPath = directoryPath;
		this.plansFilePath = directoryPath+"/plans.xml";
		this.jointPlansFilePath = directoryPath+"/jointplans.xml";
		this.cliquesFilePath = directoryPath+"/cliques.xml";
	}

	public boolean allPathsExist() {
		final boolean plans = new File( plansFilePath ).exists();
		final boolean jointPlans = new File( jointPlansFilePath ).exists();
		final boolean cliques = new File( cliquesFilePath ).exists();

		return plans && jointPlans && cliques;
	}
}

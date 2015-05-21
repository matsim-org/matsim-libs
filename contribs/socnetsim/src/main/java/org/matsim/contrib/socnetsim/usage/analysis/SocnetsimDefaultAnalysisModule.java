/* *********************************************************************** *
 * project: org.matsim.*
 * SocnetsimDefaultAnalysisModule.java
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
package org.matsim.contrib.socnetsim.usage.analysis;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.socnetsim.framework.cliques.Clique;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.FixedGroupsIdentifier;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupIdentifier;
import org.matsim.contrib.socnetsim.usage.replanning.GroupReplanningConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.listener.ControlerListener;

import java.util.Collections;

/**
 * @author thibautd
 */
public class SocnetsimDefaultAnalysisModule extends AbstractModule {

	@Override
	public void install() {
		final Identifier groupIdentifier = new Identifier();
		binder().requestInjection( groupIdentifier );

		this.addControlerListenerBinding().toProvider( 
				new Provider<ControlerListener>() {
					@Inject OutputDirectoryHierarchy controlerIO;
					@Inject Scenario scenario;

					@Override
					public ControlerListener get() {
						return new FilteredScoreStats(
									((GroupReplanningConfigGroup) scenario.getConfig().getModule( GroupReplanningConfigGroup.GROUP_NAME )).getGraphWriteInterval(),
									controlerIO,
									scenario,
									groupIdentifier);
					}
				});

		this.addControlerListenerBinding().toProvider( 
				new Provider<ControlerListener>() {
					@Inject OutputDirectoryHierarchy controlerIO;
					@Inject Scenario scenario;

					@Override
					public ControlerListener get() {

						return new JointPlanSizeStats(
									((GroupReplanningConfigGroup) scenario.getConfig().getModule( GroupReplanningConfigGroup.GROUP_NAME )).getGraphWriteInterval(),
									controlerIO,
									scenario,
									groupIdentifier);
					}
				});

		this.addControlerListenerBinding().toProvider( 
				new Provider<ControlerListener>() {
					@Inject OutputDirectoryHierarchy controlerIO;
					@Inject Scenario scenario;

					@Override
					public ControlerListener get() {

						return new JointTripsStats(
									((GroupReplanningConfigGroup) scenario.getConfig().getModule( GroupReplanningConfigGroup.GROUP_NAME )).getGraphWriteInterval(),
									controlerIO,
									scenario,
									groupIdentifier);
					}
				});


	}

	private static class Identifier implements AbstractPlanAnalyzerPerGroup.GroupIdentifier {
		private AbstractPlanAnalyzerPerGroup.GroupIdentifier delegate;
		
		@Inject
		public void injectDelegate( final GroupIdentifier replanningIdentifier ) {
			this.delegate = replanningIdentifier instanceof FixedGroupsIdentifier ?
				new CliquesSizeGroupIdentifier(
						((FixedGroupsIdentifier) replanningIdentifier).getGroupInfo() ) :
				new AbstractPlanAnalyzerPerGroup.GroupIdentifier() {
					private final Iterable<Id<Clique>> groups = Collections.<Id<Clique>>singleton( Id.create( "all" , Clique.class ) );

					@Override
					public Iterable<Id<Clique>> getGroups(final Person person) {
						return groups;
					}
				};
		}

		@Override
		public Iterable<Id<Clique>> getGroups(final Person person) {
			return delegate.getGroups( person );
		}

	}
}


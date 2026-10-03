/*
 * Copyright (C) 2026 MOIA GmbH
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.remoteoperations.analysis;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.extension.operations.DrtOperationsParams;
import org.matsim.contrib.drt.extension.operations.remoteoperations.RemoteGuidanceOperators;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.controler.MatsimServices;

/**
 * Installs the remote-guidance operator-utilisation analysis: a controller-scoped
 * {@link RemoteGuidanceAnalysisTracker} (event handler) feeding a {@link RemoteGuidanceAnalysisControlerListener}
 * (per-iteration file writer), bound modally in the same idiom as {@code DrtShiftEfficiencyModeModule}. Only installed
 * for modes that actually configure remote guidance; otherwise the module is inert.
 *
 * @author nkuehnel / MOIA
 */
public class RemoteGuidanceAnalysisModeModule extends AbstractDvrpModeModule {

	private final DrtConfigGroup drtConfigGroup;

	public RemoteGuidanceAnalysisModeModule(DrtConfigGroup drtConfigGroup) {
		super(drtConfigGroup.getMode());
		this.drtConfigGroup = drtConfigGroup;
	}

	@Override
	public void install() {
		boolean remoteGuidance = ((DrtWithExtensionsConfigGroup) drtConfigGroup).getDrtOperationsParams()
				.flatMap(DrtOperationsParams::getRemoteGuidanceParams)
				.isPresent();
		if (!remoteGuidance) {
			return;
		}

		bindModal(RemoteGuidanceAnalysisTracker.class)
				.toProvider(modalProvider(getter -> new RemoteGuidanceAnalysisTracker(getMode())))
				.asEagerSingleton();
		addEventHandlerBinding().to(modalKey(RemoteGuidanceAnalysisTracker.class));

		bindModal(RemoteGuidanceAnalysisControlerListener.class).toProvider(modalProvider(getter ->
				new RemoteGuidanceAnalysisControlerListener(drtConfigGroup,
						getter.getModal(RemoteGuidanceAnalysisTracker.class),
						getter.getModal(RemoteGuidanceOperators.class),
						getter.getModal(DrtEventSequenceCollector.class),
						getter.getModal(Network.class),
						getter.get(MatsimServices.class))));
		addControllerListenerBinding().to(modalKey(RemoteGuidanceAnalysisControlerListener.class));
	}
}

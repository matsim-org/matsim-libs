/* *********************************************************************** *
// * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.freight.receiver;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.freight.carriers.Carrier;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.freight.receiver.replanning.ReceiverReplanningUtils;
import org.matsim.freight.receiver.replanning.ReceiverStrategyManager;

public final class ReceiverModule extends AbstractModule {
    final private static Logger LOG = LogManager.getLogger(ReceiverModule.class);
    private ReceiverReplanningType replanningType = null;
    private Boolean createPNG = true;
	final private ReceiverCostAllocation costAllocation;

	/**
	 * Creating the module that deals with freight receivers.
	 * @param costAllocation an instance of {@link ReceiverCostAllocation} that
	 *                       specifies how the {@link Carrier}
	 *                       costs are to be allocated to the different {@link Receiver}s.
	 */
    public ReceiverModule(ReceiverCostAllocation costAllocation) {
		this.costAllocation = costAllocation;
    }

    @Override
    public void install() {
        /* ConfigGroup */
        ReceiverConfigGroup configGroup;
        configGroup = ConfigUtils.addOrGetModule(this.getConfig(), ReceiverConfigGroup.NAME, ReceiverConfigGroup.class);

        /* Carrier */
        this.addControlerListenerBinding().to(ReceiverTriggersCarrierReplanningListener.class);

        bind(ReceiverScoringFunctionFactory.class).toInstance(new ReceiverScoringFunctionFactoryMoneyOnly());
		bind(ReceiverCostAllocation.class).toInstance(costAllocation);

        /* Check defaults */
        if (this.replanningType != null) {
            ReceiverReplanningType currentType = configGroup.getReplanningType();
            if (currentType != this.replanningType) {
                LOG.warn("   Overwriting the receiver replanning: was '"
                        + currentType + "'; now '" + this.replanningType + "'");
                configGroup.setReplanningType(this.replanningType);
            }
        }

		/* This module limits the number of 'levers' that you can pull, at the
		 * same time, to affect the receiver's behaviour. Consequently, the next
		 * piece of code aims to bind a (limited set of) StrategyManagers. These
		 * are used during replanning in the ReceiverControlerListener.
		 * FIXME at this point (Apr'23, JWJ), these are mutually exclusive. So, each combination must be created explicitly.
		 */
		switch (configGroup.getReplanningType()) {
			case timeWindow -> bind(ReceiverStrategyManager.class).toProvider(ReceiverReplanningUtils.createStrategyManager(ReceiverReplanningType.timeWindow));
			case serviceTime -> bind(ReceiverStrategyManager.class).toProvider(ReceiverReplanningUtils.createStrategyManager(ReceiverReplanningType.serviceTime));
			case orderFrequency -> bind(ReceiverStrategyManager.class).toProvider(ReceiverReplanningUtils.createStrategyManager(ReceiverReplanningType.orderFrequency));
			default -> throw new RuntimeException("Strategy manager for '" + configGroup.getReplanningType() + "' not implemented yet!!");
		}

        addControlerListenerBinding().to(ReceiverControlerListener.class);
        //FIXME override the createPNG

        /* Statistics and output */
//        CarrierScoreStats scoreStats = new CarrierScoreStats( ReceiverUtils.getCarriers( controler.getScenario() ), controler.getScenario().getConfig().controler().getOutputDirectory() + "/carrier_scores", this.createPNG);
        addControlerListenerBinding().to(ReceiverScoreStats.class);
    }


    public boolean isCreatingPNG() {
        return this.createPNG;
    }

    public void setCreatePNG(boolean createPNG) {
        this.createPNG = createPNG;
    }

    public void setReplanningType(ReceiverReplanningType type) {
        this.replanningType = type;
    }
}



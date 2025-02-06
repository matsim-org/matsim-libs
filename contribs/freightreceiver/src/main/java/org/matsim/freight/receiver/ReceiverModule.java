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

import com.google.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.controller.CarrierScoringFunctionFactory;
import org.matsim.freight.carriers.controller.CarrierStrategyManager;
import org.matsim.freight.carriers.usecases.analysis.CarrierScoreStats;
import org.matsim.freight.carriers.usecases.analysis.LegHistogram;
import org.matsim.freight.receiver.replanning.ReceiverReplanningUtils;
import org.matsim.freight.receiver.replanning.ReceiverStrategyManager;

public final class ReceiverModule extends AbstractModule {
    final private static Logger LOG = LogManager.getLogger(ReceiverModule.class);
    private ReceiverReplanningType replanningType = null;
    private Boolean createPNG = true;
	final private ReceiverCostAllocation costAllocation;
	private CarrierScoringFunctionFactory scoringFunctionFactory;
	private Provider<CarrierStrategyManager> strategyManagerProvider;
	private CarrierScoreStats carrierScoreStats;

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
		/* Bind all the necessary infrastructure for the carrier. Based on
		 * org.matsim.freight.carriers.usecases.chessboard.RunChessboard
		 * ... taken out again. */
		if(strategyManagerProvider != null){
			bind(CarrierStrategyManager.class).toProvider(strategyManagerProvider);
		}
		if(scoringFunctionFactory != null){
			bind(CarrierScoringFunctionFactory.class).toInstance(scoringFunctionFactory);
		}
		if(carrierScoreStats != null) {
			addControlerListenerBinding().toInstance(carrierScoreStats);
		}

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

		/* Carrier scoring stats. */
		addControlerListenerBinding().toInstance((IterationEndsListener) event -> {

			//TODO Only write out the plans and stats if required. */
            String dir = event.getServices().getControlerIO().getIterationPath(event.getIteration());

            // write plans
            CarriersUtils.writeCarriers(CarriersUtils.getCarriers(event.getServices().getScenario()), dir, "carrierPlans.xml", String.valueOf(event.getIteration()));

            //write stats
            final LegHistogram freightOnly = new LegHistogram(900).setInclPop(false);
            addEventHandlerBinding().toInstance(freightOnly);

            final LegHistogram withoutFreight = new LegHistogram(900);
            addEventHandlerBinding().toInstance(withoutFreight);

            freightOnly.writeGraphic(dir + "/" + event.getIteration() + ".legHistogram_freight.png");
            freightOnly.reset(event.getIteration());

            withoutFreight.writeGraphic(dir + "/" + event.getIteration() + ".legHistogram_withoutFreight.png");
            withoutFreight.reset(event.getIteration());
        });

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

	/**
	 * It is necessary to provide the Receiver module with a description of how
	 * the carriers will score themselves. During the <code>install()</code>
	 * stage, the code will try to check if there is already a
	 * {@link CarrierScoringFunctionFactory} set.
	 *
	 * TODO Implement this check!!
	 */
	public void setScoringFunctionFactory(CarrierScoringFunctionFactory factory){
		this.scoringFunctionFactory = factory;
	}

	/**
	 * It is necessary to provide the Receiver module with a description of how
	 * the carriers will replan. During the <code>install()</code> stage, the
	 * code will try to check if there is already a
	 * {@link CarrierScoringFunctionFactory} set.
	 *
	 * TODO Implement this check!!
	 */
	public void setCarrierStrategyManagerProvider(Provider<CarrierStrategyManager> strategyManagerProvider){
		this.strategyManagerProvider = strategyManagerProvider;
	}

	public void setCarrierScoreStats(CarrierScoreStats carrierScoreStats){
		this.carrierScoreStats = carrierScoreStats;
	}

}



package org.matsim.contrib.freightReceiver.replanning;

/**
 * A single entry point for receiver replanning.
 */
public class ReceiverReplanningUtils {

    public static TimeWindowReceiverOrderStrategyManagerImpl createTimeWindowFactory(){
        return new TimeWindowReceiverOrderStrategyManagerImpl();
    }

    public static ServiceTimeReceiverOrderStrategyManagerImpl createServiceTimeFactory(){
        return new ServiceTimeReceiverOrderStrategyManagerImpl();
    }

    public static NumDelReceiverOrderStrategyManagerImpl createNumberOfDeliveryFactory(){
        return new NumDelReceiverOrderStrategyManagerImpl();
    }
}

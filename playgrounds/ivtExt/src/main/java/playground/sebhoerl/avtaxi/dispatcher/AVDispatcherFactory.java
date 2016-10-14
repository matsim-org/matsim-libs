package playground.sebhoerl.avtaxi.dispatcher;

import playground.sebhoerl.avtaxi.data.AVVehicle;

import java.util.Collection;

public interface AVDispatcherFactory {
    AVDispatcher createDispatcher();
}

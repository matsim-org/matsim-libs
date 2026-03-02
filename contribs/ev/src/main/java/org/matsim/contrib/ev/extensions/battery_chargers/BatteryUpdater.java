package org.matsim.contrib.ev.extensions.battery_chargers;

import org.matsim.api.core.v01.IdMap;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

public class BatteryUpdater implements BatteryChargerStateEventHandler, IterationEndsListener {
    private final IdMap<Charger, Double> state = new IdMap<>(Charger.class);

    private final ChargingInfrastructureSpecification infrastructure;

    public BatteryUpdater(ChargingInfrastructureSpecification infrastructure) {
        this.infrastructure = infrastructure;
    }

    @Override
    public void handleEvent(BatteryChargerStateEvent event) {
        state.compute(event.getChargerId(), (id, value) -> event.getSoc());
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        for (var entry : state.entrySet()) {
            BatteryChargerSettings.setInitialSoc(
                    infrastructure.getChargerSpecifications().get(entry.getKey()).getAttributes(), entry.getValue());
        }
    }
}

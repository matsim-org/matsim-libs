package org.matsim.contrib.ev.analysis;

import org.matsim.contrib.common.zones.ZoneSystemUtils;
import org.matsim.contrib.ev.charging.EnergyChargedEvent;
import org.matsim.contrib.ev.charging.EnergyChargedEventHandler;
import org.matsim.contrib.ev.discharging.IdlingEnergyConsumptionEventHandler;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

public class ZonalEnergyDemandListener implements IterationStartsListener, IterationEndsListener, EnergyChargedEventHandler  {
    private final ChargingInfrastructureSpecification infrastructure;

    public ZonalEnergyDemandListener() {
        ZoneSystemUtils.createZoneSystem()
    }
    
    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'notifyIterationStarts'");
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'notifyIterationEnds'");
    }
    @Override
    public void handleEvent(EnergyChargedEvent event) {
        ChargerSpecification charger = infrastrcuture.getChargerSpecifications().get(event.getChargerId());
    }
}

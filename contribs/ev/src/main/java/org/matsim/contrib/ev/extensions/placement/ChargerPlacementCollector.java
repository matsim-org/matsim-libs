package org.matsim.contrib.ev.extensions.placement;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.contrib.ev.charging.EnergyChargedEvent;
import org.matsim.contrib.ev.charging.EnergyChargedEventHandler;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.strategic.scoring.ChargingPlanScoring;

/**
 * Collects information on the chargers that will be used for placement
 */
public class ChargerPlacementCollector implements EnergyChargedEventHandler, PersonMoneyEventHandler {
    private final IdMap<Charger, Double> energy = new IdMap<>(Charger.class);
    private final IdMap<Charger, Double> revenue = new IdMap<>(Charger.class);
    private final IdSet<Charger> used = new IdSet<>(Charger.class);

    @Override
    public void handleEvent(EnergyChargedEvent event) {
        energy.compute(event.getChargerId(), (id, val) -> {
            return (val == null ? 0.0 : val) + event.getEnergy();
        });

        used.add(event.getChargerId());
    }

    @Override
    public void handleEvent(PersonMoneyEvent event) {
        if (event.getPurpose().equals(ChargingPlanScoring.MONEY_EVENT_PURPOSE)) {
            Id<Charger> chargerId = Id.create(event.getTransactionPartner(), Charger.class);

            revenue.compute(chargerId, (id, val) -> {
                return (val == null ? 0.0 : val) + event.getAmount();
            });
        }
    }

    public double getEnergy(Id<Charger> chargerId) {
        return energy.getOrDefault(chargerId, 0.0);
    }

    public double getRevenue(Id<Charger> chargerId) {
        return revenue.getOrDefault(chargerId, 0.0);
    }

    public boolean isUsed(Id<Charger> chargerId) {
        return used.contains(chargerId);
    }

    public void clear() {
        energy.clear();
        revenue.clear();
        used.clear();
    }
}

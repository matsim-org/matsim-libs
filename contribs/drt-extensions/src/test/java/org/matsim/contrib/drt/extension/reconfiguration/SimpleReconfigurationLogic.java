package org.matsim.contrib.drt.extension.reconfiguration;

import java.util.List;
import java.util.Optional;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.extension.reconfiguration.logic.CapacityReconfigurationLogic;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.controler.Controler;

public class SimpleReconfigurationLogic implements CapacityReconfigurationLogic {
    private final Id<Link> linkId;
    private final DvrpLoadType loadType;

    SimpleReconfigurationLogic(Id<Link> linkId, DvrpLoadType loadType) {
        this.loadType = loadType;
        this.linkId = linkId;
    }

    @Override
    public Optional<DvrpLoad> getUpdatedStartCapacity(DvrpVehicle vehicle) {
        return Optional.empty();
    }

    @Override
    public List<ReconfigurationItem> getCapacityUpdates(DvrpVehicle dvrpVehicle) {
        DvrpLoad newVehicleLoad;
        if ((int) dvrpVehicle.getCapacity().getElement(0) > 0) {
            newVehicleLoad = DvrpLoadType.fromArray(loadType, 0, 4);
        } else if ((int) dvrpVehicle.getCapacity().getElement(1) > 0) {
            newVehicleLoad = DvrpLoadType.fromArray(loadType, 4, 0);
        } else {
            throw new IllegalStateException();
        }
        return List.of(new ReconfigurationItem(12 * 3600, this.linkId, newVehicleLoad));
    }

    static public void install(Controler controller, String mode) {
        controller.addOverridingModule(new AbstractDvrpModeModule(mode) {
            @Override
            public void install() {
                bindModal(CapacityReconfigurationLogic.class).toProvider(modalProvider(getter -> {
                    Network network = getter.getModal(Network.class);
                    Link link = network.getLinks().values().iterator().next();
                    return new SimpleReconfigurationLogic(link.getId(), getter.getModal(DvrpLoadType.class));
                }));
            }
        });
    }
}
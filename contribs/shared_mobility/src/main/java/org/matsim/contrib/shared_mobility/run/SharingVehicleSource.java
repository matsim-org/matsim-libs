package org.matsim.contrib.shared_mobility.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.shared_mobility.io.SharingServiceSpecification;
import org.matsim.contrib.shared_mobility.io.SharingVehicleSpecification;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

/**
 * @author steffenaxer
 */
public class SharingVehicleSource implements AgentSource {
    private QSim qsim;
    private SharingServiceSpecification specification;

    public SharingVehicleSource(QSim qSim, SharingServiceSpecification specification) {
        this.qsim = qSim;
        this.specification = specification;
    }

    @Override
    public void insertAgentsIntoMobsim() {

        VehiclesFactory factory = this.qsim.getScenario().getVehicles().getFactory();

        for (SharingVehicleSpecification veh : specification.getVehicles()) {
            Id<Link> startLink = veh.getStartLinkId().get();
            Id<Vehicle> vehId = Id.createVehicleId(veh.getId().toString());
            Vehicle basicVehicle = factory.createVehicle(vehId, VehicleUtils.createDefaultVehicleType());
            QVehicleImpl qvehicle = new QVehicleImpl(basicVehicle);
            qvehicle.setCurrentLink(this.qsim.getScenario().getNetwork().getLinks().get(startLink));
            qsim.addParkedVehicle(qvehicle, startLink);
        }
    }
}

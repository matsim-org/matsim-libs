package org.matsim.mosaic;

import org.eclipse.mosaic.interactions.traffic.VehicleUpdates;
import org.eclipse.mosaic.lib.geo.CartesianPoint;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.rti.api.IllegalValueException;
import org.eclipse.mosaic.rti.api.InternalFederateException;
import org.eclipse.mosaic.rti.api.RtiAmbassador;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bridge between MATSim drt fleet and mosaic co-simulation framework.
 */
@SuppressWarnings("rawtypes")
public class MosaicDrtEngine implements DrtOptimizer, MobsimInitializedListener, MobsimBeforeSimStepListener, MobsimBeforeCleanupListener {

	private final RtiAmbassador rti;
	private final Fleet fleet;

	@Inject
	public MosaicDrtEngine(RtiAmbassador rti, Fleet fleet) {
		this.rti = rti;
		this.fleet = fleet;
	}

	@Override
	public void requestSubmitted(Request request) {

	}

	@Override
	public void nextTask(DvrpVehicle vehicle) {

	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent event) {

		List<VehicleData> vehicles = new ArrayList<>();

		// send initial vehicle states
		for (Map.Entry<Id<DvrpVehicle>, DvrpVehicle> e : fleet.getVehicles().entrySet()) {

			Coord coord = e.getValue().getStartLink().getCoord();
			CartesianPoint xy = CartesianPoint.xy(coord.getX(), coord.getY());

			// TODO: determin correct crs
			VehicleData.Builder builder = new VehicleData.Builder(0, e.getKey().toString())
					.position(xy.toGeo(), xy);

			vehicles.add(builder.create());
		}

		VehicleUpdates updates = new VehicleUpdates(0, List.of(), vehicles, List.of());
		try {
			rti.triggerInteraction(updates);
		} catch (IllegalValueException | InternalFederateException ex) {
			ex.printStackTrace();
		}

	}

	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {

	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
	}
}

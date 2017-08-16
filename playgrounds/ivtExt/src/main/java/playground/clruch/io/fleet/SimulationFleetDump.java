// code by jph
package playground.clruch.io.fleet;

import java.util.ArrayList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.QuadTree;

import ch.ethz.idsc.tensor.Tensors;
import playground.clruch.export.AVStatus;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.SimulationObjects;
import playground.clruch.net.StorageSubscriber;
import playground.clruch.net.VehicleContainer;

enum SimulationFleetDump {
	;
	public static void of(DayTaxiRecord dayTaxiRecord, Network network, MatsimStaticDatabase db) {

		final double[] networkBounds = NetworkUtils.getBoundingBox(network.getNodes().values());
		final QuadTree<Link> quadTree = new QuadTree<>( //
				networkBounds[0], networkBounds[1], networkBounds[2], networkBounds[3]);

		System.out.println("bounding box = " + Tensors.vectorDouble(networkBounds));
		// ---
		for (Link link : db.getLinkInteger().keySet())
			quadTree.put(link.getCoord().getX(), link.getCoord().getY(), link);

		int dropped = 0;
		for (int now : dayTaxiRecord.keySet()) {
			SimulationObject simulationObject = new SimulationObject();
			simulationObject.now = now;
			simulationObject.vehicles = new ArrayList<>();
			for (TaxiStamp taxiStamp : dayTaxiRecord.get(now)) {
				try {
					Coord xy = db.referenceFrame.coords_fromWGS84.transform(taxiStamp.gps);
					Link center = quadTree.getClosest(xy.getX(), xy.getY());
					VehicleContainer vc = new VehicleContainer();
					vc.vehicleIndex = taxiStamp.id;
					vc.linkIndex = db.getLinkIndex(center);
					vc.avStatus = AVStatus.REBALANCEDRIVE;
					simulationObject.vehicles.add(vc);
				} catch (Exception exception) {
					System.out.println("fail " + taxiStamp.gps);
					++dropped;
				}
			}
			SimulationObjects.sortVehiclesAccordingToIndex(simulationObject);
			new StorageSubscriber().handle(simulationObject);
		}
		System.out.println("dropped total " + dropped);

	}

}

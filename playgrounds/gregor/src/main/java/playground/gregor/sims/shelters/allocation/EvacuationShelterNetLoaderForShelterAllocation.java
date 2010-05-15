package playground.gregor.sims.shelters.allocation;

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.evacuation.base.Building;
import org.matsim.evacuation.flooding.FloodingReader;
import org.matsim.evacuation.shelters.EvacuationShelterNetLoader;

public class EvacuationShelterNetLoaderForShelterAllocation extends EvacuationShelterNetLoader {

	private NetworkLayer network;
	private List<Building> buildings;
	private Scenario scenario;

	public EvacuationShelterNetLoaderForShelterAllocation(
			List<Building> buildings, Scenario scenario, List<FloodingReader> netcdf) {
		super(buildings, scenario,netcdf);
		this.network = (NetworkLayer) scenario.getNetwork();
		this.buildings = buildings;
		this.scenario = scenario;
	}


	public void generateShelterLinks(int numOfPers) {

		Node saveNode = this.network.getNodes().get(new IdImpl("en1")); //TODO GL Apr. 09 -- evacuation node should not retrieved via String id
		Id saveLinkId = saveNode.getOutLinks().values().iterator().next().getId();
		int cap = 0;
		
		Node toNode = this.network.createAndAddNode(new IdImpl("en3"), new CoordImpl(662433,9898853));
		
		for (Building building : this.buildings) {
			if (!building.isQuakeProof()) {
				continue;
			}
			cap += building.getShelterSpace();
			double flowCap = 0.4 * building.getMinWidth() * this.scenario.getConfig().simulation().getTimeStepSize();
//			flowCap = 6;
			String shelterId = building.getId().toString();
			Coord c = MGC.point2Coord(building.getGeo().getCentroid());
			Node from = this.network.getNearestNode(c);
			while (from.getId().toString().contains("sn")) {
				from = from.getInLinks().values().iterator().next().getFromNode();
			}
			Node sn1 = this.network.createAndAddNode(new IdImpl("sn" + shelterId + "a"), c);
			Node sn2 = this.network.createAndAddNode(new IdImpl("sn" + shelterId + "b"), c);
			Link l1 = this.network.createAndAddLink(new IdImpl("sl" + shelterId + "a"), from, sn1, 1.66 , 1.66, flowCap, 1); //FIXME find right values flow cap, lanes, ...
			Link l2 = this.network.createAndAddLink(new IdImpl("sl" + shelterId + "b"), sn1,sn2, 10 , 1.66, flowCap, 1); //FIXME find right values flow cap, lanes, ...
			this.getShelterLinkMapping().put(l2.getId(), building);
			Link l3 = this.network.createAndAddLink(new IdImpl("sl" + shelterId + "c"), sn2,toNode, 10 , 10000, 10000, 1); //FIXME find right values flow cap, lanes, ...
			this.getShelterLinks().add(l1);
			this.getShelterLinks().add(l2);
			this.getShelterLinks().add(l3);
//			try {
//				bw.append(from.getId() + "," + flowCap + "," + building.getShelterSpace() + "\n");
//			} catch (IOException e) {
//				e.printStackTrace();
//			}

		}
		int superCap = numOfPers - cap;
		Building superShelter = new Building(new IdImpl("super_shelter"),0,0,0,0,superCap,10000,1,null);
		this.buildings.add(superShelter);
		this.getShelterLinkMapping().put(saveLinkId, superShelter);
		
//		try {
//			bw.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		this.network.connect();
	}

	
}

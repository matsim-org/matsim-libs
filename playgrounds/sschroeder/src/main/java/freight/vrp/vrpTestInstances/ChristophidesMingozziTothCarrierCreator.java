package freight.vrp.vrpTestInstances;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.mzilske.freight.carrier.Carrier;
import playground.mzilske.freight.carrier.CarrierContract;
import playground.mzilske.freight.carrier.CarrierOffer;
import playground.mzilske.freight.carrier.CarrierShipment;
import playground.mzilske.freight.carrier.CarrierUtils;
import playground.mzilske.freight.carrier.CarrierVehicle;

public class ChristophidesMingozziTothCarrierCreator {
	
	private Collection<Carrier> carriers;
	
	private NetworkImpl network;
	
	private int scale;
	
	public ChristophidesMingozziTothCarrierCreator(Collection<Carrier> carriers, Network network, int scale) {
		super();
		this.carriers = carriers;
		this.network = (NetworkImpl)network;
		this.scale = scale;
	}

	public void createCarriers(String filename) {
		BufferedReader reader = IOUtils.getBufferedReader(filename);
		int counter = 0;
		String line = null;
		Id carrierId = makeId("fooCarrier");
		Carrier carrier = null;
		Integer vehicleCapacity = null; 
		Id depotLinkId = null;
		try {
			while((line = reader.readLine()) != null){
				line = line.replace("\r", "");
				line = line.trim();
				String[] tokens = line.split(" ");
				if(counter == 0){
					vehicleCapacity = Integer.parseInt(tokens[1].trim());
				}
				else if(counter == 1){
					depotLinkId = getNearestLinkId(makeCoord(tokens[0].trim(),tokens[1].trim()));
					carrier = CarrierUtils.createCarrier("depot", depotLinkId.toString());
					CarrierVehicle carrierVehicle = new CarrierVehicle(makeId("foo_vehicle"), depotLinkId);
					carrierVehicle.setCapacity(vehicleCapacity);
					carrier.getCarrierCapabilities().getCarrierVehicles().add(carrierVehicle);
					carriers.add(carrier);
					
				}
				else{
					Id toLinkId = getNearestLinkId(makeCoord(tokens[0].trim(),tokens[1].trim()));
					CarrierShipment shipment = CarrierUtils.createShipment(depotLinkId, toLinkId, Integer.parseInt(tokens[2].trim()),  0.0, Double.MAX_VALUE, 0.0, Double.MAX_VALUE);
					CarrierOffer offer = new CarrierOffer();
					offer.setId(carrierId);
					CarrierContract contract = new CarrierContract(shipment, offer);
					carrier.getContracts().add(contract);
				}
				counter++;
			}
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private Id getNearestLinkId(Coord coord) {
		Link link = network.getNearestLink(coord);
		return link.getId();
	}

	private Coord makeCoord(String xString, String yString) {
		double x = scale*Double.parseDouble(xString);
		double y = scale*Double.parseDouble(yString);
		return new CoordImpl(x,y);
	}

	private Id makeId(String string) {
		return new IdImpl(string);
	}

}

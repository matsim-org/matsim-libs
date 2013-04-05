//package vehicles;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//
//import org.apache.log4j.Logger;
//import org.matsim.api.core.v01.Id;
//import org.matsim.contrib.freight.carrier.Carrier;
//import org.matsim.contrib.freight.carrier.Carriers;
//import org.matsim.core.basic.v01.IdImpl;
//import org.matsim.core.utils.io.IOUtils;
//
//public class CarrierVehicleReader {
//	
//	private static Logger logger = Logger.getLogger(CarrierVehicleReader.class);
//	
//	private Carriers carriers;
//
//	public CarrierVehicleReader(Carriers carriers) {
//		super();
//		this.carriers = carriers;
//	}
//	
//	public void read(String vehicleFile) {
//		logger.info("create vehicles");
//		logger.info("note that all existing vehicles are deleted. thus, all carriers get a new vehicle-fleet defined in " + vehicleFile);
//		logger.info("add new vehicles by adding a new line in " + vehicleFile);
//		removeExistingVehicles();
//		BufferedReader reader = IOUtils.getBufferedReader(vehicleFile);
//		String line = null;
//		int carrierIdIndex = 0;
//		int typeIdIndex = 1;
//		int nOfVehicleIndex = 2;
//		int capIndex = 3;
//		int earlyIndex = 4;
//		int lateIndex = 5;
//		boolean firstLine = true;
//		try {
//			while((line = reader.readLine()) != null){
//				if(firstLine){
//					firstLine = false;
//					continue;
//				}
//				String[] tokens = line.split(";");
//				Id carrierId = makeId(tokens[carrierIdIndex]);
//				String typeId = tokens[typeIdIndex];
//				int nOfVehicles = Integer.parseInt(tokens[nOfVehicleIndex]);
//				int cap = Integer.parseInt(tokens[capIndex]);
//				double early = Double.parseDouble(tokens[earlyIndex]);
//				double late = Double.parseDouble(tokens[lateIndex]);
//				Carrier c = carriers.getCarriers().get(carrierId);
//				if(c == null){
//					throw new IllegalStateException("carrier " + carrierId + " does not exist");
//				}
//				getVehiclesOfThisType(c,typeId,nOfVehicles,cap,early,late);
//			}
//			reader.close();
//		} catch (NumberFormatException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		logger.info("done");
//	}
//
//	private void getVehiclesOfThisType(Carrier c, String typeId, int nOfVehicles, int cap, double early, double late) {
//		for(int i=0;i<nOfVehicles;i++){
//			
//			new CarrierFactory().createAndAddVehicle(c, c.getId().toString() + "_" + typeId + "_" + (i+1), c.getDepotLinkId().toString(), cap, typeId, early, late);
//		}
//	}
//
//	private void removeExistingVehicles() {
//		for(Carrier c : carriers.getCarriers().values()){
//			c.getCarrierCapabilities().getCarrierVehicles().clear();
//		}
//		
//	}
//
//	private Id makeId(String string) {
//		return new IdImpl(string);
//	}
//
//}

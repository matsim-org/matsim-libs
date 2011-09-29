package kid;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kid.filter.AttributeFilter;
import kid.filter.DefaultChainAttributeFilter;
import kid.filter.DefaultLegAttributeFilter;
import kid.filter.DefaultVehicleAttributeFilter;
import kid.filter.DefaultVehicleFilter;
import kid.filter.VehicleFilter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.IOUtils;

/**
 * Reads Kid-data according to three input files:
 * Vehicle-Data (KiD_2002_Fahrzeug-Datei.txt)
 * Chain-Data (KiD2002_Fahrtenketten-Datei.txt)
 * Leg-Data (KiD2002_(Einzel)Fahrten-Datei.txt)
 * 
 * Currently, transport-chains and legs must be in ascending order (as it is in the original data).
 * 
 * @author sschroeder
 *
 */

public class KiDDataReader {
	
	private static Logger logger = Logger.getLogger(KiDDataReader.class);

	private ScheduledVehicles scheduledVehicles;

	private String vehicleFile = null;
	
	private String transportChainFile = null;
	
	private String transportLegFile = null;
	
	//private List<Vehicle> vehicles = new ArrayList<Vehicle>();
	
	private Map<Id,Vehicle> vehicles = new HashMap<Id,Vehicle>();
	
	private Map<Id,List<TransportLeg>> transportLegs = new HashMap<Id, List<TransportLeg>>();
	
	private Map<Id,List<TransportChain>> transportChains = new HashMap<Id, List<TransportChain>>();
	
	private VehicleFilter vehicleFilter = new DefaultVehicleFilter(); 
	
	private ScheduledVehicleFilter scheduledVehicleFilter = new DefaultScheduledVehicleFilter();
	
	private AttributeFilter vehicleAttributeFilter = new DefaultVehicleAttributeFilter();
	
	private AttributeFilter chainAttributeFilter = new DefaultChainAttributeFilter();
	
	private AttributeFilter legAttributeFilter = new DefaultLegAttributeFilter();
	
	public KiDDataReader(ScheduledVehicles vehicles) {
		super();
		this.scheduledVehicles = vehicles;
	}
	
	
	public void setVehicleAttributeFilter(AttributeFilter vehicleAttributeFilter) {
		this.vehicleAttributeFilter = vehicleAttributeFilter;
	}


	public void setChainAttributeFilter(AttributeFilter chainAttributeFilter) {
		this.chainAttributeFilter = chainAttributeFilter;
	}

	public void setLegAttributeFilter(AttributeFilter legAttributeFilter) {
		this.legAttributeFilter = legAttributeFilter;
	}

	public void run() {
		verify();
		try {
			readVehicles();
			readTransportLegs();
			readTransportChains();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			logger.error(e);
			System.exit(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error(e);
			System.exit(1);
		}
		
		buildScheduledVehicles();
	}

	private void readVehicles() throws FileNotFoundException, IOException {
		logger.info("read vehicles");
		BufferedReader reader = IOUtils.getBufferedReader(vehicleFile);
		String line = null;
		boolean firstLine = true;
		List<String> attributeNames = new ArrayList<String>();
		while((line = reader.readLine()) != null){
			line = line.replaceAll("\r", "");
			line = line.replace("\n", "");
			String[] tokens = line.split("\t");
			if(firstLine){
				for(int i=0;i<tokens.length;i++){
					attributeNames.add(tokens[i].trim());
				}
				firstLine = false;
				continue;
			}
			Id vehicleId = new IdImpl(tokens[0]);
			Vehicle vehicle = new Vehicle(vehicleId);
			for(int i=0;i<tokens.length;i++){
				if(vehicleAttributeFilter.judge(attributeNames.get(i))){
					vehicle.getAttributes().put(attributeNames.get(i), tokens[i].trim());
				}
			}
			if(judge(vehicle)){
				vehicles.put(vehicleId,vehicle);
			}
			else{
				continue;
			}
		}
	}

	private boolean judge(Vehicle vehicle) {
		if(!vehicleFilter.judge(vehicle)){
			return false;
		}
		return true;
	}


	public void setVehicleFilter(VehicleFilter vehicleFilter) {
		this.vehicleFilter = vehicleFilter;
	}


	private void readTransportLegs() throws IOException {
		logger.info("read transport legs");
		BufferedReader reader = IOUtils.getBufferedReader(transportLegFile);
		String line = null;
		boolean firstLine = true;
		List<String> attributeNames = new ArrayList<String>();
		while((line = reader.readLine()) != null){
			line = line.replaceAll("\r", "");
			line = line.replace("\n", "");
			String[] tokens = line.split("\t");
			if(firstLine){
				for(int i=0;i<tokens.length;i++){
					attributeNames.add(tokens[i].trim());
				}
				firstLine = false;
				continue;
			}
			Id vehicleId = new IdImpl(tokens[0].trim());
			if(vehicles.containsKey(vehicleId)){
				int transportLegId = Integer.parseInt(tokens[1].trim());
				TransportLeg transportLeg = new TransportLeg(transportLegId);
				for(int i=0;i<tokens.length;i++){
					if(legAttributeFilter.judge(attributeNames.get(i))){
						transportLeg.getAttributes().put(attributeNames.get(i), tokens[i].trim());
					}
				}
				if(transportLegs.containsKey(vehicleId)){
					transportLegs.get(vehicleId).add(transportLeg);
				}
				else{
					List<TransportLeg> transportLegList = new ArrayList<TransportLeg>();
					transportLegList.add(transportLeg);
					transportLegs.put(vehicleId, transportLegList);
				}
			}
			else { 
				continue; 
			}
		}
	}

	private void readTransportChains() throws FileNotFoundException, IOException {
		logger.info("read transport chains");
		BufferedReader reader = IOUtils.getBufferedReader(transportChainFile);
		String line = null;
		boolean firstLine = true;
		List<String> attributeNames = new ArrayList<String>();
		while((line = reader.readLine()) != null){
			line = line.replaceAll("\r", "");
			line = line.replace("\n", "");
			String[] tokens = line.split("\t");
			if(firstLine){
				for(int i=0;i<tokens.length;i++){
					attributeNames.add(tokens[i].trim());
				}
				firstLine = false;
				continue;
			}
			Id vehicleId = new IdImpl(tokens[0].trim());
			if(vehicles.containsKey(vehicleId)){
				int transportChainId = Integer.parseInt(tokens[1].trim());
				TransportChain transportChain = new TransportChain(transportChainId);
				for(int i=0;i<tokens.length;i++){
					if(chainAttributeFilter.judge(attributeNames.get(i))){
						transportChain.getAttributes().put(attributeNames.get(i), tokens[i].trim());
					}
				}
				if(transportChains.containsKey(vehicleId)){
					transportChains.get(vehicleId).add(transportChain);
				}
				else{
					List<TransportChain> transportChainList = new ArrayList<TransportChain>();
					transportChainList.add(transportChain);
					transportChains.put(vehicleId, transportChainList);
				}
			}
			else {
				continue;
			}
		}
		
	}

	private void buildScheduledVehicles() {
		logger.info("build scheduled vehicles");
		for(Vehicle vehicle : vehicles.values()){
			try{
				List<TransportChain> chains = transportChains.get(vehicle.getId());
				List<TransportLeg> legs = transportLegs.get(vehicle.getId());
				ScheduledVehicle scheduledVehicle = null;
				if(chains == null || legs == null){
					List<ScheduledTransportChain> scheduledChains = new ArrayList<ScheduledTransportChain>();
					scheduledVehicle = new ScheduledVehicle(vehicle, scheduledChains);	
				}
				else{
					List<ScheduledTransportChain> scheduledChains = buildScheduledChains(chains,legs);
					scheduledVehicle = new ScheduledVehicle(vehicle, scheduledChains);
				}
				if(scheduledVehicleFilter.judge(scheduledVehicle)){
					scheduledVehicles.getScheduledVehicles().put(vehicle.getId(), scheduledVehicle);
				}
				
			}
			catch(NullPointerException e){
				logger.error("vehicleId="+vehicle.getId());
				throw new NullPointerException(e.toString());
			}
			
		}
		
	}

	private List<ScheduledTransportChain> buildScheduledChains(List<TransportChain> chains, List<TransportLeg> legs) {
		List<ScheduledTransportChain> scheduledChains = new ArrayList<ScheduledTransportChain>();
		Integer currentFirstLegId = null;
		TransportChain currentChain = null;
		for(TransportChain c : chains){
			if(currentFirstLegId == null){
				currentFirstLegId = Integer.parseInt(c.getAttributes().get(KiDSchema.LEGID_OF_FIRSTLEG));
				currentChain = c;
			}
			else{
				Integer nextFirstLegId = Integer.parseInt(c.getAttributes().get(KiDSchema.LEGID_OF_FIRSTLEG));
				List<TransportLeg> currentLegs = new ArrayList<TransportLeg>();
				for(int i=currentFirstLegId.intValue();i<nextFirstLegId.intValue();i++){
					currentLegs.add(legs.get(i-1));
				}
				ScheduledTransportChain sTC = new ScheduledTransportChain(currentChain, currentLegs);
				scheduledChains.add(sTC);
				
				currentFirstLegId = nextFirstLegId;
				currentChain = c;
			}
		}
		List<TransportLeg> currentLegs = new ArrayList<TransportLeg>();
		for(int i=currentFirstLegId.intValue();i<=legs.size();i++){
			currentLegs.add(legs.get(i-1));
		}
		ScheduledTransportChain sTC = new ScheduledTransportChain(currentChain, currentLegs);
		scheduledChains.add(sTC);
		return scheduledChains;
	}

	private void verify() {
		if(vehicleFile == null || transportChainFile == null || transportLegFile == null){
			throw new RuntimeException("cannot read kid-data, since at least one file has not been set");
		}
	}

	public void setVehicleFile(String vehicleFile) {
		this.vehicleFile = vehicleFile;
	}

	public void setScheduledVehicleFilter(ScheduledVehicleFilter scheduledVehicleFilter) {
		this.scheduledVehicleFilter = scheduledVehicleFilter;
	}


	public void setTransportChainFile(String transportChainFile) {
		this.transportChainFile = transportChainFile;
	}

	public void setTransportLegFile(String transportLegFile) {
		this.transportLegFile = transportLegFile;
	}
	
	
	
	
}

package playground.tschlenther.parkingSearch.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.events.StartParkingSearchEvent;
import org.matsim.contrib.parking.parkingsearch.events.StartParkingSearchEventHandler;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;


/**
 * a simpler version of ParkingSearchAndEgressTimeEvaluator in org.matsim.contrib.parking.parkingsearch.evaluation
 * @author schlenther
 *
 */
public class SearchTimeEvaluator implements PersonArrivalEventHandler, StartParkingSearchEventHandler, PersonEntersVehicleEventHandler{

	Map<Id<Person>,Double> searchTime = new HashMap<>();
	Map<Id<Vehicle>,Id<Person>> drivers = new HashMap<>();
	private Set<Id<Link>> monitoredLinks;
	private List<String> linkTimeStamps = new ArrayList<>();
	private double[] parkingCounts = new double[96];
	private double[] parkingTime = new double[96];
	
	public SearchTimeEvaluator(Set<Id<Link>> monitoredLinks) {
		this.monitoredLinks = monitoredLinks;
	}
	
	@Override
	public void reset(int iteration) {
		this.searchTime.clear();;
		this.drivers.clear();
		this.linkTimeStamps.clear();
		this.parkingCounts = new double[96];
		this.parkingTime = new double[96];
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		//Assumes: Agent = driver, or at least the person that initiates a ride (i.e. a taxi passenger that lets his taxi search for parking or so...)
				this.drivers.put(event.getVehicleId(), event.getPersonId());
	}

	@Override
	public void handleEvent(StartParkingSearchEvent event) {
		if (this.monitoredLinks.contains(event.getLinkId())){
			Id<Person> pid = this.drivers.get(event.getVehicleId());
			if (pid!=null){
				this.searchTime.put(pid, event.getTime());	
			}
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (this.searchTime.containsKey(event.getPersonId())){
			if (event.getLegMode().equals(TransportMode.car)){
				double parkingTime = event.getTime() - searchTime.remove(event.getPersonId());
				int timeSlot = (int) (event.getTime() / 900);
				if (timeSlot<96){
					this.parkingCounts[timeSlot]++;
					this.parkingTime[timeSlot]+=parkingTime;
					String stamp = "" + (int) (event.getTime()/900) + ";" + event.getTime() + ";" + event.getLinkId() + ";"+parkingTime;
					this.linkTimeStamps.add(stamp);
				}
			}
		}
	}

	public void writeStats(String filename){
	BufferedWriter bw = IOUtils.getBufferedWriter(filename);
	DecimalFormat df = new DecimalFormat("##.##");	
		try {
			bw.write("timeSlot;parkingCounts;averageSearchTime");
			for (int i = 0; i<this.parkingCounts.length;i++){
				bw.newLine();
				bw.write(i+";"+parkingCounts[i]+";"+df.format(parkingTime[i]/parkingCounts[i]));
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
	}
	
	public void writeLinkTimeStamps(String filename){
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
			try {
				bw.write("SlotNr;SimTime;LinkID;searchTime");
				for (String s: this.linkTimeStamps){
					bw.newLine();
					bw.write(s);
				}
				bw.flush();
				bw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
			
		}
	
}

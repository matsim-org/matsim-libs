/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.jbischoff.carsharing.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
/**
 * @author jbischoff
 *
 */
public class CarsharingGrepTimerTask
    extends TimerTask
    
	
{
	Map<Id<CarsharingVehicle>,CarsharingVehicle> vehicleRegister = new HashMap<Id<CarsharingVehicle>, CarsharingVehicle>();
    DriveNowParser dnp = new DriveNowParser();
    Car2GoParser cp = new Car2GoParser(); 
    static SimpleDateFormat MIN = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    int run = 0;
    
    
    public static void main(String[] args)
    {
        CarsharingGrepTimerTask dngt = new CarsharingGrepTimerTask();
        Timer t = new Timer();
        t.scheduleAtFixedRate(dngt, 0, 60*1000);
    }

    @Override
    public void run()
    {
    	String folder = "./";

    	File car2go = new File(folder+"car2go");
    	car2go.mkdirs();

    	File drivenow = new File(folder+"drivenow");
    	drivenow.mkdirs();
    	Map<Id<CarsharingVehicleData>,CarsharingVehicleData> currentvehicles = new HashMap<Id<CarsharingVehicleData>, CarsharingVehicleData>();
    	
    	currentvehicles.putAll(dnp.grepAndDumpOnlineDatabase(drivenow.getAbsolutePath()+"/"));
    	currentvehicles.putAll(cp.grepAndDumpOnlineDatabase(car2go.getAbsolutePath()+"/"));
    	System.out.println(MIN.format(System.currentTimeMillis())+": "+currentvehicles.size());
    	syncVehiclesAndGenerateTrips(currentvehicles);
    	if (run % 30 == 0){
    		generateTripFiles(folder);
    	}
    	run++;
    }

	private void generateTripFiles(String folder) {
		
		for (CarsharingVehicle vehicle : this.vehicleRegister.values() ){
			vehicle.sortRides();
			String filename = folder + vehicle.getProvider()+"_"+vehicle.getLicensePlate()+"_"+vehicle.getVin()+".txt";
			for (CarsharingRide ride : vehicle.getRides()){
			try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)))) {
				out.println(ride.toLine());
			}catch (IOException e) {
				e.printStackTrace();
			}
			}
			
			vehicle.getRides().clear();
		}

	}

	private void syncVehiclesAndGenerateTrips(
			Map<Id<CarsharingVehicleData>, CarsharingVehicleData> currentvehicles) {
		
		long time = System.currentTimeMillis();
		for (Entry<Id<CarsharingVehicleData>,CarsharingVehicleData> entry : currentvehicles.entrySet()){
			Id<CarsharingVehicle> vin = Id.create(entry.getKey().toString(), CarsharingVehicle.class);
			if (! this.vehicleRegister.containsKey(vin)) {
				CarsharingVehicle vehicle = new CarsharingVehicle(entry.getValue().getProvider(), entry.getValue().getLicense() ,vin);
				vehicle.setFuel(entry.getValue().getFuel());
				vehicle.setMileage(entry.getValue().getMileage());
				vehicle.setTime(time);
				vehicle.setPosition(entry.getValue().getLocation());
//				System.out.println("created vehicle "+vehicle.getLicensePlate()+" "+vehicle.getP);

				this.vehicleRegister.put(vin, vehicle);
				continue;
			}
			CarsharingVehicle currentVehicle = this.vehicleRegister.get(vin);
			
			if (!currentVehicle.getPosition().equals(entry.getValue().getLocation())){
				
				HereMapsRouteGrepper rg = new HereMapsRouteGrepper(currentVehicle.getPosition(), entry.getValue().getLocation());
				VBBRouteCatcher vbb = new VBBRouteCatcher(currentVehicle.getPosition(), entry.getValue().getLocation(), time);
				CarsharingRide ride = new CarsharingRide(currentVehicle.getPosition(), entry.getValue().getLocation(), currentVehicle.getTime(), time, currentVehicle.getFuel(), entry.getValue().getFuel(), currentVehicle.getMileage(), entry.getValue().getMileage(), rg.getDistance(), rg.getBaseTime(), rg.getTravelTime(), vbb.getBestTransfers(), vbb.getBestRideTime());
				currentVehicle.addRide(ride);
				
				currentVehicle.setFuel(entry.getValue().getFuel());
				currentVehicle.setMileage(entry.getValue().getMileage());
				currentVehicle.setPosition(entry.getValue().getLocation());
				currentVehicle.setTime(time);
				System.out.println("ride registered. "+currentVehicle.getLicensePlate());
			}
			else {
				currentVehicle.setTime(time);
				// nothing has happened, so we just update the timestamp
			}
			
		}
		
	}
    
      
	
}



class CarsharingVehicle{
	
	private List<CarsharingRide> rides = new ArrayList<>();
	private String provider;
	private String licensePlate;
	private Id<CarsharingVehicle> vin;
	
	private long mileage;
	private long time;
	private Coord position = null;
	private double fuel;
	
	
	public CarsharingVehicle(String provider, String licensePlate,
			Id<CarsharingVehicle> vin) {
		super();
		this.provider = provider;
		this.licensePlate = licensePlate;
		this.vin = vin;
	}
	
	public void sortRides() {
		Collections.sort(this.rides);
	}

	public void addRide(CarsharingRide ride){
		this.rides.add(ride);
	}

	public List<CarsharingRide> getRides() {
		return rides;
	}

	public long getMileage() {
		return mileage;
	}

	public void setMileage(long mileage) {
		this.mileage = mileage;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public Coord getPosition() {
		return position;
	}

	public void setPosition(Coord position) {
		this.position = position;
	}

	public double getFuel() {
		return fuel;
	}

	public void setFuel(double fuel) {
		this.fuel = fuel;
	}

	public String getProvider() {
		return provider;
	}

	public String getLicensePlate() {
		return licensePlate;
	}

	public Id<CarsharingVehicle> getVin() {
		return vin;
	}
	
	
	
	
	
}
class CarsharingRide implements Comparable<CarsharingRide>
{
		private Coord from;
		private Coord to;
		
		private long start;
		private long end;
		
		private double fuelBefore;
		private double fuelAfter;
		
		private double mileageBefore;
		private double mileageAfter;
		
		private double roadAlternativeKM;
		private double roadAlternativeTime;
		private double roadAlternativeCongestedTime;
		
		private double ptAlternativeTransfers;
		private double ptAlternativeTime;
		
		
		public CarsharingRide(Coord from, Coord to, long start, long end,
				double fuelBefore, double fuelAfter, double mileageBefore,
				double mileageAfter, double roadAlternativeKM,
				double roadAlternativeTime, double roadAlternativeCongestedTime ,double ptAlternativeTransfers,
				double ptAlternativeTime) {
			super();
			this.from = from;
			this.to = to;
			this.start = start;
			this.end = end;
			this.fuelBefore = fuelBefore;
			this.fuelAfter = fuelAfter;
			this.mileageBefore = mileageBefore;
			this.mileageAfter = mileageAfter;
			this.roadAlternativeKM = roadAlternativeKM;
			this.roadAlternativeTime = roadAlternativeTime;
			this.roadAlternativeCongestedTime = roadAlternativeCongestedTime;
			this.ptAlternativeTransfers = ptAlternativeTransfers;
			this.ptAlternativeTime = ptAlternativeTime;
		}
		
		
		@Override
		public String toString() {
			return "CarsharingRide [from=" + from + ", to=" + to + ", start="
					+ start + ", end=" + end + ", fuelBefore=" + fuelBefore
					+ ", fuelAfter=" + fuelAfter + ", mileageBefore="
					+ mileageBefore + ", mileageAfter=" + mileageAfter
					+ ", roadAlternativeKM=" + roadAlternativeKM
					+ ", roadAlternativeTime=" + roadAlternativeTime
					+ ", ptAlternativeKM=" + ptAlternativeTransfers
					+ ", ptAlternativeTime=" + ptAlternativeTime + "]";
		}
		
		public String toLine() {
			return   from.getX() + "," +from.getY()+ "\t" + to.getX() + ","+to.getY() +"\t" + CarsharingGrepTimerTask.MIN.format(start) + "\t" + CarsharingGrepTimerTask.MIN.format(end) + "\t" + Math.round(fuelBefore) + "\t" + Math.round(fuelAfter) + "\t" + Math.round(mileageBefore) + "\t" + Math.round(mileageAfter) + "\t" + Math.round(roadAlternativeKM) + "\t" + Math.round(roadAlternativeTime) + "\t" +Math.round(roadAlternativeCongestedTime) + "\t" + Math.round(ptAlternativeTransfers) + "\t" + Math.round(ptAlternativeTime);
					
		}


		public Coord getFrom() {
			return from;
		}
		public Coord getTo() {
			return to;
		}
		public long getStart() {
			return start;
		}
		public long getEnd() {
			return end;
		}
		public double getFuelBefore() {
			return fuelBefore;
		}
		public double getFuelAfter() {
			return fuelAfter;
		}
		public double getMileageBefore() {
			return mileageBefore;
		}
		public double getMileageAfter() {
			return mileageAfter;
		}
		public double getRoadAlternativeKM() {
			return roadAlternativeKM;
		}
		public double getRoadAlternativeTime() {
			return roadAlternativeTime;
		}
		public double getPtAlternativeKM() {
			return ptAlternativeTransfers;
		}
		public double getPtAlternativeTime() {
			return ptAlternativeTime;
		}


		@Override
		public int compareTo(CarsharingRide o) {
			Long startt = start;
			return startt.compareTo(o.getStart());
		}
		
		
		
	}

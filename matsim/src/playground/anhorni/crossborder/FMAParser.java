package playground.anhorni.crossborder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;

import playground.anhorni.crossborder.verification.Verification;

public class FMAParser extends Parser {
	private Hashtable<Integer, Zone> zones = new Hashtable<Integer, Zone>();
	private Verification verification;

	public FMAParser(NetworkLayer network, String file, Hashtable<Integer, Zone> zones) {
		super(network, file);
		this.zones=zones;
	}
	
	public Verification getVerification() {
		return verification;
	}

	public void setVerification(Verification verification) {
		this.verification = verification;
	}
	
	@Override
	public int parse(String type, int startTime, int actPersonNumber) {		
		// To get a good start time distribution we have to parse the volumes of every
		// matrix before creating and writing relations
		int nrPlans=this.preParse(type, startTime);
		
		int recentlyAddedNumberOfPersons = 0;
		try {
			FileReader file_reader = new FileReader(this.file);
			BufferedReader buffered_reader = new BufferedReader(file_reader);

			// Skip header
			String curr_line = buffered_reader.readLine(); 
			for (int i=0; i<7; i++) {
				curr_line = buffered_reader.readLine();
			}
					
			// create the single files with type+i
			// needed because of limited heap memory
			OnlineWriter onlineWriter=new OnlineWriter();
			onlineWriter.setFileName("output/"+type+startTime);
			onlineWriter.setNetwork(this.network);
			onlineWriter.initWriter();
						
			while ((curr_line = buffered_reader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);

				// Origin	Dest	Vol 
				// 0       	1		2 
				Relation rel=new Relation();
				Zone fromZone=this.zones.get(Integer.parseInt(entries[0].trim()));
				Zone toZone=this.zones.get(Integer.parseInt(entries[1].trim()));
								
				// only use relations where the fromZone is located outside Switzerland
				if (fromZone.getId()>Config.chNumbers) {
					
					rel.setFromZone(fromZone);
					rel.setToZone(toZone);
					
					
					if (!Config.lookAtTransit && rel.checkTransit()) {continue;}
					
					double vol=Config.calibration[startTime]*(double)Double.parseDouble(entries[2].trim());
					rel.setVolume(vol);	
					this.verification.addToAggregatedVolume(type, startTime, vol);
					rel.setType(type);
					rel.setStartTime(startTime);					
					rel.assignPlansToRelations(super.network);
					rel.assignStartingTime(nrPlans, recentlyAddedNumberOfPersons);
					ArrayList<Plan> plansRel=rel.getPlans();
										
					if (rel.checkTransit()) {
						this.verification.addTransitTripsPerHour(startTime, plansRel.size());
					}		
					
					onlineWriter.setPlans(plansRel);
					onlineWriter.write(actPersonNumber+recentlyAddedNumberOfPersons);
					recentlyAddedNumberOfPersons+=plansRel.size();
				}//if
			}
			onlineWriter.endWrite();
			buffered_reader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		this.verification.setXTripsPerHour(type, startTime, 
				recentlyAddedNumberOfPersons);
		return recentlyAddedNumberOfPersons;
	}


	//Instead of parsing the matrix files twice TimeBins could be used! See the remarks in TimeBin.
	public int preParse(String actType, int h) {		
		int recentlyAddedNumberOfPersons = 0;
		double difference=0.0;
		try {
			FileReader file_reader = new FileReader(this.file);
			BufferedReader buffered_reader = new BufferedReader(file_reader);

			// Skip header
			String curr_line = buffered_reader.readLine(); 
			for (int i=0; i<7; i++) {
				curr_line = buffered_reader.readLine();
			}
			
			while ((curr_line = buffered_reader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);

				// Origin	Dest	Vol 
				// 0       	1		2 
				Relation rel=new Relation();
				Zone fromZone=this.zones.get(Integer.parseInt(entries[0].trim()));
				Zone toZone=this.zones.get(Integer.parseInt(entries[1].trim()));
								
				// only use relations where the fromZone is located outside Switzerland
				if (fromZone.getId()>Config.chNumbers) {					
					rel.setFromZone(fromZone);
					rel.setToZone(toZone);
					
					if (!Config.lookAtTransit && rel.checkTransit()) {continue;}
					
					double vol=Config.calibration[h]*(double)Double.parseDouble(entries[2].trim());
					rel.setVolume(vol);	
					rel.setType(actType);					
					rel.assignPlansToRelations(super.network);
					ArrayList<Plan> plansRel=rel.getPlans();					
					recentlyAddedNumberOfPersons+=plansRel.size();
					
					difference+=vol-plansRel.size();
				}//if
			}
			buffered_reader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		this.verification.addXDifference(actType, difference);
		return recentlyAddedNumberOfPersons;
	}
}

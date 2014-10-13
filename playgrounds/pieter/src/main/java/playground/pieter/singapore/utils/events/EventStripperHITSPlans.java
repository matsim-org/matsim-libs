package playground.pieter.singapore.utils.events;

import playground.pieter.singapore.hits.HITSData;
import playground.pieter.singapore.utils.events.listeners.TrimEventWriterHITS;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;


public class EventStripperHITSPlans {
	private ArrayList<String> origIds;
	private final DataBaseAdmin dba;
	private final HITSData hitsData;
	private EventsManager events;
	
	public EventStripperHITSPlans(HITSData hitsData, DataBaseAdmin dba) throws SQLException{
		this.dba = dba;
		this.hitsData = hitsData;
		this.populateList();
	}
	
	private EventStripperHITSPlans(DataBaseAdmin dba) throws SQLException{
		this.dba = dba;
		this.hitsData = null;
		this.populateList();
	}
	
	private void populateList() throws SQLException{
		ResultSet rs;
		try {
			rs = dba.executeQuery("select distinct pax_idx from hitsshort where t10_mode is not null;");
			this.origIds = new ArrayList<>();
			while(rs.next()){
				origIds.add(rs.getString(1));
			}
			this.origIds.trimToSize();
			System.out.println("loaded all pax_idxs.");
		} catch (NoConnectionException e) {
			e.printStackTrace();
		}
	}
	
	void stripEvents(String inFileName, String outfileName){
		this.events = EventsUtils.createEventsManager();
		TrimEventWriterHITS filteredWriter = new TrimEventWriterHITS(outfileName,this.origIds);
		events.addHandler(filteredWriter);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(inFileName);
		filteredWriter.closeFile();
	}
	
	public void writeHITSPersonSummary(String inFileName, String outfileName){
		this.events = EventsUtils.createEventsManager();
//		HITSPersonSummaryWriter hpsw = new HITSPersonSummaryWriter(filename, filter)
//		events.addHandler(filteredWriter);
//		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
//		reader.parse(inFileName);
//		filteredWriter.closeFile();
	}

	
	public static void main(String[] args) {
		System.out.println(new java.util.Date());

		DataBaseAdmin dba;
		try {
			dba = new DataBaseAdmin(new File("data/hitsdb.properties"));
			EventStripperHITSPlans es = new EventStripperHITSPlans(dba);
			es.stripEvents("data/0.events.xml.gz", "data/0.events_stripped.xml.gz");
		} catch (IOException | SQLException | ClassNotFoundException | IllegalAccessException | InstantiationException e) {
			e.printStackTrace();
		}


    }



}

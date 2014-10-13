package playground.pieter.singapore.utils.events;

import playground.pieter.singapore.hits.HITSData;
import playground.pieter.singapore.utils.events.listeners.TravelTimeListener;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;

public class PersonTravelInfoFromEventsToSQL {
	private HashMap<String, Double> paxTravelTimes;
	private HashMap<String, Double> paxTravelDists;
	private HashMap<String,String> paxModes;
	private final DataBaseAdmin dba;

    public PersonTravelInfoFromEventsToSQL(HITSData hitsData, DataBaseAdmin dba)
			throws SQLException {
		this.dba = dba;
	}

	private PersonTravelInfoFromEventsToSQL(DataBaseAdmin dba)
			throws SQLException {
		this.dba = dba;
	}

	void readTravelTimes(String inFileName) {
        EventsManager events = EventsUtils.createEventsManager();
		TravelTimeListener ttl = new TravelTimeListener();
		events.addHandler(ttl);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(inFileName);
		this.paxTravelTimes = ttl.getPaxTravelTimes();
		this.paxModes = ttl.getPaxModes();
	}

	void writePersonSummary(DataBaseAdmin dba, String tableName) {
		try {
			dba.executeStatement(String.format("DROP TABLE IF EXISTS %s;",
					tableName));
			dba.executeStatement(String.format("CREATE TABLE %s("
					+ "id VARCHAR(45)," + "traveltime real,mode text" + ")", tableName));
			for(Entry<String, Double> e:this.paxTravelTimes.entrySet()){
				String sqlInserter = "INSERT INTO %s " +
						"VALUES(\'%s\',%f,\'%s\');";
				dba.executeUpdate(String.format(sqlInserter,tableName,e.getKey(),e.getValue(),paxModes.get(e.getKey())));
				
			}
		} catch (SQLException | NoConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println(new java.util.Date());

		DataBaseAdmin dba;
		String simPath = null;

		JFileChooser chooser = new JFileChooser("./data/zoutput");
//		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.showOpenDialog(new JPanel());
		simPath = chooser.getSelectedFile().getPath();
		String s = JOptionPane.showInputDialog("set the name of the output table");


		
		try {
			dba = new DataBaseAdmin(new File("data/matsim2.properties"));
			PersonTravelInfoFromEventsToSQL es = new PersonTravelInfoFromEventsToSQL(
					dba);
			es.readTravelTimes(simPath);
			es.writePersonSummary(dba, s);
		} catch (IOException | SQLException | ClassNotFoundException | IllegalAccessException | InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }

}

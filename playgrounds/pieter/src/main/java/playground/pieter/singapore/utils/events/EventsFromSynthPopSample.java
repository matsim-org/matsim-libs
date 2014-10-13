package playground.pieter.singapore.utils.events;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;

public class EventsFromSynthPopSample {
	public static void main(String[] args) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException, IOException,
			SQLException, NoConnectionException {
		DataBaseAdmin dba = new DataBaseAdmin(new File(
				"data/matsim2postgres.properties"));
		ResultSet rs = dba.executeQuery("select synth_person_id  from d_demandgen_input.sample_selector ," +
				" d_demandgen_input.matsim2pt1_assignment_09112012 " +
				"where d_demandgen_input.sample_selector.synth_hh_id =" +
				" d_demandgen_input.matsim2pt1_assignment_09112012.synth_hh_id " +
				"and sample_01pct=1;");
		ArrayList<String> list = new ArrayList<>();
		while(rs.next()){
			list.add(rs.getString("synth_person_id"));
		}
		
		
		// TODO Auto-generated method stub
		//
		// ArrayList<String> ids = new ArrayList<String>();
		// ids.add("4101962"); //transit user
		// ids.add("77878"); //car user
		try {
			EventsStripper stripper = new EventsStripper(list);
			stripper.stripEvents(args[0], args[1], Double.parseDouble(args[2]),
					Boolean.parseBoolean(args[3]));
			
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out
					.println("Strips events file to a target events file.\n"
							+ "Arguments:\n"
							+ "inFileName (events file) outfileName (events file) frequency (0-1, i.e. fraction of ids to actually use)\n"
							+ "extracttransitDriverEvents(true/false)");
		}
	}

}

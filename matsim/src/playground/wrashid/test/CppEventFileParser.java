package playground.wrashid.test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TimeZone;

import playground.wrashid.DES.EventLog;
import playground.wrashid.DES.SimulationParameters;

import sun.security.jgss.TokenTracker;

public class CppEventFileParser {

	public static ArrayList<EventLog> eventLog=null;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String eventFileName=args[0];
		CppEventFileParser eventFileParser=new CppEventFileParser();
		eventFileParser.parse(eventFileName);
	}
	
	public void parse(String eventFileName){
		
		CppEventFileParser test = new CppEventFileParser();
		eventLog = test.parseFile(eventFileName);
		for(int i=0;i<eventLog.size();i++) {
			//eventLog.get(i).print();
		}
	}

	public ArrayList<EventLog> parseFile(String filePath) {
		ArrayList<EventLog> rows = new ArrayList<EventLog>();
		try {
			FileReader fr = new FileReader(filePath);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			StringTokenizer tonckenizer = null;
			line = br.readLine();
			while (line != null) {
				
				tonckenizer = new StringTokenizer(line);
				String token = null;
				token = tonckenizer.nextToken();
				double first = Double.parseDouble(token);
				token = tonckenizer.nextToken();
				int second = Integer.parseInt(token);
				token = tonckenizer.nextToken();
				int third = Integer.parseInt(token);
				token = tonckenizer.nextToken();
				int fourth = Integer.parseInt(token);
				token = tonckenizer.nextToken();
				int fifth = Integer.parseInt(token);
				token = tonckenizer.nextToken();
				int sixth = Integer.parseInt(token);
				token = tonckenizer.nextToken();
				String eventType = token;
				
				
				// there is one eventType called 'enter net' => it is split into two tockens, so need to take that into account
				if (tonckenizer.hasMoreTokens()){
					token = tonckenizer.nextToken();
					eventType += " " + token;
				}
				
				
				//change type label
				if (eventType.equalsIgnoreCase("starting")){
					eventType=SimulationParameters.START_LEG;
				} else if (eventType.equalsIgnoreCase("end")){
					eventType=SimulationParameters.END_LEG;
				} else if (eventType.equalsIgnoreCase("enter")){
					eventType=SimulationParameters.ENTER_LINK;
				} else if (eventType.equalsIgnoreCase("leave")){
					eventType=SimulationParameters.LEAVE_LINK;
				}
				

				// ignore 'enter net' events (which seem useless)
				if (!eventType.equalsIgnoreCase("enter net")){
					EventLog eventLog=new EventLog(first,second,third,fourth,fifth,sixth,eventType);
					rows.add(eventLog);
				}
				
				line = br.readLine();
			}
		} catch (Exception ex) {
			System.out.println(ex);
		}

		return rows;
	}

}

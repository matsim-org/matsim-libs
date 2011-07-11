package playground.sergioo.GTFS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

public class FixerGTFSFiles {

	/**
	 * Changes the calendar references in the trips file
	 * @throws IOException
	 */
	public static void fixGTFSBusSingapore() throws IOException {
		File oldFile=new File("C:/Users/sergioo/Desktop/Desktop/buses/trips2.txt");
		File newFile=new File("C:/Users/sergioo/Desktop/Desktop/buses/trips.txt");
		BufferedReader reader = new BufferedReader(new FileReader(oldFile));
		PrintWriter writer = new PrintWriter(newFile);
		String line = reader.readLine();
		writer.println(line);
		line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",");
			if(parts[1].endsWith("saturday"))
				parts[1]="saturday";
			else if(parts[1].endsWith("sunday"))
				parts[1]="sunday";
			else if(parts[1].endsWith("weekday"))
				parts[1]="weekday";
			writer.print(parts[0]);
			int i=1;
			for(;i<parts.length;i++)
				writer.print(","+parts[i]);
			for(;i<5;i++)
				writer.print(",");
			writer.println();
			line=reader.readLine();
		}
		writer.close();
		reader.close();
	}
	/**
	 * Erases the E and S routes
	 * @throws IOException
	 */
	public static void fixGTFSBusSingapore2() throws IOException {
		Collection<String> trips = new HashSet<String>();
		File oldFile=new File("./data/gtfs/buses/trips2.txt");
		File newFile=new File("./data/gtfs/buses/trips.txt");
		BufferedReader reader = new BufferedReader(new FileReader(oldFile));
		PrintWriter writer = new PrintWriter(newFile);
		String line = reader.readLine();
		writer.println(line);
		line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",");
			if(!parts[2].endsWith("-S") && !parts[2].endsWith("-E")) {
				trips.add(parts[2]);
				writer.println(line);
			}
			else if(parts[2].endsWith("-E") && !trips.contains(parts[2].substring(0, parts[2].lastIndexOf('-')))) {
				String tripId = parts[2].substring(0, parts[2].lastIndexOf('-'));
				trips.add(tripId);
				writer.print(parts[0]+","+parts[1]+","+tripId);
				int i=3;
				for(;i<parts.length;i++)
					writer.print(","+parts[i]);
				for(;i<5;i++)
					writer.print(",");
				writer.println();
			}
			line=reader.readLine();
		}
		writer.close();
		reader.close();
		Map<String,String> startDepartures = new HashMap<String,String>();
		Map<String,String> endDepartures = new HashMap<String,String>();
		oldFile=new File("./data/gtfs/buses/stop_times2.txt");
		newFile=new File("./data/gtfs/buses/stop_times.txt");
		reader = new BufferedReader(new FileReader(oldFile));
		writer = new PrintWriter(newFile);
		line = reader.readLine();
		writer.println(line);
		line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",");
			if(parts[0].endsWith("-S")) {
				String tripId = parts[0].substring(0, parts[0].lastIndexOf('-'));
				if(startDepartures.get(tripId)==null)
					startDepartures.put(tripId, parts[2]);
				writer.print(tripId);
				for(int i=1;i<parts.length;i++)
					writer.print(","+parts[i]);
				writer.println();
			}
			if(parts[0].endsWith("-E")) {
				String tripId = parts[0].substring(0, parts[0].lastIndexOf('-'));
				if(endDepartures.get(tripId)==null) {
					String[] parts3 = parts[2].split(":");
					int hour = Integer.parseInt(parts3[0]);
					if(hour<12) {
						hour+=24;
						endDepartures.put(tripId, Integer.toString(hour)+":"+parts3[1]+":"+parts3[2]);
					}
					else
						endDepartures.put(tripId, parts[2]);
				}
			}
			line=reader.readLine();
		}
		writer.close();
		reader.close();
		oldFile=new File("./data/gtfs/buses/frequencies2.txt");
		newFile=new File("./data/gtfs/buses/frequencies.txt");
		reader = new BufferedReader(new FileReader(oldFile));
		writer = new PrintWriter(newFile);
		line = reader.readLine();
		writer.println(line);
		String previous = line;
		line = reader.readLine();
		String[] parts = line.split(",");
		String tripId = parts[0];
		previous = parts[0]+","+startDepartures.get(parts[0]);
		for(int i=2;i<parts.length;i++)
			previous += ","+parts[i];
		line=reader.readLine();
		while(line!=null) {
			parts = line.split(",");
			if(!parts[0].equals(tripId)) {
				String[] parts2 = previous.split(",");
				for(int i=0;i<2;i++)
					writer.print(parts2[i]+",");
				writer.println(endDepartures.get(parts2[0])+","+parts2[3]);
				tripId = parts[0];
				previous = parts[0]+","+startDepartures.get(parts[0]);
				for(int i=2;i<parts.length;i++)
					previous += ","+parts[i];
			}
			else {
				writer.println(previous);
				previous = line;
			}
			line=reader.readLine();
		}
		String[] parts2 = previous.split(",");
		for(int i=0;i<2;i++)
			writer.print(parts2[i]+",");
		writer.println(endDepartures.get(parts2[0])+","+parts2[3]);
		writer.close();
		reader.close();
	}
	/**
	 * Changes the calendar references in the trips file
	 * @throws IOException
	 */
	public static void fixGTFSTrainSingapore() throws IOException {
		File oldFile=new File("C:/Users/sergioo/Documents/2011/Work/FCL/Operations/Data/GoogleTransitFeed/ProcessedData/Trains/trips2.txt");
		File newFile=new File("C:/Users/sergioo/Documents/2011/Work/FCL/Operations/Data/GoogleTransitFeed/ProcessedData/Trains/trips.txt");
		BufferedReader reader = new BufferedReader(new FileReader(oldFile));
		PrintWriter writer = new PrintWriter(newFile);
		String line = reader.readLine();
		writer.println(line);
		line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",");
			if(parts[1].endsWith("weeksatday"))
				parts[1]="weeksatday";
			else if(parts[1].endsWith("sunday"))
				parts[1]="sunday";
			else if(parts[1].endsWith("weekday"))
				parts[1]="weekday";
			else if(parts[1].contains("daily"))
				parts[1]="daily";
			else
				System.out.println("Error");
			writer.print(parts[0]);
			int i=1;
			for(;i<parts.length;i++)
				writer.print(","+parts[i]);
			for(;i<5;i++)
				writer.print(",");
			writer.println();
			line=reader.readLine();
		}
		writer.close();
		reader.close();
	}
	/**
	 * Erases the E and S routes
	 * @throws IOException
	 */
	public static void fixGTFSTrainSingapore2() throws IOException {
		SortedMap<String,TripAux> trips = new TreeMap<String,TripAux>();
		File oldFile=new File("./data/gtfs/trains/trips2.txt");
		File newFile=new File("./data/gtfs/trains/trips.txt");
		BufferedReader reader = new BufferedReader(new FileReader(oldFile));
		PrintWriter writer = new PrintWriter(newFile);
		String line = reader.readLine();
		writer.println(line);
		line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",");
			if(!parts[2].contains("_first") && !parts[2].contains("_last")) {
				TripAux tripAux = trips.get(parts[2]);
				if(tripAux==null) {
					trips.put(parts[2],new TripAux());
					tripAux = trips.get(parts[2]);
				}
				tripAux.setLine(line);
			}
			else if(parts[2].contains("_first")) {
				String tripId=parts[2].replaceAll("_first", "");
				TripAux tripAux = trips.get(tripId);
				if(tripAux==null) {
					tripAux = trips.put(parts[2],new TripAux());
					tripAux = trips.get(parts[2]);
					tripAux.setLine(line);
				}
				tripAux.addFirst(parts[2]);
			}
			else {
				String tripId=parts[2].replaceAll("_last", "");
				TripAux tripAux = trips.get(tripId);
				if(tripAux==null) {
					tripAux = trips.put(parts[2],new TripAux());
					tripAux = trips.get(parts[2]);
					tripAux.setLine(line);
				}
				tripAux.addLast(parts[2]);
			}
			line=reader.readLine();
		}
		for(Entry<String, TripAux> trip:trips.entrySet()) {
			writer.println(trip.getValue().getLine());
			System.out.println(trip.getKey()+" "+trip.getValue().getFirsts().size()+" "+trip.getValue().getLasts().size());
		}
		writer.close();
		reader.close();
		Map<String,String> startDepartures = new HashMap<String,String>();
		Map<String,String> endDepartures = new HashMap<String,String>();
		oldFile=new File("./data/gtfs/trains/stop_times2.txt");
		newFile=new File("./data/gtfs/trains/stop_times.txt");
		reader = new BufferedReader(new FileReader(oldFile));
		writer = new PrintWriter(newFile);
		line = reader.readLine();
		writer.println(line);
		line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",");
			if(parts[0].contains("_first")) {
				String tripId = parts[0].replaceAll("_first", "");
				if(trips.containsKey(tripId)) {
					if(startDepartures.get(tripId)==null)
						startDepartures.put(tripId, parts[2]);
					writer.print(tripId);
					for(int i=1;i<parts.length;i++)
						writer.print(","+parts[i]);
					writer.println();
				}
			}
			else if(parts[0].contains("_last")) {
				String tripId = parts[0].replaceAll("_last", "");;
				if(trips.containsKey(tripId)) {
					if(endDepartures.get(tripId)==null)
						endDepartures.put(tripId, parts[2]);
				}
				else if(trips.containsKey(parts[0]))
					writer.println(line);
			}
			else {
				if(trips.get(parts[0]).getFirsts().size()==0)
					writer.println(line);
			}
			line=reader.readLine();
		}
		writer.close();
		reader.close();
		oldFile=new File("./data/gtfs/trains/frequencies2.txt");
		newFile=new File("./data/gtfs/trains/frequencies.txt");
		reader = new BufferedReader(new FileReader(oldFile));
		writer = new PrintWriter(newFile);
		line = reader.readLine();
		writer.println(line);
		String previous = line;
		line = reader.readLine();
		String[] parts = line.split(",");
		String tripId = parts[0];
		previous = parts[0]+","+startDepartures.get(parts[0]);
		for(int i=2;i<parts.length;i++)
			previous += ","+parts[i];
		line=reader.readLine();
		while(line!=null) {
			parts = line.split(",");
			if(!parts[0].equals(tripId)) {
				String[] parts2 = previous.split(",");
				if(endDepartures.get(parts2[0])!=null) {
					for(int i=0;i<2;i++)
						writer.print(parts2[i]+",");
					writer.println(endDepartures.get(parts2[0])+","+parts2[3]);
				}
				else
					writer.println(previous);
				tripId = parts[0];
				previous = parts[0]+","+startDepartures.get(parts[0]);
				for(int i=2;i<parts.length;i++)
					previous += ","+parts[i];
			}
			else {
				writer.println(previous);
				previous = line;
			}
			line=reader.readLine();
		}
		String[] parts2 = previous.split(",");
		if(endDepartures.get(parts2[0])!=null) {
			for(int i=0;i<2;i++)
				writer.print(parts2[i]+",");
			writer.println(endDepartures.get(parts2[0])+","+parts2[3]);
		}
		else
			writer.println(previous);
		writer.close();
		reader.close();
	}
}

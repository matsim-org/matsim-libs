package playground.singapore.springcalibration.run;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.QuadTree;

public class TaxiUtils {
	
	private final static Logger log = Logger.getLogger(TaxiUtils.class);
	public static String wait4Taxi = "wait4taxi";
	public static String taxi_walk = "walk2taxi";
	private QuadTree<TaxiWaitingTime> taxiWaitingTimesQuadTree;
	
	
	public TaxiUtils(SingaporeConfigGroup config) {
		this.intialize(config);
	}
	
	private void intialize(SingaporeConfigGroup config) {
		
		String waitingTimesFile = config.getTaxiWaitingTimeFile();
		log.info("Initializing TaxiUtils ... with file " + waitingTimesFile);
		ArrayList<TaxiWaitingTime> tmpMap = this.readWaitingTimesFile(waitingTimesFile);
		this.taxiWaitingTimesQuadTree = this.createQuadTree(tmpMap);
			
//		try {
//		    Thread.sleep(20000);                 //1000 milliseconds is one second.
//		} catch(InterruptedException ex) {
//		    Thread.currentThread().interrupt();
//		}
	}
	
	private ArrayList<TaxiWaitingTime> readWaitingTimesFile(String waitingTimesFile) {
		File file = new File(waitingTimesFile);
	    List<String> lines;
	    ArrayList<TaxiWaitingTime> tmpMap = new ArrayList<TaxiWaitingTime>();
		try {
			lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
			log.info(lines.size() + " lines");			
			int cnt = 0;	
			for (String line : lines) {
				if (cnt == 0) {
					cnt++;
					continue; // skip header
				}			
				TaxiWaitingTime waitingTime = new TaxiWaitingTime();				
				String[] elements = line.split(";", -1);	
				
				double x = Double.parseDouble(elements[0]);
				double y = Double.parseDouble(elements[1]);
				
				Coord centroid = new Coord(x,y);
				waitingTime.setCentroid(centroid);
				
				for (int i = 0; i < 24; i++) {
					double t = Double.parseDouble(elements[14 + i]); // given in minutes
					waitingTime.setWaitingTime(i, t * 60.0);
				}				
				tmpMap.add(waitingTime);					        
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return tmpMap;
	}
	
	public double getWaitingTime(Coord coord, int hour) {
		hour = Math.min(hour, 23);
		return this.taxiWaitingTimesQuadTree.getClosest(coord.getX(), coord.getY()).getWaitingTime(hour);
	}
	
	private QuadTree<TaxiWaitingTime> createQuadTree(ArrayList<TaxiWaitingTime> tmpMap) {		
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
				
		for (TaxiWaitingTime waitingTime : tmpMap) {
			double x = waitingTime.getCentroid().getX();
			double y = waitingTime.getCentroid().getY();
			
			if (x < minX) {
				minX = x;
			}
			if (y < minY) {
				minY = y;
			}
			if (x > maxX) {
				maxX = x;
			}
			if (y > maxY) {
				maxY = y;
			}
		}		
		QuadTree<TaxiWaitingTime> quadTree = new QuadTree<TaxiWaitingTime>(minX, minY, maxX, maxY);
		
		for (TaxiWaitingTime waitingTime: tmpMap) {					
			quadTree.put(waitingTime.getCentroid().getX(), waitingTime.getCentroid().getY(), waitingTime);	
		}
		return quadTree;
	}

}

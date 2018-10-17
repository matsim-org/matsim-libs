package org.matsim.contrib.accessibility;

import java.io.BufferedReader;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesUtils;

public class AccessibilityAVUtils {
	private static final Logger LOG = Logger.getLogger(AccessibilityAVUtils.class);
 	private static final String OUTPUT_FILE = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/av/waittimes_500_access_grid/merged_10.csv";
 	//
	public static boolean avMode = false; // <--- Adjust
	//
	
	public static ActivityFacilities createActivityFacilitiesWithWaitingTime() {
		ActivityFacilities activityFacilities = FacilitiesUtils.createActivityFacilities() ;
		double waitingTimeIfNotReported = 30*60; // <--- Adjust
		double additionalTimeOffset = 10*60; // <--- Adjust
		int columnIndex = 15;
		
		LOG.info("read column 8");
		BufferedReader br = null;
		try {
			br = IOUtils.getBufferedReader(OUTPUT_FILE);
			String header = br.readLine(); // header
			String[] headerArray = header.split(";");
			if (!headerArray[columnIndex].equals("8")) {
				throw new RuntimeException("wrong column index");
			}
			String line = br.readLine(); // first line
			int facId = 1;
			
			while (line != null) {
				String[] s = line.split(";");
				
				// create activityFacility
				double coordX = Double.valueOf(s[0]);
				double coordY = Double.valueOf(s[1]);
				ActivityFacility actFac = activityFacilities.getFactory().createActivityFacility(Id.create(facId, ActivityFacility.class), 
						new Coord(coordX, coordY));
				
				double waitingTime;
				if (s.length <= columnIndex ) {
					waitingTime = waitingTimeIfNotReported;
				} else {
					waitingTime = Double.valueOf(s[columnIndex]);
				}
				//
				if (waitingTime == 0) {
					waitingTime = waitingTimeIfNotReported;
				}
				//
				
				actFac.getAttributes().putAttribute("waitingTime", waitingTime);
//				actFac.getAttributes().putAttribute("waitingTime", waitingTime + additionalTimeOffset);
//				actFac.getAttributes().putAttribute("waitingTime", additionalTimeOffset);
				
				activityFacilities.addActivityFacility(actFac);
				
				line = br.readLine();
				facId++;
			}
			br.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return activityFacilities;
	}
}  
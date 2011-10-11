package playground.sergioo.CountsFileGenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

public class CountsFileGenerator {

	private static final String SEPARATOR = "\t";
	private static final String[] MODES = {"M/Cycle","Car","Taxi","LGV","HGV","Private Bus","Public Bus","Bus Total","Others","Observation","Total"};
	private enum TIMES {
		EIGHT(8,9),
		NINE(9,10),
		TEN(10,11),
		EIGHTEEN(18,20),
		NINETEEN(19,21),
		TWENTY(20,22);
		private final int hour;
		private final int position;
		private TIMES(final int hour, final int position) {
			this.hour = hour;
			this.position = position;
		}
		public int getHour() {
			return this.hour;
		}
		public int getPosition() {
			return this.position;
		}
	}
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(final String[] args) throws IOException {
		Map<String, Coord> countLocations = new HashMap<String, Coord>();
		CoordinateTransformation cT = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_SVY21, TransformationFactory.WGS84_UTM48N);
		BufferedReader readerLocations = new BufferedReader(new FileReader(new File("./data/counts/CountLocations.txt")));
		readerLocations.readLine();
		String line  = readerLocations.readLine();
		while(line!=null) {
			String[] parts = line.split(SEPARATOR);
			String code = parts[1];
			Coord location = new CoordImpl(Double.parseDouble(parts[5]), Double.parseDouble(parts[6]));
			location = cT.transform(location);
			countLocations.put(code, location);
			line = readerLocations.readLine();
		}
		readerLocations.close();
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(scenario);
		matsimNetworkReader.readFile("./data/networks/singapore1.xml");
		Network network = scenario.getNetwork();
		Map<String,Counts> allCounts = new HashMap<String, Counts>();
		for(String mode:MODES)
			allCounts.put(mode, new Counts());
		BufferedReader readerLinks = new BufferedReader(new FileReader(new File("./data/counts/CountLinks.txt")));
		line  = readerLinks.readLine();
		while(line!=null) {
			String[] parts = line.split(SEPARATOR);
			for(String mode:MODES) {
				Count count = allCounts.get(mode).createCount(new IdImpl(parts[2]), parts[0]+SEPARATOR+parts[1]);
				count.setCoord(new CoordImpl(Double.parseDouble(parts[3]), Double.parseDouble(parts[4])));
			}
			line = readerLinks.readLine();
		}
		readerLinks.close();
		BufferedReader readerValues = new BufferedReader(new FileReader(new File("./data/counts/CountValues.txt")));
		readerValues.readLine();
		line  = readerValues.readLine();
		String[] parts = line.split(SEPARATOR);
		while(line!=null) {
			String code = parts[3];
			String name = parts[5];
			String movement = parts[6];
			if(name.equals("5pm to 8pm"))
				System.out.println(line);
			Map<String,Count> aCounts = null;
			for(Count count:allCounts.get(MODES[0]).getCounts().values())
				if(count.getCsId().equals(code+SEPARATOR+movement)) {
					aCounts = new HashMap<String, Count>();
					for(String mode:MODES)
						aCounts.put(mode,allCounts.get(mode).getCounts().get(count.getLocId()));
				}
			if(aCounts==null) {
				Entry<String, Coord> entryLocation = null;
				for(Entry<String, Coord> countLocationE:countLocations.entrySet())
					if(countLocationE.getKey().equals(code))
						entryLocation = countLocationE;
					Coord location = entryLocation == null?new CoordImpl(0,0):entryLocation.getValue();
					CountsWindow window = new CountsWindow(name+"     "+(entryLocation == null?"":entryLocation.getKey())+"     "+code+"     "+movement, network);
					for(Coord point:countLocations.values())
						window.addPoint(point);
					window.selectPoint(location);
					window.setVisible(true);
					while(window.isVisible())
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					Link selected = window.getSelectedLink();
					if(window.isReadyToExit() && selected!=null) {
						PrintWriter writerLinks = new PrintWriter(new FileWriter(new File("./data/counts/CountLinks.txt"),true));
						writerLinks.println(code+SEPARATOR+movement+SEPARATOR+selected.getId().toString()+SEPARATOR+location.getX()+SEPARATOR+location.getY());
						writerLinks.flush();
						writerLinks.close();
						aCounts = new HashMap<String, Count>();
						for(String mode:MODES) {
							Count count = allCounts.get(mode).createCount(selected.getId(), code+SEPARATOR+movement);
							aCounts.put(mode,count);
							count.setCoord(location);
						}
					}
			}
			while(line!=null && parts[3].equals(code) && parts[6].equals(movement)) {
				if(aCounts!=null) {
					Count modeCount = aCounts.get(parts[8]);
					for(TIMES time:TIMES.values()) {
						String value = parts[time.getPosition()];
						if(!value.equals("-"))
							modeCount.createVolume(time.getHour(), Double.parseDouble(value));
					}
				}
				line = readerValues.readLine();
				if(line!=null)
					parts = line.split(SEPARATOR);
			}
			if(aCounts==null)
				System.out.println("Error: "+code+SEPARATOR+movement+SEPARATOR+name);
		}
		readerValues.close();
		for(String mode:MODES) {
			CountsWriter writerCounts = new CountsWriter(allCounts.get(mode));
			mode = mode.replaceAll(" ", "");
			mode = mode.replaceAll("/", "");
			writerCounts.write("./data/counts/counts"+mode+".xml");
		}
	}

}

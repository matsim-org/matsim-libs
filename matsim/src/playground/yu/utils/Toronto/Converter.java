/**
 * 
 */
package playground.yu.utils.Toronto;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.basic.v01.IdImpl;
import org.matsim.basic.v01.BasicLeg.Mode;
import org.matsim.gbl.Gbl;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.utils.io.IOUtils;
import org.matsim.utils.misc.Time;

/**
 * @author yu
 * 
 */
public class Converter {
	public static class ZoneXY {
		private String zoneId, x, y;

		public ZoneXY(String zoneId, String x, String y) {
			this.zoneId = zoneId;
			this.x = x;
			this.y = y;
		}

		public String getX() {
			return x;
		}

		public String getY() {
			return y;
		}
	}

	private Population pop;

	private Map<String, ZoneXY> zoneXYs;

	private String tmpPersonId = "";

	private int tmpEndingTime;

	private String[] tmpTabs = null;

	/**
	 * 
	 */
	public Converter() {
		// TODO Auto-generated constructor stub
	}

	public void readLine(String line) {
		String[] tabs = line.split("\t");
		String personId = tabs[0] + "-" + tabs[1];
		int ending;
		try {
			if (tmpPersonId.equals(personId)) {
				ZoneXY zoneXY = zoneXYs.get(tabs[9]);
				Plan pl = pop.getPerson(personId).getSelectedPlan();
				ending = Integer.parseInt(tabs[3]);
				int dur = 0;
				if (ending % 100 < tmpEndingTime % 100) {
					dur = 60 + (ending % 100) - (tmpEndingTime % 100) + ending
							/ 100 * 100 - tmpEndingTime / 100 * 100 - 100;
				} else {
					dur = ending - tmpEndingTime;
				}

				pl.createLeg(Mode.car, Time.UNDEFINED_TIME,
						Time.UNDEFINED_TIME, Time.UNDEFINED_TIME);
				pl.createAct(tabs[7], zoneXY.getX(), zoneXY.getY(), null, null,
						(ending / 100 < 10) ? "0" : "" + ending / 100 + ":"
								+ ending % 100, dur / 100 + ":" + dur % 100,
						null);

			} else {
				if (!pop.getPersons().isEmpty()) {
					Plan tmpPl = pop.getPerson(tmpPersonId).getSelectedPlan();
					tmpPl.createLeg(Mode.car, Time.UNDEFINED_TIME,
							Time.UNDEFINED_TIME, Time.UNDEFINED_TIME);
					ZoneXY lastZoneXY = zoneXYs.get(tmpTabs[12]);
					tmpPl.createAct(tmpTabs[10], lastZoneXY.getX(), lastZoneXY
							.getY(), null, null, null, null, null);
				}

				Person p = new Person(new IdImpl(personId));
				pop.addPerson(p);
				Plan pl = new Plan(p);
				ZoneXY zoneXY = zoneXYs.get(tabs[9]);
				ending = Integer.parseInt(tabs[3]);
				pl
						.createAct(tabs[7], zoneXY.getX(), zoneXY.getY(), null,
								null, (((ending / 100 < 10) ? "0" : "")
										+ ending / 100 + ":" + ending % 100),
								null, null);
				p.addPlan(pl);
			}
			tmpPersonId = personId;
			tmpEndingTime = ending;
			tmpTabs = tabs;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 */
	public static void main(String[] args) {
		String oldPlansFilename = "input/Toronto/fout_chains210.txt";
		String newPlansFilename = "output/Toronto/example.xml.gz";
		String zoneFilename = "input/Toronto/centroids.txt";

		Gbl.createConfig(null);

		Converter c = new Converter();

		c.zoneXYs = new HashMap<String, ZoneXY>();

		// reading zone.txt "zone,x,y" ...
		BufferedReader zoneReader;
		try {
			zoneReader = IOUtils.getBufferedReader(zoneFilename);
			String zoneLine = "";
			do {

				zoneLine = zoneReader.readLine();
				if (zoneLine != null) {
					String[] zoneLines = zoneLine.split(" ");
					List<String> ss = new ArrayList<String>();
					for (int i = 0; i < zoneLines.length; i++) {
						if (!zoneLines[i].equals("")) {
							ss.add(zoneLines[i]);
						}
					}
					String zone = ss.get(1);
					c.zoneXYs.put(zone, new ZoneXY(zone, ss.get(2), ss.get(3)));
				}
			} while (zoneLine != null);
			zoneReader.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		//		
		c.pop = new Population();
		try {
			BufferedReader reader = IOUtils.getBufferedReader(oldPlansFilename);
			PopulationWriter writer = new PopulationWriter(c.pop,
					newPlansFilename, "v4", 1.0);
			writer.writeStartPlans();
			String line = reader.readLine();
			do {
				line = reader.readLine();
				if (line != null) {
					c.readLine(line);
				}
			} while (line != null);
			reader.close();
			writer.writePersons();
			writer.writeEndPlans();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

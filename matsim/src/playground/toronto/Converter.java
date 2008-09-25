/**
 * 
 */
package playground.toronto;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.basic.v01.IdImpl;
import org.matsim.basic.v01.BasicLeg.Mode;
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.gbl.Gbl;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.utils.WorldUtils;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.io.IOUtils;
import org.matsim.utils.misc.Time;
import org.matsim.world.Layer;
import org.matsim.world.Zone;
import org.matsim.world.ZoneLayer;

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

		public String getZoneId() {
			return zoneId;
		}
	}

	private Population pop;

	public void setPop(Population pop) {
		this.pop = pop;
	}

	private ZoneLayer zones = null;

	public Layer getLayer() {
		return zones;
	}

	public void setZones(ZoneLayer zones) {
		this.zones = zones;
	}

	private Map<String, ZoneXY> zoneXYs;

	public Map<String, ZoneXY> getZoneXYs() {
		return zoneXYs;
	}

	public void setZoneXYs(Map<String, ZoneXY> zoneXYs) {
		this.zoneXYs = zoneXYs;
	}

	private String tmpPersonId = "";

	private int tmpEndingTime;

	private String[] tmpTabs = null;

	private double tmpHomeX = 0, tmpHomeY = 0;

	/**
	 * 
	 */
	public Converter() {
		// TODO Auto-generated constructor stub
	}

	public static String getEndingTimeS(String endingS) {
		int endingI = Integer.parseInt(endingS);
		return (((endingI / 100) < 10) ? "0" : "") + (endingI / 100) + ":"
				+ (endingI % 100);
	}

	public static double getEndingTimeD(String endingS) {
		int endingI = Integer.parseInt(endingS);
		return 3600 * (endingI / 100) + 60 * (endingI - endingI / 100 * 100);
	}

	public void readLine(String line) {
		String[] tabs = line.split("\t");
		String personId = tabs[0] + "-" + tabs[1];
		int ending;
		try {
			if (tmpPersonId.equals(personId)) {
				// ZoneXY zoneXY = zoneXYs.get(tabs[9]);
				Plan pl = pop.getPerson(personId).getSelectedPlan();
				ending = Integer.parseInt(tabs[3]);
				int dur = 0;
				if (ending % 100 < tmpEndingTime % 100) {
					dur = 60 + (ending % 100) - (tmpEndingTime % 100) + ending
							/ 100 * 100 - tmpEndingTime / 100 * 100 - 100;
				} else {
					dur = ending - tmpEndingTime;
				}

				pl.createLeg(Mode.car, getEndingTimeD(tmpTabs[3]),
						Time.UNDEFINED_TIME, Time.UNDEFINED_TIME);
				double x = getActX(tabs[9]);
				double y = getActY(tabs[9]);
				if (tabs[7].equals("H")) {
					x = tmpHomeX;
					y = tmpHomeY;
				}
				pl.createAct(tabs[7], x, y, null, null,
						getEndingTimeS(tabs[3]), dur / 100 + ":" + dur % 100,
						null);

			} else {
				if (!pop.getPersons().isEmpty()) {
					Person p = pop.getPerson(tmpPersonId);
					Plan tmpPl = p.getSelectedPlan();
					tmpPl.createLeg(Mode.car, getEndingTimeD(tmpTabs[3]),
							Time.UNDEFINED_TIME, Time.UNDEFINED_TIME);
					// ZoneXY lastZoneXY = zoneXYs.get(tmpTabs[12]);

					double x = getActX(tmpTabs[12]);
					double y = getActY(tmpTabs[12]);
					if (tmpTabs[10].equals("H")) {
						x = tmpHomeX;
						y = tmpHomeY;
						// System.out.println("tmpHomeX : " + tmpHomeX
						// + "\t|\ttmpHomeY : " + tmpHomeY + "\nx : " + x
						// + "\t|\ty : " + y);
					}
					tmpPl.createAct(tmpTabs[10], x, y, null, null, null, null,
							null);
					Plan nonCarPlan = new Plan(p);
					nonCarPlan.copyPlan(tmpPl);
					for (LegIterator li = nonCarPlan.getIteratorLeg(); li
							.hasNext();) {
						li.next().setMode(Mode.pt);
					}
					p.addPlan(nonCarPlan);
				}

				Person p = new Person(new IdImpl(personId));
				Plan pl = new Plan(p);
				// ZoneXY zoneXY = zoneXYs.get(tabs[9]);
				ending = Integer.parseInt(tabs[3]);

				tmpHomeX = getActX(tabs[9]);
				tmpHomeY = getActY(tabs[9]);
				pl.createAct(tabs[7], tmpHomeX, tmpHomeY, null, null,
						getEndingTimeS(tabs[3]), null, null);
				p.addPlan(pl);
				pop.addPerson(p);
			}
			tmpPersonId = personId;
			tmpEndingTime = ending;
			tmpTabs = tabs;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public double getActX(String zoneId) {
		return this.getRandomCoordInZone(zoneId).getX();
	}

	public double getActY(String zoneId) {
		return this.getRandomCoordInZone(zoneId).getY();
	}

	private Coord getRandomCoordInZone(String zoneId) {
		return WorldUtils.getRandomCoordInZone(
				(Zone) zones.getLocation(zoneId), zones);

	}

	public void createZones() {
		for (ZoneXY zxy : zoneXYs.values()) {
			createZone(zxy);
		}
		zoneXYs.clear();// //////////////////////////////////////////////
	}

	public void createZone(ZoneXY zxy) {
		zones.createZone(zxy.getZoneId(), zxy.getX(), zxy.getY(), null, null,
				null, null, null, null);
	}

	/**
	 */
	public static void main(String[] args) {
		String oldPlansFilename = "input/Toronto/fout_chains210.txt";
		String newPlansFilename = "output/Toronto/example.xml.gz";
		String zoneFilename = "input/Toronto/centroids.txt";

		Converter c = new Converter();

		Gbl.createConfig(null);
		c.setZones((ZoneLayer) Gbl.getWorld().createLayer(new IdImpl("zones"),
				"toronto_test"));

		c.setZoneXYs(new HashMap<String, ZoneXY>());

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
					c.getZoneXYs().put(zone,
							new ZoneXY(zone, ss.get(2), ss.get(3)));
				}
			} while (zoneLine != null);
			zoneReader.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		// create zones
		c.createZones();

		//		
		c.setPop(new Population());
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
		System.out.println("done.");
	}

}

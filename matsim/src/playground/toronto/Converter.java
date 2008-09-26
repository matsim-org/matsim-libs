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
		private final String zoneId, x, y;

		public ZoneXY(final String zoneId, final String x, final String y) {
			this.zoneId = zoneId;
			this.x = x;
			this.y = y;
		}

		public String getX() {
			return this.x;
		}

		public String getY() {
			return this.y;
		}

		public String getZoneId() {
			return this.zoneId;
		}
	}

	private Population pop;

	public void setPop(final Population pop) {
		this.pop = pop;
	}

	private ZoneLayer zones = null;

	public Layer getLayer() {
		return this.zones;
	}

	public void setZones(final ZoneLayer zones) {
		this.zones = zones;
	}

	private Map<String, ZoneXY> zoneXYs;

	public Map<String, ZoneXY> getZoneXYs() {
		return this.zoneXYs;
	}

	public void setZoneXYs(final Map<String, ZoneXY> zoneXYs) {
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

	public static String getEndingTimeS(final String endingS) {
		int endingI = Integer.parseInt(endingS);
		return (((endingI / 100) < 10) ? "0" : "") + (endingI / 100) + ":"
				+ (endingI % 100);
	}

	public static double getEndingTimeD(final String endingS) {
		int endingI = Integer.parseInt(endingS);
		return 3600 * (endingI / 100) + 60 * (endingI - endingI / 100 * 100);
	}

	public void readLine(final String line) {
		String[] tabs = line.split("\t");
		String personId = tabs[0] + "-" + tabs[1];
		int ending;
		try {
			if (this.tmpPersonId.equals(personId)) {
				// ZoneXY zoneXY = zoneXYs.get(tabs[9]);
				Plan pl = this.pop.getPerson(personId).getSelectedPlan();
				ending = Integer.parseInt(tabs[3]);
				int dur = 0;
				if (ending % 100 < this.tmpEndingTime % 100) {
					dur = 60 + (ending % 100) - (this.tmpEndingTime % 100) + ending
							/ 100 * 100 - this.tmpEndingTime / 100 * 100 - 100;
				} else {
					dur = ending - this.tmpEndingTime;
				}

				pl.createLeg(Mode.car, getEndingTimeD(this.tmpTabs[3]),
						Time.UNDEFINED_TIME, Time.UNDEFINED_TIME);
				Coord tmpCoord = getRandomCoordInZone(tabs[9]);
				double x = tmpCoord.getX();
				double y = tmpCoord.getY();
				if (tabs[7].equals("H")) {
					x = this.tmpHomeX;
					y = this.tmpHomeY;
				}
				pl.createAct(tabs[7], x, y, null, null,
						getEndingTimeS(tabs[3]), dur / 100 + ":" + dur % 100,
						null);

			} else {
				if (!this.pop.getPersons().isEmpty()) {
					Person p = this.pop.getPerson(this.tmpPersonId);
					Plan tmpPl = p.getSelectedPlan();
					tmpPl.createLeg(Mode.car, getEndingTimeD(this.tmpTabs[3]),
							Time.UNDEFINED_TIME, Time.UNDEFINED_TIME);
					// ZoneXY lastZoneXY = zoneXYs.get(tmpTabs[12]);

					Coord tmpCoord2 = getRandomCoordInZone(tabs[12]);
					double x = tmpCoord2.getX();
					double y = tmpCoord2.getY();
					if (this.tmpTabs[10].equals("H")) {
						x = this.tmpHomeX;
						y = this.tmpHomeY;
						// System.out.println("tmpHomeX : " + tmpHomeX
						// + "\t|\ttmpHomeY : " + tmpHomeY + "\nx : " + x
						// + "\t|\ty : " + y);
					}
					tmpPl.createAct(this.tmpTabs[10], x, y, null, null, null, null,
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

				Coord coord = getRandomCoordInZone(tabs[9]);
				this.tmpHomeX = coord.getX();
				this.tmpHomeY = coord.getY();
				pl.createAct(tabs[7], this.tmpHomeX, this.tmpHomeY, null, null,
						getEndingTimeS(tabs[3]), null, null);
				p.addPlan(pl);
				this.pop.addPerson(p);
			}
			this.tmpPersonId = personId;
			this.tmpEndingTime = ending;
			this.tmpTabs = tabs;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Coord getRandomCoordInZone(final String zoneId) {
		return WorldUtils.getRandomCoordInZone(
				(Zone) this.zones.getLocation(zoneId), this.zones);

	}

	public void createZones() {
		for (ZoneXY zxy : this.zoneXYs.values()) {
			createZone(zxy);
		}
		this.zoneXYs.clear();// //////////////////////////////////////////////
	}

	public void createZone(final ZoneXY zxy) {
		this.zones.createZone(zxy.getZoneId(), zxy.getX(), zxy.getY(), null, null,
				null, null, null, null);
	}

	/**
	 */
	public static void main(final String[] args) {
		String oldPlansFilename = "D:\\Test\\UT\\Sep24\\input\\fout_chains210.txt";
		String newPlansFilename = "D:\\Test\\UT\\Sep24\\output\\example.xml.gz";
		String zoneFilename = "D:\\Test\\UT\\Sep24\\input\\centroids.txt";

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
					if (ss.size() >= 3) {
						String zone = ss.get(1);
						c.getZoneXYs().put(zone,
							new ZoneXY(zone, ss.get(2), ss.get(3)));
					}
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

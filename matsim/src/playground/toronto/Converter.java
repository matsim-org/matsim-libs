package playground.toronto;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.basic.v01.IdImpl;
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.BasicLeg.Mode;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.population.PersonImpl;
import org.matsim.population.PopulationImpl;
import org.matsim.population.PopulationWriter;
import org.matsim.utils.WorldUtils;
import org.matsim.utils.io.IOUtils;
import org.matsim.world.Layer;
import org.matsim.world.Zone;
import org.matsim.world.ZoneLayer;

/**
 * @author yu
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

	private double tmpEndTime;

	private String[] tmpTabs = null;

	private Coord tmpHome = null;

	public Converter() {
	}

	public static double convertTime(final String endingS) {
		int endingI = Integer.parseInt(endingS);
		int hours = endingI / 100;
		int minutes = endingI % 100;
		return hours * 3600 + minutes * 60;
	}

	/**
	 * The general idea: we read line for line from the file. When reading a new line,
	 * we compare if it is a new person or the same person as the last line (stored in this.tmpLine
	 * and this.tmpPersonId). If it is a new person, we add the final home-activity to the last
	 * person and start a new person. If it is the same person, we just add the activity and the
	 * leg to the person (this.tmpPersonId).
	 *
	 * @param line
	 */
	public void readLine(final String line) {
		String[] tabs = line.split("\t");
		String personId = tabs[0] + "-" + tabs[1];
		double endTime;
		try {
			if (this.tmpPersonId.equals(personId)) {
				// this line is about the same person as the line before.
				// "extend" the plan of that person with a Leg and an Act

				// ZoneXY zoneXY = zoneXYs.get(tabs[9]);
				Plan pl = this.pop.getPerson(new IdImpl(personId)).getSelectedPlan();
				endTime = convertTime(tabs[3]);
				double dur = endTime - this.tmpEndTime;

				Leg leg = pl.createLeg(Mode.car);
				leg.setDepartureTime(convertTime(this.tmpTabs[3]));

				Coord tmpCoord = getRandomCoordInZone(tabs[9]);
				if (tabs[7].equals("H")) {
					tmpCoord = this.tmpHome;
				}

				Act act = pl.createAct(tabs[7], tmpCoord);
				act.setEndTime(convertTime(tabs[3]));
				act.setDuration(dur);

			} else {
				// it is a new person
				// finish the person from the line before, add final trip back to home
				// then start the new person

				if (!this.pop.getPersons().isEmpty()) {
					Person p = this.pop.getPerson(new IdImpl(this.tmpPersonId));
					Plan tmpPl = p.getSelectedPlan();

					Leg leg = tmpPl.createLeg(Mode.car);
					leg.setDepartureTime(convertTime(this.tmpTabs[3]));
					// ZoneXY lastZoneXY = zoneXYs.get(tmpTabs[12]);

					Coord tmpCoord2 = getRandomCoordInZone(tabs[12]);
					if (this.tmpTabs[10].equals("H")) {
						tmpCoord2 = this.tmpHome;
					}
					Act lastAct = tmpPl.createAct(this.tmpTabs[10], tmpCoord2);

					// make a copy of the just finished plan and set it to use public transit mode
					Plan nonCarPlan = new org.matsim.population.PlanImpl(p);
					nonCarPlan.copyPlan(tmpPl);
					for (LegIterator li = nonCarPlan.getIteratorLeg(); li.hasNext();) {
						li.next().setMode(Mode.pt);
					}
					p.addPlan(nonCarPlan);
				}

				Person p = new PersonImpl(new IdImpl(personId));
				Plan pl = new org.matsim.population.PlanImpl(p);
				// ZoneXY zoneXY = zoneXYs.get(tabs[9]);
				endTime = convertTime(tabs[3]);

				this.tmpHome = getRandomCoordInZone(tabs[9]);
				Act homeAct = pl.createAct(tabs[7], this.tmpHome);
				homeAct.setEndTime(convertTime(tabs[3]));
				p.addPlan(pl);
				this.pop.addPerson(p);
			}

			// remember the current data for comparison with next line
			this.tmpPersonId = personId;
			this.tmpEndTime = endTime;
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
		this.zoneXYs.clear();
	}

	public void createZone(final ZoneXY zxy) {
		this.zones.createZone(zxy.getZoneId(), zxy.getX(), zxy.getY(), null, null,
				null, null, null, null);
	}

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
		c.setPop(new PopulationImpl());
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

package playground.toronto;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.io.IOUtils;

import playground.balmermi.world.Layer;
import playground.balmermi.world.World;
import playground.balmermi.world.WorldUtils;
import playground.balmermi.world.Zone;
import playground.balmermi.world.ZoneLayer;

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
		if (this.tmpPersonId.equals(personId)) {
			// this line is about the same person as the line before.
			// "extend" the plan of that person with a Leg and an Act

			// ZoneXY zoneXY = zoneXYs.get(tabs[9]);
			Plan pl = this.pop.getPersons().get(new IdImpl(personId)).getSelectedPlan();
			endTime = convertTime(tabs[3]);
			double dur = endTime - this.tmpEndTime;

			LegImpl leg = ((PlanImpl) pl).createAndAddLeg(TransportMode.car);
			leg.setDepartureTime(convertTime(this.tmpTabs[3]));

			Coord tmpCoord = getRandomCoordInZone(tabs[9]);
			if (tabs[7].equals("H")) {
				tmpCoord = this.tmpHome;
			}

			ActivityImpl act = ((PlanImpl) pl).createAndAddActivity(tabs[7], tmpCoord);
			act.setEndTime(convertTime(tabs[3]));
			act.setMaximumDuration(dur);

		} else {
			// it is a new person
			// finish the person from the line before, add final trip back to home
			// then start the new person

			if (!this.pop.getPersons().isEmpty()) {
				Person p = this.pop.getPersons().get(new IdImpl(this.tmpPersonId));
				Plan tmpPl = p.getSelectedPlan();

				LegImpl leg = ((PlanImpl) tmpPl).createAndAddLeg(TransportMode.car);
				leg.setDepartureTime(convertTime(this.tmpTabs[3]));
				// ZoneXY lastZoneXY = zoneXYs.get(tmpTabs[12]);

				Coord tmpCoord2 = getRandomCoordInZone(tabs[12]);
				if (this.tmpTabs[10].equals("H")) {
					tmpCoord2 = this.tmpHome;
				}
				ActivityImpl lastAct = ((PlanImpl) tmpPl).createAndAddActivity(this.tmpTabs[10], tmpCoord2);

				// make a copy of the just finished plan and set it to use public transit mode
				PlanImpl nonCarPlan = new org.matsim.core.population.PlanImpl(p);
				nonCarPlan.copyPlan(tmpPl);
				for (PlanElement pe : nonCarPlan.getPlanElements()) {
					if (pe instanceof Leg) {
						((Leg) pe).setMode(TransportMode.pt);
					}
				}
				p.addPlan(nonCarPlan);
			}

			PersonImpl p = new PersonImpl(new IdImpl(personId));
			PlanImpl pl = new org.matsim.core.population.PlanImpl(p);
			// ZoneXY zoneXY = zoneXYs.get(tabs[9]);
			endTime = convertTime(tabs[3]);

			this.tmpHome = getRandomCoordInZone(tabs[9]);
			ActivityImpl homeAct = pl.createAndAddActivity(tabs[7], this.tmpHome);
			homeAct.setEndTime(convertTime(tabs[3]));
			p.addPlan(pl);
			this.pop.addPerson(p);
		}

		// remember the current data for comparison with next line
		this.tmpPersonId = personId;
		this.tmpEndTime = endTime;
		this.tmpTabs = tabs;
	}

	private Coord getRandomCoordInZone(final String zoneId) {
		return WorldUtils.getRandomCoordInZone(
				(Zone) this.zones.getLocation(new IdImpl(zoneId)), this.zones);
	}

	public void createZones() {
		for (ZoneXY zxy : this.zoneXYs.values()) {
			createZone(zxy);
		}
		this.zoneXYs.clear();
	}

	public void createZone(final ZoneXY zxy) {
		this.zones.createZone(new IdImpl(zxy.getZoneId()), zxy.getX(), zxy.getY(), null, null,
				null, null);
	}

	public static void main(final String[] args) {
		String oldPlansFilename = "D:\\Test\\UT\\Sep24\\input\\fout_chains210.txt";
		String newPlansFilename = "D:\\Test\\UT\\Sep24\\output\\example.xml.gz";
		String zoneFilename = "D:\\Test\\UT\\Sep24\\input\\centroids.txt";

		Converter c = new Converter();

		ScenarioImpl scenario = new ScenarioImpl();
		c.setZones((ZoneLayer) new World().createLayer(new IdImpl("zones")));

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
		c.setPop(new ScenarioImpl().getPopulation());
		try {
			BufferedReader reader = IOUtils.getBufferedReader(oldPlansFilename);
			PopulationWriter writer = new PopulationWriter(c.pop, null);
			writer.writeStartPlans(newPlansFilename);
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

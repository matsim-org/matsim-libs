package playground.toronto.demand.dprecated;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.balmermi.world.Layer;
import playground.balmermi.world.World;
import playground.balmermi.world.WorldUtils;
import playground.balmermi.world.Zone;
import playground.balmermi.world.ZoneLayer;

/**
 * @author yu, modified by jiang
 * 							- household coordinates remains the same for all household members
 * 							- modified tabs[] to read in TASHA mode choice outputs
 * 							- ignore duplicated records
 * 							- add last leg to the very last trip
 * 							- much more robust: still works when trip chain does not start or end at home
 *											    (output statistics at the end)
 *							- shifted TASHA time by -4 hours in order to correspond with MATSim simulation time [0-23 hour]
 *
 * June 2012: This class is considered obsolete. Use demand.CreatePlansFromTrips instead.
 */
@Deprecated
public class ModdedConverterE {
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
	private String tmpHhldId = "";
	private String tmpType = "H";
	private String tmpTripId = "";
	private double tmpEndTime;
	private boolean tmpGotH = false;
	private double count1 = 0;
	private double count2 = 0;

	private String[] tmpTabs = null;

	private Coord tmpHome = null;

	public ModdedConverterE() {
	}

	public static double convertTime(final String endingS) {
		int endingI = Integer.parseInt(endingS);
		endingI=endingI-400;   //shift back 4 hours
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
		if(line!=null){
			String[] tabs = line.split(",");
			if (tabs[4].equals("L")){ //change "go-back-home-for-lunch trip to home trip"
				tabs[4]="H";
			}
			if (tabs[7].equals("L")){
				tabs[7]="H";
			}


			String tripId = tabs[0] + "-" + tabs[1] + "-" + tabs[2];
			String mode = tabs[16];
			if(!this.tmpTripId.equals(tripId)&&mode.equals("0")){
				String personId = tabs[0] + "-" + tabs[1] + "-" + tabs[17];
				String hhldId = tabs[0];
				double endTime;
				try {
					if (this.tmpPersonId.equals(personId)) {
						// this line is about the same person as the line before.
						// "extend" the plan of that person with a Leg and an Act

						Plan pl = this.pop.getPersons().get(Id.create(personId, Person.class)).getSelectedPlan();
						endTime = convertTime(tabs[3]);
						double dur = endTime - this.tmpEndTime;

						Leg leg = PopulationUtils.createAndAddLeg( ((Plan) pl), (String) TransportMode.car );
						leg.setDepartureTime(convertTime(this.tmpTabs[3]));

						Coord tmpCoord;
						if ((tabs[4].equals("H"))&&this.tmpGotH) { //if the trip starts at home and home coordinate has been set
							tmpCoord = this.tmpHome;
						}else if((tabs[4].equals("H"))&&!this.tmpGotH){ // if the trip starts at home but home coordinate hasn't been set
							tmpCoord = getRandomCoordInZone(Id.create(tabs[6], Zone.class));
							this.tmpHome = tmpCoord;
							this.tmpGotH = true;
						}
						else{ //if the trip starts away from home
							tmpCoord = getRandomCoordInZone(Id.create(tabs[6], Zone.class));
						}
						this.tmpType = tabs[7];
						final Coord coord = tmpCoord;
						Activity act = PopulationUtils.createAndAddActivityFromCoord(((Plan) pl), (String) tabs[4], coord);
						act.setEndTime(convertTime(tabs[3]));
						act.setMaximumDuration(dur);
					} else {
						// it is a new person
						// finish the person from the line before, add final trip
						// then start the new person

						if (!this.pop.getPersons().isEmpty()) {
							Person p = this.pop.getPersons().get(Id.create(this.tmpPersonId, Person.class));
							Plan tmpPl = p.getSelectedPlan();

							Leg leg = PopulationUtils.createAndAddLeg( ((Plan) tmpPl), (String) TransportMode.car );
							leg.setDepartureTime(convertTime(this.tmpTabs[3]));
							// ZoneXY lastZoneXY = zoneXYs.get(tmpTabs[12]);

							Coord tmpCoord2;
							if ((this.tmpTabs[7].equals("H"))&&this.tmpGotH) { // the person's last trip ends at home and home coordinate has already been set
								tmpCoord2 = this.tmpHome;
							}else if ((this.tmpTabs[7].equals("H"))&&!this.tmpGotH){ // the person's last trip ends at home but home coordinate hasn't been set
								tmpCoord2 = getRandomCoordInZone(Id.create(this.tmpTabs[9], Zone.class));
								this.tmpHome = tmpCoord2;
								this.tmpGotH = true;
							}else{ //the person's last trip does not end at home
								tmpCoord2 = getRandomCoordInZone(Id.create(this.tmpTabs[9], Zone.class));
								this.count2+=1;
								System.out.println(this.tmpPersonId);
							}
							final Coord coord = tmpCoord2;
							Activity lastAct = PopulationUtils.createAndAddActivityFromCoord(((Plan) tmpPl), (String) this.tmpTabs[7], coord);

						}

						Person p = PopulationUtils.getFactory().createPerson(Id.create(personId, Person.class));
						Plan pl = PopulationUtils.createPlan(p);
						// ZoneXY zoneXY = zoneXYs.get(tabs[9]);
						endTime = convertTime(tabs[3]);
						this.tmpType = tabs[4];
						Coord tmpCoord3;
						if (!this.tmpHhldId.equals(hhldId)&&this.tmpType.equals("H")) {  //1st person of a household start first trip from home
							this.tmpHome = getRandomCoordInZone(Id.create(tabs[6], Zone.class));
							tmpCoord3 = this.tmpHome;
							this.tmpGotH = true;
						}else if (this.tmpHhldId.equals(hhldId)&&this.tmpType.equals("H")){ //other people of the same household start first trip from home
							tmpCoord3 = this.tmpHome;
							this.tmpGotH = true;
						}else if (this.tmpHhldId.equals(hhldId)&&!this.tmpType.equals("H")){ //other people of the same household start first trip away from home
							tmpCoord3 = getRandomCoordInZone(Id.create(tabs[6], Zone.class));
							this.tmpGotH = true;
							this.count1 += 1;
							System.out.println(personId);
						}else{ //1st person of a household start first trip away from home
							tmpCoord3 = getRandomCoordInZone(Id.create(tabs[6], Zone.class));
							this.tmpGotH = false;
							this.count1 += 1;
							System.out.println(personId);
						}
						final Coord coord = tmpCoord3;
						Activity homeAct = PopulationUtils.createAndAddActivityFromCoord(pl, (String) tabs[4], coord);
						homeAct.setEndTime(convertTime(tabs[3]));
						p.addPlan(pl);
						this.pop.addPerson(p);
					}

					// remember the current data for comparison with next line
					this.tmpPersonId = personId;
					this.tmpHhldId = hhldId;
					this.tmpEndTime = endTime;
					this.tmpTabs = tabs;
					this.tmpTripId = tripId;
					if (this.tmpTabs[4].equals("L")){
						this.tmpTabs[4]="H";
					}
					if (this.tmpTabs[7].equals("L")){
						this.tmpTabs[7]="H";
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}else{

			Person p = this.pop.getPersons().get(Id.create(this.tmpPersonId, Person.class));
			Plan tmpPl = p.getSelectedPlan();


			Leg leg = PopulationUtils.createAndAddLeg( ((Plan) tmpPl), (String) TransportMode.car );
			leg.setDepartureTime(convertTime(this.tmpTabs[3]));

			Coord tmpCoord2;
			if ((this.tmpTabs[7].equals("H"))&&this.tmpGotH) {
				tmpCoord2 = this.tmpHome;
			}else if ((this.tmpTabs[7].equals("H"))&&!this.tmpGotH){
				tmpCoord2 = getRandomCoordInZone(Id.create(this.tmpTabs[9], Zone.class));
				this.tmpHome = tmpCoord2;
				this.tmpGotH = true;
			}else{
				tmpCoord2 = getRandomCoordInZone(Id.create(this.tmpTabs[9], Zone.class));
				this.count2+=1;
				System.out.println(this.tmpPersonId);
			}
			final Coord coord = tmpCoord2;
			Activity lastAct = PopulationUtils.createAndAddActivityFromCoord(((Plan) tmpPl), (String) this.tmpTabs[7], coord);
			System.out.println("# of chains that do not start at home: " + this.count1);
			System.out.println("# of chains that do not end at home: " + this.count2);
		}
	}

	private Coord getRandomCoordInZone(final Id<Zone> zoneId) {
		return WorldUtils.getRandomCoordInZone(
				this.zones.getLocation(zoneId), this.zones);
	}

	public void createZones() {
		for (ZoneXY zxy : this.zoneXYs.values()) {
			createZone(zxy);
		}
		this.zoneXYs.clear();
	}

	public void createZone(final ZoneXY zxy) {
		this.zones.createZone(Id.create(zxy.getZoneId(), Zone.class), zxy.getX(), zxy.getY(), null, null,
				null, null);
	}

	public static void main(final String[] args) {
		String oldPlansFilename = "C:\\Thesis_HJY\\matsim\\input\\ConvertPlan\\fout_modechoices_recleaned.txt";
		String newPlansFilename = "C:\\Thesis_HJY\\matsim\\output\\ConvertPlan\\plansE.xml.gz";
		String zoneFilename = "C:\\Thesis_HJY\\matsim\\input\\ConvertPlan\\centroids.txt";
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		ModdedConverterE c = new ModdedConverterE();

		c.setZones((ZoneLayer) new World().createLayer(Id.create("zones", Layer.class)));
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
//						if(zone.equals("2146")){  //change zone 2146 to 2145
//							zone = "2145";
//						}
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
		c.setPop(scenario.getPopulation());
		try {
			BufferedReader reader = IOUtils.getBufferedReader(oldPlansFilename);
			StreamingPopulationWriter writer = new StreamingPopulationWriter(c.pop, scenario.getNetwork());
			writer.writeStartPlans(newPlansFilename);
			String line;// = reader.readLine();
			do {
				line = reader.readLine();
				if (line != null) {
					c.readLine(line);
				}
			} while (line != null);
			c.readLine(line);
			//c.readLine(line);

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

package playground.anhorni.csestimation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;

public class SurveyReader {
	private TreeMap<Id<Person>, EstimationPerson> population;
	private final static Logger log = Logger.getLogger(SurveyReader.class);
	private WGS84toCH1903LV03 trafo = new WGS84toCH1903LV03();

	public TreeMap<Id<Person>, EstimationPerson> getPopulation() {
		return population;
	}
	
	public SurveyReader(TreeMap<Id<Person>, EstimationPerson> population) {
		this.population = population;
	}

	// 0: username,		1: age,		2: sex,		3: income,	4: hhsize,	5: shoppingPerson,	6: purchasesPerMonth,	
	// 7: H_Street,		8: H_nbr,	9: H_PLZ,	10: H_city, 11: H_Lat, 12: H_Lng,
	// 13: fCar, 		14: fPt, 	15: fBike,	16: fWalk,	17: job, 
	// 18: W_Street,	19: W_nbr,	20: W_PLZ,	21: W_city, 22: W_Lat, 23: W_Lng,			24: noAddressWork, 
	// 25: mode,		26: fHome,	27: fWork,	28: fInter,	29: fOther

	public void readDumpedPersons(String file) {		
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String curr_line;
			while ((curr_line = br.readLine()) != null) {
				String[] entrs = curr_line.split(";", -1);
				Id<Person> userId = Id.create(entrs[0].trim().substring(4, 8), Person.class);
				
				EstimationPerson person = new EstimationPerson(userId);
				this.population.put(userId, person);
				
				person.setAge(Math.max(0, Integer.parseInt(entrs[1].trim())));
				
				if (entrs[2].trim().equals("w")) {
					person.setSex("f");
				}
				else if (entrs[2].trim().equals("m")) {
					person.setSex("m");
				}
				else {
					person.setSex("-99");
				}

				person.setWeight(1.0);
				// TODO: hh income -99 = AHV
				if (!entrs[3].trim().equals("")) {
					person.setHhIncome(Integer.parseInt(entrs[3].trim()));
				}
				else {
					person.setHhIncome(-1);
				}
				person.setHhSize(Integer.parseInt(entrs[4].trim()));
				person.setNbrPersonShoppingTripsMonth(Integer.parseInt(entrs[5].trim()));
				person.setNbrShoppingTripsMonth(entrs[6].trim());

				Location hlocation = new Location(Id.create(-1, Location.class));
				person.setHomeLocation(hlocation);
				hlocation.setCity(entrs[10].trim());

				Coord hcoords = new Coord(Double.parseDouble(entrs[12].trim()), Double.parseDouble(entrs[11].trim()));
				
				hlocation.setCoord(this.trafo.transform(hcoords)); // TODO: prüfen bei NULL
				
				person.setModesForShopping(SurveyControler.modes.indexOf("car"), SurveyControler.frequency.indexOf(entrs[13].trim().replaceAll("car", "")));
				person.setModesForShopping(SurveyControler.modes.indexOf("pt"), SurveyControler.frequency.indexOf(entrs[14].trim().replaceAll("pt", "")));
				person.setModesForShopping(SurveyControler.modes.indexOf("bike"), SurveyControler.frequency.indexOf(entrs[15].trim().substring(1)));
				person.setModesForShopping(SurveyControler.modes.indexOf("walk"), SurveyControler.frequency.indexOf(entrs[16].trim().substring(1)));
				
				boolean hasJob = entrs[17].trim().equals("yes");
				
				if (hasJob) {
					Location wlocation = new Location(Id.create(-2, Location.class));
					person.setEmployed(hasJob);
					person.setWorkLocation(wlocation);
					wlocation.setCity(entrs[21].trim());
					wlocation.setCoord(this.trafo.transform(new Coord(Double.parseDouble(entrs[22].trim()), Double.parseDouble(entrs[23].trim()))));
				
					if (entrs[25].trim().contains("car")) person.setModeForWorking(SurveyControler.modes.indexOf("car"), true);
					if (entrs[25].trim().contains("pt")) person.setModeForWorking(SurveyControler.modes.indexOf("pt"), true);
					if (entrs[25].trim().contains("bike")) person.setModeForWorking(SurveyControler.modes.indexOf("bike"), true);
					if (entrs[25].trim().contains("walk")) person.setModeForWorking(SurveyControler.modes.indexOf("walk"), true);		
					
					person.setAreaToShop(0, SurveyControler.frequency.indexOf(entrs[26].trim().replaceAll("home", "")));
					person.setAreaToShop(1, SurveyControler.frequency.indexOf(entrs[27].trim().replaceAll("work", "")));
					person.setAreaToShop(2, SurveyControler.frequency.indexOf(entrs[28].trim().replaceAll("inter", "")));
					person.setAreaToShop(3, SurveyControler.frequency.indexOf(entrs[29].trim().replaceAll("other", "")));
				}				
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 		
	}

	// 0:	id, 			1: shop_id,		2: uname,		3: visitedByUser,	4: addedByUser,	5: awareness,	6: frequency,
	// 7: iProducts,		8: iPtChange,	9: iDistance,	10: iPrize,			11: iParking,	12: iAtmo,		13: iOpentimes,	14: furtherReasons,
	// 15: disadvantages,	16: disad_text,	
	// 17: startLoc,	18: endLoc,			19: saved,		20: inCS	

	public void readDumpedPersonShops(String file) {		
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String curr_line;
			Id<Person> prevId = Id.create(-99, Person.class);
			int countAware = 0;
			int countVisited = 0;
			while ((curr_line = br.readLine()) != null) {
				String[] entrs = curr_line.split(";", -1);
				Id<Location> shopId = Id.create(entrs[1].trim(), Location.class);
				Id<Person> userId = Id.create(entrs[2].trim().substring(4, 8), Person.class);
				
				EstimationPerson person = this.population.get(userId);
				ShopLocation store = new ShopLocation(shopId);				
				int aware = 0;
				int visited = 0;
				boolean nullStore = false;
				
				if (entrs[5].trim().equals("yes")) {
					aware = 1;
					countAware++;
				}
				else if (entrs[5].trim().equals("no")) {
					aware = -1;
					store.setVisitFrequency("never");
				}
				else if (entrs[5].trim().equals("NULL")) {
					nullStore = true;
				}
				
				
				if (entrs[3].trim().equals("yes")) {
					visited = 1;
					store.setVisitFrequency(entrs[6].trim());
					countVisited++;
				}
				else if (entrs[3].trim().equals("no")) {
					visited = -1;
					store.setVisitFrequency(entrs[6].trim());
				}
				else if (entrs[3].trim().equals("NULL")) {
					nullStore = true;
				}				
				if (person == null) {
					log.error("person null for user " + userId);
					System.exit(1);
				}
				else {
					if (nullStore) {
						person.addNullStore(store);
					}
					else {
						person.addStore(store, aware, visited);	
					}
				}
				if (prevId.compareTo(userId) != 0 && prevId.compareTo(Id.create(-99, Person.class)) != 0) {
					log.info("parsed user: " + prevId + " " + countAware + " aware stores " + countVisited + " visited stores");
					countAware = 0;
					countVisited = 0;
				}
				prevId = Id.create(entrs[2].trim().substring(4, 8), Person.class);
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 		
	}
}

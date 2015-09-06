package playground.mkillat.staedtebau;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.mkillat.tmc.StringTimeToDouble;



public class Analyse implements Runnable {
	List <KordonElement> kd1_rein = new ArrayList <KordonElement>();
	List <KordonElement> kd1_raus = new ArrayList <KordonElement>();
	List <KordonElement> kd2_rein = new ArrayList <KordonElement>();
	List <KordonElement> kd2_raus = new ArrayList <KordonElement>();
	List <KordonElement> kd3_rein = new ArrayList <KordonElement>();
	List <KordonElement> kd3_raus = new ArrayList <KordonElement>();
	List <KordonElement> kd4_rein = new ArrayList <KordonElement>();
	List <KordonElement> kd4_raus = new ArrayList <KordonElement>();
	List <KordonElement> kd5_rein = new ArrayList <KordonElement>();
	List <KordonElement> kd5_raus = new ArrayList <KordonElement>();
	List <KordonElement> kd6_rein = new ArrayList <KordonElement>();
	List <KordonElement> kd6_raus = new ArrayList <KordonElement>();
	List <KordonElement> kd7_rein = new ArrayList <KordonElement>();
	List <KordonElement> kd7_raus = new ArrayList <KordonElement>();
	List <KordonElement> kd8_rein = new ArrayList <KordonElement>();
	List <KordonElement> kd8_raus = new ArrayList <KordonElement>();
	
	String filename = "C:/Users/Marie/Dropbox/Städtebau/KD_Master_Originaldaten.csv";
	String configFile = "C:/Users/Marie/workspace_MATsim_d/playgrounds/mkillat/input/staedtebau/0.config.xml";


	public static void main(String[] args) {
		Analyse test = new Analyse();
		test.run();
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
//		KordonElementReader(filename);
//		AnalyseDerDaten();
		analysis();
	}
	
	


	

	private void analysis() {
		String configBase = "./input/staedtebau/config.xml";
		String networkBase = "input/staedtebau/bermannstr.xml";
		String eventsBase = "C:/Users/Marie/workspace_MATsim_d/playgrounds/mkillat/output/staedtebau/ITERS/it.10/10.events.xml.gz";
//		String eventsBase = "C:/Users/Marie/workspace_MATsim_d/playgrounds/mkillat/output/bus_berlin3_nce5/ITERS/it.0/0.events.xml.gz";
		String plansBase =  "input/staedtebau/plans.xml";
		String configMeasure = "./input/staedtebau/configM.xml";
		String networkMeasure = "input/staedtebau/bermannstr2.xml";
		String eventsMeasure = "C:/Users/Marie/workspace_MATsim_d/playgrounds/mkillat/output/staedtebauM/ITERS/it.10/10.events.xml.gz";
		
		Config config1 = ConfigUtils.loadConfig(configBase);
		config1.network().setInputFile(networkBase);
		config1.plans().setInputFile(plansBase);
		Scenario scenario1 = ScenarioUtils.loadScenario(config1);
		List <Id> allAgents = new ArrayList<Id>();
		
		for (Id id: scenario1.getPopulation().getPersons().keySet() ){
			allAgents.add(id);
		}
		
		System.out.println(allAgents.size());
		
		
		Map<Id<Person>, Double> agentsOnBergmann = PotsdamEventFileReaderPersonUseBridge.EventFileReader(configBase, eventsBase);
		System.out.println(agentsOnBergmann.size());
		
		Map<Id<Person>, Double> agentsOnBergmann2 = PotsdamEventFileReaderPersonUseBridge.EventFileReader(configMeasure, eventsMeasure);
		System.out.println(agentsOnBergmann2.size());
		
	}

	private void KordonElementReader(String filename) {

			
			
			FileReader fr;
			BufferedReader br;
			try {
				fr = new FileReader(new File (filename));
				br = new BufferedReader(fr);
				String line = null;
				br.readLine(); //Erste Zeile (Kopfzeile) wird �bersprungen.
				br.readLine(); //Erste Zeile (Kopfzeile) wird �bersprungen.
				br.readLine(); //Erste Zeile (Kopfzeile) wird �bersprungen.
				while ((line = br.readLine()) != null) {
					String[] result = line.split(";");

					String timeS = result[0];
					StringTimeToDouble aa = new StringTimeToDouble();
					double time = aa.transformer(timeS);
					
					KordonElement kd1_1K = new KordonElement(result[1], time);
					kd1_rein.add(kd1_1K);
					KordonElement kd1_2K = new KordonElement(result[2], time);
					kd1_rein.add(kd1_2K);
					KordonElement kd1_3K = new KordonElement(result[3], time);
					kd1_raus.add(kd1_3K);
					KordonElement kd2_123K = new KordonElement(result[4], time);
					kd2_raus.add(kd2_123K);
					KordonElement kd2_4K = new KordonElement(result[5], time);
					kd2_rein.add(kd2_4K);
					KordonElement kd2_5K = new KordonElement(result[6], time);
					kd2_rein.add(kd2_5K);
					KordonElement kd2_6K = new KordonElement(result[7], time);
					kd2_rein.add(kd2_6K);
					KordonElement kd3_1K = new KordonElement(result[8], time);
					kd3_raus.add(kd3_1K);
					KordonElement kd3_3K = new KordonElement(result[9], time);
					kd3_rein.add(kd3_3K);
					KordonElement kd3_4K = new KordonElement(result[10], time);
					kd3_rein.add(kd3_4K);
					KordonElement kd4_1K = new KordonElement(result[11], time);
					kd4_raus.add(kd4_1K);
					KordonElement kd4_2K = new KordonElement(result[12], time);
					kd4_raus.add(kd4_2K);
					KordonElement kd4_3K = new KordonElement(result[13], time);
					kd4_raus.add(kd4_3K);
					KordonElement kd4_4K = new KordonElement(result[14], time);
					kd4_rein.add(kd4_4K);
					KordonElement kd4_5K = new KordonElement(result[15], time);
					kd4_rein.add(kd4_5K);
					KordonElement kd4_6K = new KordonElement(result[16], time);
					kd4_rein.add(kd4_6K);
					KordonElement kd5_1K = new KordonElement(result[17], time);
					kd5_rein.add(kd5_1K);
					KordonElement kd5_2K = new KordonElement(result[18], time);
					kd5_rein.add(kd5_2K);
					KordonElement kd5_3K = new KordonElement(result[19], time);
					kd5_rein.add(kd5_3K);
					KordonElement kd5_4K = new KordonElement(result[20], time);
					kd5_raus.add(kd5_4K);
					KordonElement kd5_5K = new KordonElement(result[21], time);
					kd5_raus.add(kd5_5K);
					KordonElement kd5_6K = new KordonElement(result[22], time);
					kd5_raus.add(kd5_6K);
					KordonElement kd6_1K = new KordonElement(result[23], time);
					kd6_rein.add(kd6_1K);
					KordonElement kd6_2K = new KordonElement(result[24], time);
					kd6_rein.add(kd6_2K);
					KordonElement kd6_3K = new KordonElement(result[25], time);
					kd6_rein.add(kd6_3K);
					KordonElement kd6_4K = new KordonElement(result[26], time);
					kd6_raus.add(kd6_4K);
					KordonElement kd6_5K = new KordonElement(result[27], time);
					kd6_raus.add(kd6_5K);
					KordonElement kd6_6K = new KordonElement(result[28], time);
					kd6_raus.add(kd6_6K);
					KordonElement kd7_1K = new KordonElement(result[29], time);
					kd7_rein.add(kd7_1K);
					KordonElement kd7_2K = new KordonElement(result[30], time);
					kd7_rein.add(kd7_2K);
					KordonElement kd7_3K = new KordonElement(result[31], time);
					kd7_rein.add(kd7_3K);
					KordonElement kd7_4K = new KordonElement(result[32], time);
					kd7_raus.add(kd7_4K);
					KordonElement kd7_5K = new KordonElement(result[33], time);
					kd7_raus.add(kd7_5K);
					KordonElement kd7_6K = new KordonElement(result[34], time);
					kd7_raus.add(kd7_6K);
					KordonElement kd8_1K = new KordonElement(result[35], time);
					kd8_rein.add(kd8_1K);
					KordonElement kd8_2K = new KordonElement(result[36], time);
					kd8_rein.add(kd8_2K);
					KordonElement kd8_3K = new KordonElement(result[37], time);
					kd8_raus.add(kd8_3K);
					KordonElement kd8_4K = new KordonElement(result[38], time);
					kd8_raus.add(kd8_4K);
					
					
					
			
			}
			
		} catch (FileNotFoundException e) {
			System.err.println("File not found...");
				e.printStackTrace();
		} catch (NumberFormatException e) {
			System.err.println("Wrong No. format...");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("I/O error...");
			e.printStackTrace();
			}
			
		

	}
	
	private void AnalyseDerDaten() {

		kd1_rein = DoppelteEintaegeLoeschen(kd1_rein);
		kd1_raus = DoppelteEintaegeLoeschen(kd1_raus);
		kd2_raus = DoppelteEintaegeLoeschen(kd2_raus);
		kd2_rein = DoppelteEintaegeLoeschen(kd2_rein);
		kd3_raus = DoppelteEintaegeLoeschen(kd2_raus);
		kd3_rein = DoppelteEintaegeLoeschen(kd3_rein);
		kd4_raus = DoppelteEintaegeLoeschen(kd4_raus);
		kd4_rein = DoppelteEintaegeLoeschen(kd4_rein);
		kd5_raus = DoppelteEintaegeLoeschen(kd5_raus);
		kd5_rein = DoppelteEintaegeLoeschen(kd5_rein);
		kd6_raus = DoppelteEintaegeLoeschen(kd6_raus);
		kd6_rein = DoppelteEintaegeLoeschen(kd6_rein);
		kd7_raus = DoppelteEintaegeLoeschen(kd7_raus);
		kd7_rein = DoppelteEintaegeLoeschen(kd7_rein);
		kd8_raus = DoppelteEintaegeLoeschen(kd8_raus);
		kd8_rein = DoppelteEintaegeLoeschen(kd8_rein);
		
		List <KordonElement> alleRein = new ArrayList<KordonElement>();
		List <KordonElement> alleRaus = new ArrayList <KordonElement>();
		alleRein.addAll(kd1_rein);
		alleRein.addAll(kd2_rein);
		alleRein.addAll(kd3_rein);
		alleRein.addAll(kd4_rein);
		alleRein.addAll(kd5_rein);
		alleRein.addAll(kd6_rein);
		alleRein.addAll(kd7_rein);
		alleRein.addAll(kd8_rein);
		
		alleRaus.addAll(kd1_raus);
		alleRaus.addAll(kd2_raus);
		alleRaus.addAll(kd3_raus);
		alleRaus.addAll(kd4_raus);
		alleRaus.addAll(kd5_raus);
		alleRaus.addAll(kd6_raus);
		alleRaus.addAll(kd7_raus);
		alleRaus.addAll(kd8_raus);
		
		Map <String, Double> alleKennzeichen = new HashMap<String, Double>();
		
		for (int i = 0; i < alleRein.size(); i++) {
			if (alleKennzeichen.containsKey(alleRein.get(i))){
				
			}else{
				alleKennzeichen.put(alleRein.get(i).kennzeichen, alleRein.get(i).time);
			}
			
		}
		
		for (int i = 0; i < alleRaus.size(); i++) {
			if (alleKennzeichen.containsKey(alleRaus.get(i))){
				
			}else{
				alleKennzeichen.put(alleRaus.get(i).kennzeichen, alleRaus.get(i).time);
			}
			
		}
		System.out.println("Anzahl aller Fahrzeuge: " + alleKennzeichen.size());
		
		Map <String, Double> durchgangsverkehr = new HashMap<String, Double>();
		
		System.out.println("Anzahl Reinfahrende: " + alleRein.size());
		System.out.println("Anzahl Rausfahrende: " + alleRaus.size());
		
		for (int i = 0; i < alleRaus.size(); i++) {
			for (int j = 0; j < alleRein.size(); j++) {
				if (alleRaus.get(i).kennzeichen.equals(alleRein.get(j).kennzeichen)){
					durchgangsverkehr.put(alleRaus.get(i).kennzeichen,alleRaus.get(i).time);
				}
			}
		}

		
		System.out.println("Anzahl Durchgangsverkehr: " + durchgangsverkehr.size());
		
	
		
		Map <String, Double> von1nach2 = Verbinden(kd1_rein, kd2_raus);
		Map <String, Double> von1nach3 = Verbinden(kd1_rein, kd3_raus);
		Map <String, Double> von1nach4 = Verbinden(kd1_rein, kd4_raus);
		Map <String, Double> von1nach5 = Verbinden(kd1_rein, kd5_raus);
		Map <String, Double> von1nach6 = Verbinden(kd1_rein, kd6_raus);
		Map <String, Double> von1nach7 = Verbinden(kd1_rein, kd7_raus);
		Map <String, Double> von1nach8 = Verbinden(kd1_rein, kd8_raus);
		Map <String, Double> von2nach1 = Verbinden(kd2_rein, kd1_raus);
		Map <String, Double> von2nach3 = Verbinden(kd2_rein, kd3_raus);
		Map <String, Double> von2nach4 = Verbinden(kd2_rein, kd4_raus);
		Map <String, Double> von2nach5 = Verbinden(kd2_rein, kd5_raus);
		Map <String, Double> von2nach6 = Verbinden(kd2_rein, kd6_raus);
		Map <String, Double> von2nach7 = Verbinden(kd2_rein, kd7_raus);
		Map <String, Double> von2nach8 = Verbinden(kd2_rein, kd8_raus);
		Map <String, Double> von3nach1 = Verbinden(kd3_rein, kd1_raus);
		Map <String, Double> von3nach2 = Verbinden(kd3_rein, kd2_raus);
		Map <String, Double> von3nach4 = Verbinden(kd3_rein, kd4_raus);
		Map <String, Double> von3nach5 = Verbinden(kd3_rein, kd5_raus);
		Map <String, Double> von3nach6 = Verbinden(kd3_rein, kd6_raus);
		Map <String, Double> von3nach7 = Verbinden(kd3_rein, kd7_raus);
		Map <String, Double> von3nach8 = Verbinden(kd3_rein, kd8_raus);
		Map <String, Double> von4nach1 = Verbinden(kd4_rein, kd1_raus);
		Map <String, Double> von4nach2 = Verbinden(kd4_rein, kd2_raus);
		Map <String, Double> von4nach3 = Verbinden(kd4_rein, kd3_raus);
		Map <String, Double> von4nach5 = Verbinden(kd4_rein, kd5_raus);
		Map <String, Double> von4nach6 = Verbinden(kd4_rein, kd6_raus);
		Map <String, Double> von4nach7 = Verbinden(kd4_rein, kd7_raus);
		Map <String, Double> von4nach8 = Verbinden(kd4_rein, kd8_raus);
		Map <String, Double> von5nach1 = Verbinden(kd5_rein, kd1_raus);
		Map <String, Double> von5nach2 = Verbinden(kd5_rein, kd2_raus);
		Map <String, Double> von5nach3 = Verbinden(kd5_rein, kd3_raus);
		Map <String, Double> von5nach4 = Verbinden(kd5_rein, kd4_raus);
		Map <String, Double> von5nach6 = Verbinden(kd5_rein, kd6_raus);
		Map <String, Double> von5nach7 = Verbinden(kd5_rein, kd7_raus);
		Map <String, Double> von5nach8 = Verbinden(kd5_rein, kd8_raus);
		Map <String, Double> von6nach1 = Verbinden(kd6_rein, kd1_raus);
		Map <String, Double> von6nach2 = Verbinden(kd6_rein, kd2_raus);
		Map <String, Double> von6nach3 = Verbinden(kd6_rein, kd3_raus);
		Map <String, Double> von6nach4 = Verbinden(kd6_rein, kd4_raus);
		Map <String, Double> von6nach5 = Verbinden(kd6_rein, kd5_raus);
		Map <String, Double> von6nach7 = Verbinden(kd6_rein, kd7_raus);
		Map <String, Double> von6nach8 = Verbinden(kd6_rein, kd8_raus);
		Map <String, Double> von7nach1 = Verbinden(kd7_rein, kd1_raus);
		Map <String, Double> von7nach2 = Verbinden(kd7_rein, kd2_raus);
		Map <String, Double> von7nach3 = Verbinden(kd7_rein, kd3_raus);
		Map <String, Double> von7nach4 = Verbinden(kd7_rein, kd4_raus);
		Map <String, Double> von7nach5 = Verbinden(kd7_rein, kd5_raus);
		Map <String, Double> von7nach6 = Verbinden(kd7_rein, kd6_raus);
		Map <String, Double> von7nach8 = Verbinden(kd7_rein, kd8_raus);
		Map <String, Double> von8nach1 = Verbinden(kd8_rein, kd1_raus);
		Map <String, Double> von8nach2 = Verbinden(kd8_rein, kd2_raus);
		Map <String, Double> von8nach3 = Verbinden(kd8_rein, kd3_raus);
		Map <String, Double> von8nach4 = Verbinden(kd8_rein, kd4_raus);
		Map <String, Double> von8nach5 = Verbinden(kd8_rein, kd5_raus);
		Map <String, Double> von8nach6 = Verbinden(kd8_rein, kd6_raus);
		Map <String, Double> von8nach7 = Verbinden(kd8_rein, kd7_raus);

		Coord kd1 = new Coord(13.39659333, 52.48638676);
		Coord kd2 = new Coord(13.39442611, 52.48647823);
		Coord kd3 = new Coord(13.38602006, 52.48736022);
		Coord kd4 = new Coord(13.38644251, 52.49006487);
		Coord kd5 = new Coord(13.39087889, 52.49228923);
		Coord kd6 = new Coord(13.39274101, 52.49192912);
		Coord kd7 = new Coord(13.39440331, 52.49156902);
		Coord kd8 = new Coord(13.39806587, 52.48888896);
		
		List <Population> populations = new ArrayList<Population>();
		populations.add(createPopulation(von1nach2, kd1, kd2));
		populations.add(createPopulation(von1nach3, kd1, kd3));
		populations.add(createPopulation(von1nach4, kd1, kd4));
		populations.add(createPopulation(von1nach5, kd1, kd5));
		populations.add(createPopulation(von1nach6, kd1, kd6));
		populations.add(createPopulation(von1nach7, kd1, kd7));
		populations.add(createPopulation(von1nach8, kd1, kd8));
		populations.add(createPopulation(von2nach1, kd2, kd1));
		populations.add(createPopulation(von2nach3, kd2, kd3));
		populations.add(createPopulation(von2nach4, kd2, kd4));
		populations.add(createPopulation(von2nach5, kd2, kd5));
		populations.add(createPopulation(von2nach6, kd2, kd6));
		populations.add(createPopulation(von2nach7, kd2, kd7));
		populations.add(createPopulation(von2nach8, kd2, kd8));
		populations.add(createPopulation(von3nach1, kd3, kd1));
		populations.add(createPopulation(von3nach2, kd3, kd2));
		populations.add(createPopulation(von3nach4, kd3, kd4));
		populations.add(createPopulation(von3nach5, kd3, kd5));
		populations.add(createPopulation(von3nach6, kd3, kd6));
		populations.add(createPopulation(von3nach7, kd3, kd7));
		populations.add(createPopulation(von3nach8, kd3, kd8));
		populations.add(createPopulation(von4nach1, kd4, kd1));
		populations.add(createPopulation(von4nach2, kd4, kd2));
		populations.add(createPopulation(von4nach3, kd4, kd3));
		populations.add(createPopulation(von4nach5, kd4, kd5));
		populations.add(createPopulation(von4nach6, kd4, kd6));
		populations.add(createPopulation(von4nach7, kd4, kd7));
		populations.add(createPopulation(von4nach8, kd4, kd8));
		populations.add(createPopulation(von5nach1, kd5, kd1));
		populations.add(createPopulation(von5nach2, kd5, kd2));
		populations.add(createPopulation(von5nach3, kd5, kd3));
		populations.add(createPopulation(von5nach4, kd5, kd4));
		populations.add(createPopulation(von5nach6, kd5, kd6));
		populations.add(createPopulation(von5nach7, kd5, kd7));
		populations.add(createPopulation(von5nach8, kd5, kd8));
		populations.add(createPopulation(von6nach1, kd6, kd1));
		populations.add(createPopulation(von6nach2, kd6, kd2));
		populations.add(createPopulation(von6nach3, kd6, kd3));
		populations.add(createPopulation(von6nach4, kd6, kd4));
		populations.add(createPopulation(von6nach5, kd6, kd5));
		populations.add(createPopulation(von6nach7, kd6, kd7));
		populations.add(createPopulation(von6nach8, kd6, kd8));
		populations.add(createPopulation(von7nach1, kd7, kd1));
		populations.add(createPopulation(von7nach2, kd7, kd2));
		populations.add(createPopulation(von7nach3, kd7, kd3));
		populations.add(createPopulation(von7nach4, kd7, kd4));
		populations.add(createPopulation(von7nach5, kd7, kd5));
		populations.add(createPopulation(von7nach6, kd7, kd6));
		populations.add(createPopulation(von7nach8, kd7, kd8));
		populations.add(createPopulation(von8nach1, kd8, kd1));
		populations.add(createPopulation(von8nach2, kd8, kd2));
		populations.add(createPopulation(von8nach3, kd8, kd3));
		populations.add(createPopulation(von8nach4, kd8, kd4));
		populations.add(createPopulation(von8nach5, kd8, kd5));
		populations.add(createPopulation(von8nach6, kd8, kd6));
		populations.add(createPopulation(von8nach7, kd8, kd7));
		Config config = org.matsim.core.config.ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		Network network = scenario.getNetwork();
		Population population = scenario.getPopulation();
		
		
		for (int i = 0; i < populations.size(); i++) {
			Map<Id<Person>, ? extends Person> temp = populations.get(i).getPersons();
			for (Entry<Id<Person>, ? extends Person> entry : temp.entrySet()) {
				population.addPerson(entry.getValue());
			}
		}
		System.out.println("**********************************" + population.getPersons().size());
		PopulationWriter writer = new PopulationWriter(population, network);
		
		writer.write("input/staedtebau/plans.xml");
		
		
		
		
		
		
		
	}
	
	

	
	
	
	private List <KordonElement>  DoppelteEintaegeLoeschen(List <KordonElement> kordonElements) {
		List<KordonElement> output = new ArrayList<KordonElement>();
		for (int i = 0; i < kordonElements.size(); i++) {
			if (kordonElements.get(i).kennzeichen.equals("0")){
				
			}else{
				output.add(kordonElements.get(i));
			}
			
		}
		
		
		
		
		return output;
	}
	
	public Map <String, Double >  Verbinden (List<KordonElement> rein, List <KordonElement> raus){
		Map <String, Double> vonNach = new HashMap<String, Double>();
		for (int i = 0; i < rein.size(); i++) {
			for (int j = 0; j < raus.size(); j++) {
				if (rein.get(i).kennzeichen.equals(raus.get(j).kennzeichen) ){
					if(rein.get(i).time==raus.get(j).time || rein.get(i).time==raus.get(j).time + 5*60){
						vonNach.put(rein.get(i).kennzeichen, rein.get(i).time);
					}
			}
		}		
		}
		
		
		return vonNach;
		
	}
	
	public Population createPopulation (Map <String, Double> input, Coord coordRein, Coord coordRaus){
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM33N);
		
		
		Config config = org.matsim.core.config.ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		Network network = scenario.getNetwork();
		Population population = scenario.getPopulation();
		PopulationFactory factory = population.getFactory();
		
		double coordReinX= coordRein.getX();
		double coordReinY= coordRein.getY();
		double coordRausX= coordRaus.getX();
		double coordRausY= coordRaus.getY();
		
		for (Entry<String, Double> entry : input.entrySet()) { 
			String  bla =String.valueOf(Math.random());
			String id = entry.getKey() +  bla;
			
			Person person = factory.createPerson(Id.create(id, Person.class));
			population.addPerson(person);
			Plan plan = factory.createPlan();
			Activity home = factory.createActivityFromCoord("home",  ct.transform(new Coord(coordReinX, coordReinY)));
			home.setEndTime(entry.getValue() + Math.random()* 300 + 1 );
			plan.addActivity(home);
			Leg homeToWork = factory.createLeg(TransportMode.car);
			plan.addLeg(homeToWork);


			Activity work = factory.createActivityFromCoord("work", ct.transform(new Coord(coordRausX, coordRausY)));
			work.setEndTime(14 * 60 * 60);
			plan.addActivity(work);
			
//			Leg workToHome = factory.createLeg(TransportMode.car);
//			plan.addLeg(workToHome);
//			
//			Activity backHome = factory.createActivityFromCoord("home",  ct.transform(scenario.createCoord (coordReinX, coordReinY)));
//			plan.addActivity(backHome);
			
			person.addPlan(plan);
			
		}

		return population;
		
	}
	
}

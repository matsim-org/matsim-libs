package playground.anhorni.csestimation;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.analysis.Bins;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.CoordUtils;

public class SurveyAnalyzer {
	private final static Logger log = Logger.getLogger(SurveyAnalyzer.class);	
	private TreeMap<Id<Person>, EstimationPerson> population;
	private String outdir;
	private DecimalFormat formatter = new DecimalFormat("0.0");
	TreeMap<Id<Location>, ShopLocation> ucs;
	
	public SurveyAnalyzer(TreeMap<Id<Person>, EstimationPerson> population, String outdir) {
		this.population = population;
		this.outdir = outdir;
		new File(this.outdir).mkdirs();
	}
	
	public SurveyAnalyzer(Population population, String outdir) {
		this.setPopulation(population);
		this.outdir = outdir;
	} 
	
	public void setPopulation(TreeMap<Id<Person>, EstimationPerson> population) {
		this.population = population;
	}
		
	public void setPopulation(Population population) {
		this.population = new TreeMap<Id<Person>, EstimationPerson>();
		for (Person p:population.getPersons().values()) {
			EstimationPerson person = (EstimationPerson)p;
			this.population.put(p.getId(), person);		
		}
	}
	
	public void analyzePS() {
		Bins sizePS = new Bins(1, 14, "sizePS");
		Bins distPS = new Bins(250.0, 6250.0, "distPS");
				
		for (EstimationPerson p : this.population.values()) {
			ArrayList<Double> dist2Home = new ArrayList<Double>();	
			boolean exclude = false;
			int frequentlyCnt = 0;			
			for (Id storeId : p.getPersonLocations().getVisitedStoresInQuerySet()) {				
				if (p.getPersonLocations().getNullAwareOrnullVisitedStoresInQuerySet().contains(storeId) ||
						p.getHomeLocation().getCoord() == null) {
					exclude = true;
					break;
				}
				else {	
					frequentlyCnt++;
					ShopLocation store = this.ucs.get(storeId);
					if (storeId.compareTo(Id.create("200023", Location.class))==0 || storeId.compareTo(Id.create("200025", Location.class))==0) continue;
					dist2Home.add(CoordUtils.calcDistance(p.getHomeLocation().getCoord(), store.getCoord()));					
				}
			}
			if (!exclude) {
				for (double v : dist2Home) {
					distPS.addVal(v, 1.0);
				}
				sizePS.addVal(frequentlyCnt, 1.0);
				
				if (frequentlyCnt == 0) {
					log.info("person " + p.getId().toString() + " has no PS");
				}
				
			}	
		}
		sizePS.plotBinnedDistribution(this.outdir, "sizePS", "");
		distPS.plotBinnedDistribution(this.outdir, "distPS", "");
	}
	
	public void analyzeArea() {
		log.info("analyzeArea ...");
		Bins areaHome = new Bins(1, 7, "harea");
		Bins areaWork = new Bins(1, 7, "warea");
		Bins areaInter = new Bins(1, 7, "iarea");
		Bins areaOther = new Bins(1, 7, "oarea");
		
		for (EstimationPerson p : this.population.values()) {
			if (p.isEmployed()) {
				areaHome.addVal(p.getAreaToShop()[0], 1.0);
				areaWork.addVal(p.getAreaToShop()[1], 1.0);
				areaInter.addVal(p.getAreaToShop()[2], 1.0);
				areaOther.addVal(p.getAreaToShop()[3], 1.0);
			}
		}	
		areaHome.plotBinnedDistribution(this.outdir, "areaHome", "");
		areaWork.plotBinnedDistribution(this.outdir, "areaWork", "");
		areaInter.plotBinnedDistribution(this.outdir, "areaInter", "");
		areaOther.plotBinnedDistribution(this.outdir, "areaOther", "");
	}
	
	public void analyzeHomeSets(String cl) {
		log.info("analyzeHomeSets ...");
		Bins awareness = new Bins(1, 11, cl + "_Hawareness");
		Bins frequently = new Bins(1, 11, cl + "_Hfrequently");
		Bins distance10 = new Bins(250, 3000, cl + "_Hdistance10");
		Bins distance9 = new Bins(250, 3000, cl + "_Hdistance9");
		Bins distance8 = new Bins(250, 3000, cl + "_Hdistance8");
		Bins distanceAwareness = new Bins(250, 3000, cl + "_Hdist_awareness");
		
		for (EstimationPerson p : this.population.values()) {
			double awarenessCnt = p.getHomeset().getAwarenessCnt();				
			if (awarenessCnt >= 0.0) {
				awareness.addVal(awarenessCnt, 1.0);
				
				int cnt = 0;
				for (Double v : p.getHomeset().getShops().keySet()) {
					cnt++;
					if (cnt == 8) {
						distance8.addVal(v, 1.0);
					}
					if (cnt == 9) {
						distance9.addVal(v, 1.0);
					}
					if (cnt == 10) {
						distance10.addVal(v, 1.0);
					}	
				}			
				distanceAwareness.addVal(p.getHomeset().getMaxDistanceAwareness(), 1.0);
			}
			double frequentlyCnt = p.getHomeset().getFrequentlyVisitedCnt();
			if (frequentlyCnt >= 0.0)  {
				frequently.addVal(frequentlyCnt, 1.0);
			}
		}
		awareness.plotBinnedDistribution(this.outdir, "", "");
		frequently.plotBinnedDistribution(this.outdir, "", "");
		distance10.plotBinnedDistribution(this.outdir, "Hdistance10", "[m]");
		distance9.plotBinnedDistribution(this.outdir, "Hdistance9", "[m]");
		distance8.plotBinnedDistribution(this.outdir, "Hdistance8", "[m]");
		distanceAwareness.plotBinnedDistribution(this.outdir, "Hdistance_aware", "[m]");
	}
		
	private void analyzeVariableBinSizeMZ() {
		log.info("analyzeVariableBinSizeMZ ...");
		double totalGenderWeight = 0.0;
		double totalAgeWeight = 0.0;
		double totalIncomeWeight = 0.0;
		
		for (EstimationPerson p : this.population.values()) {
			double weight = p.getWeight();
			int age = p.getAge();
			
			if (age >= 18) {
				totalAgeWeight += weight;
			}
			double income = p.getHhIncome();
			if (income > 0.0) { // exclude missing values
				totalIncomeWeight += weight;
			}
			String gender = p.getSex();
			if (gender.contains("f") || gender.contains("m")) {
				totalGenderWeight += weight;
			}
			
		}
		double genders[] = {0.0, 0.0};
		double incomes[] = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		double ages[] = {0.0, 0.0, 0.0, 0.0};
		
		for (EstimationPerson p : this.population.values()) {
			double weight = p.getWeight();
			if (p.getAge() > 0) {
				if (p.getAge() > 0) {
					double age = p.getAge();
					if (age >= 18 && age <= 35) {
						ages[0] += weight / totalAgeWeight * 100.0;
					}
					else if (age >= 26 && age <= 50) {
						ages[1] += weight / totalAgeWeight * 100.0;
					}
					else if (age >= 51 && age <= 65) {
						ages[2] += weight / totalAgeWeight * 100.0;
					}
					else if (age > 65) {
						ages[3] += weight / totalAgeWeight * 100.0;
					}				
				}				
			}
			int incomeCat = (int)(p.getHhIncome() - 1);
			if (incomeCat >= 0) {
				incomes[incomeCat] += weight / totalIncomeWeight * 100.0;
			}
			if (p.getSex().equals("m")) {
				genders[0] += weight / totalGenderWeight * 100.0;
			}
			else if (p.getSex().equals("f")) {
				genders[1] += weight / totalGenderWeight * 100.0;
			}
		}
		for (int i = 0; i < ages.length; i++) {
			log.info("age bin " + i + ": " + formatter.format(ages[i]));
		}
		log.info("-------------------------");
		for (int i = 0; i < genders.length; i++) {
			log.info("gender bin " + i + ": " + formatter.format(genders[i]));
		}
		log.info("-------------------------");
		for (int i = 0; i < incomes.length; i++) {
			log.info("incomes bin " + i + ": " + formatter.format(incomes[i]));
		}
		log.info("-------------------------");
	}
	
	public void analyzeVariableBinSize() {
		log.info("analyzeVariableBinSize ...");
		double totalIncomeWeight = 0.0;
		double totalAgeWeight = 0.0;
		double totalGenderWeight = 0.0;
		
		for (EstimationPerson p:this.population.values()) {
			double weight = p.getWeight();
			double age = p.getAge();
			if (age >= 18) {
				totalAgeWeight += weight;
			}
			double income = p.getHhIncome();
			if (income > 0.0) { // exclude missing values
				totalIncomeWeight += weight;
			}
			String gender = p.getSex();
			if (gender.contains("f") || gender.contains("m")) {
				totalGenderWeight += weight;
			}		
		}	
		log.info("totalIncomeWeight: " + totalIncomeWeight);
		log.info("totalAgeWeight: " + totalAgeWeight);
		log.info("totalGenderWeight: " + totalGenderWeight);
		
		double genders[] = {0.0, 0.0};
		double incomes[] = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		double ages[] = {0.0, 0.0, 0.0, 0.0};
		
		for (EstimationPerson p:this.population.values()) {
			double weight = p.getWeight();
			if (p.getAge() > 0) {
				double age = p.getAge();
				if (age >= 18 && age <= 35) {
					ages[0] += weight / totalAgeWeight * 100.0;
				}
				else if (age >= 26 && age <= 50) {
					ages[1] += weight / totalAgeWeight * 100.0;
				}
				else if (age >= 51 && age <= 65) {
					ages[2] += weight / totalAgeWeight * 100.0;
				}
				else if (age > 65) {
					ages[3] += weight / totalAgeWeight * 100.0;
				}				
			}
			double income = p.getHhIncome();
			if (income >= 0.0) {
				if (income < 2000) {
					incomes[0] += weight / totalIncomeWeight * 100.0;					
				}
				else if (income >= 2000 && income <= 4000) {
					incomes[1] += weight / totalIncomeWeight * 100.0;
				}
				else if (income >= 4001 && income <= 6000) {
					incomes[2] += weight / totalIncomeWeight * 100.0;
				}
				else if (income >= 6001 && income <= 8000) {
					incomes[3] += weight / totalIncomeWeight * 100.0;
				}
				else if (income >= 8001 && income <= 10000) {
					incomes[4] += weight / totalIncomeWeight * 100.0;
				}
				else if (income >= 10001 && income <= 12000) {
					incomes[5] += weight / totalIncomeWeight * 100.0;
				}
				else if (income >= 12001 && income <= 14000) {
					incomes[6] += weight / totalIncomeWeight * 100.0;
				}
				else if (income >= 14001 && income <= 16000) {
					incomes[7] += weight / totalIncomeWeight * 100.0;
				}
				else if (income > 16000) {
					incomes[8] += weight / totalIncomeWeight * 100.0;
				}
			}
			if (p.getSex().equals("m")) {
				genders[0] += weight / totalGenderWeight * 100.0;
			}
			else if (p.getSex().equals("f")) {
				genders[1] += weight / totalGenderWeight * 100.0;
			}
		}
		for (int i = 0; i < ages.length; i++) {
			log.info("age bin " + i + ": " + formatter.format(ages[i]));
		}
		log.info("-------------------------");
		for (int i = 0; i < genders.length; i++) {
			log.info("gender bin " + i + ": " + formatter.format(genders[i]));
		}
		log.info("-------------------------");
		for (int i = 0; i < incomes.length; i++) {
			log.info("incomes bin " + i + ": " + formatter.format(incomes[i]));
		}
		log.info("-------------------------");
	}
	
	public void analyzeMZ() {
		log.info("Analyzing " + this.population.size() + " persons");
		this.analyzeVariableBinSizeMZ();
		
		Bins ageBins = new Bins(10, 100, "age"); //	interval, maxVal
		Bins incomeBins = new Bins(1, 9, "income");
		
		for (EstimationPerson p:this.population.values()) {				
			if (p.getAge() > 0) {
				ageBins.addVal(p.getAge(), p.getWeight());
			}			
			if (p.getHhIncome() > 0.0) {
				incomeBins.addVal(p.getHhIncome() - 1.0, p.getWeight());
			}	
		}
		ageBins.plotBinnedDistribution(outdir, "age bins", " year");
		incomeBins.plotBinnedDistribution(outdir, "income bins", " income cat");		
	}
	
	public void setUcs(TreeMap<Id<Location>, ShopLocation> ucs) {
		this.ucs = ucs;
	}
}

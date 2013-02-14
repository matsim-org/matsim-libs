package playground.anhorni.finalruns;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.anhorni.csestimation.ShopLocation;
import playground.anhorni.csestimation.ShopsEnricher;

public class AddAttributesAndEpsilons {	
	private double avg_size = 0.0;
	private double avg_price = 0.0;
	
	private ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private final static Logger log = Logger.getLogger(AddAttributesAndEpsilons.class);
	
	private String outdir;
	
	public static void main(final String[] args) {		
		AddAttributesAndEpsilons adapter = new AddAttributesAndEpsilons();		
		adapter.run(args[0], args[1], args[2], args[3], args[4]);		
		log.info("Adaptation finished -----------------------------------------");
	}
	
	public void run(final String plansFilePath, final String networkFilePath, final String facilitiesFilePath, final String outputFolder, final String bzFile) {
		this.init(plansFilePath, networkFilePath, facilitiesFilePath, outputFolder);
		this.assignSizeAndPrice(bzFile);
	}
	
	private void init(final String plansFilePath, final String networkFilePath, final String facilitiesFilePath, final String outputFolder) {		
		this.outdir = outputFolder;
		new MatsimNetworkReader(scenario).readFile(networkFilePath);		
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesFilePath);
		MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenario);
		populationReader.readFile(plansFilePath);
	}
	
	private void assignSizeAndPrice(final String bzFile) {
		TreeMap<Id, ShopLocation> shops = new TreeMap<Id, ShopLocation>();
		
		for (ActivityFacility f:this.scenario.getActivityFacilities().getFacilities().values()) {
			shops.put(f.getId(), new ShopLocation(f.getId(), f.getCoord()));
		}		
		ShopsEnricher enricher = new ShopsEnricher();
		enricher.setShops(shops);
		try {
			enricher.readBZ(bzFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
		enricher.assignSize();
		enricher.assignPrice();
		
		this.computeAttributeAverages(shops);
		this.writeScaledValues(shops);
	}
	
	private void computeAttributeAverages(TreeMap<Id, ShopLocation> shops) {
		int cntSize = 0;
		int cntPrice = 0;
		for (ShopLocation shop:shops.values()) {
			// write size and price
			if (shop.getPrice() > 0) {
				cntPrice++;
				this.avg_price += shop.getPrice();
			}
			if (shop.getSize() > 0) {
				cntSize++;
				this.avg_size += shop.getSize();
			}
		}
		this.avg_price /= cntPrice;
		this.avg_size /= cntSize;
	}
	
	private void writeScaledValues(TreeMap<Id, ShopLocation> shops) {
		ObjectAttributes facilitiyAttributes = new ObjectAttributes();	
		for (ShopLocation shop:shops.values()) {
			double priceScaled = this.avg_price;
			double sizeScaled = this.avg_size;
			if (shop.getPrice() > 0) {
				priceScaled = shop.getPrice() - this.avg_price;
			}
			if (shop.getSize() > 0) {
				sizeScaled = shop.getSize() - this.avg_size;
			}
			facilitiyAttributes.putAttribute(shop.getId().toString(), "sizeScaled", sizeScaled);
			facilitiyAttributes.putAttribute(shop.getId().toString(), "priceScaled", priceScaled);
		}
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(facilitiyAttributes);
		attributesWriter.writeFile(this.outdir + "facilityAttributes.xml");
		
		log.info("average size: " + this.avg_size);
		log.info("average price: " + this.avg_price);		
	}	
}

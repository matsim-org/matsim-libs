package playground.gregor.sims.run;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.evacuation.base.Building;
import org.matsim.evacuation.base.BuildingsShapeReader;
import org.matsim.evacuation.base.EvacuationNetFromNetcdfGenerator;
import org.matsim.evacuation.base.EvacuationNetGenerator;
import org.matsim.evacuation.base.NetworkChangeEventsFromNetcdf;
import org.matsim.evacuation.flooding.FloodingReader;
import org.matsim.evacuation.riskaversion.RiskCostFromFloodingData;
import org.matsim.evacuation.run.EvacuationQSimControllerII;
import org.matsim.evacuation.socialcost.SocialCostCalculatorSingleLink;
import org.matsim.evacuation.travelcosts.PluggableTravelCostCalculator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.gregor.sims.shelters.allocation.EvacuationShelterNetLoaderForShelterAllocation;
import playground.gregor.sims.shelters.allocation.GreedyShelterAllocator;
import playground.gregor.sims.shelters.allocation.ShelterAllocationRePlanner;
import playground.gregor.sims.shelters.allocation.ShelterCounter;
import playground.gregor.sims.shelters.allocation.ShelterLocationRePlannerII;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;

public class ShelterAllocationController extends Controler {

	final private static Logger log = Logger.getLogger(EvacuationQSimControllerII.class);

	private List<Building> buildings;

	private List<FloodingReader> netcdfReaders = null;

	private EvacuationShelterNetLoaderForShelterAllocation esnl = null;

	private HashMap<Id, Building> shelterLinkMapping = null;

	PluggableTravelCostCalculator pluggableTravelCost = null;

	private int shift;

	private double pshelter;

	private String plans;




	public ShelterAllocationController(String[] args, int shift, double pshelter, String plans) {
		super(args);
		this.shift = shift;
		this.pshelter = pshelter;
		this.setOverwriteFiles(true);
		this.plans = plans;
		//		this.config.scenario().setUseSignalSystems(true);
		//		this.config.scenario().setUseLanes(true);
		this.config.setQSimConfigGroup(new QSimConfigGroup());
	}


	@Override
	protected void setUp(){
		super.setUp();

		//		if (this.scenarioData.getConfig().evacuation().isLoadShelters()) {
		//			loadShelterSignalSystems();
		//		}

		if (this.scenarioData.getConfig().evacuation().isSocialCostOptimization()) {
			initSocialCostOptimization();
		}

		if (this.scenarioData.getConfig().evacuation().isRiskMinimization()) {
			initRiskMinimization();
		}



		initPluggableTravelCostCalculator();
		if (shift >= 1) {
			
			ShelterCounter sc = new ShelterCounter(this.scenarioData.getNetwork(), this.shelterLinkMapping);
			if (shift == 2) {
				ShelterLocationRePlannerII sLRP = new ShelterLocationRePlannerII(this.getScenario(), this.pluggableTravelCost, this.getTravelTimeCalculator(), this.buildings,sc);
				this.addControlerListener(sLRP);
//			this.events.addHandler(sc);
			}
			
			ShelterAllocationRePlanner sARP = new ShelterAllocationRePlanner(this.getScenario(), this.pluggableTravelCost, this.getTravelTimeCalculator(), this.buildings,sc, this.pshelter);
			this.addControlerListener(sARP);
		}


		unloadNetcdfReaders();
	}

	private void initSocialCostOptimization() {
		initPluggableTravelCostCalculator();
		SocialCostCalculatorSingleLink sc = new SocialCostCalculatorSingleLink(this.network,getEvents());
		this.pluggableTravelCost.addTravelCost(sc);
		this.events.addHandler(sc);
		this.strategyManager = loadStrategyManager();
		this.addControlerListener(sc);
	}


	private void initRiskMinimization() {
		initPluggableTravelCostCalculator();
		loadNetcdfReaders();

		RiskCostFromFloodingData rc = new RiskCostFromFloodingData(this.network, this.netcdfReaders,getEvents(),this.scenarioData.getConfig().evacuation().getBufferSize());
		this.pluggableTravelCost.addTravelCost(rc);
		this.events.addHandler(rc);
	}

	private void initPluggableTravelCostCalculator() {
		if (this.pluggableTravelCost == null) {
			if (this.travelTimeCalculator == null) {
				this.travelTimeCalculator = this.getTravelTimeCalculatorFactory().createTravelTimeCalculator(this.network, this.config.travelTimeCalculator());
			}
			

			
			this.pluggableTravelCost = new PluggableTravelCostCalculator(this.travelTimeCalculator);
			this.setTravelCostCalculatorFactory(new TravelCostCalculatorFactory() {


				// This is thread-safe because pluggableTravelCost is thread-safe.

				@Override
				public PersonalizableTravelCost createTravelCostCalculator(
						PersonalizableTravelTime timeCalculator,
						CharyparNagelScoringConfigGroup cnScoringGroup) {
					return ShelterAllocationController.this.pluggableTravelCost;
				}

			});
		}
	}

//	private void loadShelterSignalSystems() {
//		this.config.network().setLaneDefinitionsFile("nullnull");
//
//		ShelterInputCounterSignalSystems sic = new ShelterInputCounterSignalSystems(this.scenarioData,this.shelterLinkMapping);
//		this.events.addHandler(sic);
//
//		this.addControlerListener(new ShelterDoorBlockerSetup());
//		this.getQueueSimulationListener().add(sic);
//
//	}


	private void unloadNetcdfReaders() {
		this.netcdfReaders = null;
		log.info("netcdf readers destroyed");
	}

	private void loadNetcdfReaders() {
		if (this.netcdfReaders != null) {
			return;
		}
		log.info("loading netcdf readers");
		int count = this.scenarioData.getConfig().evacuation().getSWWFileCount();
		if (count <= 0) {
			return;
		}
		this.netcdfReaders  = new ArrayList<FloodingReader>();
		double offsetEast = this.scenarioData.getConfig().evacuation().getSWWOffsetEast();
		double offsetNorth = this.scenarioData.getConfig().evacuation().getSWWOffsetNorth();
		for (int i = 0; i < count; i++) {
			String netcdf = this.scenarioData.getConfig().evacuation().getSWWRoot() + "/" + this.scenarioData.getConfig().evacuation().getSWWFilePrefix() + i + this.scenarioData.getConfig().evacuation().getSWWFileSuffix();
			FloodingReader fr = new FloodingReader(netcdf);
			fr.setReadTriangles(true);
			fr.setOffset(offsetEast, offsetNorth);
			this.netcdfReaders.add(fr);
		}
		log.info("done.");
	}

	private void loadNetWorkChangeEvents(NetworkImpl net) {
		loadNetcdfReaders();
		if (this.netcdfReaders == null) {
			throw new RuntimeException("No netcdf reader could be loaded!");
		} else if (!net.getFactory().isTimeVariant()) {
			throw new RuntimeException("Network layer is not time variant!");
		} else if (net.getNetworkChangeEvents() != null) {
			throw new RuntimeException("Network change events allready loaded!");
		}
		List<NetworkChangeEvent> events = new NetworkChangeEventsFromNetcdf(this.netcdfReaders,this.scenarioData).createChangeEvents();
		net.setNetworkChangeEvents(events);
	}


	@Override
	protected void loadData() {
		super.loadData();

		// network
		NetworkImpl net = this.scenarioData.getNetwork();

		if (this.scenarioData.getConfig().evacuation().isLoadShelters()) {
			if (this.buildings == null) {
				this.buildings = BuildingsShapeReader.readDataFile(this.config.evacuation().getBuildingsFile(),this.config.evacuation().getSampleSize());
			}
			if (this.scenarioData.getConfig().evacuation().isGenerateEvacNetFromSWWFile()) {
				loadNetcdfReaders();
			}
			this.esnl = new EvacuationShelterNetLoaderForShelterAllocation(this.buildings,this.scenarioData,this.netcdfReaders);
			net = this.esnl.getNetwork();
			this.shelterLinkMapping = this.esnl.getShelterLinkMapping();

		} else {
			if (this.scenarioData.getConfig().evacuation().isGenerateEvacNetFromSWWFile()) {
				loadNetcdfReaders();
				new EvacuationNetFromNetcdfGenerator(net, this.scenarioData.getConfig(), this.netcdfReaders).run();
			} else {
				new EvacuationNetGenerator(net, this.config).run();
			}
		}

		if (this.scenarioData.getConfig().network().isTimeVariantNetwork() && this.scenarioData.getConfig().evacuation().isGenerateEvacNetFromSWWFile() ) {
			loadNetWorkChangeEvents(net);
		}


		if (this.scenarioData.getConfig().evacuation().isLoadPopulationFromShapeFile()) {
			if (this.scenarioData.getPopulation().getPersons().size() > 0 ) {
				throw new RuntimeException("Population already loaded. In order to load population from shape file, the population input file paramter in the population section of the config.xml must not be set!");
			}
			// population
			if (this.buildings == null) {
				this.buildings = BuildingsShapeReader.readDataFile(this.config.evacuation().getBuildingsFile(),this.config.evacuation().getSampleSize());
			}

			if (this.scenarioData.getConfig().evacuation().isGenerateEvacNetFromSWWFile()) {
				new GreedyShelterAllocator(this.scenarioData.getPopulation(),this.buildings,this.scenarioData,this.esnl,this.netcdfReaders).getPopulation();
			} else {
				new GreedyShelterAllocator(this.scenarioData.getPopulation(),this.buildings,this.scenarioData,this.esnl,null).getPopulation();
			}
		} else {
//			throw new RuntimeException("This does not work!");
			//			if (this.scenarioData.getConfig().evacuation().getEvacuationScanrio() != EvacuationScenario.night) {
			//				throw new RuntimeException("Evacuation simulation from plans file so far only works for the night scenario.");
			//			}
			//			new EvacuationPlansGenerator(this.population,this.network,this.network.getLinks().get(new IdImpl("el1"))).run();
		}

//		this.scenarioData.getPopulation().getPersons().clear();
//		new PopulationReaderMatsimV4(getScenario()).readFile(this.plans);
//		this.population = this.scenarioData.getPopulation();

		//		if (this.scenarioData.getConfig().evacuation().isLoadShelters()) {
		//			this.esnl.generateShelterLinks();
		//		}
	}

	public static void main(final String[] args) {
		int shift = Integer.parseInt(args[1]);
		double pshelter = Double.parseDouble(args[2]);
		String plans = args[3];
//		String shelterFile = args[3];
		final Controler controler = new ShelterAllocationController(args,shift,pshelter,plans);
		controler.run();
//		try {
//			dumpShelters(((ShelterAllocationController)controler).buildings,"/home/laemmel/devel/allocation/output/output_shelters.shp");
//		} catch (FactoryRegistryException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (SchemaException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IllegalAttributeException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		System.exit(0);
	}


	private static void dumpShelters(List<Building> buildings2, String string) throws FactoryRegistryException, SchemaException, IllegalAttributeException, IOException {
		CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.WGS84_UTM47S);
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("Point",Point.class, true, null, null, targetCRS);
		AttributeType id = AttributeTypeFactory.newAttributeType("id", String.class);
		AttributeType cap = AttributeTypeFactory.newAttributeType("capacity", Integer.class);
//		AttributeType agLost = AttributeTypeFactory.newAttributeType("agLost", Integer.class);
//		AttributeType agLostRate = AttributeTypeFactory.newAttributeType("agLostRate", Double.class);
//		AttributeType agLostPerc = AttributeTypeFactory.newAttributeType("agLostPerc", Integer.class);
//		AttributeType agLostPercStr = AttributeTypeFactory.newAttributeType("agLostPercStr", String.class);
		
		FeatureType ft = FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, id,cap}, "Shelters");
		List<Feature> fts = new ArrayList<Feature>();
		for (Building b : buildings2) {
			if (b.isQuakeProof()){
				Geometry geo = b.getGeo();
				if (geo == null) {
					continue;
				}
				Point p = null;
				if (geo instanceof MultiPoint) {
					p = (Point) geo.getGeometryN(0);
				} else {
					p = (Point) geo;
				}
				fts.add(ft.create(new Object[] {p,b.getId().toString(),b.getShelterSpace()}));
			}
		}
		ShapeFileWriter.writeGeometries(fts, string);
	}

}

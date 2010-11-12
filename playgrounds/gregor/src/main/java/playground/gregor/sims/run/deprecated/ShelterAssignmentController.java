/* *********************************************************************** *
 * project: org.matsim.*
 * ShelterAllocationController.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.gregor.sims.run.deprecated;

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
import org.matsim.core.config.Module;
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
import org.matsim.evacuation.config.EvacuationConfigGroup;
import org.matsim.evacuation.flooding.FloodingReader;
import org.matsim.evacuation.riskaversion.RiskCostFromFloodingData;
import org.matsim.evacuation.run.EvacuationQSimControllerII;
import org.matsim.evacuation.socialcost.SocialCostCalculatorSingleLink;
import org.matsim.evacuation.travelcosts.PluggableTravelCostCalculator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.gregor.sims.shelters.assignment.EvacuationShelterNetLoaderForShelterAllocation;
import playground.gregor.sims.shelters.assignment.GreedyShelterAllocator;
import playground.gregor.sims.shelters.assignment.ShelterAssignmentRePlanner;
import playground.gregor.sims.shelters.assignment.ShelterCapacityRePlanner;
import playground.gregor.sims.shelters.assignment.ShelterCounter;
import playground.gregor.sims.shelters.assignment.ShelterLocationRePlannerII;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;

@Deprecated
public class ShelterAssignmentController extends Controler {

	final private static Logger log = Logger.getLogger(EvacuationQSimControllerII.class);

	private List<Building> buildings;

	private List<FloodingReader> netcdfReaders = null;

	private EvacuationShelterNetLoaderForShelterAllocation esnl = null;

	private HashMap<Id, Building> shelterLinkMapping = null;

	PluggableTravelCostCalculator pluggableTravelCost = null;

	private final int shift;

	private final double pshelter;

	private final String plans;

	private EvacuationConfigGroup ec;

	public ShelterAssignmentController(String[] args, int shift, double pshelter, String plans) {
		super(args);
		this.shift = shift;
		this.pshelter = pshelter;
		setOverwriteFiles(true);
		this.plans = plans;
		// this.config.scenario().setUseSignalSystems(true);
		// this.config.scenario().setUseLanes(true);
		this.config.setQSimConfigGroup(new QSimConfigGroup());
	}

	@Override
	protected void setUp() {
		super.setUp();

		// if (this.scenarioData.getConfig().evacuation().isLoadShelters()) {
		// loadShelterSignalSystems();
		// }

		if (this.ec.isSocialCostOptimization()) {
			initSocialCostOptimization();
		}

		if (this.ec.isRiskMinimization()) {
			initRiskMinimization();
		}

		initPluggableTravelCostCalculator();
		if (this.shift >= 1) {

			ShelterCounter sc = new ShelterCounter(this.scenarioData.getNetwork(), this.shelterLinkMapping);
			if (this.shift == 2) {
				ShelterCapacityRePlanner scap = new ShelterCapacityRePlanner(getScenario(), this.pluggableTravelCost, getTravelTimeCalculator(), this.buildings, sc);
				addControlerListener(scap);

				// ShelterLocationRePlannerII sLRP = new
				// ShelterLocationRePlannerII(getScenario(),
				// this.pluggableTravelCost, getTravelTimeCalculator(),
				// this.buildings, sc);
				// addControlerListener(sLRP);
				// this.events.addHandler(sc);
			}

			ShelterAssignmentRePlanner sARP = new ShelterAssignmentRePlanner(getScenario(), this.pluggableTravelCost, getTravelTimeCalculator(), this.buildings, sc);
			addControlerListener(sARP);
		}

		unloadNetcdfReaders();
	}

	private void initSocialCostOptimization() {
		initPluggableTravelCostCalculator();
		SocialCostCalculatorSingleLink sc = new SocialCostCalculatorSingleLink(this.network, this.config.travelTimeCalculator().getTraveltimeBinSize(), getEvents());
		this.pluggableTravelCost.addTravelCost(sc);
		this.events.addHandler(sc);
		this.strategyManager = loadStrategyManager();
		addControlerListener(sc);
	}

	private void initRiskMinimization() {
		initPluggableTravelCostCalculator();
		loadNetcdfReaders();

		RiskCostFromFloodingData rc = new RiskCostFromFloodingData(this.network, this.netcdfReaders, getEvents(), this.ec.getBufferSize());
		this.pluggableTravelCost.addTravelCost(rc);
		this.events.addHandler(rc);
	}

	private void initPluggableTravelCostCalculator() {
		if (this.pluggableTravelCost == null) {
			if (this.travelTimeCalculator == null) {
				this.travelTimeCalculator = getTravelTimeCalculatorFactory().createTravelTimeCalculator(this.network, this.config.travelTimeCalculator());
			}

			this.pluggableTravelCost = new PluggableTravelCostCalculator(this.travelTimeCalculator);
			setTravelCostCalculatorFactory(new TravelCostCalculatorFactory() {

				// This is thread-safe because pluggableTravelCost is
				// thread-safe.

				@Override
				public PersonalizableTravelCost createTravelCostCalculator(PersonalizableTravelTime timeCalculator, CharyparNagelScoringConfigGroup cnScoringGroup) {
					return ShelterAssignmentController.this.pluggableTravelCost;
				}

			});
		}
	}

	// private void loadShelterSignalSystems() {
	// this.config.network().setLaneDefinitionsFile("nullnull");
	//
	// ShelterInputCounterSignalSystems sic = new
	// ShelterInputCounterSignalSystems(this.scenarioData,this.shelterLinkMapping);
	// this.events.addHandler(sic);
	//
	// this.addControlerListener(new ShelterDoorBlockerSetup());
	// this.getQueueSimulationListener().add(sic);
	//
	// }

	private void unloadNetcdfReaders() {
		this.netcdfReaders = null;
		log.info("netcdf readers destroyed");
	}

	private void loadNetcdfReaders() {
		if (this.netcdfReaders != null) {
			return;
		}
		log.info("loading netcdf readers");
		int count = this.ec.getSWWFileCount();
		if (count <= 0) {
			return;
		}
		this.netcdfReaders = new ArrayList<FloodingReader>();
		double offsetEast = this.ec.getSWWOffsetEast();
		double offsetNorth = this.ec.getSWWOffsetNorth();
		for (int i = 0; i < count; i++) {
			String netcdf = this.ec.getSWWRoot() + "/" + this.ec.getSWWFilePrefix() + i + this.ec.getSWWFileSuffix();
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
		List<NetworkChangeEvent> events = new NetworkChangeEventsFromNetcdf(this.netcdfReaders, this.scenarioData).createChangeEvents();
		net.setNetworkChangeEvents(events);
	}

	@Override
	protected void loadData() {
		super.loadData();

		Module m = this.config.getModule("evacuation");
		this.ec = new EvacuationConfigGroup(m);
		this.config.getModules().put("evacuation", this.ec);

		// network
		NetworkImpl net = this.scenarioData.getNetwork();

		if (this.ec.isLoadShelters()) {
			if (this.buildings == null) {
				this.buildings = BuildingsShapeReader.readDataFile(this.ec.getBuildingsFile(), this.ec.getSampleSize());
			}
			if (this.ec.isGenerateEvacNetFromSWWFile()) {
				loadNetcdfReaders();
			}
			this.esnl = new EvacuationShelterNetLoaderForShelterAllocation(this.buildings, this.scenarioData, this.netcdfReaders);
			net = this.esnl.getNetwork();
			this.shelterLinkMapping = this.esnl.getShelterLinkMapping();

		} else {
			if (this.ec.isGenerateEvacNetFromSWWFile()) {
				loadNetcdfReaders();
				new EvacuationNetFromNetcdfGenerator(net, this.scenarioData.getConfig(), this.netcdfReaders).run();
			} else {
				new EvacuationNetGenerator(net, this.config).run();
			}
		}

		if (this.scenarioData.getConfig().network().isTimeVariantNetwork() && this.ec.isGenerateEvacNetFromSWWFile()) {
			loadNetWorkChangeEvents(net);
		}

		if (this.ec.isLoadPopulationFromShapeFile()) {
			if (this.scenarioData.getPopulation().getPersons().size() > 0) {
				throw new RuntimeException("Population already loaded. In order to load population from shape file, the population input file paramter in the population section of the config.xml must not be set!");
			}
			// population
			if (this.buildings == null) {
				this.buildings = BuildingsShapeReader.readDataFile(this.ec.getBuildingsFile(), this.ec.getSampleSize());
			}

			if (this.ec.isGenerateEvacNetFromSWWFile()) {
				new GreedyShelterAllocator(this.scenarioData.getPopulation(), this.buildings, this.scenarioData, this.esnl, this.netcdfReaders).getPopulation();
			} else {
				new GreedyShelterAllocator(this.scenarioData.getPopulation(), this.buildings, this.scenarioData, this.esnl, null).getPopulation();
			}
		} else {
			// throw new RuntimeException("This does not work!");
			// if
			// (this.scenarioData.getConfig().evacuation().getEvacuationScanrio()
			// != EvacuationScenario.night) {
			// throw new
			// RuntimeException("Evacuation simulation from plans file so far only works for the night scenario.");
			// }
			// new
			// EvacuationPlansGenerator(this.population,this.network,this.network.getLinks().get(new
			// IdImpl("el1"))).run();
		}

		// this.scenarioData.getPopulation().getPersons().clear();
		// new PopulationReaderMatsimV4(getScenario()).readFile(this.plans);
		// this.population = this.scenarioData.getPopulation();

		// if (this.scenarioData.getConfig().evacuation().isLoadShelters()) {
		// this.esnl.generateShelterLinks();
		// }
	}

	public static void main(final String[] args) {
		int shift = Integer.parseInt(args[1]);
		double pshelter = Double.parseDouble(args[2]);
		String plans = args[3];
		// String shelterFile = args[3];
		final Controler controler = new ShelterAssignmentController(args, shift, pshelter, plans);
		controler.run();
		// try {
		// dumpShelters(((ShelterAllocationController)controler).buildings,"/home/laemmel/devel/allocation/output/output_shelters.shp");
		// } catch (FactoryRegistryException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (SchemaException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (IllegalAttributeException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		System.exit(0);
	}

	private static void dumpShelters(List<Building> buildings2, String string) throws FactoryRegistryException, SchemaException, IllegalAttributeException, IOException {
		CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.WGS84_UTM47S);
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("Point", Point.class, true, null, null, targetCRS);
		AttributeType id = AttributeTypeFactory.newAttributeType("id", String.class);
		AttributeType cap = AttributeTypeFactory.newAttributeType("capacity", Integer.class);
		// AttributeType agLost =
		// AttributeTypeFactory.newAttributeType("agLost", Integer.class);
		// AttributeType agLostRate =
		// AttributeTypeFactory.newAttributeType("agLostRate", Double.class);
		// AttributeType agLostPerc =
		// AttributeTypeFactory.newAttributeType("agLostPerc", Integer.class);
		// AttributeType agLostPercStr =
		// AttributeTypeFactory.newAttributeType("agLostPercStr", String.class);

		FeatureType ft = FeatureTypeFactory.newFeatureType(new AttributeType[] { geom, id, cap }, "Shelters");
		List<Feature> fts = new ArrayList<Feature>();
		for (Building b : buildings2) {
			if (b.isQuakeProof()) {
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
				fts.add(ft.create(new Object[] { p, b.getId().toString(), b.getShelterSpace() }));
			}
		}
		ShapeFileWriter.writeGeometries(fts, string);
	}

}

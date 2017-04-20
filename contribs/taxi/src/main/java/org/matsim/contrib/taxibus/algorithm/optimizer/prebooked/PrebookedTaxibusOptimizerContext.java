package org.matsim.contrib.taxibus.algorithm.optimizer.prebooked;

import org.matsim.api.core.v01.*;
import org.matsim.contrib.taxibus.algorithm.scheduler.TaxibusScheduler;
import org.matsim.contrib.taxibus.run.configuration.TaxibusConfigGroup;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.*;

public class PrebookedTaxibusOptimizerContext {
	public final Fleet vrpData;
	public final Scenario scenario;
	public final MobsimTimer timer;
	public final TravelTime travelTime;
	public final TravelDisutility travelDisutility;
	public final TaxibusScheduler scheduler;
	public final RequestDeterminator requestDeterminator;
	public final RequestFilter requestFilter;
	public final String workingDirectory;
	public final TaxibusConfigGroup tbcg;

	public final double clustering_period_min;
	public final double prebook_period_min;
	public final int capacity;
	public final int vehiclesAtSameTime;
	public final int clusteringRounds;
	public final double minOccupancy;
	public final boolean returnToDepot;

	/**
	 * Optimizer context object for prebooked taxibus algorithms.
	 * 
	 * @param vrpData
	 * @param scenario
	 * @param timer
	 * @param travelTime
	 * @param travelDisutility
	 * @param scheduler
	 * @param tbcg
	 */
	public PrebookedTaxibusOptimizerContext(Fleet vrpData, Scenario scenario, MobsimTimer timer, TravelTime travelTime,
			TravelDisutility travelDisutility, TaxibusScheduler scheduler, TaxibusConfigGroup tbcg) {
		this.vrpData = vrpData;
		this.scenario = scenario;
		this.returnToDepot = tbcg.isReturnToDepot();
		this.timer = timer;
		this.travelTime = travelTime;
		this.travelDisutility = travelDisutility;
		this.scheduler = scheduler;
		this.workingDirectory = scenario.getConfig().controler().getOutputDirectory();
		this.tbcg = tbcg;
		capacity = tbcg.getVehCap();
		clustering_period_min = tbcg.getClustering_period_min();
		prebook_period_min = tbcg.getPrebook_period_min();
		clusteringRounds = tbcg.getClusteringRounds();
		minOccupancy = tbcg.getMinOccupancy();
		vehiclesAtSameTime = tbcg.getNumberOfVehiclesDispatchedAtSameTime();
		Coord coord1 = scenario.getNetwork().getLinks().get(Id.createLinkId(tbcg.getServiceAreaCentroid_1()))
				.getCoord();
		Coord coord2 = scenario.getNetwork().getLinks().get(Id.createLinkId(tbcg.getServiceAreaCentroid_2()))
				.getCoord();
		this.requestDeterminator = new CentroidBasedRequestDeterminatorAndFilter(coord1, coord2,
				tbcg.getServiceArea_1_Radius_m(), tbcg.getServiceArea_2_Radius_m());
		this.requestFilter = (CentroidBasedRequestDeterminatorAndFilter)requestDeterminator;
	}
}

package org.matsim.contrib.freight.vrp.algorithms;

import java.util.Collection;
import java.util.Collections;
import java.util.Random;

import org.matsim.contrib.freight.vrp.algorithms.rr.InitialSolutionBestInsertion;
import org.matsim.contrib.freight.vrp.algorithms.rr.InitialSolutionFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.JobDistanceAvgCosts;
import org.matsim.contrib.freight.vrp.algorithms.rr.RecreationBestInsertion;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreate;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinRadial;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinRandom;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinStrategyManager;
import org.matsim.contrib.freight.vrp.algorithms.rr.ThresholdFunctionSchrimpf;
import org.matsim.contrib.freight.vrp.algorithms.rr.listener.RuinAndRecreateListener;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.ServiceProviderAgentFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.SingleDepotDistribTWSPFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.TourCost;
import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.TourImpl;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemSolver;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemSolution;
import org.matsim.contrib.freight.vrp.utils.RandomNumberGeneration;

public class CVRPTWSolver implements VehicleRoutingProblemSolver {

	private final VehicleRoutingProblem vrp;

	private Random random = RandomNumberGeneration.getRandom();

	private int iterations = 100;

	private int warmupIterations = 10;

	private Collection<RuinAndRecreateListener> listener;
	
	private VehicleRoutingProblemSolution iniSolution;

	public CVRPTWSolver(final VehicleRoutingProblem vrp) {
		super();
		this.vrp = vrp;
		listener = Collections.EMPTY_LIST;
	}

	public CVRPTWSolver(VehicleRoutingProblem vrp, VehicleRoutingProblemSolution initialSolution) {
		this.vrp = vrp;
		this.iniSolution = initialSolution;
	}

	@Override
	public VehicleRoutingProblemSolution solve() {
		checkWhetherProblemIsOfCorrectType();

		RuinAndRecreate ruinAndRecreateAlgo = new RuinAndRecreate(vrp);
		InitialSolutionFactory iniSolutionFactory = new InitialSolutionBestInsertion(getSPFactory());
		ruinAndRecreateAlgo.setInitialSolutionFactory(iniSolutionFactory);
		
		ruinAndRecreateAlgo.setIterations(iterations);
		ruinAndRecreateAlgo.setWarmUpIterations(warmupIterations);
		ruinAndRecreateAlgo.setTourAgentFactory(getSPFactory());
		ruinAndRecreateAlgo.setRuinStrategyManager(new RuinStrategyManager());

		RecreationBestInsertion bestInsertion = new RecreationBestInsertion();
		bestInsertion.setRandom(random);
		ruinAndRecreateAlgo.setRecreationStrategy(bestInsertion);

		RuinRadial radialRuin = new RuinRadial(vrp, new JobDistanceAvgCosts(
				vrp.getCosts()));
		radialRuin.setRuinFraction(0.3);
		radialRuin.setRandom(random);

		RuinRandom randomRuin = new RuinRandom(vrp);
		randomRuin.setRuinFraction(0.5);
		randomRuin.setRandom(random);

		ruinAndRecreateAlgo.getRuinStrategyManager().addStrategy(radialRuin,
				0.5);
		ruinAndRecreateAlgo.getRuinStrategyManager().addStrategy(randomRuin,
				0.5);

		ruinAndRecreateAlgo.setThresholdFunction(new ThresholdFunctionSchrimpf(
				0.1));

		for (RuinAndRecreateListener l : listener) {
			ruinAndRecreateAlgo.getControlerListeners().add(l);
		}

		return ruinAndRecreateAlgo.solve();
	}

	private ServiceProviderAgentFactory getSPFactory() {
		TourCost tourCost = new TourCost() {

			@Override
			public double getTourCost(TourImpl tour, Driver driver,
					Vehicle vehicle) {
				return vehicle.getType().vehicleCostParams.fix
						+ tour.tourData.transportCosts;
			}
		};
		return new SingleDepotDistribTWSPFactory(tourCost, vrp.getCosts());
	}

	private void checkWhetherProblemIsOfCorrectType() {
		String location = null;
		for (Vehicle v : vrp.getVehicles()) {
			if (location == null) {
				location = v.getLocationId();
			} else if (!location.toString().equals(v.getLocationId())) {
				throw new IllegalStateException(
						"if you use this solver "
								+ this.getClass().toString()
								+ "), all vehicles must have the same depot-location. vehicle "
								+ v.getId() + " has not.");
			}
		}
		for (Job j : vrp.getJobs().values()) {
			if (location == null) {
				return;
			}
			if (j instanceof Shipment) {
				Shipment s = (Shipment) j;
				if (!s.getFromId().equals(location)) {
					throw new IllegalStateException(
							"if you use this solver, all shipments must have the same from-location. errorShipment "
									+ s);
				}
			}

		}

	}

	public int getIterations() {
		return iterations;
	}

	public void setIterations(int iterations) {
		this.iterations = iterations;
	}

	public int getWarmupIterations() {
		return warmupIterations;
	}

	public void setWarmupIterations(int warmupIterations) {
		this.warmupIterations = warmupIterations;
	}

	public void setListener(Collection<RuinAndRecreateListener> listener) {
		this.listener = listener;

	}

}

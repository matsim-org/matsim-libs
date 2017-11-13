package org.matsim.contrib.carsharing.relocation.infrastructure;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.carsharing.relocation.demand.CarsharingVehicleRelocationContainer;
import org.matsim.contrib.carsharing.relocation.events.DispatchRelocationsEvent;
import org.matsim.contrib.carsharing.relocation.events.handlers.DispatchRelocationsEventHandler;
import org.matsim.contrib.carsharing.relocation.listeners.CarSharingDemandTracker;
import org.matsim.contrib.carsharing.relocation.qsim.RelocationAgent;
import org.matsim.contrib.carsharing.relocation.qsim.RelocationInfo;
import org.matsim.contrib.carsharing.relocation.utils.RelocationZoneKmlWriter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.replanning.strategies.KeepLastSelected;
import org.matsim.core.utils.misc.Time;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.matrices.Matrix;

import com.google.inject.Inject;
import com.vividsolutions.jts.geom.MultiPolygon;

public class AverageDemandRelocationListener implements IterationStartsListener, DispatchRelocationsEventHandler {

	public static final Logger log = Logger.getLogger("dummy");

	@Inject private CarsharingVehicleRelocationContainer carsharingVehicleRelocation;

	@Inject private CarSharingDemandTracker demandTracker;
	@Inject StrategyManager strategyManager;
	// temp
	@Inject private OutputDirectoryHierarchy outputDirectoryHierarchy;

	private int iteration;

	protected Map<String, Map<Double, Matrices>> avgODMatrices;

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		this.iteration = event.getIteration();

		if (this.iteration >= this.carsharingVehicleRelocation.moduleEnableAfterIteration()) {
			List<Map<String, Map<Double, Matrices>>> previousODMatricesList = new ArrayList<Map<String, Map<Double, Matrices>>>();

			int demandEstimateIterations = this.carsharingVehicleRelocation.demandEstimateIterations();
			for (int i = 1; i <= demandEstimateIterations; i++) {
				Map<String, Map<Double, Matrices>> previousODMatrices = this.demandTracker.getODMatrices(iteration - i);

				if (null != previousODMatrices) {
					previousODMatricesList.add(previousODMatrices);
				}
			}

			this.avgODMatrices = this.calculateAvgODMAtrices(previousODMatricesList);

			//this.writeAvgODMatrices();

			//this.writeAvgODMatricesMap();
		}	
		GenericPlanStrategy<Plan, Person> strategy = null;
		List<GenericPlanStrategy<Plan, Person>> strategies = strategyManager.getStrategies(null);
		for (GenericPlanStrategy<Plan, Person> str : strategies) {
			if (str.toString().equals("KeepSelected")) {
				strategy = str;
			}
			
		}
		if (iteration % 2 == 0 && iteration > 0) {
			strategyManager.changeWeightOfStrategy(strategy, null, 0.0);
			strategyManager.setMaxPlansPerAgent(5);
		}
		else {
			strategyManager.changeWeightOfStrategy(strategy, null, 0.0);
			strategyManager.setMaxPlansPerAgent(5);

		}

	}

	@Override
	public void reset(int iteration) {
		// do nothing
	}

	@Override
	public void handleEvent(DispatchRelocationsEvent event) {
		String companyId = event.getCompanyId();
		Double start = event.getStart();
		Double end = event.getEnd();
		
		//demand for carsharing
		Matrices companyODMatrices = this.getAvgODMatrices(companyId, start);
		List<RelocationZone> companyRelocationZones = this.carsharingVehicleRelocation.getRelocationZones(companyId);

		if (null != companyODMatrices) {
			for (RelocationZone relocationZone : companyRelocationZones) {
				Id<RelocationZone> relocationZoneId = relocationZone.getId();

				Matrix rentals = companyODMatrices.getMatrix("rentals");
				Matrix noVehicle = companyODMatrices.getMatrix("no_vehicle");

				Double numRequests = new Double(0);
				Double numReturns = new Double(0);

				List<Entry> rentalsFromLocEntries = rentals.getFromLocEntries(relocationZoneId.toString());
				if (null != rentalsFromLocEntries) {
					for (Entry rentalOriginEntry : rentalsFromLocEntries) {
						numRequests += rentalOriginEntry.getValue();
					}
				}

				List<Entry> noVehicleFromLocEntries = noVehicle.getFromLocEntries(relocationZoneId.toString());
				if (null != noVehicleFromLocEntries) {
					for (Entry noVehicleOriginEntry : noVehicleFromLocEntries) {
						numRequests += noVehicleOriginEntry.getValue();
					}
				}

				List<Entry> rentalsToLocEntries = rentals.getToLocEntries(relocationZoneId.toString());
				if (null != rentalsToLocEntries) {
					for (Entry rentalDestinationEntry : rentalsToLocEntries) {
						numReturns += rentalDestinationEntry.getValue();
					}
				}

				relocationZone.setNumberOfExpectedRequests(numRequests);
				relocationZone.setNumberOfExpectedReturns(numReturns);
			}

			for (RelocationInfo info : this.calcOptimizedRelocations(start, end, companyId, companyRelocationZones)) {
				this.carsharingVehicleRelocation.addRelocation(companyId, info);
				log.info("AverageDemandRelocationListener suggests we move vehicle " + info.getVehicleId() + " from link " + info.getStartLinkId() + " to " + info.getEndLinkId());

				if (this.iteration > this.carsharingVehicleRelocation.moduleEnableAfterIteration()) {
					RelocationAgent agent = this.getRelocationAgent(companyId);

					if (agent != null) {
						info.setAgentId(agent.getId());
						agent.dispatchRelocation(info);
					}
				}
			}
		}
	}

	protected ArrayList<RelocationInfo> calculateRelocations(Double start, Double end, String companyId, List<RelocationZone> relocationZones) {
		String timeSlot = Time.writeTime(start) + " - " + Time.writeTime(end);
		ArrayList<RelocationInfo> relocations = new ArrayList<RelocationInfo>();
		Map<Integer, Double> vehicleSurplusZones = new HashMap<Integer, Double>();
		Map<Integer, Double> vehicleDemandZones = new HashMap<Integer, Double>();

		for (RelocationZone relocationZone : relocationZones) {
			double numberOfVehicles = relocationZone.getNumberOfSurplusVehicles();

			if (numberOfVehicles >= 1) {
				vehicleSurplusZones.put(new Integer(relocationZones.indexOf(relocationZone)), Math.floor(numberOfVehicles));
			} else if (numberOfVehicles <= -1) {
				vehicleDemandZones.put(new Integer(relocationZones.indexOf(relocationZone)), Math.floor(Math.abs(numberOfVehicles)));
			}
		}

		List<Map.Entry<Integer, Double>> vehicleSurplusList = new LinkedList<Map.Entry<Integer, Double>>(vehicleSurplusZones.entrySet());
		List<Map.Entry<Integer, Double>> vehicleDemandList = new LinkedList<Map.Entry<Integer, Double>>(vehicleDemandZones.entrySet());

        Collections.sort(vehicleDemandList, new Comparator<Map.Entry<Integer, Double>>()
        {
            public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        for (Map.Entry<Integer, Double> vehicleDemandEntry : vehicleDemandList) {
            while (vehicleDemandEntry.getValue() > 0) {
                RelocationZone originZone = null;
                Link originLink = null;

                RelocationZone destinationZone = relocationZones.get(vehicleDemandEntry.getKey());
                Link destinationLink = NetworkUtils.getNearestLink(this.carsharingVehicleRelocation.getNetwork(), destinationZone.getCenter());

                String vehicleId = "";

                Collections.sort(vehicleSurplusList, new Comparator<Map.Entry<Integer, Double>>()
                {
                    public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
                        return o2.getValue().compareTo(o1.getValue());
                    }
                });

                try {
                    Map.Entry<Integer, Double> surplusVehiclesEntry = vehicleSurplusList.get(0);
                    originZone = relocationZones.get(surplusVehiclesEntry.getKey());

                    LinkedList<Map.Entry<Link,ArrayList<String>>> vehiclesList = new LinkedList<Map.Entry<Link, ArrayList<String>>>(originZone.getVehicles().entrySet());
                    Map.Entry<Link, ArrayList<String>> vehiclesEntry = vehiclesList.get(0);
                    originLink = vehiclesEntry.getKey();
                    vehicleId = vehiclesEntry.getValue().get(0);
                    originZone.removeVehicles(originLink, new ArrayList<String>(Arrays.asList(new String[]{vehicleId})));

                    surplusVehiclesEntry.setValue(surplusVehiclesEntry.getValue() - 1);
                } catch (IndexOutOfBoundsException e) {
                    break;
                }

                relocations.add(new RelocationInfo(timeSlot, companyId, vehicleId, originLink.getId(), destinationLink.getId(), originZone.getId().toString(), destinationZone.getId().toString()));
                vehicleDemandEntry.setValue(vehicleDemandEntry.getValue() - 1);
            }
        }

		return relocations;
	}
	
	
	private ArrayList<RelocationInfo> calcOptimizedRelocations(Double start, Double end, String companyId, List<RelocationZone> relocationZones) {
		
		ArrayList<RelocationInfo> relocations = new ArrayList<RelocationInfo>();

		String timeSlot = Time.writeTime(start) + " - " + Time.writeTime(end);
		Map<Integer, Double> vehicleSurplusZones = new HashMap<Integer, Double>();
		Map<Integer, Double> vehicleDemandZones = new HashMap<Integer, Double>();

		for (RelocationZone relocationZone : relocationZones) {

			
			double numberOfVehicles = relocationZone.getNumberOfSurplusVehicles();

			if (numberOfVehicles >= 1) {
				
				if ((relocationZone.getNumberOfVehicles() + relocationZone.getNumberOfExpectedReturns() / 2.0) - relocationZone.getNumberOfExpectedRequests() > 0)				
					vehicleSurplusZones.put(new Integer(relocationZones.indexOf(relocationZone)), Math.floor((relocationZone.getNumberOfVehicles() + (int)(relocationZone.getNumberOfExpectedReturns() / 2.0)) - relocationZone.getNumberOfExpectedRequests()));
				
			} else if (numberOfVehicles <= -1) {
				vehicleDemandZones.put(new Integer(relocationZones.indexOf(relocationZone)), Math.floor(Math.abs(numberOfVehicles)));
			}
		}

		List<Map.Entry<Integer, Double>> vehicleSurplusList = new LinkedList<Map.Entry<Integer, Double>>(vehicleSurplusZones.entrySet());
		List<Map.Entry<Integer, Double>> vehicleDemandList = new LinkedList<Map.Entry<Integer, Double>>(vehicleDemandZones.entrySet());

		 Collections.sort(vehicleDemandList, new Comparator<Map.Entry<Integer, Double>>()
	        {
	            public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
	                return o2.getValue().compareTo(o1.getValue());
	            }
	        });
		 
		 Collections.sort(vehicleSurplusList, new Comparator<Map.Entry<Integer, Double>>()
	        {
	            public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
	                return o2.getValue().compareTo(o1.getValue());
	            }
	        });
		
		while (!vehicleDemandList.isEmpty() && vehicleDemandList.get(0).getValue() > 0 && !vehicleSurplusList.isEmpty() && vehicleSurplusList.get(0).getValue() > 0) {
			
			RelocationZone destinationZone = relocationZones.get(vehicleDemandList.get(0).getKey());
	        Link destinationLink = NetworkUtils.getNearestLink(this.carsharingVehicleRelocation.getNetwork(), destinationZone.getCenter());
			
	        Map.Entry<Integer, Double> surplusVehiclesEntry = vehicleSurplusList.get(0);
	        RelocationZone originZone = relocationZones.get(surplusVehiclesEntry.getKey());

            LinkedList<Map.Entry<Link,ArrayList<String>>> vehiclesList = new LinkedList<Map.Entry<Link, ArrayList<String>>>(originZone.getVehicles().entrySet());
            if (vehiclesList.isEmpty()) {
            	
            	vehicleSurplusList.remove(0);
            	
            	continue;
            }
            Map.Entry<Link, ArrayList<String>> vehiclesEntry = vehiclesList.get(0);
            Link originLink = vehiclesEntry.getKey();
            String vehicleId = vehiclesEntry.getValue().get(0);
            originZone.removeVehicles(originLink, new ArrayList<String>(Arrays.asList(new String[]{vehicleId})));

            surplusVehiclesEntry.setValue(surplusVehiclesEntry.getValue() - 1);       

            relocations.add(new RelocationInfo(timeSlot, companyId, vehicleId, originLink.getId(), destinationLink.getId(), originZone.getId().toString(), destinationZone.getId().toString()));
            vehicleDemandList.get(0).setValue(vehicleDemandList.get(0).getValue() - 1);
			
			 Collections.sort(vehicleDemandList, new Comparator<Map.Entry<Integer, Double>>()
		        {
		            public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
		                return o2.getValue().compareTo(o1.getValue());
		            }
		        });
			 
			 Collections.sort(vehicleSurplusList, new Comparator<Map.Entry<Integer, Double>>()
		        {
		            public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
		                return o2.getValue().compareTo(o1.getValue());
		            }
		        });
		}
		
		return relocations;
	}
	
	

	public Map<String, Map<Double, Matrices>> getAvgODMatrices() {
		return this.avgODMatrices;
	}

	public Map<Double, Matrices> getAvgODMatrices(String companyId) {
		Map<String, Map<Double, Matrices>> ODMatrices = this.getAvgODMatrices();

		if ((null != ODMatrices) && (ODMatrices.keySet().contains(companyId))) {
			return ODMatrices.get(companyId);
		}

		return null;
	}

	public Matrices getAvgODMatrices(String companyId, Double time) {
		Map<Double, Matrices> ODMatrices = this.getAvgODMatrices(companyId);

		if ((null != ODMatrices) && (ODMatrices.keySet().contains(time))) {
			return ODMatrices.get(time);
		}

		return null;
	}

	public Matrix getAvgODMatrix(String companyId, Double time, String eventType) {
		Matrices companyODMatrices = this.getAvgODMatrices(companyId, time);

		if ((null != companyODMatrices) && (companyODMatrices.getMatrices().keySet().contains(eventType))) {
			return companyODMatrices.getMatrix(eventType);
		}

		return null;
	}

	protected Map<String, Map<Double, Matrices>> calculateAvgODMAtrices(List<Map<String, Map<Double, Matrices>>> ODMatricesList) {
		Map<String, Map<Double, Matrices>> avgODMatrices = new HashMap<String, Map<Double, Matrices>>();

		for (Map<String, Map<Double, Matrices>> ODMatrices : ODMatricesList) {
			avgODMatrices = this.addODMatrices(avgODMatrices, ODMatrices);
		}

		avgODMatrices = this.divideODMatrices(avgODMatrices, ODMatricesList.size());

		return avgODMatrices;
	}

	protected Map<String, Map<Double, Matrices>> addODMatrices(Map<String, Map<Double, Matrices>> ODMatrices1,
			Map<String, Map<Double, Matrices>> ODMatrices2) {
		if (ODMatrices1.isEmpty()) {
			return ODMatrices2;
		}

		for (java.util.Map.Entry<String, Map<Double, Matrices>> companyODMatricesEntry : ODMatrices1.entrySet()) {
			String companyId = companyODMatricesEntry.getKey();
			Map<Double, Matrices> companyODMatrices1 = companyODMatricesEntry.getValue();

			Map<Double, Matrices> companyODMatrices2 = ODMatrices2.get(companyId);

			if (null != companyODMatrices2) {
				for (java.util.Map.Entry<Double, Matrices> intervalODMatrices1Entry : companyODMatrices1.entrySet()) {
					Double start = intervalODMatrices1Entry.getKey();
					Matrices intervalODMatrices1 = intervalODMatrices1Entry.getValue();

					Matrices intervalODMatrices2 = companyODMatrices2.get(start);

					if (null != intervalODMatrices2) {
						Matrix Matrix1rentals = intervalODMatrices1.getMatrix("rentals");

						for (ArrayList<Entry> fromLocEntries : intervalODMatrices2.getMatrix("rentals").getFromLocations().values()) {
							for (Entry fromLocEntry: fromLocEntries) {
								String originId = fromLocEntry.getFromLocation();
								String destinationId = fromLocEntry.getToLocation();

								double value = fromLocEntry.getValue();

								Entry relation = Matrix1rentals.getEntry(originId, destinationId);

								if (null != relation) {
									value += relation.getValue();
								}

								Matrix1rentals.setEntry(originId, destinationId, value);
							}
						}

						Matrix Matrix1noVehicle = intervalODMatrices1.getMatrix("no_vehicle");

						for (ArrayList<Entry> fromLocEntries : intervalODMatrices2.getMatrix("no_vehicle").getFromLocations().values()) {
							for (Entry fromLocEntry: fromLocEntries) {
								String originId = fromLocEntry.getFromLocation();
								String destinationId = fromLocEntry.getToLocation();

								double value = fromLocEntry.getValue();

								Entry relation = Matrix1noVehicle.getEntry(originId, destinationId);

								if (null != relation) {
									value += relation.getValue();
								}

								Matrix1noVehicle.setEntry(originId, destinationId, value);
							}
						}
					}
				}
			}
		}

		return ODMatrices1;
	}

	protected Map<String, Map<Double, Matrices>> divideODMatrices(Map<String, Map<Double, Matrices>> ODMatrices,
			double divisor) {

		for (Map<Double, Matrices> companyODMatrices : ODMatrices.values()) {
			for (Matrices intervalODMatrices : companyODMatrices.values()) {
				for (Matrix matrix : intervalODMatrices.getMatrices().values()) {
					for (ArrayList<Entry> fromLocationEntries : matrix.getFromLocations().values()) {
						for (Entry fromLocationEntry : fromLocationEntries) {
							double value = fromLocationEntry.getValue();
							fromLocationEntry.setValue(value / divisor);
						}
					}
				}
			}
		}

		return ODMatrices;
	}

	protected RelocationAgent getRelocationAgent(String companyId) {
		Map<Id<Person>, RelocationAgent> relocationAgents = this.carsharingVehicleRelocation.getRelocationAgents(companyId);

		for (RelocationAgent relocationAgent : relocationAgents.values()) {
			if (relocationAgent.getRelocations().isEmpty()) {
				return relocationAgent;
			}
		}

		return null;
	}

	protected void writeAvgODMatrices() {
		for (java.util.Map.Entry<String, Map<Double, Matrices>> companyEntry : this.avgODMatrices.entrySet()) {
			String companyId = companyEntry.getKey();

			Map<Double, Matrices> companyODMatrices = companyEntry.getValue();

			for (java.util.Map.Entry<Double, Matrices> timeEntry : companyODMatrices.entrySet()) {
				double start = timeEntry.getKey();
				String filename = this.outputDirectoryHierarchy.getIterationFilename(this.iteration, "CS-OD-AVG." + companyId + "." + start + ".txt");
				final MatricesWriter matricesWriter = new MatricesWriter(timeEntry.getValue());
				matricesWriter.write(filename);
			}
		}
	}

	protected void writeAvgODMatricesMap() {
		RelocationZoneKmlWriter writer = new RelocationZoneKmlWriter();

		for (java.util.Map.Entry<String, List<RelocationZone>> relocationZoneEntry : this.carsharingVehicleRelocation.getRelocationZones().entrySet()) {
			String companyId = relocationZoneEntry.getKey();
			Map<Id<RelocationZone>, MultiPolygon> polygons = new HashMap<Id<RelocationZone>, MultiPolygon>();

			for (RelocationZone relocationZone : relocationZoneEntry.getValue()) {
				polygons.put(relocationZone.getId(), (MultiPolygon) relocationZone.getPolygon().getAttribute("the_geom"));
			}

			writer.setPolygons(polygons);

			for (java.util.Map.Entry<Double, Matrices> avgODMatricesEntry : this.getAvgODMatrices(companyId).entrySet()) {
				Double time = avgODMatricesEntry.getKey();
				Matrices avgODMatrices = avgODMatricesEntry.getValue();
				String filename = this.outputDirectoryHierarchy.getIterationFilename(this.iteration, companyId + "." + time + ".relocation_zones_avg.xml");

				Map<Id<RelocationZone>, Map<String, Object>> relocationZoneStatiData = new TreeMap<Id<RelocationZone>, Map<String, Object>>();

				if (null != avgODMatrices) {
					for (RelocationZone relocationZone : relocationZoneEntry.getValue()) {
						Id<RelocationZone> relocationZoneId = relocationZone.getId();
						Map<String, Object> relocationZoneContent = new HashMap<String, Object>();

						Matrix rentals = avgODMatrices.getMatrix("rentals");
						Matrix noVehicle = avgODMatrices.getMatrix("no_vehicle");

						Double numRequests = new Double(0);
						Double numReturns = new Double(0);

						List<Entry> rentalsFromLocEntries = rentals.getFromLocEntries(relocationZoneId.toString());
						if (null != rentalsFromLocEntries) {
							for (Entry rentalOriginEntry : rentalsFromLocEntries) {
								numRequests += rentalOriginEntry.getValue();
							}
						}

						List<Entry> noVehicleFromLocEntries = noVehicle.getFromLocEntries(relocationZoneId.toString());
						if (null != noVehicleFromLocEntries) {
							for (Entry noVehicleOriginEntry : noVehicleFromLocEntries) {
								numRequests += noVehicleOriginEntry.getValue();
							}
						}

						List<Entry> rentalsToLocEntries = rentals.getToLocEntries(relocationZoneId.toString());
						if (null != rentalsToLocEntries) {
							for (Entry rentalDestinationEntry : rentalsToLocEntries) {
								numReturns += rentalDestinationEntry.getValue();
							}
						}

						Double level = (0 - numRequests);
						relocationZoneContent.put("level", level);

						DecimalFormat decimalFormat = new DecimalFormat( "#,###,###,##0.0" );
						String content = "ID: " + relocationZoneId.toString() + " requests: " + decimalFormat.format(numRequests) + " returns: " + decimalFormat.format(numReturns);
						relocationZoneContent.put("content", content);

						relocationZoneStatiData.put(relocationZoneId, relocationZoneContent);
					}

					writer.writeFile(time, filename, relocationZoneStatiData);
				}
			}
		}
	}

}

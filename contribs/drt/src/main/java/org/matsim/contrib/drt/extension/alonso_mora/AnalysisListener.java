package org.matsim.contrib.drt.extension.alonso_mora;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.timeprofile.TimeProfileCharts;
import org.matsim.contrib.common.timeprofile.TimeProfileCharts.ChartType;
import org.matsim.contrib.common.util.ChartSaveUtils;
import org.matsim.contrib.drt.extension.alonso_mora.InformationCollector.GraphInformation;
import org.matsim.contrib.drt.extension.alonso_mora.InformationCollector.OccupancyInformation;
import org.matsim.contrib.drt.extension.alonso_mora.InformationCollector.ReassignmentInformation;
import org.matsim.contrib.drt.extension.alonso_mora.InformationCollector.RebalancingInformation;
import org.matsim.contrib.drt.extension.alonso_mora.InformationCollector.SolverInformation;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.assignment.AssignmentSolver.Solution.Status;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import com.google.common.collect.ImmutableMap;

/**
 * Listener that writes output for the Alonso-Mora dispatcher.
 */
class AnalysisListener implements IterationEndsListener {
	private final InformationCollector information;
	private final RequestAggregationHandler requestHandler;
	private final OutputDirectoryHierarchy outputHierarchy;

	private final List<Long> numberOfInvalidSolutions = new LinkedList<>();
	private final List<Long> numberOfNonOptimalSolutions = new LinkedList<>();
	private final List<Long> numberOfOptimalSolutions = new LinkedList<>();
	private final List<Long> numberOfReassignments = new LinkedList<>();

	public AnalysisListener(InformationCollector information, OutputDirectoryHierarchy outputHierarchy,
			RequestAggregationHandler requestHandler) {
		this.information = information;
		this.outputHierarchy = outputHierarchy;
		this.requestHandler = requestHandler;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		List<SolverInformation> solverData = information.clearSolverInformation();
		List<GraphInformation> graphInformation = information.clearGraphInformation();
		List<RebalancingInformation> rebalancingInformation = information.clearRebalancingInformation();
		List<ReassignmentInformation> reassignmentInformation = information.clearReassignmentInformation();

		numberOfInvalidSolutions.add(solverData.stream().filter(s -> s.status.equals(Status.FAILURE)).count());
		numberOfNonOptimalSolutions.add(solverData.stream().filter(s -> s.status.equals(Status.FEASIBLE)).count());
		numberOfOptimalSolutions.add(solverData.stream().filter(s -> s.status.equals(Status.OPTIMAL)).count());
		numberOfReassignments.add((long) reassignmentInformation.stream().mapToInt(i -> i.reassignments).sum());

		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

		{ // Solution time by simulation time
			try {
				File path = new File(outputHierarchy.getIterationFilename(event.getIteration(), "am_solution"));

				TimeSeriesCollection dataSet = new TimeSeriesCollection();
				TimeSeries timeSeries = new TimeSeries("Solution time");
				dataSet.addSeries(timeSeries);

				for (int i = 0; i < solverData.size(); i++) {
					Second second = new Second(sdf.parse(Time.writeTime(solverData.get(i).simulationTime)));
					timeSeries.add(second, solverData.get(i).solutionTime);
				}

				JFreeChart chart = ChartFactory.createTimeSeriesChart("Assignment problem solution", "Time",
						"Solution time [s]", dataSet);
				ChartSaveUtils.saveAsPNG(chart, path.toString(), 1500, 1000);

			} catch (ParseException e) {
				throw new IllegalStateException(e);
			}
		}

		{ // Solution time by simulation time
			try {
				File path = new File(outputHierarchy.getIterationFilename(event.getIteration(), "am_dynamics"));

				TimeSeriesCollection dataSet = new TimeSeriesCollection();

				TimeSeries rebalancingTimeSeries = new TimeSeries("Rebalancing directives");
				TimeSeries reassignmentTimeSeries = new TimeSeries("Reassignments");
				TimeSeries requestGraphTimeSeries = new TimeSeries("Request graph size");
				TimeSeries tripGraphTimeSeries = new TimeSeries("Trip graph size");

				dataSet.addSeries(reassignmentTimeSeries);
				dataSet.addSeries(rebalancingTimeSeries);
				dataSet.addSeries(requestGraphTimeSeries);
				dataSet.addSeries(tripGraphTimeSeries);

				for (int i = 0; i < graphInformation.size(); i++) {
					Second second = new Second(sdf.parse(Time.writeTime(graphInformation.get(i).simulationTime)));
					requestGraphTimeSeries.add(second, graphInformation.get(i).requestGraphSize);
					tripGraphTimeSeries.add(second, graphInformation.get(i).tripGraphSize);
				}

				for (int i = 0; i < rebalancingInformation.size(); i++) {
					Second second = new Second(sdf.parse(Time.writeTime(rebalancingInformation.get(i).simulationTime)));
					rebalancingTimeSeries.add(second, rebalancingInformation.get(i).rebalancingDirectives);
				}

				for (int i = 0; i < reassignmentInformation.size(); i++) {
					Second second = new Second(
							sdf.parse(Time.writeTime(reassignmentInformation.get(i).simulationTime)));
					reassignmentTimeSeries.add(second, reassignmentInformation.get(i).reassignments);
				}

				JFreeChart chart = ChartFactory.createTimeSeriesChart("Alonso-Mora dynamics", "Time", "Items", dataSet);
				ChartSaveUtils.saveAsPNG(chart, path.toString(), 1500, 1000);
			} catch (ParseException e) {
				throw new IllegalStateException(e);
			}
		}

		{ // Solution information
			try {
				BufferedWriter writer = IOUtils.getBufferedWriter(
						outputHierarchy.getIterationFilename(event.getIteration(), "am_runtime.csv"));

				writer.write(String.join(";", Arrays.asList( //
						"simulation_time", //
						"request_graph_size", //
						"request_graph_time", //
						"trip_graph_size", //
						"trip_graph_time", //
						"assignment_time", //
						"assignment_status", //
						"relocation_time" //
				)) + "\n");

				for (int i = 0; i < solverData.size(); i++) {
					SolverInformation solver = solverData.get(i);
					GraphInformation graph = graphInformation.get(i);
					RebalancingInformation rebalancing = rebalancingInformation.get(i);

					writer.write(String.join(";", Arrays.asList( //
							String.valueOf(solver.simulationTime), //
							String.valueOf(graph.requestGraphSize), //
							String.valueOf(graph.requestGraphTime), //
							String.valueOf(graph.tripGraphSize), //
							String.valueOf(graph.tripGraphTime), //
							String.valueOf(solver.solutionTime), //
							String.valueOf(solver.status), //
							String.valueOf(rebalancing.rebalancingTime) //
					)) + "\n");
				}

				writer.close();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}

		{
			try {
				BufferedWriter writer = IOUtils
						.getBufferedWriter(outputHierarchy.getOutputFilename("output_am_reassignments.csv"));

				writer.write("iteration;reassignments\n");

				for (int i = 0; i < numberOfReassignments.size(); i++) {
					writer.write(String.join(",",
							new String[] { String.valueOf(i), String.valueOf(numberOfReassignments.get(i)) }) + "\n");
				}

				writer.close();
			} catch (IOException e) {
			}
		}

		{ // Failures by iteration
			File path = new File(outputHierarchy.getOutputFilename("output_am_solutions"));

			DefaultCategoryDataset dataSet = new DefaultCategoryDataset();

			for (int i = 0; i < numberOfInvalidSolutions.size(); i++) {
				dataSet.addValue((Number) numberOfInvalidSolutions.get(i), "Invalid", i);
				dataSet.addValue((Number) numberOfNonOptimalSolutions.get(i), "Non-optimal", i);
				dataSet.addValue((Number) numberOfOptimalSolutions.get(i), "Optimal", i);
			}

			JFreeChart chart = ChartFactory.createBarChart("Assignment solutions", "Iteration", "Solutions", dataSet);
			ChartSaveUtils.saveAsPNG(chart, path.toString(), 1500, 1000);
		}

		{ // Failures by iteration
			try {
				File path = new File(outputHierarchy.getOutputFilename("output_am_solutions.csv"));
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));

				writer.write(String.join(";", new String[] { //
						"iteration", "optimal", "non_optimal", "invalid" //
				}) + "\n");

				for (int i = 0; i < numberOfInvalidSolutions.size(); i++) {
					writer.write(String.join(";", new String[] { String.valueOf(i), //
							String.valueOf(numberOfOptimalSolutions.get(i)), //
							String.valueOf(numberOfNonOptimalSolutions.get(i)), //
							String.valueOf(numberOfInvalidSolutions.get(i)) //
					}) + "\n");
				}

				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		List<OccupancyInformation> occupancyInformation = information.clearOccupancyInformation();

		{ // Occupancy by passengers
			try {
				File path = new File(
						outputHierarchy.getIterationFilename(event.getIteration(), "am_occupancy_persons.csv"));
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));

				int maximumPersons = occupancyInformation.stream()
						.mapToInt(item -> item.occupiedCountByPassengers.size()).max().orElse(0);

				List<String> header = new ArrayList<>(Arrays.asList("time"));

				for (int k = 0; k < maximumPersons; k++) {
					header.add(k + "pax");
				}

				writer.write(String.join(";", header) + "\n");

				for (int i = 0; i < occupancyInformation.size(); i++) {
					OccupancyInformation row = occupancyInformation.get(i);

					List<String> processedRow = new ArrayList<>(Arrays.asList( //
							String.valueOf(row.simulationTime), //
							String.valueOf(row.occupiedCountByPassengers.get(0))));

					for (int k = 0; k < maximumPersons; k++) {
						processedRow.add(String.valueOf(row.occupiedCountByPassengers.get(k)));
					}

					writer.write(String.join(";", processedRow) + "\n");
				}

				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		{ // Occupancy by passengers
			int maximumPersons = occupancyInformation.stream().mapToInt(item -> item.occupiedCountByPassengers.size())
					.max().orElse(0);

			List<Double> times = occupancyInformation.stream().map(i -> i.simulationTime).collect(Collectors.toList());
			List<String> names = IntStream.range(0, maximumPersons).mapToObj(k -> k + " pax")
					.collect(Collectors.toList());

			List<ImmutableMap<String, Double>> data = occupancyInformation.stream().map(info -> {
				Map<String, Double> item = new HashMap<>();

				for (int k = 0; k < maximumPersons; k++) {
					item.put(names.get(k), (double) info.occupiedCountByPassengers.get(k));
				}

				return ImmutableMap.copyOf(item);
			}).collect(Collectors.toList());

			Collections.reverse(names);
			DefaultTableXYDataset dataset = TimeProfileCharts.createXYDataset(names, times, data);

			JFreeChart stackedChart = TimeProfileCharts.chartProfile(dataset, ChartType.StackedArea);
			stackedChart.getXYPlot().getRenderer().setSeriesPaint(Math.max(maximumPersons - 1, 0), Color.LIGHT_GRAY);
			ChartSaveUtils.saveAsPNG(stackedChart,
					outputHierarchy.getIterationFilename(event.getIteration(), "am_occupancy_passengers"), 1500, 1000);
		}

		{ // Occupancy by requests
			try {
				File path = new File(
						outputHierarchy.getIterationFilename(event.getIteration(), "am_occupancy_requests.csv"));
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));

				int maximumRequests = occupancyInformation.stream()
						.mapToInt(item -> item.occupiedCountByRequests.size()).max().orElse(0);

				List<String> header = new ArrayList<>(Arrays.asList("time"));

				for (int k = 0; k < maximumRequests; k++) {
					header.add(k + "req");
				}

				writer.write(String.join(";", header) + "\n");

				for (int i = 0; i < occupancyInformation.size(); i++) {
					OccupancyInformation row = occupancyInformation.get(i);

					List<String> processedRow = new ArrayList<>(Arrays.asList( //
							String.valueOf(row.simulationTime), //
							String.valueOf(row.occupiedCountByRequests.get(0))));

					for (int k = 0; k < maximumRequests; k++) {
						processedRow.add(String.valueOf(row.occupiedCountByRequests.get(k)));
					}

					writer.write(String.join(";", processedRow) + "\n");
				}

				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		{ // Occupancy by passengers
			int maximumRequests = occupancyInformation.stream().mapToInt(item -> item.occupiedCountByRequests.size())
					.max().orElse(0);

			List<Double> times = occupancyInformation.stream().map(i -> i.simulationTime).collect(Collectors.toList());
			List<String> names = IntStream.range(0, maximumRequests).mapToObj(k -> k + " req")
					.collect(Collectors.toList());

			List<ImmutableMap<String, Double>> data = occupancyInformation.stream().map(info -> {
				Map<String, Double> item = new HashMap<>();

				for (int k = 0; k < maximumRequests; k++) {
					item.put(names.get(k), (double) info.occupiedCountByRequests.get(k));
				}

				return ImmutableMap.copyOf(item);
			}).collect(Collectors.toList());

			Collections.reverse(names);
			DefaultTableXYDataset dataset = TimeProfileCharts.createXYDataset(names, times, data);

			JFreeChart stackedChart = TimeProfileCharts.chartProfile(dataset, ChartType.StackedArea);
			stackedChart.getXYPlot().getRenderer().setSeriesPaint(Math.max(maximumRequests - 1, 1), Color.LIGHT_GRAY);
			ChartSaveUtils.saveAsPNG(stackedChart,
					outputHierarchy.getIterationFilename(event.getIteration(), "am_occupancy_requests"), 1500, 1000);
		}

		{
			List<Set<Id<Request>>> requests = requestHandler.consolidate();

			try {
				File path = new File(
						outputHierarchy.getIterationFilename(event.getIteration(), "am_group_requests.txt"));
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));

				for (Set<Id<Request>> set : requests) {
					writer.write(set.stream().map(String::valueOf).collect(Collectors.joining(",")) + "\n");
				}

				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

package org.matsim.contrib.carsharing.relocation.listeners;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.carsharing.relocation.demand.CarsharingVehicleRelocationContainer;
import org.matsim.contrib.carsharing.relocation.events.handlers.DemandDistributionHandler;
import org.matsim.contrib.carsharing.relocation.infrastructure.RelocationZone;
import org.matsim.contrib.carsharing.relocation.qsim.RelocationInfo;
import org.matsim.contrib.carsharing.relocation.utils.RelocationZoneKmlWriter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;

import com.google.inject.Inject;
import com.vividsolutions.jts.geom.MultiPolygon;

public class KmlWriterListener implements IterationStartsListener, IterationEndsListener {
	int frequency = 0;

	@Inject private CarsharingVehicleRelocationContainer carsharingVehicleRelocation;

	@Inject private OutputDirectoryHierarchy outputDirectoryHierarchy;

	@Inject private DemandDistributionHandler demandDistributionHandler;

	public KmlWriterListener(int frequency) {
		this.frequency = frequency;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		this.carsharingVehicleRelocation.reset();
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (event.getIteration() % this.frequency == 0) {
			// write relocation zone KML files
			RelocationZoneKmlWriter writer = new RelocationZoneKmlWriter();

			for (Entry<String, List<RelocationZone>> relocationZoneEntry : this.carsharingVehicleRelocation.getRelocationZones().entrySet()) {
				String companyId = relocationZoneEntry.getKey();
				Map<Id<RelocationZone>, MultiPolygon> polygons = new HashMap<Id<RelocationZone>, MultiPolygon>();

				for (RelocationZone relocationZone : relocationZoneEntry.getValue()) {
					polygons.put(relocationZone.getId(), (MultiPolygon) relocationZone.getPolygon().getAttribute("the_geom"));
				}

				writer.setPolygons(polygons);

				for (Entry<Double, Map<Id<RelocationZone>, Map<String, Double>>> relocationZoneStatiEntry : this.carsharingVehicleRelocation.getStatus().get(companyId).entrySet()) {
					Double time = relocationZoneStatiEntry.getKey();
					Map<Id<RelocationZone>, Map<String, Double>> relocationZoneStati = relocationZoneStatiEntry.getValue();
					String filename = this.outputDirectoryHierarchy.getIterationFilename(event.getIteration(), companyId + "." + time + ".relocation_zones.xml");

					Map<Id<RelocationZone>, Map<String, Object>> relocationZoneStatiData = new TreeMap<Id<RelocationZone>, Map<String, Object>>();

					for (Entry<Id<RelocationZone>, Map<String, Double>> relocationZoneStatusEntry : relocationZoneStati.entrySet()) {
						Id<RelocationZone> relocationZoneId = relocationZoneStatusEntry.getKey();
						Map<String, Double> relocationZoneStatus = relocationZoneStatusEntry.getValue();
						Map<String, Object> relocationZoneContent = new HashMap<String, Object>();

						Double numVehicles = relocationZoneStatus.get("vehicles");
						Double numRequests = relocationZoneStatus.get("actualRequests");
						Double numReturns = relocationZoneStatus.get("actualReturns");
						Double numRequestsExpected = relocationZoneStatus.get("expectedRequests");
						Double numReturnsExpected = relocationZoneStatus.get("expectedReturns");

						Double level = (numVehicles + numReturns - numRequests);
						relocationZoneContent.put("level", level);

						DecimalFormat decimalFormat = new DecimalFormat( "#,###,###,##0.0" );
						String content = "ID: " + relocationZoneId.toString() + " vehicles: " + decimalFormat.format(numVehicles) + " requests: " + decimalFormat.format(numRequests) + " (expected: " + decimalFormat.format(numRequestsExpected) + ")" + " returns: " + decimalFormat.format(numReturns) + " (expected: " + decimalFormat.format(numReturnsExpected) + ")";
						relocationZoneContent.put("content", content);

						relocationZoneStatiData.put(relocationZoneId, relocationZoneContent);
					}

					writer.writeFile(time, filename, relocationZoneStatiData);
				}
			}

			// log relocations
			final BufferedWriter outRelocations = IOUtils.getBufferedWriter(this.outputDirectoryHierarchy.getIterationFilename(event.getIteration(), "relocations.txt"));
			try {
				for (Entry<String, List<RelocationInfo>> companyEntry : this.carsharingVehicleRelocation.getRelocations().entrySet()) {
					List<RelocationInfo> companyRelocations = companyEntry.getValue();

					outRelocations.write("timeSlot	startZone	endZone	startTime	endTime	startLink	endLink	companyID	vehicleID	agentID");
					outRelocations.newLine();

					for (RelocationInfo i: companyRelocations) {
						outRelocations.write(i.toString());
						outRelocations.newLine();
					}
				}

				outRelocations.flush();
				outRelocations.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// log trip OD-Matrices
			for (Entry<String, Map<Double, Matrices>> companyEntry : this.demandDistributionHandler.getODMatrices().entrySet()) {
				String companyId = companyEntry.getKey();

				Map<Double, Matrices> companyODMatrices = companyEntry.getValue();

				for (Entry<Double, Matrices> timeEntry : companyODMatrices.entrySet()) {
					double start = timeEntry.getKey();
					String filename = this.outputDirectoryHierarchy.getIterationFilename(event.getIteration(), "CS-OD-Matrix." + companyId + "." + start + ".txt");
					final MatricesWriter matricesWriter = new MatricesWriter(timeEntry.getValue());
					matricesWriter.write(filename);
				}
			}
		}
	}
}

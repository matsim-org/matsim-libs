// code by jph
package playground.clruch.io.fleet;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.queuey.util.FileDelete;
import playground.clruch.dispatcher.core.RequestStatus;
import playground.clruch.net.LinkSpeedUtils;
import playground.clruch.net.MatsimStaticDatabase;

public enum FleetUtils {
    ;

    public static DayTaxiRecord processData(File file, DayTaxiRecord dayTaxiRecord, boolean takeLog) throws Exception {

        // Going through all timestamps and check for offservice vehicles && parse requests
        final int MAXTIME = dayTaxiRecord.getNow(dayTaxiRecord.lastTimeStamp);
        final int TIMESTEP = 10;

        final HashSet<List<String>> requestStateTransitions = new HashSet<>();
        final HashSet<List<String>> requestTrails = new HashSet<>();

        // Create logs for taxiTrails
        File logFolder = new File("logs/" + file.getName().substring(0, 10));

        if (takeLog) {
            System.out.println("INFO Checking for OFFSERVICE & RequestStatus for " + dayTaxiRecord.size() + " vehicles");
            if (logFolder.exists())
                FileDelete.of(logFolder, 2, 10000);
            logFolder.mkdirs();
        }

        for (int vehicleIndex = 0; vehicleIndex < dayTaxiRecord.size(); ++vehicleIndex) {
            TaxiTrail taxiTrail = dayTaxiRecord.get(vehicleIndex);
            if (vehicleIndex % 10 == 0)
                System.out.println("INFO Checking vehicle: " + vehicleIndex);

            for (int now = 0; now < MAXTIME; now += TIMESTEP) {
                taxiTrail.checkOffService(now);
            }

            // System.out.println("Found keyset for vehicle " + vehicleIndex + ": " + dayTaxiRecord.get(vehicleIndex).getKeySet().toString());
            int iteration = 0;
            RequestContainerUtils rcUtils = new RequestContainerUtils(taxiTrail, null);
            for (Integer now : dayTaxiRecord.get(vehicleIndex).getKeySet()) {
                // System.out.println("Parsing requestStatus for vehicle " + vehicleIndex + " at time " + now);
                taxiTrail.setRequestStatus(now, RequestStatusParser.parseRequestStatus(now, taxiTrail));
                if (Objects.nonNull(taxiTrail.getLastEntry(now)) && RequestStatusParser.isOutlierRequest(taxiTrail.interp(now).getValue().requestStatus,
                        taxiTrail.getLastEntry(now).getValue().requestStatus))
                    taxiTrail.setRequestStatus(taxiTrail.getLastEntry(now).getKey(), RequestStatus.CANCELLED);
                // Add all kind of "Vermittlungsstatus" change transitions into hash map
                if (iteration > 2 && takeLog) {
                    requestStateTransitions.add(FleetReaderLogUtils.getRequestStateTransition(now, taxiTrail));
                    if (rcUtils.isValidRequest(now, false)) {
                        List<String> requestTrail = new ArrayList<String>();
                        requestTrail = rcUtils.dumpRequestTrail(now);
                        requestTrails.add(requestTrail);
                    }
                    FleetReaderLogUtils.writeTrailLogFile(logFolder, dayTaxiRecord.get(vehicleIndex), vehicleIndex);
                }
                iteration++;
            }
        }
        if (takeLog) {
            FleetReaderLogUtils.writeRequestTransitions(logFolder, requestStateTransitions);
            FleetReaderLogUtils.writeRequestTrails(logFolder, requestTrails);
        }
        return dayTaxiRecord;
    }

    public static DayTaxiRecord getLinkData(DayTaxiRecord dayTaxiRecord, Network network, MatsimStaticDatabase db) {
        for (int vehicleIndex = 0; vehicleIndex < dayTaxiRecord.size(); ++vehicleIndex) {
            TaxiTrail taxiTrail = dayTaxiRecord.get(vehicleIndex);
            LinkSpeedUtils lsUtils = new LinkSpeedUtils(taxiTrail, network, db);
            if (vehicleIndex % 10 == 0)
                System.out.println("INFO Getting link data for vehicle: " + vehicleIndex);

            // Loop through data and try to calculcate link speeds
            for (Integer now : dayTaxiRecord.get(vehicleIndex).getKeySet()) {
                taxiTrail.setLinkData(now, lsUtils.getLinkfromCoord(taxiTrail.interp(now).getValue().gps), lsUtils.getLinkSpeed(now));
            }
        }
        return dayTaxiRecord;
    }
}
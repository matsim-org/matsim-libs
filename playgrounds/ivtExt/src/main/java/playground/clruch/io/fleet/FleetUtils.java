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

    public static DayTaxiRecord postProcessData(File file, DayTaxiRecord dayTaxiRecord, boolean takeLog, int TIME_STEP) throws Exception {

        // Going through all timestamps and check for offservice vehicles && parse requests
        final int MAXTIME = dayTaxiRecord.getNow(dayTaxiRecord.lastTimeStamp);

        // final HashSet<List<String>> requestStateTransitions = new HashSet<>();
        final HashSet<List<RequestStatus>> requestTrails = new HashSet<>();
        final List<List<RequestStatus>> allRequestTrails = new ArrayList<List<RequestStatus>>();

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

            for (int now = 0; now < MAXTIME; now += TIME_STEP) {
                taxiTrail.checkOffService(now);
            }

            // System.out.println("Found keyset for vehicle " + vehicleIndex + ": " + dayTaxiRecord.get(vehicleIndex).getKeySet().toString());
            RequestContainerUtils rcUtils = new RequestContainerUtils(taxiTrail, null);
            for (Integer now : dayTaxiRecord.get(vehicleIndex).getKeySet()) {
                // System.out.println("Parsing requestStatus for vehicle " + vehicleIndex + " at time " + now);
                taxiTrail.setRequestStatus(now, RequestStatusParser.parseRequestStatus(now, taxiTrail));
                if (Objects.nonNull(taxiTrail.getLastEntry(now)) && RequestStatusParser.isOutlierRequest(taxiTrail.interp(now).getValue().requestStatus,
                        taxiTrail.getLastEntry(now).getValue().requestStatus))
                    taxiTrail.setRequestStatus(taxiTrail.getLastEntry(now).getKey(), RequestStatus.CANCELLED);
            }
            if (takeLog) {
                int iteration = 0;
                for (Integer now : dayTaxiRecord.get(vehicleIndex).getKeySet()) {
                    // Add all kind of "Vermittlungsstatus" change transitions into hash map
                    if (iteration > 2) {
                        // requestStateTransitions.add(FleetReaderLogUtils.getRequestStateTransition(now, taxiTrail));
                        if (rcUtils.isValidRequest(now, false)) {
                            List<RequestStatus> requestTrail = new ArrayList<RequestStatus>();
                            requestTrail = rcUtils.dumpRequestTrail(now);
                            requestTrails.add(requestTrail);
                            allRequestTrails.add(requestTrail);
                        }
                    }
                    iteration++;
                }
                // FleetReaderLogUtils.writeTrailLogFile(logFolder, dayTaxiRecord.get(vehicleIndex), vehicleIndex);
            }
        }
        if (takeLog) {
            // FleetReaderLogUtils.writeRequestTransitions(logFolder, requestStateTransitions);
            System.out.println("INFO writing " + requestTrails.size() + " unique Requests to logfolder");
            System.out.println("INFO out " + allRequestTrails.size() + " total amount of Requests");
            FleetReaderLogUtils.writeRequestTrails(logFolder, requestTrails);
            FleetReaderLogUtils.countAllRequests(requestTrails);
        }
        return dayTaxiRecord;
    }

    public static DayTaxiRecord getLinkData(DayTaxiRecord dayTaxiRecord, Network network, MatsimStaticDatabase db) {
        LinkSpeedUtils lsUtils = new LinkSpeedUtils(dayTaxiRecord.get(0), network, db);
        for (int vehicleIndex = 0; vehicleIndex < dayTaxiRecord.size(); ++vehicleIndex) {
            TaxiTrail taxiTrail = dayTaxiRecord.get(vehicleIndex);
            lsUtils.setTaxiTrail(taxiTrail);
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
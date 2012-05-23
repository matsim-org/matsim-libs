/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.vrp.taxi.wal;

import java.util.List;
import java.util.concurrent.*;

import org.matsim.api.core.v01.*;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.Schedule.ScheduleStatus;
import pl.poznan.put.vrp.dynamic.data.schedule.Task.TaskStatus;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiOptimizerFactory;
import playground.michalm.vrp.data.MatsimVrpData;
import playground.michalm.vrp.data.jdbc.JdbcWriter;
import playground.michalm.vrp.data.network.MatsimVrpGraph;
import playground.michalm.vrp.taxi.TaxiSimEngine;
import playground.michalm.vrp.taxi.wal.Command.CommandType;


public class WalTaxiSimEngine
    extends TaxiSimEngine
{
    enum RealTimeMode
    {
        BEFORE, IN, AFTER;
    }


    private JdbcWriter jdbcWriter;
    private String dbFileName;

    private MServer mServer;
    private RealTimeMode realTimeMode = null;

    private int startTime;
    private int endTime;
    private long timeOffset;

    private Vehicle realVehicle;

    private volatile boolean keepListening;
    private BlockingQueue<Command> commandQueue;

    private MatsimVrpData data;
    private VrpData vrpData;
    private MatsimVrpGraph vrpGraph;
    private Scenario scenario;


    public WalTaxiSimEngine(Netsim netsim, MatsimVrpData data,
            TaxiOptimizerFactory optimizerFactory, String dbFileName)
    {
        super(netsim, data, optimizerFactory);
        this.dbFileName = dbFileName;

        this.data = data;
        this.vrpData = data.getVrpData();
        this.vrpGraph = data.getVrpGraph();
        this.scenario = data.getScenario();
    }


    @Override
    public void onPrepareSim()
    {
        System.err.println("SERVER initialization");

        mServer = new MServer();
        mServer.initServer();

        // <-- INIT $startTime$ $endTime$ $realVehicleCount$

        Command command = mServer.readCommand();
        assertCommandType(command, CommandType.INIT);

        startTime = command.getParam(0);
        endTime = command.getParam(1);
        int realVehicleCount = command.getParam(2);

        if (realVehicleCount != 1) {
            throw new IllegalArgumentException();
        }

        List<Vehicle> vehicles = vrpData.getVehicles();
        List<Depot> depots = vrpData.getDepots();

        // <-- VEHICLE $vehId$ $linkId$
        command = mServer.readCommand();
        assertCommandType(command, CommandType.VEHICLE);

        int realVehId = command.getParam(0);
        int linkId = command.getParam(1);

        System.err.println("Creating a new depot...");
        int depotId = depots.size();
        Depot depot = new DepotImpl(depotId, "D_" + depotId, vrpGraph.getVertex(scenario
                .createId(linkId + "")));
        depots.add(depot);

        System.err.println("Creating a vehicle with the default params...");
        int vehId = vehicles.size();
        realVehicle = new VehicleImpl(vehId, "real_" + realVehId, depot, 1, 0, startTime, endTime,
                endTime - startTime);
        vehicles.add(realVehicle);

        super.onPrepareSim();

        // update JDBC
        jdbcWriter = new JdbcWriter(data, dbFileName);
        jdbcWriter.simulationInitialized();
        realTimeMode = RealTimeMode.BEFORE;
    }


    private void assertCommandType(Command command, CommandType type)
    {
        if (command.getType() != type) {
            throw new RuntimeException("Incorrect command type: " + type);
        }
    }


    @Override
    public void doSimStep(double time)
    {
        super.doSimStep(time);

        if (realTimeMode == RealTimeMode.AFTER) {
            return;
        }

        if (realTimeMode == RealTimeMode.BEFORE) {
            if (time < startTime) {
                return;// BEFORE real time
            }
            else if (time == startTime) {
                // --> STARTED
                startListening();
                mServer.writeCommand(new Command(CommandType.STARTED));

                realTimeMode = RealTimeMode.IN;

                timeOffset = System.currentTimeMillis();
                System.err.println("SERVER initialization DONE!");
            }
            else {
                throw new IllegalStateException();
            }
        }

        //no waiting, trigger the 1st task
        if (realVehicle.getSchedule().getStatus() == ScheduleStatus.PLANNED) { 
            realVehicle.getSchedule().nextTask();
            nextTask(realVehicle);
        }

        // listen to new Commands
        while (commandQueue.peek() != null) {
            // TASK_ENDED $taskId$
            Command command = commandQueue.poll();
            assertCommandType(command, CommandType.TASK_ENDED);
            realVehicleTaskEnded(command.getParam(0), time);
        }


        if (time >= endTime) {
            ScheduleStatus schedStatus = realVehicle.getSchedule().getStatus();// refresh!!

            if (schedStatus == ScheduleStatus.UNPLANNED || schedStatus == ScheduleStatus.COMPLETED) {
                realTimeMode = RealTimeMode.AFTER;
                return;// AFTER real time
            }
        }

        try {
            long realTime = System.currentTimeMillis() - timeOffset;
            long simTime = (long) ( (time - startTime) * 1000);
            long simAheadOfReal = simTime - realTime;

            if (simAheadOfReal > 0) {
                Thread.sleep(simAheadOfReal);
            }
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private void realVehicleTaskEnded(int taskId, double now)
    {
        Task task = JdbcWriter.findTask(taskId, data.getVrpData().getVehicles());

        if (task.getStatus() != TaskStatus.STARTED) {
            throw new IllegalStateException();
        }

        Schedule schedule = task.getSchedule();
        updateAndOptimizeBeforeNextTask(schedule.getVehicle(), now);
        schedule.nextTask();
        nextTask(schedule.getVehicle());
    }


    @Override
    protected void optimize(double now)
    {
        super.optimize(now);

        if (realTimeMode != null) {
            jdbcWriter.schedulesReoptimized();
            writeCommandInRealTimeMode(new Command(CommandType.SCHEDULES_REOPTIMIZED));
        }
    }


    @Override
    protected boolean update(Vehicle vrpVehicle)
    {
        if (super.update(vrpVehicle)) {
            jdbcWriter.scheduleUpdated(vrpVehicle.getSchedule());
            writeCommandInRealTimeMode(new Command(CommandType.SCHEDULE_UPDATED, vrpVehicle.getId()));
            return true;
        }
        else {
            return false;
        }
    };


    @Override
    public void taxiRequestSubmitted(Request request, double now)
    {
        // no new Vertex has been added (since we use FixedSizeVrpGraph)
        // no need to call jdbcWriter.newVertex and mServer(VERTEX) therefore...

        // for each new Request a new Customer is created
        Customer customer = request.getCustomer();
        jdbcWriter.newCustomer(customer);
        writeCommandInRealTimeMode(new Command(CommandType.CUSTOMER, customer.getId()));

        jdbcWriter.newRequest(request);
        writeCommandInRealTimeMode(new Command(CommandType.REQUEST, request.getId()));

        super.taxiRequestSubmitted(request, now);
    }


    public void nextTask(Vehicle vrpVehicle)
    {
        jdbcWriter.nextTask(vrpVehicle.getSchedule());
        writeCommandInRealTimeMode(new Command(CommandType.NEXT_TASK, vrpVehicle.getId()));
    }


    public void notifyMoveOverNode(Vehicle vrpVehicle, Id oldLinkId, Id newLinkId)
    {
        jdbcWriter.nextLink(vrpVehicle, newLinkId);
        writeCommandInRealTimeMode(new Command(CommandType.NEXT_LINK, vrpVehicle.getId()));
    }


    private void writeCommandInRealTimeMode(Command command)
    {
        if (realTimeMode == RealTimeMode.IN) {
            mServer.writeCommand(command);
        }
    }


    public void startListening()
    {
        keepListening = true;
        commandQueue = new LinkedBlockingQueue<Command>();

        System.err.println("When to stop Listening???!!!");

        Runnable runnable = new Runnable() {
            @Override
            public void run()
            {
                while (keepListening) {
                    Command command = mServer.readCommand();

                    try {
                        commandQueue.put(command);
                    }
                    catch (InterruptedException e) {
                        System.err.println("Cannot put new command into the queue");
                        throw new RuntimeException(e);
                    }
                }
            }
        };

        new Thread(runnable).start();
    }


    public void stopListening()
    {
        keepListening = false;
    }
}

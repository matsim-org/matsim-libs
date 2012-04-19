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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.Schedule.ScheduleStatus;
import pl.poznan.put.vrp.dynamic.optimizer.VrpOptimizerFactory;
import playground.michalm.vrp.data.MatsimVrpData;
import playground.michalm.vrp.data.jdbc.JdbcWriter;
import playground.michalm.vrp.data.model.DynVehicle;
import playground.michalm.vrp.data.network.MatsimVrpGraph;
import playground.michalm.vrp.taxi.TaxiSimEngine;
import playground.michalm.vrp.taxi.wal.Command.CommandType;


public class WalTaxiSimEngine
    extends TaxiSimEngine
    implements ScheduleListener
{
    private JdbcWriter jdbcWriter;
    private String dbFileName;
    private MServer mServer;
    private int startTime;
    private int endTime;

    private boolean dbInitialized = false;

    private volatile boolean keepListening;
    private BlockingQueue<Command> commandQueue;

    private MatsimVrpData data;
    private VrpData vrpData;
    private MatsimVrpGraph vrpGraph;
    private Scenario scenario;


    public WalTaxiSimEngine(Netsim netsim, MatsimVrpData data,
            VrpOptimizerFactory optimizerFactory, String dbFileName)
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

        List<Vehicle> vehicles = vrpData.getVehicles();
        List<Depot> depots = vrpData.getDepots();

        for (int i = 0; i < realVehicleCount; i++) {
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
            Vehicle vehicle = new VehicleImpl(vehId, "real_" + realVehId, depot, 1, 0, startTime,
                    endTime, endTime - startTime);
            vehicles.add(vehicle);
        }

        super.onPrepareSim();

        // update JDBC
        jdbcWriter = new JdbcWriter(data, dbFileName);
        jdbcWriter.simulationInitialized();
        dbInitialized = true;

        // --> STARTED
        System.err.println("Not synchronized with $startTime$");
        startListening();
        mServer.writeCommand(new Command(CommandType.STARTED));

        System.err.println("SERVER initialization DONE!");
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

        // listen to new Commands
        while (commandQueue.peek() != null) {
            // TASK_ENDED $taskId$ [$time$ -- dla symulacji w real-time - pole niepotrzebne]
            Command command = commandQueue.poll();
            assertCommandType(command, CommandType.TASK_ENDED);

            int taskId = command.getParam(0);

            System.err.println("No procesing for TASK_ENDED " + taskId);
            // update
            // reoptimize
            // nextTask
        }

        // instead of TASK_ENDED processing
        for (Vehicle v : vrpData.getVehicles()) {
            if (! (v instanceof DynVehicle)) {
                Schedule sched = v.getSchedule();

                switch (sched.getStatus()) {
                    case PLANNED:
                        realVehicleLogicSimulation(sched, time, sched.getBeginTime());
                        break;

                    case STARTED:
                        realVehicleLogicSimulation(sched, time, sched.getCurrentTask().getEndTime());
                        break;

                    case UNPLANNED:
                    case COMPLETED:
                        continue;

                    default:
                        throw new RuntimeException();
                }
            }
        }

        try {
            long realTime = (System.currentTimeMillis()) % (24 * 60 * 60 * 1000);
            long simTime = (long) (time * 1000);
            long simAheadOfReal = simTime - realTime;

            if (simAheadOfReal > 0) {
                Thread.sleep(simAheadOfReal);
            }
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private void realVehicleLogicSimulation(Schedule schedule, double now, int nextTaskTime)
    {
        int simTime = (int)now;

        if (simTime > nextTaskTime) {
            throw new RuntimeException("Shouldn't happen");
        }
        else if (simTime == nextTaskTime) {
            if (schedule.getStatus() == ScheduleStatus.STARTED) {
                updateScheduleBeforeNextTask(schedule.getVehicle(), now);
                optimize(now);// TODO: this may be optional (depending on the algorithm)
            }

            schedule.nextTask();
        }
    }


    @Override
    public void optimize(double now)
    {
        super.optimize(now);

        if (dbInitialized) {
            jdbcWriter.schedulesReoptimized();
            mServer.writeCommand(new Command(CommandType.SCHEDULES_REOPTIMIZED));
        }
    }


    @Override
    public void updateScheduleBeforeNextTask(Vehicle vrpVehicle, double now)
    {
        super.updateScheduleBeforeNextTask(vrpVehicle, now);

        jdbcWriter.scheduleUpdated(vrpVehicle.getSchedule());
        mServer.writeCommand(new Command(CommandType.SCHEDULE_UPDATED, vrpVehicle.getId()));
    }


    @Override
    public void taxiRequestSubmitted(Request request, double now)
    {
        // no new Vertex has been added (since we use FixedSizeVrpGraph)
        // no need to call jdbcWriter.newVertex and mServer(VERTEX) therefore...

        // for each new Request a new Customer is created
        Customer customer = request.getCustomer();
        jdbcWriter.newCustomer(customer);
        mServer.writeCommand(new Command(CommandType.CUSTOMER, customer.getId()));

        jdbcWriter.newRequest(request);
        mServer.writeCommand(new Command(CommandType.REQUEST, request.getId()));

        super.taxiRequestSubmitted(request, now);
    }


    @Override
    public void currentTaskChanged(Schedule schedule)
    {
        jdbcWriter.nextTask(schedule);
        mServer.writeCommand(new Command(CommandType.NEXT_TASK, schedule.getVehicle().getId()));
    }


    @Override
    public void taskAdded(Task task)
    {}


    @Override
    public void afterSim()
    {
        // TODO Auto-generated method stub
        super.afterSim();
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

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

package playground.michalm.vrp.data.jdbc;

import java.sql.*;
import java.util.List;

import org.matsim.api.core.v01.Id;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.model.Request.ReqStatus;
import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.Schedule.ScheduleStatus;
import pl.poznan.put.vrp.dynamic.data.schedule.Task.TaskStatus;
import pl.poznan.put.vrp.dynamic.data.schedule.Task.TaskType;
import playground.michalm.vrp.data.MatsimVrpData;
import playground.michalm.vrp.data.model.DynAgentVehicle;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.data.network.shortestpath.*;
import cern.colt.Arrays;


public class JdbcWriter
{
    private static final String DB_URL_1 = "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)};DBQ=";
    private static final String DB_URL_2 = ";DriverID=22;READONLY=false";

    private VrpData data;
    private MatsimVrpGraph vrpGraph;

    private Connection con;

    private PreparedStatement tasksDelete;
    private PreparedStatement schedulesDelete;
    private PreparedStatement requestsDelete;
    private PreparedStatement customersDelete;
    private PreparedStatement vehiclesDelete;
    private PreparedStatement depotsDelete;
    private PreparedStatement vertexesDelete;

    private PreparedStatement vertexInsert;
    private PreparedStatement depotInsert;
    private PreparedStatement vehicleInsert;
    private PreparedStatement scheduleInsert;
    private PreparedStatement customerInsert;
    private PreparedStatement requestInsert;

    private PreparedStatement plannedTaskDelete;
    private PreparedStatement driveTaskInsert;
    private PreparedStatement serveTaskInsert;
    private PreparedStatement waitTaskInsert;
    private PreparedStatement requestStatusUpdate;
    private PreparedStatement scheduleStatusUpdate;

    private PreparedStatement taskTimesUpdate;
    private PreparedStatement taskStatusUpdate;
    private PreparedStatement scheduleCurrentTaskUpdate;

    private PreparedStatement scheduleCurrentLinkUpdate;


    public JdbcWriter(MatsimVrpData matsimData, String dbFileName)
    {
        this.data = matsimData.getVrpData();
        vrpGraph = matsimData.getMatsimVrpGraph();

        try {
            String url = DB_URL_1 + dbFileName + DB_URL_2;
            con = DriverManager.getConnection(url, "", "");

            if (null == con) {
                throw new RuntimeException("Unable to connect to data source " + url);
            }

            con.setAutoCommit(false);

            tasksDelete = con.prepareStatement("delete * from Tasks");
            schedulesDelete = con.prepareStatement("delete * from Schedules");
            requestsDelete = con.prepareStatement("delete * from Requests");
            customersDelete = con.prepareStatement("delete * from Customers");
            vehiclesDelete = con.prepareStatement("delete * from Vehicles");
            depotsDelete = con.prepareStatement("delete * from Depots");
            vertexesDelete = con.prepareStatement("delete * from Vertexes");

            vertexInsert = con.prepareStatement(//
                    "insert into Vertexes (Vertex_Id, Name, X, Y, Link_Id) values (?, ?, ?, ?, ?)");

            depotInsert = con.prepareStatement(//
                    "insert into Depots (Depot_Id, Name, Vertex_Id) values (?, ?, ?)");

            vehicleInsert = con.prepareStatement(//
                    "insert into Vehicles (Vehicle_Id, Name, Depot_Id, Capacity, Cost, T0, T1, "
                            + "TimeLimit, VehicleType) " //
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?)");

            scheduleInsert = con.prepareStatement(//
                    "insert into Schedules (Schedule_Id, Vehicle_Id, Status, CurrentTask_Id, CurrentLink_Id) "
                            + "values (?, ?, ?, null, ?)");

            customerInsert = con.prepareStatement(//
                    "insert into Customers (Customer_Id, Name, Vertex_Id) values (?, ?, ?)");

            requestInsert = con.prepareStatement(//
                    "insert into Requests (Request_Id, Customer_Id, FromVertex_Id, ToVertex_Id, "
                            + "Quantity, Priority, Duration, T0, T1, " + "FixedVehicle, Status, "
                            + "SubmissionTime) " //
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

            plannedTaskDelete = con.prepareStatement(//
                    "delete * from Tasks where TaskStatus='" + TaskStatus.PLANNED.name() + "'");

            driveTaskInsert = con.prepareStatement(//
                    "insert into Tasks (Task_Id, Task_Idx, Schedule_Id, TaskType, TaskStatus, "
                            + "BeginTime, EndTime, FromVertex_Id, ToVertex_Id, Link_Ids) "
                            + "values (?, ?, ?, '" + TaskType.DRIVE.name() + "', ?, ?, ?, ?, ?, ?)");

            serveTaskInsert = con.prepareStatement(//
                    "insert into Tasks (Task_Id, Task_Idx, Schedule_Id, TaskType, TaskStatus, "
                            + "BeginTime, EndTime, Vertex_Id, Request_Id) " //
                            + "values (?, ?, ?, '" + TaskType.SERVE.name() + "', ?, ?, ?, ?, ?)");

            waitTaskInsert = con.prepareStatement(//
                    "insert into Tasks (Task_Id, Task_Idx, Schedule_Id, TaskType, TaskStatus, "
                            + "BeginTime, EndTime, Vertex_Id) " //
                            + "values (?, ?, ?, '" + TaskType.WAIT.name() + "', ?, ?, ?, ?)");

            requestStatusUpdate = con.prepareStatement(//
                    "update Requests set Status=? where Request_Id=?");

            scheduleStatusUpdate = con.prepareStatement(//
                    "update Schedules set Status=? where Schedule_Id=?");

            taskTimesUpdate = con.prepareStatement(//
                    "update Tasks set BeginTime=?, EndTime=? where Schedule_Id=? and Task_Idx=?");

            taskStatusUpdate = con.prepareStatement(//
                    "update Tasks set TaskStatus=? where Schedule_Id=? and Task_Idx=?");

            scheduleCurrentTaskUpdate = con.prepareStatement(//
                    "update Schedules set CurrentTask_Id=? where Schedule_Id=?");

            scheduleCurrentLinkUpdate = con.prepareStatement(//
                    "update Schedules set CurrentLink_Id=? where Schedule_Id=?");
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public void simulationInitialized()
    {
        try {
            // clear database
            tasksDelete.executeUpdate();
            schedulesDelete.executeUpdate();
            requestsDelete.executeUpdate();
            customersDelete.executeUpdate();
            vehiclesDelete.executeUpdate();
            depotsDelete.executeUpdate();
            vertexesDelete.executeUpdate();

            // insert Vertex
            for (Vertex v : data.getVrpGraph().getVertices()) {
                vertexInsert.setInt(1, v.getId());
                vertexInsert.setString(2, v.getName());
                vertexInsert.setDouble(3, v.getX());
                vertexInsert.setDouble(4, v.getY());
                vertexInsert.setString(5, ((MatsimVertex)v).getLink().getId().toString());
                vertexInsert.executeUpdate();
            }

            // insert Depot
            for (Depot d : data.getDepots()) {
                depotInsert.setInt(1, d.getId());
                depotInsert.setString(2, d.getName());
                depotInsert.setInt(3, d.getVertex().getId());
                depotInsert.executeUpdate();
            }

            // insert Vehicle and Schedule
            for (Vehicle v : data.getVehicles()) {
                vehicleInsert.setInt(1, v.getId());
                vehicleInsert.setString(2, v.getName());
                vehicleInsert.setInt(3, v.getDepot().getId());
                vehicleInsert.setInt(4, v.getCapacity());
                vehicleInsert.setDouble(5, v.getCost());
                vehicleInsert.setInt(6, v.getT0());
                vehicleInsert.setInt(7, v.getT1());
                vehicleInsert.setInt(8, v.getTimeLimit());
                vehicleInsert.setString(9, v instanceof DynAgentVehicle ? "V" : "R");
                vehicleInsert.executeUpdate();

                scheduleInsert.setInt(1, v.getId());
                scheduleInsert.setInt(2, v.getId());
                scheduleInsert.setString(3, v.getSchedule().getStatus().name());

                if (v instanceof DynAgentVehicle) {
                    scheduleInsert.setString(4, ((DynAgentVehicle)v).getAgentLogic().getDynAgent()
                            .getCurrentLinkId().toString());
                }
                else {
                    scheduleInsert.setString(4, null);
                }

                scheduleInsert.executeUpdate();
            }

            // insert Customer
            for (Customer c : data.getCustomers()) {
                customerInsert.setInt(1, c.getId());
                customerInsert.setString(2, c.getName());
                customerInsert.setInt(3, c.getVertex().getId());
                customerInsert.executeUpdate();
            }

            // insert Request
            for (Request r : data.getRequests()) {
                requestInsert.setInt(1, r.getId());
                requestInsert.setInt(2, r.getCustomer().getId());
                requestInsert.setInt(3, r.getFromVertex().getId());
                requestInsert.setInt(4, r.getToVertex().getId());
                requestInsert.setInt(5, r.getQuantity());
                requestInsert.setDouble(6, r.getPriority());
                requestInsert.setInt(7, r.getDuration());
                requestInsert.setInt(8, r.getT0());
                requestInsert.setInt(9, r.getT1());
                requestInsert.setBoolean(10, r.getFixedVehicle());
                requestInsert.setString(11, r.getStatus().name());
                requestInsert.setInt(12, r.getSubmissionTime());
                requestInsert.executeUpdate();
            }

            con.commit();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    // currently unused
    @SuppressWarnings("unused")
    private void fillWithTaskForTesting()
    {
        try {
            PreparedStatement scheduleCurrentTaskUpdate = con.prepareStatement("update Schedules "
                    + "set CurrentTask_Id=? " + "where Schedules.Schedule_Id=?");

            for (Vehicle v : data.getVehicles()) {
                Schedule s = v.getSchedule();

                for (Task t : s.getTasks()) {
                    PreparedStatement taskInsert;

                    switch (t.getType()) {
                        case DRIVE:
                            taskInsert = driveTaskInsert;
                            DriveTask dt = (DriveTask)t;
                            driveTaskInsert.setInt(7, dt.getFromVertex().getId());
                            driveTaskInsert.setInt(8, dt.getToVertex().getId());

                            ShortestPath path = MatsimArcs.getShortestPath(vrpGraph,
                                    dt.getFromVertex(), dt.getToVertex(), dt.getBeginTime());

                            driveTaskInsert.setString(9, Arrays.toString(path.linkIds));

                            break;

                        case SERVE:
                            taskInsert = serveTaskInsert;
                            ServeTask st = (ServeTask)t;
                            serveTaskInsert.setInt(7, st.getAtVertex().getId());
                            serveTaskInsert.setInt(8, st.getRequest().getId());
                            break;

                        case WAIT:
                            taskInsert = waitTaskInsert;
                            WaitTask wt = (WaitTask)t;
                            waitTaskInsert.setInt(7, wt.getAtVertex().getId());
                            break;

                        default:
                            throw new RuntimeException("Unsupported TaskType: " + t.getType());
                    }

                    int taskId = calcTaskId(t);
                    taskInsert.setInt(1, taskId);
                    taskInsert.setInt(2, t.getTaskIdx());
                    taskInsert.setInt(3, v.getId());
                    taskInsert.setString(4, t.getStatus().name());
                    taskInsert.setInt(5, t.getBeginTime());
                    taskInsert.setInt(6, t.getEndTime());
                    taskInsert.executeUpdate();

                    if (t.getStatus() == TaskStatus.STARTED) {
                        scheduleCurrentTaskUpdate.setInt(1, taskId);
                        scheduleCurrentTaskUpdate.setInt(2, v.getId());
                        scheduleCurrentTaskUpdate.executeUpdate();
                    }

                    // / [scheduleId * 1000 + taskIdx] -> task_id
                    // / task_id %1000 -> taskIdx
                    // / task_id /1000 -> scheduleId
                }
            }

            con.commit();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }


    public void newVertex(Vertex v)
    {
        try {
            vertexInsert.setInt(1, v.getId());
            vertexInsert.setString(2, v.getName());
            vertexInsert.setDouble(3, v.getX());
            vertexInsert.setDouble(4, v.getY());
            vertexInsert.executeUpdate();

            con.commit();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public void newCustomer(Customer c)
    {
        try {
            customerInsert.setInt(1, c.getId());
            customerInsert.setString(2, c.getName());
            customerInsert.setInt(3, c.getVertex().getId());
            customerInsert.executeUpdate();

            con.commit();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public void newRequest(Request r)
    {
        try {
            requestInsert.setInt(1, r.getId());
            requestInsert.setInt(2, r.getCustomer().getId());
            requestInsert.setInt(3, r.getFromVertex().getId());
            requestInsert.setInt(4, r.getToVertex().getId());
            requestInsert.setInt(5, r.getQuantity());
            requestInsert.setDouble(6, r.getPriority());
            requestInsert.setInt(7, r.getDuration());
            requestInsert.setInt(8, r.getT0());
            requestInsert.setInt(9, r.getT1());
            requestInsert.setBoolean(10, r.getFixedVehicle());
            requestInsert.setString(11, r.getStatus().name());
            requestInsert.setInt(12, r.getSubmissionTime());
            requestInsert.executeUpdate();

            con.commit();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public void schedulesReoptimized()
    {
        try {
            // remove old planned Tasks
            plannedTaskDelete.executeUpdate();

            // insert new Tasks (only planned)
            for (Vehicle v : data.getVehicles()) {
                Schedule s = v.getSchedule();
                List<Task> tasks = s.getTasks();

                int firstPlannedTaskIdx;

                switch (s.getStatus()) {
                    case UNPLANNED:
                    case COMPLETED:
                        continue;

                    case PLANNED:
                        firstPlannedTaskIdx = 0;
                        break;

                    case STARTED:
                        firstPlannedTaskIdx = s.getCurrentTask().getTaskIdx() + 1;
                        break;

                    default:
                        throw new RuntimeException();
                }

                for (int i = firstPlannedTaskIdx; i < s.getTaskCount(); i++) {
                    Task t = tasks.get(i);
                    PreparedStatement taskInsert;

                    switch (t.getType()) {
                        case DRIVE:
                            taskInsert = driveTaskInsert;
                            DriveTask dt = (DriveTask)t;
                            driveTaskInsert.setInt(7, dt.getFromVertex().getId());
                            driveTaskInsert.setInt(8, dt.getToVertex().getId());

                            ShortestPath path = MatsimArcs.getShortestPath(vrpGraph,
                                    dt.getFromVertex(), dt.getToVertex(), dt.getBeginTime());

                            driveTaskInsert.setString(9, Arrays.toString(path.linkIds));

                            break;

                        case SERVE:
                            taskInsert = serveTaskInsert;
                            ServeTask st = (ServeTask)t;
                            serveTaskInsert.setInt(7, st.getAtVertex().getId());
                            serveTaskInsert.setInt(8, st.getRequest().getId());
                            break;

                        case WAIT:
                            taskInsert = waitTaskInsert;
                            WaitTask wt = (WaitTask)t;
                            waitTaskInsert.setInt(7, wt.getAtVertex().getId());
                            break;

                        default:
                            throw new RuntimeException("Unsupported TaskType: " + t.getType());
                    }

                    taskInsert.setInt(1, calcTaskId(t));
                    taskInsert.setInt(2, t.getTaskIdx());
                    taskInsert.setInt(3, v.getId());
                    taskInsert.setString(4, t.getStatus().name());
                    taskInsert.setInt(5, t.getBeginTime());
                    taskInsert.setInt(6, t.getEndTime());
                    taskInsert.executeUpdate();
                }
            }

            // update Reqs
            for (Request r : data.getRequests()) {
                ReqStatus status = r.getStatus();
                switch (status) {
                    case PLANNED:
                    case UNPLANNED:
                    case REJECTED:
                        requestStatusUpdate.setString(1, status.name());
                        requestStatusUpdate.setInt(2, r.getId());
                        requestStatusUpdate.executeUpdate();
                        break;

                    case INACTIVE:
                    case CANCELLED:
                    case STARTED:
                    case PERFORMED:
                        // optimizaton has no effect on such requests
                        break;

                    default:
                        throw new RuntimeException("Unsupported ReqStatus: " + status);
                }
            }

            // update Schedule
            for (Vehicle v : data.getVehicles()) {
                ScheduleStatus status = v.getSchedule().getStatus();

                switch (status) {
                    case UNPLANNED:
                    case PLANNED:
                        scheduleStatusUpdate.setString(1, status.name());
                        scheduleStatusUpdate.setInt(2, v.getId());
                        scheduleStatusUpdate.executeUpdate();
                        break;

                    case STARTED:
                    case COMPLETED:
                        // optimizaton has no effect on such schedules' status
                        break;

                    default:
                        throw new RuntimeException("Unsupported ScheduleStatus: " + status);
                }
            }

            con.commit();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    private static final int FACTOR = 10000;


    public static int calcTaskId(Task task)
    {
        return task.getSchedule().getVehicle().getId() * FACTOR + task.getTaskIdx();
    }


    public static Task findTask(int taskId, List<Vehicle> vehicles)
    {
        return vehicles.get(taskId / FACTOR).getSchedule().getTasks().get(taskId % FACTOR);
    }


    public void scheduleUpdated(Schedule schedule)
    {
        try {
            List<Task> tasks = schedule.getTasks();
            int scheduleId = schedule.getVehicle().getId();

            for (int i = schedule.getCurrentTask().getTaskIdx(); i < schedule.getTaskCount(); i++) {
                Task t = tasks.get(i);

                taskTimesUpdate.setInt(1, t.getBeginTime());
                taskTimesUpdate.setInt(2, t.getEndTime());
                taskTimesUpdate.setInt(3, scheduleId);
                taskTimesUpdate.setInt(4, t.getTaskIdx());
                taskTimesUpdate.executeUpdate();
            }

            con.commit();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    // it is assumed that before switching to the next task all the "begin/endTimes" are corrected
    public void nextTask(Schedule schedule)
    {
        try {
            int scheduleId = schedule.getVehicle().getId();
            Task newTask = schedule.getCurrentTask();

            Task oldTask;
            if (newTask == null) {
                oldTask = Schedules.getLastTask(schedule);
            }
            else if (newTask.getTaskIdx() == 0) {
                oldTask = null;
            }
            else {
                oldTask = Schedules.getPreviousTask(newTask);
            }

            if (oldTask == null) { // update SCHEDULE: planned -> started
                scheduleStatusUpdate.setString(1, ScheduleStatus.STARTED.name());
                scheduleStatusUpdate.setInt(2, scheduleId);
                scheduleStatusUpdate.executeUpdate();

            }
            else {// update TASK: started -> performed
                taskStatusUpdate.setString(1, TaskStatus.PERFORMED.name());
                taskStatusUpdate.setInt(2, scheduleId);
                taskStatusUpdate.setInt(3, oldTask.getTaskIdx());
                taskStatusUpdate.executeUpdate();
            }

            if (newTask == null) { // update SCHEDULE: started -> completed
                scheduleStatusUpdate.setString(1, ScheduleStatus.COMPLETED.name());
                scheduleStatusUpdate.setInt(2, scheduleId);
                scheduleStatusUpdate.executeUpdate();

                scheduleCurrentTaskUpdate.setNull(1, Types.INTEGER);
                scheduleCurrentTaskUpdate.setInt(2, scheduleId);
                scheduleCurrentTaskUpdate.executeUpdate();
            }
            else {// update TASK: planned -> started
                taskStatusUpdate.setString(1, TaskStatus.STARTED.name());
                taskStatusUpdate.setInt(2, scheduleId);
                taskStatusUpdate.setInt(3, newTask.getTaskIdx());
                taskStatusUpdate.executeUpdate();

                scheduleCurrentTaskUpdate.setInt(1, calcTaskId(newTask));
                scheduleCurrentTaskUpdate.setInt(2, scheduleId);
                scheduleCurrentTaskUpdate.executeUpdate();
            }

            // update Request
            if (oldTask instanceof ServeTask) { // started -> performed
                Request oldRequest = ((ServeTask)oldTask).getRequest();
                requestStatusUpdate.setString(1, ReqStatus.PERFORMED.name());
                requestStatusUpdate.setInt(2, oldRequest.getId());
                requestStatusUpdate.executeUpdate();
            }

            if (newTask instanceof ServeTask) { // planned -> started
                Request newRequest = ((ServeTask)newTask).getRequest();
                requestStatusUpdate.setString(1, ReqStatus.STARTED.name());
                requestStatusUpdate.setInt(2, newRequest.getId());
                requestStatusUpdate.executeUpdate();
            }

            con.commit();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public void nextLink(Vehicle vehicle, Id linkId)
    {
        try {
            scheduleCurrentLinkUpdate.setString(1, linkId.toString());
            scheduleCurrentLinkUpdate.setInt(2, vehicle.getId());
            scheduleCurrentLinkUpdate.executeUpdate();

            con.commit();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public void close()
    {
        try {
            con.close();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

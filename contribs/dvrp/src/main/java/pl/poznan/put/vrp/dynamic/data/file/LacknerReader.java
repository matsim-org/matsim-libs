package pl.poznan.put.vrp.dynamic.data.file;

import java.io.*;
import java.util.*;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.*;


/**
 * @author michalm
 */
public class LacknerReader
{
    public enum VrpType
    {
        DISTRIBUTION, COURIER, TAXI;
    };


    public static VrpData parseFiles(String dir, String staticFile, String dynamicFile,
            VertexBuilder vertexBuilder)
        throws IOException
    {
        VrpData data = parseStaticFile(dir, staticFile, vertexBuilder);
        parseDynamicFile(dir, dynamicFile, data);
        return data;
    }


    public static VrpData parseStaticFile(String dir, String file, VertexBuilder vertexBuilder)
        throws IOException
    {
        Scanner scanner = new Scanner(new BufferedReader(new FileReader(dir + file)));

        scanner.nextLine();// 1

        // 2
        String type = scanner.nextLine().trim();// 2 - originally empty;
        VrpType vrpType = type.isEmpty() ? VrpType.DISTRIBUTION : VrpType.valueOf(type);

        scanner.nextLine();// 3
        scanner.nextLine();// 4

        // 5
        int vehCount = scanner.nextInt();
        int vehCapacity = scanner.nextInt();
        scanner.nextLine();// 5

        // 6
        String count = scanner.nextLine().trim();// 6 - originally empty;
        int custCount = count.isEmpty() ? 100 : Integer.parseInt(count);

        scanner.nextLine();// 7
        scanner.nextLine();// 8
        scanner.nextLine();// 9

        VrpData data = new VrpData();

        List<Depot> depots = new ArrayList<Depot>(1);
        List<Customer> customers = new ArrayList<Customer>(custCount);
        List<Vehicle> vehicles = new ArrayList<Vehicle>(vehCount);

        List<Request> requests = new ArrayList<Request>(custCount);

        data.setDepots(depots);
        data.setCustomers(customers);
        data.setVehicles(vehicles);

        data.setRequests(requests);

        int vertexCount = 1 + custCount * (vrpType.equals(VrpType.TAXI) ? 2 : 1);

        FixedSizeVrpGraph graph = new FixedSizeVrpGraph(vertexCount);
        data.setVrpGraph(graph);

        scanner.nextInt();// ID
        int x = scanner.nextInt();
        int y = scanner.nextInt();
        int quantity = scanner.nextInt();
        int t0 = scanner.nextInt();
        int t1 = scanner.nextInt();
        int duration = scanner.nextInt();

        String dName = "D_0";

        Vertex vertex = vertexBuilder.setName(dName).setX(x).setY(y).build();
        graph.addVertex(vertex);

        Depot depot = new DepotImpl(0, dName, vertex);
        depots.add(depot);

        int timeLimit = t1 - t0 + 1;

        for (int i = 0; i < vehCount; i++) {
            String name = "V_" + i;

            Vehicle v = new VehicleImpl(i, name, depot, vehCapacity, 0, t0, t1, timeLimit);

            vehicles.add(v);
        }

        for (int i = 0; i < custCount; i++) {
            scanner.nextInt();// ID
            x = scanner.nextInt();
            y = scanner.nextInt();
            quantity = scanner.nextInt();
            t0 = scanner.nextInt();
            t1 = scanner.nextInt();
            duration = scanner.nextInt();

            String cName = "C_" + i;

            Vertex v = vertexBuilder.setName(cName).setX(x).setY(y).build();
            graph.addVertex(v);

            Customer c = new CustomerImpl(i, cName, v);
            customers.add(c);

            // problem type specific data
            Vertex toVertex = c.getVertex();
            boolean fixedVehicle = false;

            switch (vrpType) {
                case TAXI:
                    x = scanner.nextInt();
                    y = scanner.nextInt();

                    toVertex = vertexBuilder.setName(cName).setX(x).setY(y).build();
                    graph.addVertex(toVertex);
                    break;

                case COURIER:
                    fixedVehicle = scanner.nextInt() != 0;
                    break;
            }

            requests.add(new RequestImpl(i, c, c.getVertex(), toVertex, quantity, 1, duration, t0,
                    t1, fixedVehicle));

        }

        scanner.close();

        graph.initArcs(new ArcFactory() {
            public Arc createArc(Vertex fromVertex, Vertex toVertex)
            {
                double dx = fromVertex.getX() - toVertex.getX();
                double dy = fromVertex.getY() - toVertex.getY();
                double dist = Math.sqrt(dx * dx + dy * dy);
                return new ConstantArc(fromVertex, toVertex, (int)Math.ceil(dist), dist);
            }
        });

        return data;
    }


    public static void parseDynamicFile(String dir, String file, VrpData data)
        throws IOException
    {
        List<Request> requests = data.getRequests();

        Scanner scanner = new Scanner(new BufferedReader(new FileReader(dir + file)));

        for (int i = 0; i < requests.size(); i++) {
            int id = scanner.nextInt();
            int time = scanner.nextInt();

            if (time != -1) {
                Request req = requests.get(id - 1);
                req.deactivate(time);
            }
        }
    }
}

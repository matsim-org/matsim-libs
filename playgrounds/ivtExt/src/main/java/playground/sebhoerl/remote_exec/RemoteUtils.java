package playground.sebhoerl.remote_exec;

import java.rmi.Remote;

public class RemoteUtils {
    public static void removeScenarioAndSimulations(RemoteScenario scenario) {
        for (RemoteSimulation simulation : scenario.getSimulations()) {
            simulation.remove();
        }

        scenario.remove();
    }

    public static boolean isFinished(RemoteSimulation.Status status) {
        switch (status) {
            case DONE:
            case STOPPED:
            case ERROR:
                return true;
        }

        return false;
    }

    public static boolean isFinished(RemoteSimulation simulation) {
        return isFinished(simulation.getStatus());
    }

    public static boolean isIdle(RemoteSimulation.Status status) {
        switch (status) {
            case PENDING: return false;
            case RUNNING: return false;
            case DONE: return true;
            case IDLE: return true;
            case ERROR: return true;
            case STOPPED: return true;
            default: throw new IllegalStateException();
        }
    }

    public static boolean isIdle(RemoteSimulation simulation) {
        return isIdle(simulation.getStatus());
    }

    public static boolean isActive(RemoteSimulation simulation) {
        return isActive(simulation.getStatus());
    }

    public static boolean isActive(RemoteSimulation.Status status) {
        return !isIdle(status);
    }

    public void restart(RemoteSimulation simulation) {
        if (isActive(simulation)) {
            simulation.stop();
        }

        simulation.start();
    }
}

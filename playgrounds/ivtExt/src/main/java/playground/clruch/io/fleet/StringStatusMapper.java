package playground.clruch.io.fleet;

import playground.clruch.dispatcher.core.AVStatus;

public enum StringStatusMapper {
    ;
    // TODO check assignments
    public static AVStatus apply(int now, String string, String distanceMoved, String timePassed) {
        switch (string) {
        case "Am Standplatz":
        	return check_stay(now, Double.parseDouble(distanceMoved), Double.parseDouble(timePassed));        	
        case "In Anfahrt":
        	return AVStatus.DRIVETOCUSTOMER;
        case "Angemeldet":
            return check_stay(now, Double.parseDouble(distanceMoved), Double.parseDouble(timePassed));
        case "In Umgebung":
            return check_stay(now, Double.parseDouble(distanceMoved), Double.parseDouble(timePassed));
        case "Beim Kunden":
            return AVStatus.DRIVETOCUSTOMER;
        case "Besetzt mit Kunden":
        case "Besetzt mit Fahrziel":
        case "Besetzt mit Folgeauftrag":
            return AVStatus.DRIVEWITHCUSTOMER;
        default:
            break;
        }
        throw new RuntimeException(string);
    }
    
    // New method checking for real stay, rebalance drive or offservice
    public static AVStatus check_stay(int now, double distanceMoved, double timePassed) {
        if (timePassed >= 150 && distanceMoved <= 150) // TODO magic const, check with AVSTATUS graph.
            return AVStatus.STAY;
        else
            return AVStatus.REBALANCEDRIVE;
    }
}

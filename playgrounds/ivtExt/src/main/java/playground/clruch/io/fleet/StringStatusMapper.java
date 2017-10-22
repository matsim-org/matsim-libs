package playground.clruch.io.fleet;

import playground.clruch.dispatcher.core.AVStatus;

public enum StringStatusMapper {
    ;
    // TODO check assignments
    public static AVStatus apply(String string, String distance, String time) {
        switch (string) {
        case "Am Standplatz":
        	return AVStatus.check_stay(Double.parseDouble(distance), Double.parseDouble(time));        	
        case "In Anfahrt":
        	return AVStatus.DRIVETOCUSTMER;
        case "Angemeldet":
        case "In Umgebung":
            return AVStatus.REBALANCEDRIVE;
        case "Beim Kunden":
        case "Besetzt mit Kunden":
            return AVStatus.DRIVEWITHCUSTOMER;
        case "Besetzt mit Fahrziel":
        case "Besetzt mit Folgeauftrag":
            return AVStatus.DRIVETOCUSTMER;
        default:
            break;
        }
        throw new RuntimeException(string);
    }
}

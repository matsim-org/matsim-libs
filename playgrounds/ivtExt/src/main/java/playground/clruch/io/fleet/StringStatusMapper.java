package playground.clruch.io.fleet;

import playground.clruch.dispatcher.core.AVStatus;

public enum StringStatusMapper {
    ;
    // TODO check assignments
    public static AVStatus apply(String string, String distance, String time) {
    	double wait_factor = Double.parseDouble(time) / Double.parseDouble(distance);
    	System.out.println("Wait factor " + wait_factor);
        switch (string) {
        case "Am Standplatz":
        	if (wait_factor >= 2.0)
        		return AVStatus.STAY;
        	else
        		return AVStatus.REBALANCEDRIVE;
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

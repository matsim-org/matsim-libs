package playground.clruch.io.fleet;

import playground.clruch.dispatcher.core.AVStatus;

public enum StringStatusMapper {
    ;
    // TODO check assignments
    public static AVStatus apply(String string) {
        switch (string) {
        case "Am Standplatz":
            return AVStatus.STAY;
        case "In Anfahrt":
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

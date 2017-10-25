package playground.clruch.io.fleet;

import playground.clruch.dispatcher.core.AVStatus;

public enum StringStatusMapper {
    ;
    // TODO check assignments
    public static AVStatus apply(int now, int lastTimeStamp, String string, String distanceMoved, String timePassed) {
        switch (string) {
        case "Am Standplatz":
        	return AVStatus.check_stay(now, lastTimeStamp, Double.parseDouble(distanceMoved), Double.parseDouble(timePassed));        	
        case "In Anfahrt":
        	return AVStatus.DRIVETOCUSTMER;
        case "Angemeldet":
            return AVStatus.check_stay(now, lastTimeStamp, Double.parseDouble(distanceMoved), Double.parseDouble(timePassed));
        case "In Umgebung":
            return AVStatus.check_stay(now, lastTimeStamp, Double.parseDouble(distanceMoved), Double.parseDouble(timePassed));
        case "Beim Kunden":
            return AVStatus.DRIVETOCUSTMER;
        case "Besetzt mit Kunden":
            return AVStatus.DRIVEWITHCUSTOMER;
        case "Besetzt mit Fahrziel":
            return AVStatus.DRIVEWITHCUSTOMER;
        case "Besetzt mit Folgeauftrag":
            return AVStatus.DRIVEWITHCUSTOMER;
        default:
            break;
        }
        throw new RuntimeException(string);
    }
}

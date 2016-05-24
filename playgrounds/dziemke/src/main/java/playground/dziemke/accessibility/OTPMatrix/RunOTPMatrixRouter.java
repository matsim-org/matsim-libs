package playground.dziemke.accessibility.OTPMatrix;

/**
 * @author gabriel.thunig
 */
public class RunOTPMatrixRouter {

    public static void main(String[] args) {
        String fromIndividualsFilePath = "playgrounds/dziemke/input/stops.txt";
        String toIndividualsFilePath = "playgrounds/dziemke/input/stops.txt";
        String graphParentDirectoryPath = "playgrounds/dziemke/input/";
        String outputDirectory = "playgrounds/dziemke/output/";
        String timeZone = "Europe/Berlin";
        String date = "2016-06-01";
        String departureTime = Integer.toString(8*60*60);
        String[] arguments = new String[7];
        arguments[0] = fromIndividualsFilePath;
        arguments[1] = toIndividualsFilePath;
        arguments[2] = graphParentDirectoryPath;
        arguments[3] = outputDirectory;
        arguments[4] = timeZone;
        arguments[5] = date;
        arguments[6] = departureTime;

        OTPMatrixRouter.main(arguments);
    }
}

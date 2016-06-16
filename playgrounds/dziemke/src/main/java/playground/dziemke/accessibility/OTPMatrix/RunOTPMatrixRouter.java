package playground.dziemke.accessibility.OTPMatrix;

/**
 * @author gabriel.thunig
 */
public class RunOTPMatrixRouter {

    public static void main(String[] args) {
        String fromIndividualsFilePath = "../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/gtfs/stops.txt";
        String toIndividualsFilePath = "../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/gtfs/stops.txt";
        String graphParentDirectoryPath = "../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/";
        String outputDirectory = "../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/gtfs/matrix/";
        String timeZone = "Africa/Nairobi";
        String date = "2014-05-26";
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

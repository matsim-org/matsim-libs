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
        String inputCRS = "EPSG:4326"; // EPSG:4326 = WGS 84
        String outputCRS = "EPSG:21037"; // EPSG:21037 = Arc 1960 / UTM zone 37S
        String[] arguments = new String[9];
        arguments[0] = fromIndividualsFilePath;
        arguments[1] = toIndividualsFilePath;
        arguments[2] = graphParentDirectoryPath;
        arguments[3] = outputDirectory;
        arguments[4] = timeZone;
        arguments[5] = date;
        arguments[6] = departureTime;
        arguments[7] = inputCRS;
        arguments[8] = outputCRS;

//        OTPMatrixRouter.main(arguments);
    }
}

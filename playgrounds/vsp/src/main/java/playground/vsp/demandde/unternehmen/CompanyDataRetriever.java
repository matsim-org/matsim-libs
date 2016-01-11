package playground.vsp.demandde.unternehmen;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author gthunig, dziemke
 * 
 * This class searches for company data on the bundesanzeiger-verlag.de website and writes them into an output file.
 */
public class CompanyDataRetriever {

    private static final Logger log = Logger.getLogger(CompanyDataRetriever.class);

    private static final String WEBSITE_URL = "http://www.bundesanzeiger-verlag.de";
    private static final String COMPANY_URL = "/betrifft-unternehmen/unternehmensdaten/deutsche-unternehmensdaten/" +
            "suche-nach-unternehmensdaten/firmen-details.html?tx_s4afreekmu_pi1%5Bfilter%5D=postcode&tx_s4afr" +
            "eekmu_pi1%5Bvalue%5D=1&tx_s4afreekmu_pi1%5Border%5D=name&tx_s4afreekmu_pi1%5Bdirection%5D=asc&tx" +
            "_s4afreekmu_pi1%5Blimit%5D=10&tx_s4afreekmu_pi1%5Bpage%5D=";
    private static final String ID_URL = "&tx_s4afreekmu_pi1%5Bcompany_id%";
    private static final String PAGE_URL = "/betrifft-unternehmen/unternehmensdaten/deutsche-unternehmensdaten/suche" +
            "-nach-unternehmensdaten/suchergebnisse.html?tx_s4afreekmu_pi1%5Bfilter%5D=postcode&tx_s4afreekmu" +
            "_pi1%5Bvalue%5D=1&tx_s4afreekmu_pi1%5Border%5D=name&tx_s4afreekmu_pi1%5Bdirection%5D=asc&tx_s4af" +
            "reekmu_pi1%5Blimit%5D=10&tx_s4afreekmu_pi1%5Bpage%5D=";

    /*
     * Set the webPageIndex to "1" if starting on the first webpage to be requested OR
     * to a value (n * numberOfWebpagesPerFile) + 1) for subsequent files. The reason for
     * having the latter is mainly that the readout may get interrupted due to internet
     * connectivity problems.
     */
    private static int webPageIndex = 13001;
    
    /* States how many webpages (each containing 10 companies) are requested until a
     * file is written.
     */
    private static int numberOfWebpagesPerFile = 1000;
    
    private static String outputDirectory = "../../../../Workspace/data/accessibility/berlin/companyData/";
    
    public static void main(String[] args) {
    	if (webPageIndex < 1) {
    		throw new RuntimeException("webPageIndex may only be a natural number (except zero).");
    	}
    	if (webPageIndex != 1 && (webPageIndex - 1) % numberOfWebpagesPerFile != 0) {
    		throw new RuntimeException("For consecutive files (i.e. files that do not start with "
    				+ "the first webpage, the numberOfWebpagesPerFile should be a divisor of webPageIndex "
    				+ "without rest. Otherwise collected data will most probably be incomplete.");
    	}
    	
    	int fileNumber = (webPageIndex - 1) / numberOfWebpagesPerFile + 1;
        long startTime = System.currentTimeMillis();
        boolean continueWriting = true;
        
        do {
        	String outputFile = outputDirectory + "companyData_" + fileNumber + ".csv";
        	List<CompanyData> companyData = getCompanyData(numberOfWebpagesPerFile * fileNumber);
            log.info("Number of companies to be added to file " + outputFile + " is " + companyData.size());
            if (companyData.size() != 0) {
				writeCompanyData(companyData, outputFile);
                fileNumber++;
                companyData.clear();
                long timePeriod = System.currentTimeMillis() - startTime;
                startTime = System.currentTimeMillis();
				log.info("Creating file " + outputFile + " (incl. webpage readout etc.) took " + timePeriod + " ms.");
            } else {
                continueWriting = false;
            }
        } while(continueWriting);
    }

    public static void writeCompanyData(List<CompanyData> companyData, String outputFile) {
        log.info("Writing company data to " + outputFile);

        CSVFileWriter writer = new CSVFileWriter(outputFile, ";");

        writeInitialLine(writer);
        for (CompanyData currentData : companyData) {
            writeCompanyData(writer, currentData);
        }
        writer.close();
    }

    private static void writeInitialLine(CSVFileWriter writer) {
        writer.writeField("Firmenname");
        writer.writeField("PLZ");
        writer.writeField("Ort");
        writer.writeField("Gesellschaftsform");
        writer.writeField("Handelsregisternummer");
        writer.writeField("Branche(n)");
        writer.writeField("Latitude");
        writer.writeField("Longitude");
        writer.writeNewLine();
    }

    private static void writeCompanyData
            (CSVFileWriter writer, CompanyData companyData) {
        writer.writeField(companyData.getCompanyName());
        writer.writeField(companyData.getZipCode());
        writer.writeField(companyData.getPlace());
        writer.writeField(companyData.getCorporateForm());
        writer.writeField(companyData.getCommercialRegisterNumber());
        String sector = "";
        for (int e = 0; e < companyData.getSector().size(); e++) {
            sector += companyData.getSector().get(e);
            if (e != companyData.getSector().size()-1) {
                sector += "/";
            }
        }
        writer.writeField(sector);
        writer.writeField(companyData.getxCoordinate());
        writer.writeField(companyData.getyCoordinate());
        writer.writeNewLine();
    }

    private static List<CompanyData> getCompanyData(int endInt) {

        List<CompanyData> companyData = new ArrayList<>();
        boolean continueReading = true;
        do {
            try {
                URL url = new URL(WEBSITE_URL + PAGE_URL + webPageIndex);
                try {
                    InputStreamReader inputStreamReader = new InputStreamReader(url.openStream());
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                    log.info("Reading companyData page " + webPageIndex);

                    String line;
                    try {
                        while ((line = bufferedReader.readLine()) != null) {
                            if (line.contains("href=\"" + COMPANY_URL) && line.contains("InternerLinkHighlight")) {
                                String[] splitString = line.split("%");
                                String id = splitString[splitString.length-1].split("\"")[0];
                                bufferedReader.readLine();
                                line = bufferedReader.readLine();
                                if (line.contains("<td>Berlin</td>")) {
                                    line = bufferedReader.readLine();
                                    String corporateForm = removeTagFromContent(formatContent(line), "td");
                                    CompanyData currentCompanyData = getCompanyDataFromIdAndPageIndex(id, webPageIndex);
                                    currentCompanyData.setCorporateForm(removeTapsAndSpacesBeforeLine(corporateForm));
                                    companyData.add(currentCompanyData);
                                }
                            }
                            if (webPageIndex >= endInt || line.contains("Ihre Suche ergab leider keine Treffer.")) {
                                continueReading = false;
                            }
                        }
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            webPageIndex++;
        } while(continueReading);
        return companyData;
    }

    private static CompanyData getCompanyDataFromIdAndPageIndex(String id, int pageIndex) {
        return getCompanyData(WEBSITE_URL + COMPANY_URL + pageIndex + ID_URL + id);
    }

    private static CompanyData getCompanyData(String urlString) {

        CompanyData data = new CompanyData();

        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        if (url != null) {
            try {
                InputStreamReader inputStreamReader = new InputStreamReader(url.openStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                String line;
                try {
                    while ((line = bufferedReader.readLine()) != null) {
                        if (line.contains("s4a_freekmu_single_content")) {
                            line = bufferedReader.readLine();
                            data.setCompanyName(removeTapsAndSpacesBeforeLine(
                                    removeTagFromContent(formatContent(line), "h1")));
                        }
                        if (line.contains("<h5>Ort</h5>")) {
                            line = bufferedReader.readLine();
                            String[] site = removeTagFromContent(removeTapsAndSpacesBeforeLine(line), "p").split(" ");
                            data.setZipCode(removeTapsAndSpacesBeforeLine(site[0]));
                            data.setPlace(removeTapsAndSpacesBeforeLine(site[1]));
                        }
                        if (line.contains("<h5>Register</h5>")) {
                            line = bufferedReader.readLine();
                            data.setCommercialRegisterNumber(removeTapsAndSpacesBeforeLine(
                                    removeTagFromContent(removeTapsAndSpacesBeforeLine(line), "p")));
                        }
                        if (line.contains("<h5>Branche(n):</h5>") &&
                                (line = bufferedReader.readLine()).contains("<ul>")) {
                            ArrayList<String> sector = new ArrayList<>();
                            while (!(line = bufferedReader.readLine()).contains("</ul>")) {
                                if (line.contains("<li>")) {
                                    sector.add(removeTapsAndSpacesBeforeLine(removeTagFromContent(formatContent(line), "li")));
                                }
                            }
                            data.setSector(sector);
                        }
                        if (line.contains("LatLng")) {
                            String[] coordinates = line.split("LatLng")[1].
                                    replace("(", "").replace(")", "").replace(";", "").split(", ");
                            data.setxCoordinate(removeTapsAndSpacesBeforeLine(coordinates[0]));
                            data.setyCoordinate(removeTapsAndSpacesBeforeLine(coordinates[1]));
                        }
                    }
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return data;
    }

    private static String formatContent(String content) {
        return removeTagFromContent(removeTapsAndSpacesBeforeLine(content).replace("&nbsp;", " ").replace(";", ""), "strong");
    }

    private static String removeTapsAndSpacesBeforeLine(String line) {
        line = line.replaceAll("\t", "");
        while (line.startsWith(" ")) {
            line = line.replaceFirst(" ", "");
        }
        return line;
    }

    private static String removeTagFromContent(String line, String tag) {
        return line.replace("<" + tag + ">", "").replace("</" + tag + ">", "");
    }

    private static class CompanyData {

        private String companyName = "";
        private String zipCode = "";
        private String place = "";
        private String corporateForm = "";
        private String commercialRegisterNumber = "";
        private ArrayList<String> sector = new ArrayList<>();
        private String xCoordinate = "";
        private String yCoordinate = "";

        public String getCompanyName() {
            return companyName;
        }

        public void setCompanyName(String companyName) {
            this.companyName = companyName;
        }

        public String getZipCode() {
            return zipCode;
        }

        public void setZipCode(String zipCode) {
            this.zipCode = zipCode;
        }

        public String getPlace() {
            return place;
        }

        public void setPlace(String place) {
            this.place = place;
        }

        public String getCorporateForm() {
            return corporateForm;
        }

        public void setCorporateForm(String corporateForm) {
            this.corporateForm = corporateForm;
        }

        public String getCommercialRegisterNumber() {
            return commercialRegisterNumber;
        }

        public void setCommercialRegisterNumber(String commercialRegisterNumber) {
            this.commercialRegisterNumber = commercialRegisterNumber;
        }

        public ArrayList<String> getSector() {
            return sector;
        }

        public void setSector(ArrayList<String> sector) {
            this.sector = sector;
        }

        public String getxCoordinate() {
            return xCoordinate;
        }

        public void setxCoordinate(String xCoordinate) {
            this.xCoordinate = xCoordinate;
        }

        public String getyCoordinate() {
            return yCoordinate;
        }

        public void setyCoordinate(String yCoordinate) {
            this.yCoordinate = yCoordinate;
        }

        @Override
        public String toString() {
            return "CompanyData{" +
                    "companyName='" + companyName + '\'' +
                    ", zipCode='" + zipCode + '\'' +
                    ", place='" + place + '\'' +
                    ", corporateForm='" + corporateForm + '\'' +
                    ", commercialRegisterNumber='" + commercialRegisterNumber + '\'' +
                    ", sector=" + sector +
                    ", xCoordinate='" + xCoordinate + '\'' +
                    ", yCoordinate='" + yCoordinate + '\'' +
                    '}';
        }
    }
}
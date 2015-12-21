package playground.gthunig.berlinCompanyData;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gthunig on 16.12.15.
 * This class searches for company data at the bundesanzeiger-verlag.de website and writes them out in an output file.
 */
public class CompanyDataRequester {

    private static final String WEBSITE_URL = "http://www.bundesanzeiger-verlag.de/";
    private static final String COMPANY_URL = "/betrifft-unternehmen/unternehmensdaten/deutsche-unternehmensdaten/" +
            "suche-nach-unternehmensdaten/firmen-details.html?tx_s4afreekmu_pi1%5Bfilter%5D=postcode&tx_s4afr" +
            "eekmu_pi1%5Bvalue%5D=1&tx_s4afreekmu_pi1%5Border%5D=name&tx_s4afreekmu_pi1%5Bdirection%5D=asc&tx" +
            "_s4afreekmu_pi1%5Blimit%5D=10&tx_s4afreekmu_pi1%5Bpage%5D=";
    private static final String ID_URL = "&tx_s4afreekmu_pi1%5Bcompany_id%";
    private static final String PAGE_URL = "/betrifft-unternehmen/unternehmensdaten/deutsche-unternehmensdaten/suche" +
            "-nach-unternehmensdaten/suchergebnisse.html?tx_s4afreekmu_pi1%5Bfilter%5D=postcode&tx_s4afreekmu" +
            "_pi1%5Bvalue%5D=1&tx_s4afreekmu_pi1%5Border%5D=name&tx_s4afreekmu_pi1%5Bdirection%5D=asc&tx_s4af" +
            "reekmu_pi1%5Blimit%5D=10&tx_s4afreekmu_pi1%5Bpage%5D=";
    private static final String SEPERATOR = "%";

    public static void main(String[] args) {
        long time = System.currentTimeMillis();
        List<CompanyData> companyData = getCompanyData();
        writeCompanyData(companyData, new File("companyData.txt"));
        System.out.println(System.currentTimeMillis() - time);
    }

    public static void writeCompanyData(List<CompanyData> companyData, File outputFile) {

        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(outputFile);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
        DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);

        try {
            writeInitialLine(dataOutputStream);
            for (int i = 0; i < companyData.size(); i++) {
                dataOutputStream.writeBytes(Integer.toString(i+1));
                dataOutputStream.writeBytes(SEPERATOR);
                System.out.println("i = " + i + " " + companyData.get(i));
                writeCompanyData(dataOutputStream, companyData.get(i));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            dataOutputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeCompanyData
            (DataOutputStream dataOutputStream, CompanyData companyData) throws  IOException {
        dataOutputStream.writeBytes(companyData.getCompanyName());
        dataOutputStream.writeBytes(SEPERATOR);
        dataOutputStream.writeBytes(companyData.getZipCode());
        dataOutputStream.writeBytes(SEPERATOR);
        dataOutputStream.writeBytes(companyData.getPlace());
        dataOutputStream.writeBytes(SEPERATOR);
        dataOutputStream.writeBytes(companyData.getCorporateForm());
        dataOutputStream.writeBytes(SEPERATOR);
        dataOutputStream.writeBytes(companyData.getCommercialRegisterNumber());
        dataOutputStream.writeBytes(SEPERATOR);
        for (String s : companyData.getSector()) {
            dataOutputStream.writeBytes(s);
            dataOutputStream.writeBytes(SEPERATOR);
        }
        dataOutputStream.writeBytes(companyData.getxCoordinate());
        dataOutputStream.writeBytes(SEPERATOR);
        dataOutputStream.writeBytes(companyData.getyCoordinate());
        dataOutputStream.writeBytes("\n");
    }

    private static void writeInitialLine(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeBytes("Nr");
        dataOutputStream.writeBytes(SEPERATOR);
        dataOutputStream.writeBytes("Firmenname");
        dataOutputStream.writeBytes(SEPERATOR);
        dataOutputStream.writeBytes("PLZ");
        dataOutputStream.writeBytes(SEPERATOR);
        dataOutputStream.writeBytes("Ort");
        dataOutputStream.writeBytes(SEPERATOR);
        dataOutputStream.writeBytes("Gesellschaftsform");
        dataOutputStream.writeBytes(SEPERATOR);
        dataOutputStream.writeBytes("Handelsregisternummer");
        dataOutputStream.writeBytes(SEPERATOR);
        dataOutputStream.writeBytes("Branche(n)");
        dataOutputStream.writeBytes(SEPERATOR);
        dataOutputStream.writeBytes("x Koordinate");
        dataOutputStream.writeBytes(SEPERATOR);
        dataOutputStream.writeBytes("y Koordinate");
        dataOutputStream.writeBytes("\n");
    }

    private static List<CompanyData> getCompanyData() {

        List<CompanyData> companyData = new ArrayList<>();
        int i = 0;
        boolean continueReading = true;
        do {
            i++;
            try {
                URL url = new URL(WEBSITE_URL + PAGE_URL + i);
                try {
                    InputStreamReader inputStreamReader = new InputStreamReader(url.openStream());
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                    String line;
                    try {
                        while ((line = bufferedReader.readLine()) != null) {
                            if (line.contains("href=\"" + COMPANY_URL) && line.contains("InternerLinkHighlight")) {
                                String[] splitString = line.split("%");
                                String id = splitString[splitString.length-1].split("\"")[0];
                                bufferedReader.readLine();
                                bufferedReader.readLine();
                                line = bufferedReader.readLine();
                                String corporateForm = removeTagFromContent(formatContent(line), "td");
                                CompanyData currentCompanyData = getCompanyDataFromIdAndPageIndex(id, i);
                                currentCompanyData.setCorporateForm(corporateForm);
                                companyData.add(currentCompanyData);
                            }
                        }
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            } catch (MalformedURLException e) {
                continueReading = false;
            }
            if (i > 1) {
                continueReading = false;
            }
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
                            data.setCompanyName(removeTagFromContent(formatContent(line), "h1"));
                        }
                        if (line.contains("<h5>Ort</h5>")) {
                            line = bufferedReader.readLine();
                            String[] site = removeTagFromContent(removeSpacesBeforeLine(line), "p").split(" ");
                            data.setZipCode(site[0]);
                            data.setPlace(site[1]);
                        }
                        if (line.contains("<h5>Register</h5>")) {
                            line = bufferedReader.readLine();
                            data.setCommercialRegisterNumber(removeTagFromContent(removeSpacesBeforeLine(line), "p"));
                        }
                        if (line.contains("<h5>Branche(n):</h5>") && (line = bufferedReader.readLine()).contains("<ul>")) {
                            ArrayList<String> sector = new ArrayList<>();
                            while (!(line = bufferedReader.readLine()).contains("</ul>")) {
                                if (line.contains("<li>")) {
                                    sector.add(removeTagFromContent(formatContent(line), "li"));
                                }
                            }
                            data.setSector(sector);
                        }
                        if (line.contains("LatLng")) {
                            String[] coordinates = line.split("LatLng")[1].
                                    replace("(", "").replace(")", "").replace(";", "").split(", ");
                            data.setxCoordinate(coordinates[0]);
                            data.setyCoordinate(coordinates[1]);
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
        return removeTagFromContent(removeSpacesBeforeLine(content).replace("&nbsp;", " "), "strong");
    }

    private static String removeSpacesBeforeLine(String line) {
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
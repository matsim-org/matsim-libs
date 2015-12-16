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
 */
public class CompanyDataRequester {

    private static String websiteUrl = "http://www.bundesanzeiger-verlag.de/";
    private static String searchCompanyUrl = "betrifft-unternehmen/unternehmensdaten/deutsche-unternehmensdat" +
            "en/suche-nach-unternehmensdaten/suchergebnisse.html?tx_s4afreekmu_pi1[filter]=postcode&tx_s4afre" +
            "ekmu_pi1[value]=1";
    private static String companyUrl = "/betrifft-unternehmen/unternehmensdaten/deutsche-unternehmensdaten/" +
            "suche-nach-unternehmensdaten/firmen-details.html?tx_s4afreekmu_pi1%5Bfilter%5D=postcode&tx_s4afr" +
            "eekmu_pi1%5Bvalue%5D=1&tx_s4afreekmu_pi1%5Border%5D=name&tx_s4afreekmu_pi1%5Bdirection%5D=asc&tx" +
            "_s4afreekmu_pi1%5Blimit%5D=10&tx_s4afreekmu_pi1%5Bpage%5D=1&tx_s4afreekmu_pi1%5Bcompany_id%";
    private static String pageUrl = "/betrifft-unternehmen/unternehmensdaten/deutsche-unternehmensdaten/suche" +
            "-nach-unternehmensdaten/suchergebnisse.html?tx_s4afreekmu_pi1%5Bfilter%5D=postcode&tx_s4afreekmu" +
            "_pi1%5Bvalue%5D=1&tx_s4afreekmu_pi1%5Border%5D=name&tx_s4afreekmu_pi1%5Bdirection%5D=asc&tx_s4af" +
            "reekmu_pi1%5Blimit%5D=10&tx_s4afreekmu_pi1%5Bpage%5D=";


    public static void main(String[] args) {
        long time = System.currentTimeMillis();
        CompanyDataRequester.writeCompanyData(websiteUrl + searchCompanyUrl, new File("companyData.txt"));
        System.out.println(System.currentTimeMillis() - time);
    }

    public static void writeCompanyData(String urlString, File outputFile) {

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
                FileOutputStream fileOutputStream = null;
                try {
                    fileOutputStream = new FileOutputStream(outputFile);
                } catch (FileNotFoundException e1) {
                    e1.printStackTrace();
                }
                DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);
                try {
                    List<String> ids = new ArrayList<>();
                    String nextButtonLink = null;
                    while ((line = bufferedReader.readLine()) != null) {
                        if (line.contains("href=\"" + companyUrl) && line.contains("InternerLinkHighlight")) {
                            //System.out.println(line);
                            String[] splitString = line.split("%");
                            String id = splitString[splitString.length-1].split("\"")[0];
                            ids.add(id);
                            //System.out.println("id = " + id);
                            //System.out.println("link = " + websiteUrl + companyUrl + id);
                        }
                        if (line.contains("btn_next")) {
                            String[] split = line.split("\"");
                            nextButtonLink = split[1];
                        }
                        //dataOutputStream.writeBytes(line+"\n");
                    }
                    if (nextButtonLink != null) {
                        //System.out.println(nextButtonLink);
                    }
                    System.out.println(websiteUrl + companyUrl + ids.get(0));
                    getCompanyData(websiteUrl + companyUrl + ids.get(0));
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }


        }

    }

    private static String[] getCompanyData(String urlString) {

        String[] result = new String[8];

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
                        if (line.contains("LatLng")) {
                            String[] coordinates = line.split("LatLng")[1].
                                    replace("(", "").replace(")", "").replace(";", "").split(", ");
                            result[6] = coordinates[0];
                            result[7] = coordinates[1];
                        }
                        System.out.println(line);
                    }
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }


        }


        return result;
    }
}

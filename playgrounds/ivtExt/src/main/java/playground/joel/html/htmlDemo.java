package playground.joel.html;

import java.io.File;

/**
 * Created by Joel on 26.06.2017.
 */
public class htmlDemo {
    public static void main(String[] args) {
        String fileName = "htmlDemo";

        File config = new File(args[0]);
        File file= new File(config.getParent());
        file.mkdir();

        // write document
        htmlUtils.html();
        htmlUtils.title("HTML Demo");
        htmlUtils.body();
        htmlUtils.insertText("1st line", "2nd line", "3rd line", "and so forth");
        htmlUtils.body();
        htmlUtils.html();

        // save document
        try {
            htmlUtils.saveFile(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

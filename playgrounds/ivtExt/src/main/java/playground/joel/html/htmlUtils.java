package playground.joel.html;

import playground.clruch.utils.GlobalAssert;

import java.io.*;

/**
 * Created by Joel on 26.06.2017.
 */
public abstract class htmlUtils {
    public static StringBuilder stringBuilder = new StringBuilder();
    public static boolean html = false;
    public static boolean head = false;
    public static boolean body = false;

    /**
     * called at the begin and end of document
     */
    public static void html() {
        if (html)
            stringBuilder.append("</html>");
        else {
            stringBuilder.append("<html>");
            html = true;
        }
    }

    /**
     * called at the begin and end of head
     */
    public static void head() {
        if (head)
            stringBuilder.append("</head>");
        else {
            stringBuilder.append("<head>");
            head = true;
        }
    }

    /**
     * called at the begin and end of document
     */
    public static void body() {
        if (body)
            stringBuilder.append("</body>");
        else {
            stringBuilder.append("<body>");
            body = true;
        }
    }

    /**
     * tab title
     *
     * @param title
     */
    public static void setTitle(String title) {
        head();
        stringBuilder.append("<title>" + title + "</title>");
        head();
    }

    /**
     * page title
     *
     * @param title
     */
    public static void insertTitle(String title) {
        head();
        stringBuilder.append("<h1>" + title + "</h1>");
        head();
    }

    public static void title(String title) {
        insertTitle(title);
        setTitle(title);
    }

    public static void insertText(String... line) {
        for (int i = 0; i < line.length; i++) {
            stringBuilder.append("<p>" + line[i] + "</p>");
        }
    }





    public static void saveFile(String fileName) throws IOException {
        File file = new File("output", fileName);
        // if file does exists, then delete and create a new file
        if (file.exists())
            GlobalAssert.that(file.delete());

        //write to file with OutputStreamWriter
        OutputStream outputStream = new FileOutputStream(file.getAbsoluteFile());
        Writer writer = new OutputStreamWriter(outputStream);
        writer.write(stringBuilder.toString());
        writer.close();

    }
}

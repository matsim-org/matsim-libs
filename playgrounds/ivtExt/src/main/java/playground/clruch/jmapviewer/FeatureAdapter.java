// License: GPL. For details, see Readme.txt file.
package playground.clruch.jmapviewer;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.logging.Logger;

public final class FeatureAdapter {

    private static BrowserAdapter browserAdapter = new DefaultBrowserAdapter();
    private static TranslationAdapter translationAdapter = new DefaultTranslationAdapter();
    private static LoggingAdapter loggingAdapter = new DefaultLoggingAdapter();

    private FeatureAdapter() {
        // private constructor for utility classes
    }

    public interface BrowserAdapter {
        void openLink(String url);
    }

    public interface TranslationAdapter {
        String tr(String text, Object... objects);
        // TODO: more i18n functions
    }

    public interface LoggingAdapter {
        Logger getLogger(String name);
    }

    public static void registerBrowserAdapter(BrowserAdapter browserAdapter) {
        FeatureAdapter.browserAdapter = browserAdapter;
    }

    public static void registerTranslationAdapter(TranslationAdapter translationAdapter) {
        FeatureAdapter.translationAdapter = translationAdapter;
    }

    public static void registerLoggingAdapter(LoggingAdapter loggingAdapter) {
        FeatureAdapter.loggingAdapter = loggingAdapter;
    }

    public static void openLink(String url) {
        browserAdapter.openLink(url);
    }

    public static String tr(String text, Object... objects) {
        return translationAdapter.tr(text, objects);
    }

    public static Logger getLogger(String name) {
        return loggingAdapter.getLogger(name);
    }

    public static class DefaultBrowserAdapter implements BrowserAdapter {
        @Override
        public void openLink(String url) {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                try {
                    Desktop.getDesktop().browse(new URI(url));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            } else {
                System.err.println(tr("Opening link not supported on current platform (''{0}'')", url));
            }
        }
    }

    public static class DefaultTranslationAdapter implements TranslationAdapter {
        @Override
        public String tr(String text, Object... objects) {
            return MessageFormat.format(text, objects);
        }
    }

    public static class DefaultLoggingAdapter implements LoggingAdapter {
        @Override
        public Logger getLogger(String name) {
            return Logger.getLogger(name);
        }
    }
}

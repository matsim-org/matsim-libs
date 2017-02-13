package playground.clruch.utils;

public class GlobalAssert {
    public static void of(boolean status) {
        if (!status)
            throw new RuntimeException();
    }
}

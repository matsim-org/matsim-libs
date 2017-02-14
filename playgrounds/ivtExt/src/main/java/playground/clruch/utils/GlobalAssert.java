package playground.clruch.utils;

public class GlobalAssert {
    public static void that(boolean status) {
        if (!status)
            throw new RuntimeException();
    }
}

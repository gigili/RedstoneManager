package dev.igorilic.redstonemanager.util;

public final class NumbersUtil {
    private static final String[] SUFFIXES = new String[]{"k", "m", "b", "t"};

    private NumbersUtil() {}

    public static String format(int number) {
        if (number < 1000) return String.valueOf(number);

        int exp = (int) (Math.log10(number) / 3);
        if (exp - 1 >= SUFFIXES.length) return String.valueOf(number); // out of bounds, cope

        double scaled = number / Math.pow(1000, exp);
        String suffix = SUFFIXES[exp - 1];

        return String.format("%.1f%s", scaled, suffix).replace(".0", "");
    }
}

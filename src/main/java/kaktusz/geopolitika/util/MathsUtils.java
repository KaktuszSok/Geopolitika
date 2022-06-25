package kaktusz.geopolitika.util;

import org.apache.commons.lang3.RandomUtils;

import java.util.List;

@SuppressWarnings("unused")
public class MathsUtils {

    @SuppressWarnings("ManualMinMaxCalculation") //readability nicer, less function calls
    public static float clamp(float t, float min, float max) {
        if(t < min)
            return min;
        if(t > max)
            return max;

        return t;
    }

    public static float lerp(float a, float b, float t) {
        return (1.0F - t)*a + t*b;
    }

    /**
     * Clamped lerp
     */
    public static float lerpc(float a, float b, float t) {
        return lerp(a, b, clamp(t, 0.0F, 1.0F));
    }

    public static float lerpInverse(float a, float b, float t) {
        return (t-a) / (b-a);
    }

    /**
     * Clamped lerpInverse
     */
    public static float lerpInverseC(float a, float b, float t) {
        return clamp(lerpInverse(a, b, t), 0.0F, 1.0F);
    }

    public static int randomRange(int min, int max) {
        return min + RandomUtils.nextInt(0, max-min);
    }
    public static float randomRange(float min, float max) {
        return min + RandomUtils.nextFloat()*(max-min);
    }
    public static double randomRange(double min, double max) {
        return min + RandomUtils.nextDouble()*(max-min);
    }

    /**
     * @param chance The chance to get "true", between 0 and 1
     * @return True or false based on the chance
     */
    public static boolean rollChanceFraction(double chance) {
        return rollChance100(chance*100d);
    }
    /**
     * @param chance The percent chance to get "true", out of 100
     * @return True or false based on the chance
     */
    public static boolean rollChance100(double chance) {
        if(chance >= 100)
            return true;
        return randomRange(0, 100d) < chance;
    }

    public static <T> T chooseRandom(List<T> possibilities) {
        return possibilities.get(RandomUtils.nextInt(0, possibilities.size()));
    }
    @SafeVarargs
    public static <T> T chooseRandom(T... possibilities) {
        return possibilities[RandomUtils.nextInt(0, possibilities.length)];
    }
}

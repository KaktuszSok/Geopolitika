package kaktusz.geopolitika.util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MiscUtils {

	public static <T> T chooseRandom(List<T> choices) {
		int idx = ThreadLocalRandom.current().nextInt(choices.size());
		return choices.get(idx);
	}
}

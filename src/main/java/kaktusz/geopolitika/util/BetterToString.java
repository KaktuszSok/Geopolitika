package kaktusz.geopolitika.util;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

public interface BetterToString {
	default String toStringInternal() {
		Map<String, Object> properties = new LinkedHashMap<>();
		modifyToString(properties);
		StringJoiner sj = new StringJoiner(",", this.getClass().getSimpleName() + "{", "}");
		properties.forEach((k, v) -> {
			sj.add(k + "=" + v);
		});
		return sj.toString();
	}

	void modifyToString(@Nonnull Map<String, Object> propertiesToDisplay);
}

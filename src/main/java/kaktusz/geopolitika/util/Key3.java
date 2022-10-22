package kaktusz.geopolitika.util;

import com.google.common.base.Objects;

public class Key3<A,B,C> {
	public final A a;
	public final B b;
	public final C c;

	public Key3 (A a, B b, C c) {
		this.a = a;
		this.b = b;
		this.c = c;
	}

	public boolean equals (Object obj) {
		if (obj instanceof Key3) {
			Key3<?,?,?> k = (Key3<?,?,?>)obj;
			return Objects.equal(a, k.a) && Objects.equal(b, k.b) && Objects.equal(c, k.c);
		}
		return false;
	}

	public int hashCode() {
		return Objects.hashCode(a,b,c);
	}

	@Override
	public String toString() {
		return "(" +
				"" + a +
				"," + b +
				"," + c +
				')';
	}
}

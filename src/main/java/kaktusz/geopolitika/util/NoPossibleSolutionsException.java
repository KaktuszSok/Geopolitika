package kaktusz.geopolitika.util;

/**
 * Exception for when an algorithm has been given an input with no valid solutions
 */
public class NoPossibleSolutionsException extends Exception {
	public NoPossibleSolutionsException(String message) {
		super(message);
	}
}

package com.kids.app.servent;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * This is an immutable class that holds all the information for a servent.
 *
 * @author bmilojkovic
 */
public record ServentInfo(
		String ipAddress,
		int id,
		int listenerPort,
		List<Integer> neighbors
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 5304170042791281555L;

	@Override
	public String toString() {
		return "[" + id + "|" + ipAddress + "|" + listenerPort + "]";
	}
}

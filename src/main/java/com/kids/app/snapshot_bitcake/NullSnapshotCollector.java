package com.kids.app.snapshot_bitcake;

import com.kids.app.snapshot_bitcake.acharya_badrinath.ABSnapshot;

import java.util.Map;

/**
 * This class is used if the user hasn't specified a snapshot type in config.
 * 
 * @author bmilojkovic
 *
 */
public class NullSnapshotCollector implements SnapshotCollector {

	@Override
	public void run() {}

	@Override
	public void stop() {}

	@Override
	public BitcakeManager getBitcakeManager() {
		return null;
	}

	@Override
	public Map<String, ABSnapshot> getCollectedABValues() {
		return Map.of();
	}

	@Override
	public void startCollecting() {}

}

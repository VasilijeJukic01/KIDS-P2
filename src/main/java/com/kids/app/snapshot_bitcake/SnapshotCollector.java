package com.kids.app.snapshot_bitcake;

import com.kids.app.Cancellable;
import com.kids.app.snapshot_bitcake.acharya_badrinath.ABSnapshot;

import java.util.Map;

/**
 * Describes a snapshot collector. Made not-so-flexibly for readability.
 * 
 * @author bmilojkovic
 *
 */
public interface SnapshotCollector extends Runnable, Cancellable {
	BitcakeManager getBitcakeManager();
	Map<String, ABSnapshot> getCollectedABValues();
	void startCollecting();
}
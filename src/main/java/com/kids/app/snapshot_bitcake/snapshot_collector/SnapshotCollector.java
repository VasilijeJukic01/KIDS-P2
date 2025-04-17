package com.kids.app.snapshot_bitcake.snapshot_collector;

import com.kids.app.Cancellable;
import com.kids.app.snapshot_bitcake.BitcakeManager;

/**
 * Describes a snapshot collector. Made not-so-flexibly for readability.
 * 
 * @author bmilojkovic
 *
 */
public interface SnapshotCollector extends Runnable, Cancellable {
	BitcakeManager getBitcakeManager();
	void startCollecting();
}
package com.kids.app.snapshot_bitcake;

import com.kids.app.Cancellable;

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
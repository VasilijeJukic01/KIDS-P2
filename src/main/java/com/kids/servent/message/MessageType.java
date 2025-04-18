package com.kids.servent.message;

/**
 * Enum representing different types of messages in the system.
 * Each message type corresponds to a specific action or event.
 */
public enum MessageType {
	POISON,
	TRANSACTION,
	AB_SNAPSHOT_REQUEST,
	AB_SNAPSHOT_RESPONSE,
	AV_MARKER,
	AV_DONE,
	AV_TERMINATE,
	CC_SNAPSHOT_REQUEST,
	CC_SNAPSHOT_RESPONSE,
	CC_RESUME
}

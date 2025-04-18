package com.kids.cli.command;

import com.kids.app.AppConfig;
import com.kids.app.snapshot_bitcake.snapshot_collector.SnapshotCollector;
import com.kids.cli.CLIParser;
import com.kids.servent.SimpleServentListener;
import com.kids.servent.message.util.FifoSendWorker;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class StopCommand implements CLICommand {

	private final CLIParser parser;
	private final SimpleServentListener listener;
	private final SnapshotCollector snapshotCollector;
	private final List<FifoSendWorker> senderWorkers;
	
	@Override
	public String commandName() {
		return "stop";
	}

	@Override
	public void execute(String args) {
		AppConfig.timestampedStandardPrint("Stopping...");
		parser.stop();
		listener.stop();
		senderWorkers.forEach(FifoSendWorker::stop);
		snapshotCollector.stop();
	}

}

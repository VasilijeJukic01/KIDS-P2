package com.kids.cli.command;

import com.kids.app.snapshot_bitcake.snapshot_collector.SnapshotCollector;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BitcakeInfoCommand implements CLICommand {

	private final SnapshotCollector collector;
	
	@Override
	public String commandName() {
		return "bitcake_info";
	}

	@Override
	public void execute(String args) {
		collector.startCollecting();

	}

}

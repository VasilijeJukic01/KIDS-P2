package com.kids.cli.command;

import com.kids.app.AppConfig;

public class InfoCommand implements CLICommand {

	@Override
	public String commandName() {
		return "info";
	}

	@Override
	public void execute(String args) {
		AppConfig.timestampedStandardPrint("My info: " + AppConfig.myServentInfo);
		AppConfig.timestampedStandardPrint("Neighbors:");
		StringBuilder neighbors = new StringBuilder();

		for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
			neighbors.append(neighbor).append(" ");
		}
		
		AppConfig.timestampedStandardPrint(neighbors.toString());
	}

}

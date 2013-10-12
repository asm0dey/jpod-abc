package com.github.asm0dey.m4bconverter;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.NullOutputStream;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: finkel
 * 
 * Date: 11.10.13
 * 
 * Time: 19:26
 */
public class LameMp3ToWavDecoder implements IMp3ToWavDecoder {
	@Override
	public void decode(File input, File output) {
		CommandLine line = CommandLine.parse("/usr/bin/lame --decode ${if} ${of}");
		Map<String, String> map = new HashMap<>();
		map.put("if", FilenameUtils.getName(input.getAbsolutePath()));
		map.put("of", FilenameUtils.getName(output.getAbsolutePath()));
		line.setSubstitutionMap(map);
		DefaultExecutor executor = new DefaultExecutor();
		executor.setWorkingDirectory(input.getParentFile());
		executor.setStreamHandler(new PumpStreamHandler(new NullOutputStream()));
		DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
		try {
			executor.execute(line, handler);
			handler.waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}

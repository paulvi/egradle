package de.jcup.egradle.eclipse.execution;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;

import de.jcup.egradle.core.domain.GradleContext;
import de.jcup.egradle.core.process.ProcessOutputHandler;
import de.jcup.egradle.core.process.SimpleProcessExecutor;
import de.jcup.egradle.eclipse.launch.EGradleRuntimeProcess;

public class EclipseLaunchProcessExecutor extends SimpleProcessExecutor {
	private ILaunch launch;

	public EclipseLaunchProcessExecutor(ProcessOutputHandler streamHandler, ILaunch launch) {
		super(streamHandler);
		this.launch=launch;
	}

	@Override
	protected void handleProcessStarted(GradleContext context, Process process, Date started, File workingDirectory,
			String[] commands) {
		String label = context.getCommandString();
		String path = "inside root project";

		Map<String, String> attributes = new HashMap<>();
		String timestamp = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(started);
		/*
		 * Will be shown in process information dialog - see
		 * org.eclipse.debug.internal.ui.preferences. ProcessPropertyPage
		 */
		attributes.put(DebugPlugin.ATTR_ENVIRONMENT, context.getEnvironment().toString());
		attributes.put(DebugPlugin.ATTR_CONSOLE_ENCODING, "UTF-8");
		attributes.put(DebugPlugin.ATTR_WORKING_DIRECTORY, workingDirectory.getAbsolutePath());
		attributes.put(DebugPlugin.ATTR_LAUNCH_TIMESTAMP, timestamp);
		attributes.put(DebugPlugin.ATTR_PATH, path);

		/*
		 * using an unbreakable space 00A0 to avoid unnecessary breaks in view
		 */
		String cmdLine = StringUtils.join(Arrays.asList(commands), '\u00A0');

		attributes.put(IProcess.ATTR_CMDLINE, cmdLine);
		/*
		 * bind process to runtime process, so visible and correct handled in
		 * debug UI
		 */
		EGradleRuntimeProcess rp = new EGradleRuntimeProcess(launch, process, label, attributes);
		// rp.getStreamsProxy().getOutputStreamMonitor().addListener(rp);

		handler.output("Launch started - for details see output of " + label);
		if (!rp.canTerminate()) {
			handler.output("Started process cannot terminate");
		}
	}
	
	@Override
	protected void handleOutputStreams(Process p) throws IOException {
		/*
		 * do nothing - is printed to console output on current launcher
		 */
	}

}
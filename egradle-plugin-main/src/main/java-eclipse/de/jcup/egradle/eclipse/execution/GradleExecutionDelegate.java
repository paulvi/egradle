/*
 * Copyright 2016 Albert Tregnaghi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 */
package de.jcup.egradle.eclipse.execution;

import static de.jcup.egradle.eclipse.preferences.EGradlePreferences.PREFERENCES;
import static org.apache.commons.lang3.Validate.notNull;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;

import de.jcup.egradle.core.GradleExecutor;
import de.jcup.egradle.core.GradleExecutor.Result;
import de.jcup.egradle.core.api.ForgetMeRuntimeException;
import de.jcup.egradle.core.api.GradleContextPreparator;
import de.jcup.egradle.core.config.MutableGradleConfiguration;
import de.jcup.egradle.core.domain.GradleContext;
import de.jcup.egradle.core.domain.GradleRootProject;
import de.jcup.egradle.core.process.ProcessExecutor;
import de.jcup.egradle.core.process.EGradleShellType;
import de.jcup.egradle.core.process.OutputHandler;
import de.jcup.egradle.eclipse.EGradleMessageDialog;
import de.jcup.egradle.eclipse.api.EGradleUtil;
import de.jcup.egradle.eclipse.preferences.EGradlePreferences;
import de.jcup.egradle.eclipse.preferences.PreferenceConstants;

/**
 * Execution delegate, used by {@link GradleJob} and
 * {@link GradleRunnableWithProgress}
 * 
 * @author Albert Tregnaghi
 *
 */
public class GradleExecutionDelegate {

	private GradleContext context;
	private OutputHandler systemConsoleOutputHandler;
	private Result result;
	protected GradleExecutor executor;

	public Result getResult() {
		return result;
	}

	public GradleExecutionDelegate(OutputHandler outputHandler, ProcessExecutor processExecutor,
			GradleContextPreparator additionalContextPreparator) {
		notNull(outputHandler, "'systemConsoleOutputHandler' may not be null");
		notNull(processExecutor, "'processExecutor' may not be null");
		this.systemConsoleOutputHandler = outputHandler;

		context = createContext();
		if (additionalContextPreparator!=null){
			additionalContextPreparator.prepare(context);
		}
		executor = new GradleExecutor(processExecutor);
	}

	private GradleContext createContext() {
		GradleRootProject rootProject = EGradleUtil.getRootProject();
		/* build configuration for gradle run */
		MutableGradleConfiguration config = new MutableGradleConfiguration();
		/* build context */
		GradleContext context = new GradleContext(rootProject, config);
		EGradlePreferences preferences = PREFERENCES;
		/* Default JAVA_HOME */
		String globalJavaHome = preferences.getStringPreference(PreferenceConstants.P_JAVA_HOME_PATH);
		if (!StringUtils.isEmpty(globalJavaHome)) {
			config.setGradleCommand(globalJavaHome); // its an config value so we set it to config too. 
			context.setEnvironment("JAVA_HOME", globalJavaHome); // JAVA_HOME still can be overriden by context preparator see below
		}
		context.setAmountOfWorkToDo(1);
		
		/* Call gradle settings */
		String gradleCommand = preferences.getStringPreference(PreferenceConstants.P_GRADLE_CALL_COMMAND);
		String gradleInstallPath = preferences.getStringPreference(PreferenceConstants.P_GRADLE_INSTALL_BIN_FOLDER);
		
		String shellId = preferences.getStringPreference(PreferenceConstants.P_GRADLE_SHELL);
		
		if (StringUtils.isEmpty(gradleCommand)){
			EGradleMessageDialog.INSTANCE.showError("Preferences have no gradle command set, cannot execute!");
			throw new ForgetMeRuntimeException("Illegal preference store, already shown to user");
		}
		
		config.setShellCommand(EGradleShellType.findById(shellId));
		config.setGradleBinDirectory(gradleInstallPath);
		config.setGradleCommand(gradleCommand);
		config.setWorkingDirectory(rootProject.getFolder().getAbsolutePath());
		return context;
	}

	/**
	 * Execute and give output by given progress monitor
	 * 
	 * @param monitor
	 *            - progress monitor
	 * @throws Exception
	 */
	public void execute(IProgressMonitor monitor) throws Exception {

		GradleRootProject rootProject = context.getRootProject();
		String commandString = context.getCommandString();
		String progressDescription = "Executing gradle commands:" + commandString + " in "
				+ context.getRootProject().getFolder().getAbsolutePath();

		File folder = rootProject.getFolder();
		String rootProjectFolderName = folder.getName();
		String executionStartTime = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
				.format(new Date());

		monitor.beginTask(progressDescription, context.getAmountOfWorkToDo());
		beforeExecutionDone(monitor);

		systemConsoleOutputHandler.output("\n" + executionStartTime + " " + progressDescription);
		systemConsoleOutputHandler.output("Root project '" + rootProjectFolderName + "' executing " + commandString);

		result = executor.execute(context);
		if (result.isOkay()) {
			systemConsoleOutputHandler.output("[OK]");
			return;
		} else {
			systemConsoleOutputHandler.output("[FAILED]");
		}
		try {
			afterExecutionDone(monitor);
		} catch (Exception e) {
			throw new InvocationTargetException(e);
		}finally{
			monitor.done();
		}

	}

	protected void beforeExecutionDone(IProgressMonitor monitor) throws Exception {
		/* per default do nothing */
	}

	protected void afterExecutionDone(IProgressMonitor monitor) throws Exception {
		/* per default do nothing */
	}


}
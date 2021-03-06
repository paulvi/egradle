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
 package de.jcup.egradle.eclipse.preferences;

public enum EGradlePreferenceConstants {
	P_ROOTPROJECT_PATH("pathGradleRootProject"), 
	
	P_JAVA_HOME_PATH("pathJavaHome"),
	
	P_GRADLE_CALL_TYPE("gradleCallType"),
	P_GRADLE_SHELL("commandShell"),
	P_GRADLE_INSTALL_BIN_FOLDER("pathGradleInstallation"),
	P_GRADLE_CALL_COMMAND("commandGradle"),
	P_OUTPUT_VALIDATION_ENABLED("validatEnabled"),
	P_DECORATION_SUBPROJECTS_WITH_ICON_ENABLED("validatEnabled");

	private String id;

	private EGradlePreferenceConstants(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

}
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
 package de.jcup.egradle.eclipse.launch;

import de.jcup.egradle.eclipse.Activator;

public interface EGradleLauncherConstants {

	public static final String LAUNCH_ARGUMENT = "createRuntimeProcess";
	
	public static final String LAUNCH_POST_JOB = "launch_post_job";
	/**
	 * When a attribute for the key is set it overrides the normal attribute 
	 */
	public static final String LAUNCH_TASKS_ATTRBUTE_OVERRIDE ="launch_tasks_attribute_override";
	
	public static final String PROPERTY_TASKS = "tasks";
	public static final String PROPERTY_PROJECTNAME = "projectName";
	public static final String PROPERTY_OPTIONS = "options";
	
	
	public static final String GRADLE_PROPERTIES = Activator.PLUGIN_ID+".gradleProperties";
	public static final String SYSTEM_PROPERTIES = Activator.PLUGIN_ID+".systemProperties";
	public static final String ENVIRONMENT_PROPERTIES = Activator.PLUGIN_ID+".environmentProperties";
}

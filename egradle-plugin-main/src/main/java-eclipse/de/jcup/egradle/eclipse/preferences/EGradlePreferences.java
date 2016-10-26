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

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import de.jcup.egradle.eclipse.Activator;

public class EGradlePreferences {
	

	public static EGradlePreferences PREFERENCES = new EGradlePreferences();
	private IPreferenceStore store;

	EGradlePreferences() {
		store = new ScopedPreferenceStore(InstanceScope.INSTANCE, Activator.PLUGIN_ID);
	}

	public String getStringPreference(EGradlePreferenceConstants id) {
		String data = getPreferenceStore().getString(id.getId());
		if (data==null){
			data="";
		}
		return data;
	}
	
	public IPreferenceStore getPreferenceStore() {
		return store;
	}

	public boolean isValidationEnabled() {
		boolean validationEnabled = getPreferenceStore().getBoolean(EGradlePreferenceConstants.P_VALIDATION_ENABLED.getId());
		return validationEnabled;
	}
	
	public boolean isSubProjectIconDecorationEnabled() {
		boolean validationEnabled = getPreferenceStore().getBoolean(EGradlePreferenceConstants.P_DECORATION_SUBPROJECTS_WITH_ICON_ENABLED.getId());
		return validationEnabled;
	}

	public String getGlobalJavaHomePath() {
		return getStringPreference(EGradlePreferenceConstants.P_JAVA_HOME_PATH);
	}

	public String getGradleCallCommand() {
		return getStringPreference(EGradlePreferenceConstants.P_GRADLE_CALL_COMMAND);
	}

	public String getGradleBinInstallFolder() {
		return getStringPreference(EGradlePreferenceConstants.P_GRADLE_INSTALL_BIN_FOLDER);
	}

	public String getGradleShellId() {
		return getStringPreference(EGradlePreferenceConstants.P_GRADLE_SHELL);
	}

	public String getRootProjectPath() {
		return getStringPreference(EGradlePreferenceConstants.P_ROOTPROJECT_PATH);
	}
	
}

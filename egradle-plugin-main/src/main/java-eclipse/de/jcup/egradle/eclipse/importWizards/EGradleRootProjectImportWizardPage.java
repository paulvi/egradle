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
package de.jcup.egradle.eclipse.importWizards;

import static de.jcup.egradle.eclipse.api.EGradleUtil.*;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import de.jcup.egradle.core.process.EGradleShellType;
import de.jcup.egradle.eclipse.execution.validation.ExecutionConfigDelegateAdapter;
import de.jcup.egradle.eclipse.ui.ExecutionConfigComposite;

public class EGradleRootProjectImportWizardPage extends WizardPage {

	private ExecutionConfigComposite configComposite;

	public EGradleRootProjectImportWizardPage(String pageName) {
		super(pageName);
		setTitle("Import gradle projects"); // NON-NLS-1
		setDescription("Import a gradle root project with all subprojects from given root folder"); // NON-NLS-1
		configComposite = new ExecutionConfigComposite(new InternalExecutionDelegte());
		
	}
	
	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
			
		Composite folderSelectionArea = new Composite(parent, SWT.NONE);
		GridLayout folderSelectionLayout = new GridLayout();
		folderSelectionLayout.numColumns = 3;
		folderSelectionLayout.makeColumnsEqualWidth = false;
		folderSelectionLayout.marginWidth = 0;
		folderSelectionLayout.marginHeight = 0;
		folderSelectionArea.setLayout(folderSelectionLayout);

		configComposite.createFieldEditors(folderSelectionArea);
		
		/* adopt import setting from current existing preferences value*/
		String globalJavaHomePath = getPreferences().getGlobalJavaHomePath();
		String gradleBinInstallFolder = getPreferences().getGradleBinInstallFolder();
		String gradleCallCommand = getPreferences().getGradleCallCommand();
		String gradleCallTypeID = getPreferences().getGradleCallTypeID();
		String shellId = getPreferences().getGradleShellId();
		String rootProjectPath = getPreferences().getRootProjectPath();
		
		configComposite.setGradleBinInstallFolder(gradleBinInstallFolder);
		configComposite.setGradleCallTypeId(gradleCallTypeID);
		configComposite.setGlobalJavaHomePath(globalJavaHomePath);
		configComposite.setRootProjectPath(rootProjectPath);
		configComposite.setGradleCallCommand(gradleCallCommand);
		configComposite.setShellId(shellId);

		setControl(folderSelectionArea);
	}
	
	public String getGlobalJavaHomePath(){
		return configComposite.getGlobalJavaHomePath();
	}
	
	public String getGradleRootProjectPath(){
		return  configComposite.getGradleRootPathText();
	}
	
	public EGradleShellType getShellCommand(){
		return configComposite.getShellCommand();
	}

	IPath getSelectedPath() {
		String text = getGradleRootProjectPath();
		boolean empty = StringUtils.isEmpty(text);
		setPageComplete(!empty);// FIXME ATR, 28.10.2016 - needs to be integrated in listener to enable finish button correct...
		if (empty){
			return null;
		}else{
			return new Path(text);
		}
	}

	private class InternalExecutionDelegte extends ExecutionConfigDelegateAdapter{
		
	}

	public String getGradleBinDirectory() {
		return configComposite.getGradleBinDirectory();
	}

	public String getGradleCommand() {
		return configComposite.getGradleCommand();
	}

	public String getCallTypeId() {
		return configComposite.getCallTypeId();
	}
}

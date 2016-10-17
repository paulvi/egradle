package de.jcup.egradle.eclipse.gradleeditor.document.keywords;

public enum GradleSpecialVariableKeyWords implements DocumentKeyWord {

	LOGGER("logger"),
	
	TASKS("tasks"),
	
	PROJECT("project"),
	
	OPTIONS("options"),
	
	SOURCESETS("sourceSets"),

	ROOTPROJECT("rootProject"),
	
	PROJECTDIR("projectDir"),
	
	JAVAVERSION("JavaVersion"),
	
	TYPE("type:"),
	;

	private String text;

	private GradleSpecialVariableKeyWords(String text) {
		this.text = text;
	}


	@Override
	public String getText() {
		return text;
	}
}

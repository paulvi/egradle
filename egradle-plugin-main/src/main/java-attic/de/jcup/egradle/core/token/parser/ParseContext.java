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
 package de.jcup.egradle.core.token.parser;

import java.util.ArrayList;
import java.util.List;

import de.jcup.egradle.core.token.Token;
import de.jcup.egradle.core.token.TokenImpl;
import de.jcup.egradle.core.token.TokenType;

class ParseContext {
	private char[] lineChars;
	private int pos;
	private int offset;
	private int lineNumber;
	private StringBuilder nextClosingTokenText = new StringBuilder();

	private List<String> lines = new ArrayList<>();
	private TokenParserResult result;
	private int tokencounter;
	private boolean inSingleComment;
	private boolean inMultiLineComment;
	private TokenImpl rootToken;
	private TokenImpl activeParent;
	private TokenImpl activeToken;
	private TokenImpl lastToken;
	private boolean initializationDone;
	private boolean inNormalString;
	private boolean inGString;

	public TokenImpl getActiveParent() {
		if (activeParent==null){
			activeParent=rootToken;
		}
		return activeParent;
	}

	public void setActiveParent(TokenImpl activeParent) {
		this.activeParent = activeParent;
	}

	public void setActiveToken(TokenImpl activeToken) {
		this.activeToken = activeToken;
	}

	public TokenImpl getActiveToken() {
		return activeToken;
	}

	public void setLastToken(TokenImpl lastToken) {
		this.lastToken = lastToken;
	}

	public TokenImpl getLastToken() {
		return lastToken;
	}

	public ParseContext() {
		this.rootToken = new TokenImpl(createNewTokenId());
		rootToken.setType(TokenType.ROOT);

		activeParent = rootToken;

		result = new TokenParserResult(rootToken);
	}

	public void setInSingleComment(boolean inSingleComment) {
		this.inSingleComment = inSingleComment;
	}

	public boolean isInComment() {
		return isInSingleComment() || isInMultiLineComment();
	}

	public boolean isInSingleComment() {
		return inSingleComment;
	}

	public void setInMultiLineComment(boolean inMultiLineComment) {
		this.inMultiLineComment = inMultiLineComment;
	}

	public boolean isInMultiLineComment() {
		return inMultiLineComment;
	}

	/**
	 * Create a unique ID in current parse context
	 * 
	 * @return unique identifier
	 */
	public int createNewTokenId() {
		return tokencounter++;
	}
	
	void addLine(String line) {
		lines.add(line);
	}

	TokenParserResult getResult() {
		return result;
	}

	public int getOffset() {
		return offset;
	}

	public char[] getLineChars() {
		return lineChars;
	}

	void setLineChars(char[] lineChars) {
		this.lineChars = lineChars;
	}

	void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public boolean hasNextChar() {
		int nextElement = pos + 1;
		return nextElement < lineChars.length;
	}

	/**
	 * @return next char without incrementing pos or offset
	 */
	public char getNextChar() {
		int nextElement = pos + 1;
		return lineChars[nextElement];
	}

	void goNextChar() {
		offset++;
		pos++;
	}

	public int getPos() {
		return pos;
	}

	public void resetPos() {
		pos = 0;
	}

	/**
	 * Increments pos and offset
	 */
	public void incPosAndOffset() {
		pos++;
		offset++;
	}

	public boolean canFetchNextLineCharAtPos() {
		if (lineChars == null) {
			return false;
		}
		if (lineChars.length <= pos) {
			return false;
		}
		return true;
	}

	/**
	 * Get line char at current pos - will throw {@link ArrayIndexOutOfBoundsException} when wrong used.
	 * Use {@link #canFetchNextLineCharAtPos()} before!
	 * @return char at current pos
	 */
	public char getLineCharAtPos() {
		return lineChars[pos];
	}

	public List<String> getLines() {
		return lines;
	}

	public void dispose() {
		lines = null;
		lineChars = null;
		result = null;
		rootToken = null;
	}

	public String getCurrentTextString() {
		if (nextClosingTokenText == null) {
			return null;
		}
		return nextClosingTokenText.toString();
	}

	/**
	 * Resets next closing token text
	 */
	public void resetNextClosingTokenText() {
		nextClosingTokenText = new StringBuilder();
	}

	public void appendNextClosingText(char c) {
		nextClosingTokenText.append(c);
	}

	public String toString() {
		
		StringBuilder content = new StringBuilder();
		for (String line: lines){
			content.append(line);
			content.append("\n");
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("ParseContext:");
		sb.append("\nline :" + getLines().get(getLineNumber()));
		sb.append("\n  pos:" + buildPointerString() + "(" + pos + ", char='"+getSafeLineCharAtPosString()+"')");
		String charOffsetString =  null;
		if (content.length()<offset){
			charOffsetString= ""+content.charAt(offset);
		}
		sb.append("\n offs:" + offset+", char='"+charOffsetString+"'");
		sb.append("\ncurrentText:'" + getCurrentTextString()+"'");
		sb.append("\nactiveParent:" + createTokenString(activeParent));
		sb.append("\nlastToken:" + createTokenString(lastToken));
		sb.append("\nactiveToken:" + createTokenString(activeToken));
		sb.append("\n");
		return sb.toString();
	}

	String getSafeLineCharAtPosString() {
		String charAtPos = null;
		if (canFetchNextLineCharAtPos()){
			charAtPos=""+getLineCharAtPos();
		}
		return charAtPos;
	}

	private String buildPointerString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < getPos(); i++) {
			sb.append('-');
		}
		sb.append('^');
		String pointer = sb.toString();
		return pointer;
	}

	public String createTokenString(TokenImpl tokenImpl) {
		if (tokenImpl == null) {
			return "No TokenImpl";
		}
		return tokenImpl.toIdString();
	}

	public void addProblem(String problem) {
		result.addProblem(problem, getOffset());
	}

	public void setInNormalString(boolean inString) {
		this.inNormalString = inString;
	}

	public void setInGString(boolean inString) {
		this.inGString = inString;
	}

	public boolean isInGString() {
		return inGString;
	}

	public boolean isInNormalString() {
		return inNormalString;
	}

	public boolean isInString() {
		return inGString || inNormalString;
	}

	public Token getRootToken() {
		return rootToken;
	}

	public void markInitializationDone() {
		initializationDone = true;
	}

	public boolean isInitializationDone() {
		return initializationDone;
	}

	public void appendAllRemainingTextOfLineAndIncPos() {
		for (int i = getPos(); i < lineChars.length; i++) {
			char lineCharAtPos = getLineCharAtPos();
			appendNextClosingText(lineCharAtPos);
			incPosAndOffset();
		}

	}

	public void decPosAndOffset() {
		pos--;
		offset--;
	}

}
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
 package de.jcup.egradle.core.model.groovyantlr;

import static org.codehaus.groovy.antlr.parser.GroovyTokenTypes.*;

import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.antlr.GroovySourceAST;
import org.codehaus.groovy.antlr.UnicodeEscapingReader;
import org.codehaus.groovy.antlr.parser.GroovyLexer;
import org.codehaus.groovy.antlr.parser.GroovyRecognizer;
import org.codehaus.groovy.antlr.parser.GroovyTokenTypes;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.collections.AST;
import de.jcup.egradle.core.api.Filter;
import de.jcup.egradle.core.model.BuildContext;
import de.jcup.egradle.core.model.Error;
import de.jcup.egradle.core.model.Item;
import de.jcup.egradle.core.model.ItemFilter;
import de.jcup.egradle.core.model.ItemType;
import de.jcup.egradle.core.model.Model;
import de.jcup.egradle.core.model.ModelBuilder;
import de.jcup.egradle.core.model.ModelImpl;
import de.jcup.egradle.core.model.Modifier;

/**
 * Builds a outline model containing gradle specific outline items
 * 
 * @author Albert Tregnaghi
 *
 */
public class GradleModelBuilder implements ModelBuilder {
	private InputStream is;

	private ItemFilter postCreationFilter;
	private Filter preCreationFilter;
	
	public GradleModelBuilder(InputStream is) {
		this.is = is;
	}
	
	/**
	 * Set pre creation filter to filter AST parts not wanted to be source for building items (can be used to speed up)
	 * @param preCreationFilter
	 */
	public void setPreCreationFilter(Filter preCreationFilter) {
		this.preCreationFilter = preCreationFilter;
	}

	/**
	 * Set a post creation filter - is used when adding created items to model. If it filters the
	 * created item, the item (and all children) will not be listed inside
	 * model. At postCreationFilter time the items have already the correct gradle item type
	 * 
	 * @param postCreationFilter
	 */
	public void setPostCreationFilter(ItemFilter postCreationFilter) {
		this.postCreationFilter = postCreationFilter;
	}

	public ItemFilter getPostCreationFilter() {
		if (postCreationFilter == null) {
			postCreationFilter = ItemFilter.NO_ITEMS_FILTERED;
		}
		return postCreationFilter;
	}

	@Override
	public Model build(BuildContext builderContext) throws ModelBuilderException {
		ModelImpl model = new ModelImpl();
		if (is == null) {
			return model;
		}
		InputStreamReader reader = new InputStreamReader(is);
		ExtendedSourceBuffer sourceBuffer = new ExtendedSourceBuffer();
		UnicodeEscapingReader r2 = new UnicodeEscapingReader(reader, sourceBuffer);
		GroovyLexer lexer = new GroovyLexer(r2);
		r2.setLexer(lexer);

		GroovyRecognizer parser = GroovyRecognizer.make(lexer);
		parser.setSourceBuffer(sourceBuffer);
		try {
			parser.compilationUnit();
			AST first = parser.getAST();

			Context context = new Context();
			context.buffer = sourceBuffer;

			Item rootItem = model.getRoot();
			walkThroughASTandSiblings(context, rootItem, first);

		} catch (RecognitionException | TokenStreamException e) {

			RecognitionException re = RecognitionExceptionResolver.getSharedInstance().resolveRecognitionException(e);
			Error error = new Error();
			if (re == null) {
				error.setLineNumber(1); // fall back, always use line 1 when not
										// clear..
				error.setMessage(e.getMessage());
			} else {
				int line = re.getLine();
				int column = re.getColumn();
				int offset = sourceBuffer.getOffset(line, column);
				error.setLineNumber(line);
				error.setMessage(re.getMessage());
				error.setCharStart(offset);
				error.setCharEnd(offset + 1);

			}
			if (builderContext != null) {
				builderContext.add(error);
			}
		} catch (RuntimeException e) {
			throw new ModelBuilderException("Cannot build outline model because of AST parsing problems", e);
		}

		return model;
	}

	protected void walkThroughASTandSiblings(Context context, Item parent, AST current) throws ModelBuilderException {
		while (current != null) {
			Item item = buildItem(context, parent, current);
			if (item != null) {
				if (!getPostCreationFilter().isFiltered(item)) {
					parent.add(item);
				}
			}
			current = current.getNextSibling();
		}
	}

	public Filter getPreCreationFilter() {
		if (preCreationFilter==null){
			preCreationFilter=Filter.NOTHING_FILTERED;
		}
		return preCreationFilter;
	}
	
	/**
	 * Builds new item or <code>null</code>
	 * 
	 * @param context
	 * @param parent
	 * 
	 * @param current
	 * @return new item or <code>null</code>
	 * @throws ModelBuilderException
	 */
	protected Item buildItem(Context context, Item parent, AST current) throws ModelBuilderException {
		if (getPreCreationFilter().isFiltered(current)){
			return null;
		}
		Item item;
		switch (current.getType()) {
		case CLASS_DEF:
			item = buildClass(context, current);
			break;
		case EXPR:
			item = buildExpression(context, parent, current);
			break;
		case VARIABLE_DEF:
			item = buildVariableDef(context, current);
			break;
		case PACKAGE_DEF:
			item = buildPackageDef(context, current);
			break;
		case IMPORT:
			item = buildImport(context, current);
			break;
		default:
			item = null;
		}
		if (item != null) {
			item.setClosed(true);
		}

		return item;

	}

	protected Item buildImport(Context context, AST current) {
		Item item = null;

		AST modifiers = current.getFirstChild();
		if (modifiers == null) {
			return null;
		}
		AST packageName = modifiers.getNextSibling();
		if (packageName == null) {
			return null;
		}
		item = createItem(context, current);
		String name = resolveAsSimpleString(packageName);
		item.setName(name);
		item.setItemType(ItemType.IMPORT);
		return item;
	}

	protected Item buildPackageDef(Context context, AST current) {
		Item item = null;

		AST modifiers = current.getFirstChild();
		if (modifiers == null) {
			return null;
		}
		AST packageName = modifiers.getNextSibling();
		if (packageName == null) {
			return null;
		}
		item = createItem(context, current);
		String name = resolveAsSimpleString(packageName);
		item.setName(name);
		item.setItemType(ItemType.PACKAGE);
		return item;
	}

	protected Item buildVariableDef(Context context, AST current) throws ModelBuilderException {
		/* def variable = "" */
		/* variable = "" */
		/* def String variable = "" */
		/* String variable = "" <-- no modifiers! */
		Item item = null;

		AST modifiers = null;
		AST type = null;
		AST first = current.getFirstChild();
		if (first== null) {
			return null;
		}
		int firstType = first.getType();
		if (GroovyTokenTypes.TYPE==firstType){
			type=first;
		}else if (GroovyTokenTypes.MODIFIERS==firstType){
			modifiers=first;
		}else{
			return null;
		}
		if (type==null){
			type = modifiers.getNextSibling();
			if (type == null) {
				return null;
			}
			
		}
		/* type */
		String typeDefText = null;
		AST typeDef = type.getFirstChild();
		if (typeDef != null) {
			typeDefText = typeDef.getText();
		}
		AST name = type.getNextSibling();
		String nameString;
		if (name != null) {
			nameString = name.getText();
		} else {
			nameString = "<unknown>";
		}
		item = createItem(context, current);
		item.setName(nameString);
		appendModifiers(item, modifiers);
		item.setType(typeDefText);
		item.setItemType(ItemType.VARIABLE);
		return item;
	}

	protected Item buildClass(Context context, AST current) throws ModelBuilderException {
		Item item = null;
		AST classDefModifiers = current.getFirstChild();
		if (classDefModifiers == null) {
			return null;
		}
		AST classDefName = classDefModifiers.getNextSibling();
		if (classDefName == null) {
			return null;
		}
		item = createItem(context, current);
		item.setItemType(ItemType.CLASS);
		item.setName(classDefName.getText());
		appendModifiers(item, classDefModifiers);
		return item;
	}

	protected Item buildExpression(Context context, Item parent, AST current) throws ModelBuilderException {
		AST next = current.getFirstChild();
		if (next == null) {
			return null;
		}
		if (GroovyTokenTypes.DOT == next.getType()) {
			next = next.getFirstChild();
			if (next == null) {
				return null;
			}
		}
		if (GroovyTokenTypes.METHOD_CALL != next.getType()) {
			return null;
		}
		AST methodCall = current.getFirstChild();
		if (methodCall == null) {
			return null;
		}
		if (GroovyTokenTypes.DOT == methodCall.getType()) {
			methodCall = methodCall.getFirstChild();
			if (methodCall == null) {
				return null;
			}
		}
		AST methodChild = methodCall.getFirstChild();
		if (methodChild == null) {
			return null;
		}
		AST ename = methodCall.getFirstChild();
		if (ename == null) {
			return null;
		}
		AST astForName = methodChild;
		String enameString = resolveName(astForName);
		if (enameString == null) {
			return null;
		}

		ItemType outlineType = null;
		if (methodCall.getType() == METHOD_CALL) {

			if (parent != null) {
				if (ItemType.DEPENDENCIES == parent.getItemType()) {
					outlineType = ItemType.DEPENDENCY;
				} else if (ItemType.REPOSITORIES == parent.getItemType()) {
					outlineType = ItemType.REPOSITORY;
				}
			}
			if (outlineType == null) {
				outlineType = ItemType.METHOD_CALL;
			}
		} else {
			return null;
		}
		Item item = createItem(context, current);
		item.setItemType(outlineType);
		item.setName(enameString);
		item.setClosed(true);

		AST lastAst = ename.getNextSibling();
		if ("task".equals(enameString) || enameString.startsWith("task ")) {
			item.setItemType(ItemType.TASK);
			lastAst = handleTaskClosure(enameString, item, lastAst);
		} else if (enameString.startsWith("tasks.")) {
			item.setItemType(ItemType.TASKS);
		} else if (enameString.equals("apply") || enameString.startsWith("apply ")) {
			item.setItemType(ItemType.APPLY_SETUP);
			handleApplyType(item, lastAst);
		}
		if (outlineType == ItemType.DEPENDENCY) {
			return handleDependencyAndReturnItem(methodCall, item);
		}
		if (outlineType == ItemType.REPOSITORY) {
			return item;
		}
		if (lastAst != null) {
			if (GroovyTokenTypes.CLOSABLE_BLOCK == lastAst.getType()) {
				if (item.getItemType() == ItemType.TASK) {
					item.setItemType(ItemType.TASK_CLOSURE);
				} else {
					String name = item.getName();
					if ("repositories".equals(name)) {
						item.setItemType(ItemType.REPOSITORIES);
					} else if ("allprojects".equals(name)) {
						item.setItemType(ItemType.ALL_PROJECTS);
					} else if ("subprojects".equals(name)) {
						item.setItemType(ItemType.SUB_PROJECTS);
					} else if ("dependencies".equals(name)) {
						item.setItemType(ItemType.DEPENDENCIES);
					} else if ("sourceSets".equals(name)) {
						item.setItemType(ItemType.SOURCESETS);
					} else if ("main".equals(name)) {
						item.setItemType(ItemType.MAIN);
					} else if ("jar".equals(name)) {
						item.setItemType(ItemType.JAR);
					} else if ("test".equals(name)) {
						item.setItemType(ItemType.TEST);
					} else if ("clean".equals(name)) {
						item.setItemType(ItemType.CLEAN);
					} else if ("buildscript".equals(name)) {
						item.setItemType(ItemType.BUILDSCRIPT);
					} else if ("configurations".equals(name)) {
						item.setItemType(ItemType.CONFIGURATIONS);
					} else if ("doFirst".equals(name)) {
						item.setItemType(ItemType.DO_FIRST);
					} else if ("doLast".equals(name)) {
						item.setItemType(ItemType.DO_LAST);
					} else {
						item.setItemType(ItemType.CLOSURE);
					}
				}
				/* inspect children... */
				walkThroughASTandSiblings(context, item, lastAst.getFirstChild());
			}
		}

		return item;
	}

	private void appendModifiers(Item item, AST modifiers) throws ModelBuilderException {
		if (modifiers == null) {
			return;
		}
		if (modifiers.getType() != GroovyTokenTypes.MODIFIERS) {
			return;
		}
		AST modifierAst = modifiers.getFirstChild();
		if (modifierAst == null) {
			return;
		}
		/* currently just skip annotations at all */
		while (modifierAst != null) {
			if (modifierAst.getType() != GroovyTokenTypes.ANNOTATION) {
				break;
			}
			// AST annotations = modifierAst;
			modifierAst = modifierAst.getNextSibling();
		}
		if (modifierAst == null) {
			return;
		}
		String modifierString = modifierAst.getText();
		Modifier oModifier = Modifier.DEFAULT;
		if (StringUtils.isNotBlank(modifierString)) {
			if ("private".equals(modifierString)) {
				oModifier = Modifier.PRIVATE;
			} else if ("protected".equals(modifierString)) {
				oModifier = Modifier.PROTECTED;
			} else if ("public".equals(modifierString)) {
				oModifier = Modifier.PUBLIC;
			}
		}
		item.setModifier(oModifier);
	}

	private Item handleDependencyAndReturnItem(AST methodCall, Item item) {
		AST configuration = methodCall.getFirstChild();
		AST configurationParameter = null;

		if (configuration != null) {
			configurationParameter = configuration.getNextSibling();
		}
		if (configurationParameter == null) {
			return item;
		}
		String depencyName = resolveAsSimpleString(configurationParameter);
		item.setConfiguration(configuration.getText());
		item.setName(depencyName);
		return item;
	}

	private void handleApplyType(Item item, AST lastAst) {
		if (lastAst == null) {
			return;
		}
		if (GroovyTokenTypes.ELIST != lastAst.getType()) {
			return;
		}
		/* parameter -e.g. apply from/plugin 'bla' */
		AST elist = lastAst;

		AST applyKind = elist.getFirstChild();

		if (applyKind == null) {
			return;
		}
		if (GroovyTokenTypes.LABELED_ARG != applyKind.getType()) {
			return;
		}
		AST applyLabel = applyKind.getFirstChild();
		if (applyLabel == null) {
			return;
		}
		String typeStr = applyLabel.getText();
		if ("plugin".equals(typeStr)) {
			item.setItemType(ItemType.APPLY_PLUGIN);
			item.setName("apply plugin");
		} else if ("from".equals(typeStr)) {
			item.setItemType(ItemType.APPLY_FROM);
			item.setName("apply from");
		}
		AST applyTarget = applyLabel.getNextSibling();
		if (applyTarget == null) {
			return;
		}
		String target = resolveAsSimpleString(applyTarget);
		item.setTarget(target);
	}

	private String resolveAsSimpleString(AST ast) {
		if (ast == null) {
			return "";
		}
		int type = ast.getType();
		if (GroovyTokenTypes.STRING_LITERAL == type) {
			return ast.getText();
		} else if (GroovyTokenTypes.STRING_CONSTRUCTOR == type) {
			return resolveStringOfFirstChildAndSiblings(ast);
		} else if (GroovyTokenTypes.METHOD_CALL == type) {
			return "";
		} else {

			AST firstChild = ast.getFirstChild();
			if (GroovyTokenTypes.EXPR == type) {
				return resolveName(firstChild);
			} else if (GroovyTokenTypes.SL == type) {
				return resolveStringOfFirstChildAndSiblings(ast) + " <<";
			} else if (GroovyTokenTypes.CLOSABLE_BLOCK == type) {
				return "";
			} else if (GroovyTokenTypes.SLIST == type) {
				return resolveStringOfFirstChildAndSiblings(ast);
			} else if (GroovyTokenTypes.ELIST == type) {
				return resolveStringOfFirstChildAndSiblings(ast, ", ");
			} else if (GroovyTokenTypes.DOT == type) {
				if (firstChild == null) {
					return "";
				}
				StringBuilder sb = new StringBuilder();
				sb.append(resolveAsSimpleString(firstChild));
				sb.append('.');
				sb.append(resolveAsSimpleString(firstChild.getNextSibling()));
				return sb.toString();
			} else if (GroovyTokenTypes.LABELED_ARG == type) {
				if (firstChild == null) {
					return "";
				}
				StringBuilder sb = new StringBuilder();
				sb.append(resolveAsSimpleString(firstChild));
				sb.append(':');
				sb.append(resolveAsSimpleString(firstChild.getNextSibling()));
				return sb.toString();
			}
		}
		return ast.toString();
	}

	private String resolveStringOfFirstChildAndSiblings(AST ast) {
		return resolveStringOfFirstChildAndSiblings(ast, null);
	}

	private String resolveStringOfFirstChildAndSiblings(AST ast, String separator) {
		StringBuilder sb = new StringBuilder();
		AST part = ast.getFirstChild();
		if (part == null) {
			return "";
		}
		boolean wasMethod = false;
		if (GroovyTokenTypes.METHOD_CALL == part.getType()) {
			wasMethod = true;
			part = part.getFirstChild();
		}
		if (part == null) {
			if (wasMethod) {
				return "()";
			}
			return "";
		}
		while (part != null) {
			sb.append(resolveAsSimpleString(part));
			if (wasMethod) {
				sb.append("()"); // as first shot we completely ignore
									// parameters here - maybe later improved
				return sb.toString();
			}
			part = part.getNextSibling();
			if (part != null) {
				if (separator != null) {
					sb.append(separator);
				}
			}
		}
		return sb.toString();
	}

	private AST handleTaskClosure(String enameString, Item item, AST lastAst) {
		if (lastAst == null) {
			return null;
		}
		lastAst = handleTaskNameResolving(enameString, item, lastAst);
		lastAst = handleTaskTypeResolving(item, lastAst);
		return lastAst;
	}

	private AST handleTaskTypeResolving(Item item, AST lastAst) {
		if (lastAst == null) {
			return null;
		}
		if (GroovyTokenTypes.ELIST != lastAst.getType()) {
			return lastAst;
		}
		/* parameter -e.g. task mytask (type: xyz) */
		AST elist = lastAst;
		AST nextSibling = elist.getNextSibling();

		AST labeledArg = elist.getFirstChild();

		if (labeledArg == null) {
			return nextSibling;
		}
		if (GroovyTokenTypes.LABELED_ARG != labeledArg.getType()) {
			return nextSibling;
		}
		AST type = labeledArg.getFirstChild();
		if (type == null) {
			return nextSibling;
		}
		if (GroovyTokenTypes.STRING_LITERAL == type.getType()) {
			if (!"type".equals(type.getText())) {
				return nextSibling;
			}
			AST expr = type.getNextSibling();
			if (expr == null || GroovyTokenTypes.EXPR != expr.getType()) {
				return nextSibling;
			}
			AST ident = expr.getFirstChild();
			if (ident == null || GroovyTokenTypes.IDENT != ident.getType()) {
				return nextSibling;
			}
			item.setType(ident.getText());
		}
		return nextSibling;

	}

	private AST handleTaskNameResolving(String enameString, Item item, AST lastAst) {
		if (lastAst == null) {
			return null;
		}
		if (lastAst.getType() == ELIST) {
			AST elist = lastAst;
			AST methodCall2 = elist.getFirstChild();
			if (methodCall2 != null) {
				if (GroovyTokenTypes.SL == methodCall2.getType()) {
					/* << */
					methodCall2 = methodCall2.getFirstChild();
				}
				AST name2 = methodCall2.getFirstChild();
				if (name2 != null) {
					enameString = enameString + name2.getText();
					item.setName(enameString);
					lastAst = name2.getNextSibling();
				}
			}
		}
		return lastAst;
	}

	/**
	 * naming with dot: xyz.abc.bla.tests =>
	 * 
	 * . ->. ->. ->xyz abc bla tests
	 */
	private String resolveName(AST ast) {
		StringBuilder sb = new StringBuilder();
		resolveName(sb, ast);
		return sb.toString();
	}

	private void resolveName(StringBuilder sb, AST ast) {
		if (ast == null) {
			return;
		}
		if (ast.getType() == GroovyTokenTypes.SL) {

		} else if (ast.getType() == GroovyTokenTypes.DOT) {
			/* is dot */
			AST content = ast.getFirstChild();
			resolveName(sb, content);
			AST next = ast.getNextSibling();
			if (next != null) {
				if (next.getType() == GroovyTokenTypes.IDENT) {
					sb.append('.');
					sb.append(next.getText());
				}
			}
		} else {
			/* no dot, so content separated with DOT */
			sb.append(ast.getText());
			AST next = ast.getNextSibling();
			if (next != null) {
				if (next.getType() == GroovyTokenTypes.IDENT) {
					sb.append('.');
					sb.append(next.getText());
				}else{
					if (next.getType()==GroovyTokenTypes.CLOSABLE_BLOCK){
						return;
					}
					if (next.getType()==GroovyTokenTypes.ELIST){
						sb.append(" ");
						sb.append(resolveAsSimpleString(next.getFirstChild()));
					}
				}
			}

		}

	}

	private Item createItem(Context context, AST ast) {
		Item item = new Item();
		int column = ast.getColumn();
		int line = ast.getLine();
		item.setColumn(column);
		item.setLine(line);
		item.setOffset(context.buffer.getOffset(line, column));

		if (ast instanceof GroovySourceAST) {
			GroovySourceAST gast = (GroovySourceAST) ast;
			int offset1 = item.getOffset();
			int offset2 = context.buffer.getOffset(gast.getLineLast(), gast.getColumnLast());

			int length = offset2 - offset1;
			if (length < 0) {
				/* fall back */
				length = gast.getColumnLast() - column;
			}
			item.setLength(length);
		} else {
			item.setLength(1);
		}
		return item;
	}

	private class Context {
		private ExtendedSourceBuffer buffer;
	}

}

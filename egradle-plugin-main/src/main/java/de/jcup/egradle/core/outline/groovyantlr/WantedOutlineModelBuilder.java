package de.jcup.egradle.core.outline.groovyantlr;

import static org.codehaus.groovy.antlr.parser.GroovyTokenTypes.*;

import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.antlr.GroovySourceAST;
import org.codehaus.groovy.antlr.UnicodeEscapingReader;
import org.codehaus.groovy.antlr.parser.GroovyLexer;
import org.codehaus.groovy.antlr.parser.GroovyRecognizer;
import org.codehaus.groovy.antlr.parser.GroovyTokenTypes;

import de.jcup.egradle.core.outline.OutlineItem;
import de.jcup.egradle.core.outline.OutlineItemType;
import de.jcup.egradle.core.outline.OutlineModel;
import de.jcup.egradle.core.outline.OutlineModelBuilder;
import de.jcup.egradle.core.outline.OutlineModelImpl;
import de.jcup.egradle.core.outline.OutlineModifier;
import groovyjarjarantlr.RecognitionException;
import groovyjarjarantlr.TokenStreamException;
import groovyjarjarantlr.collections.AST;

/**
 * Builds a outline model containing simply all AST parts from groovy antlr
 * 
 * @author Albert Tregnaghi
 *
 */
public class WantedOutlineModelBuilder implements OutlineModelBuilder {
	private InputStream is;

	public WantedOutlineModelBuilder(InputStream is) {
		this.is = is;
	}

	@Override
	public OutlineModel build() throws OutlineModelBuilderException {
		OutlineModelImpl model = new OutlineModelImpl();
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
			context.buffer=sourceBuffer;
			
			OutlineItem rootItem = model.getRoot();
			walkThrough(context, rootItem, null, first);

		} catch (RecognitionException | TokenStreamException e) {
			throw new OutlineModelBuilderException("Cannot build outline model because of AST parsing problems", e);
		}

		return model;
	}

	private class Context {
		private ExtendedSourceBuffer buffer;
		public AST astbefore;
	}

	private void walkThrough(Context context, OutlineItem parentItem, OutlineItem currentItem, AST current) {
		parentItem = checkForNewParentItem(parentItem, currentItem, current);

		OutlineItem newItem = createNewItem(context, current);
		/* switch current item and add closed parts */
		if (newItem != null) {
			addToParent(context, parentItem, currentItem);
			currentItem = newItem;
		}
		if (currentItem != null) {
			updateItem(context, currentItem, current);
		}

		/* dive into children */
		AST element = current.getFirstChild();
		if (element != null) {
			context.astbefore = current;
			walkThrough(context, parentItem, currentItem, element);
		}
		AST next = current.getNextSibling();
		if (next != null) {
			context.astbefore = current;
			walkThrough(context, parentItem, currentItem, next);
		} else {
			addToParent(context, parentItem, currentItem);
		}
	}

	private void addToParent(Context context ,OutlineItem parentItem, OutlineItem currentItem) {
		if (currentItem == null) {
			return;
		}
		if (parentItem == null) {
			return;
		}
		/* close last part */
		if (parentItem.hasChild(currentItem)) {
			/* already added, just a loop */
			return;
		}
		parentItem.add(currentItem);
	}

	private OutlineItem checkForNewParentItem(OutlineItem parentItem, OutlineItem currentItem, AST current) {
		return parentItem;
	}

	private void updateItem(Context context, OutlineItem item, AST ast) {
		if (item.isClosed()){
			return;
		}
		switch (ast.getType()) {
		case EXPR: // expression
			AST next = ast.getFirstChild();
			if (next.getType()== METHOD_CALL){
				item.setItemType(OutlineItemType.CLOSURE);
			}
			item.setClosed(true);
			break;
		case VARIABLE_DEF:// variable...
			AST modifiers=null;
			AST type = null;
			AST name = null;
			
			/* item type */
			item.setItemType(OutlineItemType.VARIABLE);
			/* modifiers*/
			String modifierString =null;
			modifiers = ast.getFirstChild();
			if (modifiers!=null){
				AST modifierAst = modifiers.getFirstChild();
				if (modifierAst!=null){
					modifierString = modifierAst.getText();
				}
				type = modifiers.getNextSibling();
			}
			OutlineModifier oModifier = OutlineModifier.DEFAULT;
			if (StringUtils.isNotBlank(modifierString)){
				if ("private".equals(modifierString)){
					oModifier = OutlineModifier.PRIVATE;
				}else if ("protected".equals(modifierString)){
					oModifier = OutlineModifier.PROTECTED;
				}else if ("public".equals(modifierString)){
					oModifier = OutlineModifier.PUBLIC;
				} 
			}
			item.setModifier(oModifier);
			/* type */
			if (type!=null){
				AST typeDef = type.getFirstChild();
				if (typeDef!=null){
					String typeDefText = typeDef.getText();
					item.setType(typeDefText);
				}
				name = type.getNextSibling();
			}
			if (name!=null){
				item.setName(name.getText());
			}
			item.setClosed(true);
			break;
		default:
			break;
		}
	}

	/**
	 * creates new item or <code>null</code>
	 * 
	 * @param context
	 * 
	 * @param current
	 * @return
	 */
	private OutlineItem createNewItem(Context context, AST current) {
		if (!(current instanceof GroovySourceAST)) {
			System.out.println("--ignoring on create:"+current);
			return null;
		}
		GroovySourceAST gast = (GroovySourceAST) current;
		switch (current.getType()) {
		case EXPR:
			AST next = current.getFirstChild();
			if (next==null){
				return null;
			}
			int nextType = next.getType();
			if (GroovyTokenTypes.METHOD_CALL==nextType){
				return commonCreateItem(context, gast);
			}
			return null;
		case VARIABLE_DEF:
			return commonCreateItem(context, gast);
		default:
			return null;
		}
	}

	private OutlineItem commonCreateItem(Context context, AST ast) {
		OutlineItem item = new OutlineItem();
		int column = ast.getColumn();
		int line = ast.getLine();
		item.setColumn(column);
		item.setLine(line);
		item.setOffset(context.buffer.getOffset(line,column));
		/* FIXME ATR, 11.11: length calculation does not work this way - after offset is correct calculated in context
		 * the length must be calculated by by line,column , last line last column!
		 */
		if (ast instanceof GroovySourceAST){
			GroovySourceAST gast = (GroovySourceAST) ast;
			int length = gast.getColumnLast()-column;
			item.setLength(length);
		}else{
			item.setLength(1);
		}
		return item;
	}

}
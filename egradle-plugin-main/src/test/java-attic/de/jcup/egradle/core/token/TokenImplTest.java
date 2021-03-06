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
 package de.jcup.egradle.core.token;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TokenImplTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	private TokenImpl tokenToTest;

	@Before
	public void before() {
		tokenToTest = new TokenImpl(0);
	}

	@Test
	public void new_token_has_type_UNKNOWN() {
		assertEquals(TokenType.UNKNOWN, tokenToTest.getType());
	}

	@Test
	public void new_token_with_type_set_to_null_has_type_UNKNOWN() {
		tokenToTest.setType(null);
		assertEquals(TokenType.UNKNOWN, tokenToTest.getType());
	}

	@Test
	public void new_token_with_type_set_to_TASK_has_type_TASK() {
		tokenToTest.setType(TokenType.TASK);
		assertEquals(TokenType.TASK, tokenToTest.getType());
	}

	@Test
	public void new_token_returns_not_null_for_tokens() {
		assertNotNull(tokenToTest.getChildren());
	}

	@Test
	public void list_returned_by_get_tokens_is_not_changeable() {
		expectedException.expect(UnsupportedOperationException.class);
		tokenToTest.getChildren().add(new TokenImpl(1));
	}

	@Test
	public void new_token_returns_false_for_has_children() {
		assertFalse(tokenToTest.hasChildren());
	}

	@Test
	public void new_token_returns_0_for_offset() {
		assertEquals(0, tokenToTest.getOffset());
	}

	@Test
	public void element_with_given_offset_12_returns_12_for_offset() {
		tokenToTest.setOffset(12);
		assertEquals(12, tokenToTest.getOffset());
	}

	@Test
	public void new_token_returns_length_0() {
		assertEquals(0, tokenToTest.getLength());
	}

	@Test
	public void element_with_name_123456_returns_length_6() {
		tokenToTest.setValue("123456");
		assertEquals(6, tokenToTest.getLength());
	}

	@Test
	public void element_with_one_child_returns_true_for_has_children() {
		tokenToTest.addChild(new TokenImpl(1));
		assertTrue(tokenToTest.hasChildren());
	}

	@Test
	public void new_token_returns_empty_list_for_tokens() {
		assertEquals(new ArrayList<>(), tokenToTest.getChildren());
	}

	@Test
	public void new_token_returns_null_for_name() {
		assertNull(tokenToTest.getValue());
	}

	@Test
	public void new_token_returns_negative1_for_linenumber() {
		assertEquals(-1, tokenToTest.getLineNumber());
	}

	@Test
	public void new_token_returns_null_for_parent() {
		assertNull(tokenToTest.getParent());
	}

	@Test
	public void element_with_parent_set_returns_parent() {
		TokenImpl parentElement = new TokenImpl(666);
		parentElement.addChild(tokenToTest);
		assertEquals(parentElement, tokenToTest.getParent());
	}

	@Test
	public void element_with_linenumber_set_returns_parent() {
		tokenToTest.setLineNumber(10);
		assertEquals(10, tokenToTest.getLineNumber());
	}

	@Test
	public void element_with_child_token_returns_list_with_childelement_inside() {
		TokenImpl child = new TokenImpl(1);
		tokenToTest.addChild(child);
		List<Token> elements = tokenToTest.getChildren();
		assertNotNull(elements);
		assertEquals(1, elements.size());
		assertTrue(elements.contains(child));

	}

	@Test
	public void addTokenToItselfThrowsIllegalStateException() {
		expectedException.expect(IllegalStateException.class);

		tokenToTest.addChild(tokenToTest);
	}

	@Test
	public void token0_defines_token1_as_forward__so_token0_goForward_returns_token1() {
		TokenImpl token1 = new TokenImpl(1);
		tokenToTest.chain(token1);

		assertSame(token1, tokenToTest.goForward());

	}
	
	@Test
	public void token0_defines_token1_as_forward__so_token1_goBackward_returns_token0() {
		TokenImpl token1 = new TokenImpl(1);
		tokenToTest.chain(token1);

		assertSame(tokenToTest, token1.goBackward());

	}

	@Test
	public void token0_defines_token1_as_forward__so_token0_canGoForward_returns_true() {
		TokenImpl token1 = new TokenImpl(1);
		tokenToTest.chain(token1);

		assertTrue(tokenToTest.canGoForward());

	}
	
	@Test
	public void token0_defines_token1_as_forward__so_token1_canGoBackward_returns_true() {
		TokenImpl token1 = new TokenImpl(1);
		tokenToTest.chain(token1);

		assertTrue(token1.canGoBackward());

	}

	@Test
	public void token0_defines_no_token_as_forward__so_token0_goForward_throws_illegal_state() {
		expectedException.expect(IllegalStateException.class);
		tokenToTest.goForward();
	}
	
	@Test
	public void token0_defines_no_token_as_forward__so_token0_canGoForward_returns_false() {
		assertFalse(tokenToTest.canGoForward());
	}

	@Test
	public void token0_defines_no_token_as_backward__so_token0_goBackward_throws_illegal_state() {
		expectedException.expect(IllegalStateException.class);
		tokenToTest.goBackward();
	}

	@Test
	public void token0_defines_no_token_as_backward__so_token0_hasbackward_returns_false() {
		assertFalse(tokenToTest.canGoBackward());
	}

	@Test
	public void token0_having_no_child_throws_illegal_state() {
		expectedException.expect(IllegalStateException.class);
		tokenToTest.getFirstChild();
	}

	@Test
	public void token0_having_child1_as_child_returns_child1_for_getFirstChild() {
		TokenImpl token1 = new TokenImpl(1);
		tokenToTest.addChild(token1);

		assertSame(token1, tokenToTest.getFirstChild());
	}

	@Test
	public void token0_having_child1_and_child2_as_children_returns_child1_for_getFirstChild() {
		TokenImpl token1 = new TokenImpl(1);
		TokenImpl token2 = new TokenImpl(2);
		tokenToTest.addChild(token1);
		tokenToTest.addChild(token2);

		assertSame(token1, tokenToTest.getFirstChild());
	}
}

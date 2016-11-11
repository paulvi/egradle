package de.jcup.egradle.core.outline;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class OutlineModelImplTest {

	private OutlineModelImpl modelToTest;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void before() {
		modelToTest = new OutlineModelImpl();
	}

	@Test
	public void empty_model_returns_not_null_on_getRoot() {
		assertNotNull(modelToTest.getRoot());
	}

	@Test
	public void empty_model_returns_not_null_getRoot_getChildrenUnmodifable() {
		assertNotNull(modelToTest.getRoot().getChildren());
	}

}
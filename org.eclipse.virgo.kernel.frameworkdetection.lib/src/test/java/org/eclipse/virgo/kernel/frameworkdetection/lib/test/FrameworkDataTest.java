package org.eclipse.virgo.kernel.frameworkdetection.lib.test;

import junit.framework.Assert;
import org.eclipse.virgo.kernel.frameworkdetection.lib.FrameworkData;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;



public class FrameworkDataTest {

	private static final long[] BUNDLE_IDS = { 0, 1 };
	private static final String FRAMEWORK_STATE_01 = "ACTIVE";
	private static final String FRAMEWORK_STATE_02 = "RESOLVED";
	private static final Object FRAMEWORK_BUNDLE_01 = new String(
			"java.lang.String.class");
	private static final Object FRAMEWORK_BUNDLE_02 = new Object();
	private static final String DATE_01 = "Tuesday, July 15, 2010"; 
	private static final String DATE_02 = "Tuesday, June 15, 2010";
	private static final Exception e = new Exception();
	private static final StackTraceElement[] ORIGIN = e.getStackTrace();
	private FrameworkData frkData;

	@Before
	public void createFrameworkData() {
		frkData = new FrameworkData(FRAMEWORK_STATE_01, FRAMEWORK_BUNDLE_01,
				DATE_01, ORIGIN, BUNDLE_IDS[0]);
		Assert.assertNotNull(frkData);
	}

	@Test
	public void testGetFrameworkState() {
		Assert.assertEquals(FRAMEWORK_STATE_01, frkData.getFrameworkState());

	}

	@Test
	public void testGetFrkBundle() {
		Assert.assertEquals(FRAMEWORK_BUNDLE_01.toString(), frkData.getBundle()
				.toString());
	}

	@Test
	public void testGetID() {
		Assert.assertEquals(BUNDLE_IDS[0], frkData.getID());
	}

	@Test
	public void testGetDate() {
		Assert.assertEquals(DATE_01, frkData.getDate());
	}

	@Test
	public void testGetOrigin() {
		Assert.assertEquals(ORIGIN, frkData.getOrigin());
	}

	@Test
	public void testSetID() {
		Assert.assertEquals(BUNDLE_IDS[0], frkData.getID());
		frkData.setID(BUNDLE_IDS[1]);
		Assert.assertEquals(BUNDLE_IDS[1], frkData.getID());

	}

	@Test
	public void testEqualsFrameworks() {

		FrameworkData frkData01 = new FrameworkData(FRAMEWORK_STATE_01,
				FRAMEWORK_BUNDLE_01, DATE_01, ORIGIN, BUNDLE_IDS[0]);
		FrameworkData frkData02 = new FrameworkData(FRAMEWORK_STATE_02,
				FRAMEWORK_BUNDLE_02, DATE_02, ORIGIN, BUNDLE_IDS[1]);

		Assert.assertFalse("The frameworks data sould not be the same.",
				frkData01.equals(frkData02));

		FrameworkData frkData = new FrameworkData(FRAMEWORK_STATE_01,
				FRAMEWORK_BUNDLE_01, DATE_01, ORIGIN, BUNDLE_IDS[0]);

		Assert.assertTrue("The frameworks data should be the same.",
				frkData01.equals(frkData));

	}

	@Test
	public void testToString1() {
		frkData = new FrameworkData(FRAMEWORK_STATE_01, FRAMEWORK_BUNDLE_01,
				DATE_01, ORIGIN, BUNDLE_IDS[0]);
		FrameworkData frkTestData = new StubFrameworkData();
		Assert.assertNotNull(frkData.toString());
		Assert.assertNotNull(frkTestData.toString());
		Assert.assertEquals(frkTestData.toString(), frkData.toString());

	}

	@Test
	public void testToString2() {
		frkData = new FrameworkData(FRAMEWORK_STATE_01,
				FRAMEWORK_BUNDLE_01, DATE_01, ORIGIN, BUNDLE_IDS[0]);
		StubFrameworkData frkTestData = new StubFrameworkData();
		Assert.assertNotNull(frkData.toString("-showstack"));
		Assert.assertNotNull(frkTestData.toString("-showstack"));
		Assert.assertEquals(frkTestData.toString("-showstack"),
				frkData.toString("-showstack"));
	}
	@After
	public void cleanUp(){
		frkData = null;
	}

	private class StubFrameworkData extends FrameworkData {

		public StubFrameworkData() {
			super(FRAMEWORK_STATE_01, FRAMEWORK_BUNDLE_01, DATE_01, ORIGIN,
					BUNDLE_IDS[0]);
		}

		@Override
		public String toString() {
			String result = "OSGi framework with state [" + FRAMEWORK_STATE_01
					+ "] and ID[" + BUNDLE_IDS[0] + "]\n"
					+ "- system bundle classname: ["
					+ FRAMEWORK_BUNDLE_01.getClass().getName() + "]\n"
					+ "- last changed on [" + DATE_01 + "]\n"
					+ "- ownLoader: ["
					+ FRAMEWORK_BUNDLE_01.getClass().getClassLoader() + "]\n";
			for (StackTraceElement el : ORIGIN) {
				result += el.toString() + "\n";
			}
			return result;
		}

		public String toString(String arg) {
			String result = "\nOSGi framework with state ["
					+ FRAMEWORK_STATE_01 + "] and ID[" + BUNDLE_IDS[0] + "]\n"
					+ "- system bundle classname: ["
					+ FRAMEWORK_BUNDLE_01.getClass().getName() + "]\n"
					+ "- last changed on [" + DATE_01 + "]\n"
					+ "- ownLoader: ["
					+ FRAMEWORK_BUNDLE_01.getClass().getClassLoader() + "]\n";
			if (arg != null && arg.equals("-showstack")) {
				result += "Call stack:\n";
				for (StackTraceElement el : ORIGIN) {
					result += el.toString() + "\n";
				}
			}
			return result;
		}

	}
}

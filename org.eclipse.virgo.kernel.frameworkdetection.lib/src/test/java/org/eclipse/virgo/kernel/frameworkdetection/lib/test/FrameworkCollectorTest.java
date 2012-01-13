package org.eclipse.virgo.kernel.frameworkdetection.lib.test;
 
import java.util.concurrent.ConcurrentHashMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import org.eclipse.virgo.kernel.frameworkdetection.lib.FrameworkCollector;
import org.eclipse.virgo.kernel.frameworkdetection.lib.FrameworkData;

public class FrameworkCollectorTest {

	
	private static final String FRAMEWORK_STATE_01 = "ACTIVE";
	private static final String KEYS[] = { "111", "211", "311", "411", "511",
			"611", "711" };
	private static final String FRAMEWORK_STATE_02 = "RESOLVED";
	private static final Object FRAMEWORK_BUNDLE_01 = new Object();
	private static final Object FRAMEWORK_BUNDLE_02 = new Object();
	private static final Exception e = new Exception();
	private static final StackTraceElement[] ORIGIN = e.getStackTrace();
	private static final String DATE_01 = "Tuesday, July 15, 2010";
	private long id = 0;

	

	@Test
	public void testAddFramework() {
		FrameworkCollector.addFramework(FRAMEWORK_STATE_01, KEYS[2],
				FRAMEWORK_BUNDLE_01, ORIGIN);
		FrameworkCollector.addFramework(FRAMEWORK_STATE_02, KEYS[3],
				FRAMEWORK_BUNDLE_02, ORIGIN);
		FrameworkCollector.addFramework(FRAMEWORK_STATE_01, KEYS[2],
				FRAMEWORK_BUNDLE_01, ORIGIN);
		int frameworksCount = FrameworkCollector.getFrameworks().size();
		Assert.assertTrue("There should be two frameworks added.",
				frameworksCount == 2);
	}
	

	@Test
	public void testRemoveFrameworks() {
		ConcurrentHashMap<String, FrameworkData> frkd  =  FrameworkCollector.getFrameworks();
		for(FrameworkData d : frkd.values()){
			id = d.getID();	
			System.out.println(id+" update");
		}

		FrameworkCollector.addFramework(FRAMEWORK_STATE_01, KEYS[5],
				FRAMEWORK_BUNDLE_01, ORIGIN);
		FrameworkCollector.addFramework(FRAMEWORK_STATE_02, KEYS[6],
				FRAMEWORK_BUNDLE_02, ORIGIN);
		FrameworkCollector.removeFramework(KEYS[5]);
		FrameworkCollector.removeFramework(KEYS[6]);
		ConcurrentHashMap<String, FrameworkData> frk = FrameworkCollector
				.getFrameworks();
		boolean areFrksRemoved = frk.size() == 0;

		Assert.assertTrue("The added frameworks should be removed",
				areFrksRemoved);

	}

	@Test
	public void testRemoveFramework() {
		FrameworkCollector.addFramework(FRAMEWORK_STATE_01, KEYS[0],
				FRAMEWORK_BUNDLE_01, ORIGIN);
		FrameworkCollector.addFramework(FRAMEWORK_STATE_02, KEYS[1],
				FRAMEWORK_BUNDLE_02, ORIGIN);
		FrameworkCollector.removeFramework(KEYS[0]);
		Assert.assertFalse("The framework with key: " + KEYS[0]
				+ " should not be present", FrameworkCollector.getFrameworks()
				.containsKey(KEYS[0]));
		Assert.assertTrue("The second framework should be present.",
				FrameworkCollector.getFrameworks().containsKey(KEYS[1]));
	}
	@Test
	public void testGetFrameworkByID() {
		FrameworkData frkData = new StubFrameworkData();
		Assert.assertEquals(id, frkData.getID());

		FrameworkCollector.addFramework(FRAMEWORK_STATE_01, KEYS[3],
				FRAMEWORK_BUNDLE_01, ORIGIN);
		FrameworkCollector.addFramework(FRAMEWORK_STATE_02, KEYS[4],
				FRAMEWORK_BUNDLE_02, ORIGIN);
		ConcurrentHashMap<String, FrameworkData> frkd  =  FrameworkCollector.getFrameworks();
		for(FrameworkData d : frkd.values()){
			id = d.getID();	
		}
		
		FrameworkData testdata = FrameworkCollector
				.getFrameworkByID(id);
		Assert.assertNotNull(testdata);
		Assert.assertEquals(id, testdata.getID());

	}

	@After
	public void cleanUp() {

		for (int i = 0; i < KEYS.length; i++) {
			FrameworkCollector.removeFramework(KEYS[i]);
		}
		int frameworksCount = FrameworkCollector.getFrameworks().size();
		Assert.assertTrue("There should not be any frameworks available.",
				frameworksCount == 0);
	}

	private class StubFrameworkData extends FrameworkData {

		public StubFrameworkData() {
			super(FRAMEWORK_STATE_02, FRAMEWORK_BUNDLE_02, DATE_01, ORIGIN,
					id);
		}
	}
}

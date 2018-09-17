package de.hpi.bpt.scylla.plugin.batch;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import de.hpi.bpt.scylla.SimulationTest;
import de.hpi.bpt.scylla.TestUtils;

public class SequentialCasebasedTests extends SimulationTest{
	
	public static void main(String[] args) {
		SequentialCasebasedTests x = new SequentialCasebasedTests();
		x.testParallelGateway();
	}

	protected String getFolderName() {return "BatchPlugin";}
	
	@Test
	public void testParallelGateway() {
		runSimpleSimulation(
				"BatchTestGlobalConfiguration.xml", 
				"CasebasedModelGatewayParallel.bpmn", 
				"BatchTestSimulationConfiguration.xml");
		File f = new File(".\\"+outputPath+"Process_1_processBatchActivityStats.csv");
		Assert.assertTrue(f.exists());
		List<String[]> table = TestUtils.readCSV(f);
		Assert.assertEquals(30, table.size());
		assertExecutionType(table);
		Map<String, List<String[]>> clusters = TestUtils.groupByCluster(table);
		for(List<String[]> cluster : clusters.values()) {
			assertClusterIsCaseBased(cluster);
		}
	}
	
	private static void assertExecutionType(List<String[]> table) {
		table.stream().forEach((each)->{Assert.assertEquals(BatchClusterExecutionType.SEQUENTIAL_CASEBASED.toString(), each[7]);});
	}
	
	public static void assertClusterIsCaseBased(List<String[]> cluster) {
		List<String[]> sortedActivities = cluster.stream()
				.filter((activity)->{return activity[1].equals("Activity A") || activity[1].equals("Activity B");})
				.sorted((a,b)->{return a[4].compareTo(b[4]);})
				.collect(Collectors.toList());
		
		Set<String> seenInstances = new HashSet<String>();
		String lastInstance = null;
		String lastEndTime = "";
		for(String[] activity : sortedActivities) {
			String instance = activity[0];
			String startTime = activity[3];
			if(!instance.equals(lastInstance)) {
				Assert.assertFalse(seenInstances.contains(instance));
				Assert.assertTrue(lastEndTime.compareTo(startTime) <= 0);
				seenInstances.add(instance);
			}
			lastInstance = instance;
			lastEndTime = activity[4].compareTo(lastEndTime) > 0 ? activity[4] : lastEndTime;
		}
	}

}

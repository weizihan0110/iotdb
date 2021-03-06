/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.iotdb.cluster.log.applier;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.iotdb.cluster.common.IoTDBTest;
import org.apache.iotdb.cluster.common.TestMetaGroupMember;
import org.apache.iotdb.cluster.log.LogApplier;
import org.apache.iotdb.cluster.log.logtypes.AddNodeLog;
import org.apache.iotdb.cluster.log.logtypes.PhysicalPlanLog;
import org.apache.iotdb.cluster.log.logtypes.RemoveNodeLog;
import org.apache.iotdb.cluster.rpc.thrift.Node;
import org.apache.iotdb.db.exception.StorageEngineException;
import org.apache.iotdb.db.exception.metadata.MetadataException;
import org.apache.iotdb.db.exception.metadata.StorageGroupNotSetException;
import org.apache.iotdb.db.exception.query.QueryProcessException;
import org.apache.iotdb.db.metadata.PartialPath;
import org.apache.iotdb.db.qp.physical.sys.CreateTimeSeriesPlan;
import org.apache.iotdb.db.qp.physical.sys.SetStorageGroupPlan;
import org.apache.iotdb.db.service.IoTDB;
import org.apache.iotdb.tsfile.file.metadata.enums.CompressionType;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.file.metadata.enums.TSEncoding;
import org.junit.After;
import org.junit.Test;

public class MetaLogApplierTest extends IoTDBTest {

  private Set<Node> nodes = new HashSet<>();

  private TestMetaGroupMember testMetaGroupMember = new TestMetaGroupMember() {
    @Override
    public void applyAddNode(Node newNode) {
      nodes.add(newNode);
    }

    @Override
    public void applyRemoveNode(Node oldNode) {
      nodes.remove(oldNode);
    }
  };

  private LogApplier applier = new MetaLogApplier(testMetaGroupMember);

  @Override
  @After
  public void tearDown() throws IOException, StorageEngineException {
    testMetaGroupMember.stop();
    testMetaGroupMember.closeLogManager();
    super.tearDown();
  }

  @Test
  public void testApplyAddNode()
      throws QueryProcessException, StorageGroupNotSetException, StorageEngineException {
    nodes.clear();

    Node node = new Node("localhost", 1111, 0, 2222, 55560);
    AddNodeLog log = new AddNodeLog();
    log.setNewNode(node);
    applier.apply(log);

    assertTrue(nodes.contains(node));
  }

  @Test
  public void testApplyRemoveNode()
      throws QueryProcessException, StorageGroupNotSetException, StorageEngineException {
    nodes.clear();

    Node node = testMetaGroupMember.getThisNode();
    RemoveNodeLog log = new RemoveNodeLog();
    log.setRemovedNode(node);
    applier.apply(log);

    assertFalse(nodes.contains(node));
  }

  @Test
  public void testApplyMetadataCreation()
      throws QueryProcessException, MetadataException, StorageEngineException {
    PhysicalPlanLog physicalPlanLog = new PhysicalPlanLog();
    SetStorageGroupPlan setStorageGroupPlan = new SetStorageGroupPlan(
        new PartialPath("root.applyMeta"));
    physicalPlanLog.setPlan(setStorageGroupPlan);

    applier.apply(physicalPlanLog);
    assertTrue(IoTDB.metaManager.isPathExist(new PartialPath("root.applyMeta")));

    CreateTimeSeriesPlan createTimeSeriesPlan = new CreateTimeSeriesPlan(
        new PartialPath("root.applyMeta"
            + ".s1"), TSDataType.DOUBLE, TSEncoding.RLE, CompressionType.SNAPPY,
        Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), null);
    physicalPlanLog.setPlan(createTimeSeriesPlan);
    applier.apply(physicalPlanLog);
    assertTrue(IoTDB.metaManager.isPathExist(new PartialPath("root.applyMeta.s1")));
    assertEquals(TSDataType.DOUBLE, IoTDB.metaManager.getSeriesType(new PartialPath("root"
        + ".applyMeta.s1")));
  }
}
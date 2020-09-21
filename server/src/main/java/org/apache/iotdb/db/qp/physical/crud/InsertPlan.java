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

package org.apache.iotdb.db.qp.physical.crud;

import java.util.ArrayList;
import java.util.List;
import org.apache.iotdb.db.metadata.PartialPath;
import org.apache.iotdb.db.metadata.mnode.MNode;
import org.apache.iotdb.db.qp.logical.Operator;
import org.apache.iotdb.db.qp.physical.PhysicalPlan;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.write.schema.MeasurementSchema;

public abstract class InsertPlan extends PhysicalPlan {

  protected PartialPath deviceId;
  protected String[] measurements;
  protected TSDataType[] dataTypes;
  protected MeasurementSchema[] schemas;

  // for updating last cache
  private MNode deviceMNode;

  // record the failed measurements, their reasons, and positions in "measurements"
  List<String> failedMeasurements;
  private List<Exception> failedExceptions;
  private List<Integer> failedIndices;

  public InsertPlan(Operator.OperatorType operatorType) {
    super(false, operatorType);
    super.canBeSplit = false;
  }

  public PartialPath getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(PartialPath deviceId) {
    this.deviceId = deviceId;
  }

  public String[] getMeasurements() {
    return this.measurements;
  }

  public void setMeasurements(String[] measurements) {
    this.measurements = measurements;
  }

  public TSDataType[] getDataTypes() {
    return dataTypes;
  }

  public void setDataTypes(TSDataType[] dataTypes) {
    this.dataTypes = dataTypes;
  }

  public MeasurementSchema[] getSchemas() {
    return schemas;
  }

  public void setSchemas(MeasurementSchema[] schemas) {
    this.schemas = schemas;
  }

  public List<String> getFailedMeasurements() {
    return failedMeasurements;
  }

  public List<Exception> getFailedExceptions() {
    return failedExceptions;
  }

  public int getFailedMeasurementNumber() {
    return failedMeasurements == null ? 0 : failedMeasurements.size();
  }

  public MNode getDeviceMNode() {
    return deviceMNode;
  }

  public void setDeviceMNode(MNode deviceMNode) {
    this.deviceMNode = deviceMNode;
  }

  public abstract long getMinTime();


  /**
   * @param index failed measurement index
   */
  public void markFailedMeasurementInsertion(int index, Exception e) {
    if (measurements[index] == null) {
      return;
    }
    if (failedMeasurements == null) {
      failedMeasurements = new ArrayList<>();
      failedExceptions = new ArrayList<>();
      failedIndices = new ArrayList<>();
    }
    failedMeasurements.add(measurements[index]);
    failedExceptions.add(e);
    failedIndices.add(index);
    measurements[index] = null;
    dataTypes[index] = null;
  }


  /**
   * Reconstruct this plan with the failed measurements.
   * @return the plan itself, with measurements replaced with the previously failed ones.
   */
  public InsertPlan getPlanFromFailed() {
    if (failedMeasurements == null) {
      return null;
    }
    measurements = failedMeasurements.toArray(new String[0]);
    failedMeasurements = null;
    if (dataTypes != null) {
      TSDataType[] temp = dataTypes.clone();
      dataTypes = new TSDataType[failedIndices.size()];
      for (int i = 0; i < failedIndices.size(); i++) {
        dataTypes[i] = temp[failedIndices.get(i)];
      }
    }
    if (schemas != null) {
      MeasurementSchema[] temp = schemas.clone();
      schemas = new MeasurementSchema[failedIndices.size()];
      for (int i = 0; i < failedIndices.size(); i++) {
        schemas[i] = temp[failedIndices.get(i)];
      }
    }
    failedIndices = null;
    failedExceptions = null;
    return this;
  }

}

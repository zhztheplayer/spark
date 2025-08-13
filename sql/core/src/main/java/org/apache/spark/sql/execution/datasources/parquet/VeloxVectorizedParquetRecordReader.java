/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.execution.datasources.parquet;

import com.google.common.base.Preconditions;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.types.StructType;

import java.io.IOException;
import java.time.ZoneId;

public class VeloxVectorizedParquetRecordReader extends SpecificParquetRecordReaderBase<Object> {
  private final int capacity;
  private final ZoneId convertTz;
  private final String datetimeRebaseMode;
  private final String datetimeRebaseTz;
  private final String int96RebaseMode;
  private final String int96RebaseTz;

  /**
   * If true, this class returns batches instead of rows.
   */
  private boolean returnColumnarBatch = false;

  public VeloxVectorizedParquetRecordReader(
      ZoneId convertTz,
      String datetimeRebaseMode,
      String datetimeRebaseTz,
      String int96RebaseMode,
      String int96RebaseTz,
      boolean useOffHeap,
      int capacity) {
    Preconditions.checkArgument(useOffHeap);
    this.convertTz = convertTz;
    this.datetimeRebaseMode = datetimeRebaseMode;
    this.datetimeRebaseTz = datetimeRebaseTz;
    this.int96RebaseMode = int96RebaseMode;
    this.int96RebaseTz = int96RebaseTz;
    this.capacity = capacity;
  }

  @Override
  public boolean nextKeyValue() throws IOException, InterruptedException {
    return false;
  }

  @Override
  public Object getCurrentValue() throws IOException, InterruptedException {
    return null;
  }

  @Override
  public float getProgress() throws IOException, InterruptedException {
    return 0;
  }


  public void initBatch(StructType partitionColumns, InternalRow partitionValues) {
    // Do nothing.
  }

  /**
   * Can be called before any rows are returned to enable returning columnar batches directly.
   */
  public void enableReturningBatches() {
    returnColumnarBatch = true;
  }

  @Override
  public void close() throws IOException {
    super.close();
  }
}

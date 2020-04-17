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

package org.apache.spark.resource

import org.apache.spark.annotation.Evolving
import org.apache.spark.internal.Logging

/**
 * Resource profile only associated with executor.
 */
@Evolving
private[spark] class ExecutorResourceProfile private(
    val id: Int,
    val executorResources: Map[String, ExecutorResourceRequest]
) extends Serializable with Logging {


}

object ExecutorResourceProfile extends Logging {
  val CORES: String = ResourceProfile.CORES
  val MEMORY: String = ResourceProfile.MEMORY
  val OVERHEAD_MEM: String = ResourceProfile.OVERHEAD_MEM
  val PYSPARK_MEM: String = ResourceProfile.PYSPARK_MEM

  val DEFAULT_PROFILE_ID = ExecutorResourceProfileManager.DEFAULT_PROFILE_ID
}

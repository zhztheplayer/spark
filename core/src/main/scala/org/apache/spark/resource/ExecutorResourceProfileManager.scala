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

import java.util.concurrent.atomic.AtomicInteger

import scala.collection.mutable

import org.apache.spark.internal.Logging

/**
 * Manager of executor resource profiles.
 */
private[spark] class ExecutorResourceProfileManager(
    private val rpm: ResourceProfileManager
) extends Logging {
  private val idToProfile = new mutable.HashMap[Int, ExecutorResourceProfile]()
  private val rpIdToErpId = new mutable.HashMap[Int, Int]()
  private val defaultStrategy: ExecutorResourceProfileCreationStrategy.Value =
    ExecutorResourceProfileCreationStrategy.ALWAYS_NEW
  private val defaultProfile =
    create(rpm.defaultResourceProfile, defaultStrategy)

  def create(rpId: Int,
      strategy: ExecutorResourceProfileCreationStrategy.Value): ExecutorResourceProfile = {
    val erp = create(rpm.resourceProfileFromId(rpId), strategy)
    rpIdToErpId.put(rpId, erp.id)
    erp
  }

  def getDefaultStrategy: ExecutorResourceProfileCreationStrategy.Value = {
    defaultStrategy
  }

  def getDefaultProfile: ExecutorResourceProfile = {
    defaultProfile
  }

  private def create(rp: ResourceProfile,
      strategy: ExecutorResourceProfileCreationStrategy.Value): ExecutorResourceProfile = {
    // todo
    return null
  }
}

private[spark] object ExecutorResourceProfileManager {
  val DEFAULT_PROFILE_ID: Int = 0
  private lazy val nextProfileId = new AtomicInteger(1)

  private def getNextProfileId: Int = nextProfileId.getAndIncrement()

}

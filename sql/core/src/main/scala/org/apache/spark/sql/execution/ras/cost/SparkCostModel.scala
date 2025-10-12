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

package org.apache.spark.sql.execution.ras.cost

import org.apache.gluten.ras.{Cost, CostModel}

import org.apache.spark.sql.execution.{FileSourceScanExec, PlanLater, SparkPlan}
import org.apache.spark.sql.execution.aggregate.{HashAggregateExec, ObjectHashAggregateExec, SortAggregateExec}
import org.apache.spark.sql.execution.datasources.v2.{DataSourceV2ScanExecBase, FileScan}
import org.apache.spark.sql.execution.joins.{BroadcastHashJoinExec, BroadcastNestedLoopJoinExec, CartesianProductExec, ShuffledHashJoinExec, SortMergeJoinExec}
import org.apache.spark.sql.execution.ras.plan.GroupLeafExec

object SparkCostModel extends CostModel[SparkPlan] {
  private val infLongCost = Long.MaxValue

  override def costOf(node: SparkPlan): LongCost = node match {
    case _ => LongCost(longCostOf(node))
  }

  override def costComparator(): Ordering[Cost] = Ordering.Long.on {
    case LongCost(value) => value
    case _ => throw new IllegalStateException("Unexpected cost type")
  }

  override def makeInfCost(): LongCost = LongCost(infLongCost)

  // Sum with ceil to avoid overflow.
  private def safeSum(a: Long, b: Long): Long = {
    assert(a >= 0)
    assert(b >= 0)
    val sum = a + b
    if (sum < a || sum < b) infLongCost else sum
  }

  private def longCostOf(node: SparkPlan): Long = node match {
    case n =>
      val selfCost = selfLongCostOf(n)
      (n.children.map(longCostOf).toSeq :+ selfCost).reduce[Long](safeSum)
  }

  private def selfLongCostOf(node: SparkPlan): Long = node match {
    case _: GroupLeafExec => throw new IllegalStateException()
    case _: PlanLater => infLongCost
    case _: HashAggregateExec => 1000
    case _: ObjectHashAggregateExec => 1000
    case _: SortAggregateExec => 2000
    case _: BroadcastHashJoinExec => 1000
    case _: ShuffledHashJoinExec => 2000
    case _: SortMergeJoinExec => 3000
    case _: CartesianProductExec => 10000
    case _: BroadcastNestedLoopJoinExec => 150000
    case f: FileSourceScanExec =>
      val filterStringLength = f.metadata("PushedFilters").length
      10000 + f.output.size * 1000 - filterStringLength
    case d: DataSourceV2ScanExecBase =>
      val filterStringLength = d.scan match {
        case f: FileScan =>
          f.getMetaData()("PushedFilters").length
        case _ => 0
      }
      10000 + d.output.size * 1000 - filterStringLength
    case _ => 10
  }
}


case class LongCost(value: Long) extends Cost

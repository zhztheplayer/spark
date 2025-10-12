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

package org.apache.spark.sql.execution

import org.apache.gluten.ras.{Optimization, RasExplain}
import org.apache.gluten.ras.path.Pattern
import org.apache.gluten.ras.property.PropertySet
import org.apache.gluten.ras.rule.{RasRule, Shape, Shapes}

import org.apache.spark.sql.execution.ras.cost.SparkCostModel
import org.apache.spark.sql.execution.ras.metadata.SparkMetadataModel
import org.apache.spark.sql.execution.ras.plan.SparkPlanModel
import org.apache.spark.sql.execution.ras.property.{Dist, Ord, SparkPropertyModel}
import org.apache.spark.sql.{ExperimentalMethods, SparkSession, Strategy}
import org.apache.spark.sql.catalyst.plans.logical.{LogicalPlan, ReturnAnswer}
import org.apache.spark.sql.execution.adaptive.LogicalQueryStageStrategy
import org.apache.spark.sql.execution.datasources.v2.DataSourceV2Strategy
import org.apache.spark.sql.execution.datasources.{DataSourceStrategy, FileSourceStrategy}
import org.apache.spark.sql.execution.exchange.{BroadcastExchangeExec, ShuffleExchangeExec}

class RasStrategy(val session: SparkSession)
  extends Strategy {

  private val fakeSparkPlanner = new SparkPlanner(session, new ExperimentalMethods())

  private def optimization: Optimization[SparkPlan] = Optimization[SparkPlan](
    SparkPlanModel,
    SparkCostModel,
    SparkMetadataModel,
    SparkPropertyModel,
    SparkExplain,
    RasRule.Factory.reuse(
      AsRasStrategyRule(LogicalQueryStageStrategy) ::
      AsRasStrategyRule(fakeSparkPlanner.PythonEvals) ::
      AsRasStrategyRule(new DataSourceV2Strategy(session)) ::
      AsRasStrategyRule(FileSourceStrategy) ::
      AsRasStrategyRule(DataSourceStrategy) ::
      AsRasStrategyRule(fakeSparkPlanner.SpecialLimits) ::
      AsRasStrategyRule(fakeSparkPlanner.Aggregation(true)) ::
      AsRasStrategyRule(fakeSparkPlanner.Aggregation(false)) ::
      AsRasStrategyRule(fakeSparkPlanner.Window) ::
      AsRasStrategyRule(fakeSparkPlanner.WindowGroupLimit) ::
      AsRasStrategyRule(fakeSparkPlanner.JoinSelection(true)) ::
      AsRasStrategyRule(fakeSparkPlanner.InMemoryScans) ::
      AsRasStrategyRule(fakeSparkPlanner.SparkScripts) ::
      AsRasStrategyRule(fakeSparkPlanner.BasicOperators) ::
      Nil)
  )

  override def apply(plan: LogicalPlan): Seq[SparkPlan] = plan match {
    case ReturnAnswer(root) =>
      val planner = optimization.newPlanner(PlanLater(root), PropertySet(Seq(Dist.any, Ord.any)))
      val optimized = planner.plan()
      val removed = removeSortsAndExchanges(optimized)
      Seq(removed)
    case _ =>
      Nil
  }

  private def removeSortsAndExchanges(plan: SparkPlan): SparkPlan = {
    plan.withNewChildren(plan.children.map {
      child =>
        child transformUp {
          case s: ShuffleExchangeExec => s.child
          case b: BroadcastExchangeExec => b.child
          case s: SortExec => s.child
        }
    })

  }


  private object SparkExplain extends RasExplain[SparkPlan] {
    override def describeNode(node: SparkPlan): String = node.nodeName
  }

  private case class AsRasStrategyRule(strategy: Strategy) extends RasRule[SparkPlan] {
    override def shift(node: SparkPlan): Iterable[SparkPlan] = node match {
      case PlanLater(logicalPlan) =>
        strategy.apply(logicalPlan)
    }

    override def shape(): Shape[SparkPlan] =
      Shapes.pattern(Pattern.leaf[SparkPlan](Pattern.Matchers.clazz(classOf[PlanLater])).build())
  }
}


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

package org.apache.spark.sql.execution.ras.property

import org.apache.gluten.ras.{GroupLeafBuilder, Property, PropertyDef}
import org.apache.gluten.ras.rule.EnforcerRuleFactory

import org.apache.spark.sql.catalyst.plans.physical.{BroadcastDistribution, Distribution, Partitioning, UnspecifiedDistribution}
import org.apache.spark.sql.catalyst.SQLConfHelper
import org.apache.spark.sql.execution.SparkPlan
import org.apache.spark.sql.execution.exchange.{BroadcastExchangeExec, REPARTITION_BY_COL, REPARTITION_BY_NUM, ShuffleExchangeExec}
import org.apache.spark.sql.execution.ras.plan.GroupLeafExec

sealed trait Dist extends Property[SparkPlan] {
  override def definition(): PropertyDef[SparkPlan, _ <: Property[SparkPlan]] = {
    DistDef
  }
}

object Dist {
  val any: Dist = Req(UnspecifiedDistribution)

  case class Prop(prop: Partitioning) extends Dist
  case class Req(req: Distribution) extends Dist {
    def isAny: Boolean = {
      this == any
    }
  }
}

object DistDef extends PropertyDef[SparkPlan, Dist] {
  override def any(): Dist = Dist.any
  override def getProperty(plan: SparkPlan): Dist = Dist.Prop(plan.outputPartitioning)
  override def getChildrenConstraints(
      plan: SparkPlan,
      constraint: Property[SparkPlan]): Seq[Dist] = {
    // TODO: Propagate constraints?
    plan.requiredChildDistribution.map(Dist.Req)
  }
  override def satisfies(
      property: Property[SparkPlan],
      constraint: Property[SparkPlan]): Boolean = {
    val req = constraint.asInstanceOf[Dist.Req]
    if (req.isAny) {
      return true
    }
    property.asInstanceOf[Dist.Prop].prop.satisfies(req.req)
  }

  override def assignToGroup(
      group: GroupLeafBuilder[SparkPlan],
      constraint: Property[SparkPlan]): GroupLeafBuilder[SparkPlan] = {
    group
      .asInstanceOf[GroupLeafExec.Builder]
      .withDistribution(constraint.asInstanceOf[Dist.Req].req)
  }

  val enforcerRule = new EnforcerRuleFactory.SubRule[SparkPlan] with SQLConfHelper {
    override def enforce(
        node: SparkPlan,
        constraint: Property[SparkPlan]): Iterable[SparkPlan] = {
      val req = constraint.asInstanceOf[Dist.Req]
      if (req.isAny) {
        return Nil
      }
      val out = (node, req.req) match {
        case (child, distribution) if child.outputPartitioning.satisfies(distribution) =>
          child
        case (child, BroadcastDistribution(mode)) =>
          BroadcastExchangeExec(mode, child)
        case (child, distribution) =>
          val numPartitions = distribution.requiredNumPartitions
            .getOrElse(conf.numShufflePartitions)
          val shuffleOrigin = if (distribution.requiredNumPartitions.isDefined) {
            REPARTITION_BY_NUM
          } else {
            REPARTITION_BY_COL
          }
          ShuffleExchangeExec(
            distribution.createPartitioning(numPartitions),
            child,
            shuffleOrigin)
      }
      Seq(out)
    }
  }
}

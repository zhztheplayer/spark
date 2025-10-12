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

import org.apache.spark.sql.catalyst.expressions.SortOrder
import org.apache.spark.sql.catalyst.SQLConfHelper
import org.apache.spark.sql.execution.{SortExec, SparkPlan}
import org.apache.spark.sql.execution.ras.plan.GroupLeafExec

sealed trait Ord extends Property[SparkPlan] {
  override def definition(): PropertyDef[SparkPlan, _ <: Property[SparkPlan]] = {
    OrdDef
  }
}

object Ord {
  val any: Ord = Req(Nil)

  case class Prop(prop: Seq[SortOrder]) extends Ord
  case class Req(req: Seq[SortOrder]) extends Ord {
    def isAny: Boolean = {
      this == any
    }
  }
}

object OrdDef extends PropertyDef[SparkPlan, Ord] {
  override def any(): Ord = Ord.any
  override def getProperty(plan: SparkPlan): Ord = Ord.Prop(plan.outputOrdering)
  override def getChildrenConstraints(
      plan: SparkPlan,
      constraint: Property[SparkPlan]): Seq[Ord] = {
    // TODO: Propagate constraints?
    plan.requiredChildOrdering.map(Ord.Req)
  }
  override def satisfies(
      property: Property[SparkPlan],
      constraint: Property[SparkPlan]): Boolean = {
    val req = constraint.asInstanceOf[Ord.Req]
    if (req.isAny) {
      return true
    }
    SortOrder.orderingSatisfies(property.asInstanceOf[Ord.Prop].prop, req.req)
  }

  override def assignToGroup(
      group: GroupLeafBuilder[SparkPlan],
      constraint: Property[SparkPlan]): GroupLeafBuilder[SparkPlan] = {
    group
      .asInstanceOf[GroupLeafExec.Builder]
      .withOrdering(constraint.asInstanceOf[Ord.Req].req)
  }

  val enforcerRule: EnforcerRuleFactory.SubRule[SparkPlan] =
    new EnforcerRuleFactory.SubRule[SparkPlan] with SQLConfHelper {
    override def enforce(
        node: SparkPlan,
        constraint: Property[SparkPlan]): Iterable[SparkPlan] = {
      val req = constraint.asInstanceOf[Ord.Req]
      if (req.isAny) {
        return Nil
      }
      val out = (node, req.req) match {
        case (child, requiredOrdering)
            if SortOrder.orderingSatisfies(child.outputOrdering, requiredOrdering) =>
          child
        case (child, requiredOrdering) =>
          SortExec(requiredOrdering, global = false, child = child)
      }
      Seq(out)
    }
  }
}

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

import org.apache.gluten.ras._
import org.apache.gluten.ras.property.PropertySet
import org.apache.gluten.ras.rule.{EnforcerRuleFactory, RasRule, Shape, Shapes}

import org.apache.spark.sql.execution._

object SparkPropertyModel extends PropertyModel[SparkPlan] {
  override def propertyDefs: Seq[PropertyDef[SparkPlan, _ <: Property[SparkPlan]]] =
    Seq(DistDef, OrdDef)

  override def newEnforcerRuleFactory(): EnforcerRuleFactory[SparkPlan] = {
    new EnforcerRuleFactory[SparkPlan] {
      override def newEnforcerRules(
        constraintSet: PropertySet[SparkPlan]): Seq[RasRule[SparkPlan]] = {
        val distReq = constraintSet.get(DistDef).asInstanceOf[Dist.Req]
        val ordReq = constraintSet.get(OrdDef).asInstanceOf[Ord.Req]

        val distRule = new RasRule[SparkPlan] {
          override def shift(node: SparkPlan): Iterable[SparkPlan] = {
            val out = DistDef.enforcerRule.enforce(node, distReq)
            out
          }
          override def shape(): Shape[SparkPlan] = Shapes.fixedHeight(1)
        }

        val ordRule = new RasRule[SparkPlan] {
          override def shift(node: SparkPlan): Iterable[SparkPlan] = {
            val out = OrdDef.enforcerRule.enforce(node, ordReq)
            out
          }
          override def shape(): Shape[SparkPlan] = Shapes.fixedHeight(1)
        }

        val distOrdRule = new RasRule[SparkPlan] {
          override def shift(node: SparkPlan): Iterable[SparkPlan] = {
            val out = Seq(node)
              .flatMap(DistDef.enforcerRule.enforce(_, distReq))
              .flatMap(OrdDef.enforcerRule.enforce(_, ordReq))
            out
          }
          override def shape(): Shape[SparkPlan] = Shapes.fixedHeight(1)
        }

        Seq(distRule, ordRule, distOrdRule)
      }
    }
  }
}

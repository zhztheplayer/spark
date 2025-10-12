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
import org.apache.gluten.ras.rule.{EnforcerRuleFactory, RasRule}

import org.apache.spark.sql.execution._

object SparkPropertyModel extends PropertyModel[SparkPlan] {
  override def propertyDefs: Seq[PropertyDef[SparkPlan, _ <: Property[SparkPlan]]] =
    Nil

  override def newEnforcerRuleFactory(): EnforcerRuleFactory[SparkPlan] =
    new EnforcerRuleFactory[SparkPlan] {
      override def newEnforcerRules(
        constraintSet: PropertySet[SparkPlan]): Seq[RasRule[SparkPlan]] = Nil
    }
}

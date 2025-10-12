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
package org.apache.spark.sql.execution.ras.plan

import org.apache.gluten.ras.GroupLeafBuilder

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.Attribute
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.catalyst.trees.TreeNodeTag
import org.apache.spark.sql.execution.{LeafExecNode, SparkPlan}
import java.util.concurrent.atomic.AtomicBoolean

import org.apache.spark.sql.execution.ras.metadata.{LogicalLink, SparkMetadata}

// TODO: Make this inherit from GlutenPlan.
case class GroupLeafExec(groupId: Int, metadata: SparkMetadata)
  extends LeafExecNode {

  private val frozen = new AtomicBoolean(false)

  // Set the logical link then make the plan node immutable. All future
  // mutable operations related to tagging will be aborted.
  if (metadata.logicalLink() != LogicalLink.notFound) {
    setLogicalLink(metadata.logicalLink().plan)
  }
  frozen.set(true)

  override protected def doExecute(): RDD[InternalRow] = throw new IllegalStateException()
  override def output: Seq[Attribute] = metadata.schema().output

  final override def supportsColumnar: Boolean = {
    throw new RuntimeException()
  }

  final override def supportsRowBased: Boolean = {
    throw new RuntimeException()
  }

  private def ensureNotFrozen(): Unit = {
    if (frozen.get()) {
      throw new UnsupportedOperationException()
    }
  }

  // Enclose mutable APIs.
  override def setLogicalLink(logicalPlan: LogicalPlan): Unit = {
    ensureNotFrozen()
    super.setLogicalLink(logicalPlan)
  }
  override def setTagValue[T](tag: TreeNodeTag[T], value: T): Unit = {
    ensureNotFrozen()
    super.setTagValue(tag, value)
  }
  override def unsetTagValue[T](tag: TreeNodeTag[T]): Unit = {
    ensureNotFrozen()
    super.unsetTagValue(tag)
  }
  override def copyTagsFrom(other: SparkPlan): Unit = {
    ensureNotFrozen()
    super.copyTagsFrom(other)
  }
}

object GroupLeafExec {
  class Builder private[GroupLeafExec] (override val id: Int) extends GroupLeafBuilder[SparkPlan] {
    private var metadata: SparkMetadata = _

    def withMetadata(metadata: SparkMetadata): Builder = {
      this.metadata = metadata
      this
    }

    override def build(): SparkPlan = {
      require(metadata != null)
      GroupLeafExec(id, metadata)
    }
  }

  def newBuilder(groupId: Int): Builder = {
    new Builder(groupId)
  }
}

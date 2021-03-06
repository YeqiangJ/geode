/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.redis.internal.executor;

import java.util.Collection;

import io.netty.buffer.ByteBuf;

import org.apache.geode.redis.internal.ByteArrayWrapper;
import org.apache.geode.redis.internal.Coder;
import org.apache.geode.redis.internal.CoderException;
import org.apache.geode.redis.internal.Command;
import org.apache.geode.redis.internal.ExecutionHandlerContext;
import org.apache.geode.redis.internal.Executor;
import org.apache.geode.redis.internal.GeodeRedisServer;
import org.apache.geode.redis.internal.RedisConstants;
import org.apache.geode.redis.internal.RedisDataType;
import org.apache.geode.redis.internal.RedisResponse;
import org.apache.geode.redis.internal.RegionProvider;

/**
 * The AbstractExecutor is the base of all {@link Executor} types for the {@link GeodeRedisServer}.
 */
public abstract class AbstractExecutor implements Executor {

  /**
   * Number of Regions used by GeodeRedisServer internally
   */
  public static final int NUM_DEFAULT_REGIONS = 3;

  /**
   * Max length of a list
   */
  protected static final Integer INFINITY_LIMIT = Integer.MAX_VALUE;

  /**
   * Constant of number of milliseconds in a second
   */
  protected static final int millisInSecond = 1000;

  /**
   * Checks if the given key is associated with the passed expectedDataType. If there is a mismatch,
   * a {@link RuntimeException} is thrown
   *
   * @param key Key to check
   * @param expectedDataType Type to check to
   * @param context context
   */
  public void checkDataType(ByteArrayWrapper key, RedisDataType expectedDataType,
      ExecutionHandlerContext context) {
    context.getKeyRegistrar().validate(key, expectedDataType);
  }

  protected boolean removeEntry(ByteArrayWrapper key,
      ExecutionHandlerContext context) {

    RegionProvider rC = context.getRegionProvider();
    RedisDataType type = context.getKeyRegistrar().getType(key);
    return rC.removeKey(key, type);
  }

  protected long getBoundedStartIndex(long index, long size) {
    if (size < 0L) {
      throw new IllegalArgumentException("Size < 0, really?");
    }
    if (index >= 0L) {
      return Math.min(index, size);
    } else {
      return Math.max(index + size, 0);
    }
  }

  protected long getBoundedEndIndex(long index, long size) {
    if (size < 0L) {
      throw new IllegalArgumentException("Size < 0, really?");
    }
    if (index >= 0L) {
      return Math.min(index, size);
    } else {
      return Math.max(index + size, -1);
    }
  }

  protected void respondBulkStrings(Command command, ExecutionHandlerContext context,
      Object message) {
    ByteBuf rsp;
    try {
      if (message instanceof Collection) {
        rsp = Coder.getArrayResponse(context.getByteBufAllocator(),
            (Collection<?>) message);
      } else {
        rsp = Coder.getBulkStringResponse(context.getByteBufAllocator(), message);
      }
    } catch (CoderException e) {
      command.setResponse(Coder.getErrorResponse(context.getByteBufAllocator(),
          RedisConstants.SERVER_ERROR_MESSAGE));
      return;
    }

    command.setResponse(rsp);
  }

  protected RedisResponse respondBulkStrings(Object message) {
    if (message instanceof Collection) {
      return RedisResponse.array((Collection<?>) message);
    } else {
      return RedisResponse.string((String) message);
    }
  }
}

/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.cluster.loadbalance;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;

import java.util.List;
import java.util.Random;

/**
 * random load balance(dubbo default).
 *
 * @author qianlei
 * @author william.liangf
 */
public class RandomLoadBalance extends AbstractLoadBalance {

	public static final String NAME = "random";

	private final Random random = new Random();

	/**
	 * 1.RandomLoadBalance:按权重随机调用，这种方式是DUBBO默认的负载均衡策略，源码如下。
	 * 实现思路：如果服务多实例权重相同，则进行随机调用；如果权重不同，按照总权重取随机数。
	 * 根据总权重数生成一个随机数，然后和具体服务实例的权重进行相减做偏移量，然后找出偏移量小于0的，
	 * 比如随机数为10，某一个服务实例的权重为12，那么10-12=-2<0成立，则该服务被调用。
	 * 这种策略在随机的情况下尽可能保证权重大的服务会优先被随机调用。
	 */
	protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
		int length 		   	 = invokers.size();	// invoker个数
		int[] invokerWeights = new int[length];	// 所有权重列表

		for (int i = 0; i < length; i++) {
			invokerWeights[i] = getWeight(invokers.get(i), invocation);
		}

		int totalWeight = sum(invokerWeights);// 总权重

		if (totalWeight > 0 && !isAllTheSame(invokerWeights)) {
			// 如果权重不相同且权重大于0则按总权重数随机
			int offset = random.nextInt(totalWeight);
			// 并确定随机值落在哪个片断上
			for (int i = 0; i < length; i++) {
				offset -= invokerWeights[i];
				if (offset < 0) {
					return invokers.get(i);
				}
			}
		}

		// 如果权重相同或权重为0则均等随机
		return invokers.get(random.nextInt(length));
	}

	/**
	 * 判断所有invoker的权重是否相同
	 * @param invokerWeights
	 * @return
	 */
	private boolean isAllTheSame(int[] invokerWeights) {
		if (invokerWeights.length == 1) {
			return true;
		}

		for (int i = 0; i < invokerWeights.length - 1; i++) {
			if (invokerWeights[i] != invokerWeights[i + 1]) {
				return false;
			}
		}

		return true;
	}

}
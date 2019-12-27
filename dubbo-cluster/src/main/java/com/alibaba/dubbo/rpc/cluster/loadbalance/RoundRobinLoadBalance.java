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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.AtomicPositiveInteger;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;

/**
 * Round robin load balance.
 *
 * @author qian.lei
 * @author william.liangf
 */
public class RoundRobinLoadBalance extends AbstractLoadBalance {

    public static final String NAME = "roundrobin";

    private final ConcurrentMap<String, AtomicPositiveInteger> sequences = new ConcurrentHashMap<String, AtomicPositiveInteger>();

    /**
     * 2.RoundRobinLoadBalance：轮询，按公约后的权重设置轮询比率
     * 实现思路：首先计算出多服务实例的最大最小权重，如果权重都一样（maxWeight=minWeight），则直接取模轮询；
     * 如果权重不一样，每一轮调用，都计算出一个基础的权重值，然后筛选出权重值大于基础权重值的invoker进行取模随机调用。
     */
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
    	/**
    	 * url示例：
    	 * dubbo://10.112.6.12:20880/cn.javacoder.test.dubbo.IHelloWorldService?application=test-provider&dubbo=2.5.3
    	 * &interface=cn.javacoder.test.dubbo.IHelloWorldService&methods=say&pid=6816&side=provider&timestamp=1522284835101
    	 */
        String key = invokers.get(0).getUrl().getServiceKey() + "." + invocation.getMethodName();
        int length = invokers.size(); // invoker总个数
        int[] invokerWeights = new int[length];// invoker权重列表

		for (int i = 0; i < length; i++) {
			invokerWeights[i] = getWeight(invokers.get(i), invocation);
		}

		int weightSum = sum(invokerWeights);// 总权重和
		int maxWeight = max(invokerWeights);// 最大权重
		int minWeight = min(invokerWeights);// 最小权重

		final LinkedHashMap<Invoker<T>, IntegerWrapper> invokerToWeightMap = parseInvokerToWeightMap(invokers, invokerWeights);

        AtomicPositiveInteger sequence = sequences.get(key);
        if (sequence == null) {
            sequences.putIfAbsent(key, new AtomicPositiveInteger());// 原子大于或等于0的整数
            sequence = sequences.get(key);
        }

		int currentSequence = sequence.getAndIncrement();// 第一次获取得到0

		if (maxWeight > 0 && minWeight < maxWeight) { // 权重不一样
			// 每一轮调用，都计算出一个基础的权重值，然后筛选出权重值大于基础权重值的invoker进行取模随机调用
			int mod = currentSequence % weightSum;
			for (int i = 0; i < maxWeight; i++) {
				for (Entry<Invoker<T>, IntegerWrapper> entry : invokerToWeightMap.entrySet()) {
					final Invoker<T> invoker = entry.getKey();
					final IntegerWrapper weight = entry.getValue();
					if (mod == 0 && weight.getValue() > 0) {
						return invoker;
					}
					if (weight.getValue() > 0) {
						weight.decrement();
						mod--;
					}
				}
			}
		}

        // 取模轮循
        return invokers.get(currentSequence % length);
    }

	private <T> LinkedHashMap<Invoker<T>, IntegerWrapper> parseInvokerToWeightMap(List<Invoker<T>> invokers, int[] invokerWeights) {
		LinkedHashMap<Invoker<T>, IntegerWrapper> invokerToWeightMap = new LinkedHashMap<Invoker<T>, IntegerWrapper>(8);

		for (int i = 0; i < invokers.size(); i++) {
			if (invokerWeights[i] > 0) {
				invokerToWeightMap.put(invokers.get(i), new IntegerWrapper(invokerWeights[i]));
			}
		}

		return invokerToWeightMap;
	}

    private static final class IntegerWrapper {
        private int value;

        public IntegerWrapper(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        @SuppressWarnings("unused")
		public void setValue(int value) {
            this.value = value;
        }

        public void decrement() {
            this.value--;
        }
    }

}
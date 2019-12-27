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

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.cluster.LoadBalance;

import java.util.List;

/**
 * AbstractLoadBalance
 *
 * @author william.liangf
 */
public abstract class AbstractLoadBalance implements LoadBalance {

	/**
	 * 计算预热权重
	 * <pre>
	 * 因调用处有uptime小于warmup以及weight大于等于100的限制，所以该方法总是返回一个小于等于weight的数
	 * </pre>
	 * @param uptime
	 * @param warmup
	 * @param weight
	 * @return
	 */
	static int calculateWarmupWeight(int uptime, int warmup, int weight) {
		// 公式为：ww = (uptime ÷ warmup) × weight
		int ww = (int) ((float) uptime / ((float) warmup / (float) weight));
		return ww < 1 ? 1 : (ww > weight ? weight : ww);
	}

	public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) {
		if (invokers == null || invokers.size() == 0) {
			return null;
		}
		if (invokers.size() == 1) {
			return invokers.get(0);
		}

		// 负载均衡策略选择invoker
		return doSelect(invokers, url, invocation);
	}

    protected abstract <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation);

    protected int getWeight(Invoker<?> invoker, Invocation invocation) {
    	/**
    	 * url示例：
    	 * dubbo://10.112.6.12:20880/cn.javacoder.test.dubbo.IHelloWorldService?application=test-provider&dubbo=2.5.3
    	 * &interface=cn.javacoder.test.dubbo.IHelloWorldService&methods=say&pid=6816&side=provider&timestamp=1522284835101
    	 */
    	// 先得到Provider的权重(没有设置权重时默认为100)
        int weight = invoker.getUrl().getMethodParameter(invocation.getMethodName(), Constants.WEIGHT_KEY, Constants.DEFAULT_WEIGHT);
        if (weight > 0) {
        	// 得到provider的启动时间戳
            long timestamp = invoker.getUrl().getParameter(Constants.TIMESTAMP_KEY, 0L);// timestamp=1522284835101
            if (timestamp > 0L) {
            	// provider已经运行了多少时间(毫秒)
                int uptime = (int) (System.currentTimeMillis() - timestamp);
                // 得到warmup(预热时间)的值，默认为10分钟(10 * 60 * 1000 = 600000)
                int warmup = invoker.getUrl().getParameter(Constants.WARMUP_KEY, Constants.DEFAULT_WARMUP);
                if (uptime > 0 && uptime < warmup) {
                    weight = calculateWarmupWeight(uptime, warmup, weight);
                }
            }
        }

        // 若provider运行时间大于预热时间，则返回provider的权重
        return weight;
    }

    protected int sum(int[] array) {
		int sum = 0;
		for (int i = 0; i < array.length; i++) {
			sum += array[i];
		}
		return sum;
	}

	protected int max(int[] array) {
		int max = 0;
		for (int i = 0; i < array.length; i++) {
			max = Math.max(max, array[i]);
		}
		return max;
	}

	protected int min(int[] array) {
		int min = Integer.MAX_VALUE;
		for (int i = 0; i < array.length; i++) {
			min = Math.min(min, array[i]);
		}
		return min;
	}

}
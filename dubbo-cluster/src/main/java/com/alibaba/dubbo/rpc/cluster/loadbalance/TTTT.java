package com.alibaba.dubbo.rpc.cluster.loadbalance;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.alibaba.dubbo.common.utils.AtomicPositiveInteger;

public class TTTT {

	public static void main(String[] args) {
		System.out.println(new AtomicPositiveInteger().get());

		ConcurrentMap<String, String> map = new ConcurrentHashMap<String, String>();
		String result1 = map.put("aaa", "bbb");
		String result2 = map.putIfAbsent("ccc", "ddd");
		String result3 = map.putIfAbsent("aaa", "fff");
		System.out.println(result1);
		System.out.println(result2);
		System.out.println(result3);
		System.out.println(map.get("aaa"));
	}

}

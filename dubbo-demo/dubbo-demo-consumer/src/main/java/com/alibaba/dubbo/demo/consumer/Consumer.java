package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.demo.DemoService;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by ken.lj on 2017/7/31.
 */
public class Consumer {

	public static void main(String[] args) {
		String[] configs = { "META-INF/spring/dubbo-demo-consumer.xml" };
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(configs);
		context.start();

		DemoService demoService = (DemoService) context.getBean("demoService"); // 获取远程服务代理
		String result = demoService.sayHello("world"); // 执行远程方法

		System.out.println(result); // 显示调用结果
		context.close();
	}

}
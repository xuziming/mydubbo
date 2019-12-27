package com.alibaba.dubbo.demo.provider;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by ken.lj on 2017/7/31.
 */
public class Provider {

	public static void main(String[] args) throws Exception {
		String[] configs = { "META-INF/spring/dubbo-demo-provider.xml" };
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(configs);
		context.start();

		System.in.read(); // 按任意键退出
		context.close();
	}

}
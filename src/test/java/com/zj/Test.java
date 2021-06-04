package com.zj;

import java.util.HashMap;
import java.util.Map;

import com.zj.dto.Camera;

import cn.hutool.core.convert.Convert;
import cn.hutool.json.JSONUtil;
import lombok.Getter;
import lombok.Setter;

public class Test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Map<String, Employee> map = new HashMap<>();
		Employee ee = new Student();
		Employee tt = new teacher();
		map.put("xx", ee);
		map.put("tt", tt);
		
		Employee employee = map.get("xx");
		if(employee instanceof Student) {
			Student s = (Student) employee;
			s.test();
			System.out.println(s.getName());
		} else {
			teacher s = (teacher) employee;
			System.out.println(s.getAge());
		}
		
		
	}

}

@Getter
@Setter
class Student extends Employee{
	private String name = "张三";

	@Override
	public void test() {
		System.out.println(33);
	}
	
}
@Getter
@Setter
class teacher extends Employee{
	private int age = 222;
}

abstract class Employee {
	public void test() {
		System.out.println(11);
	}
}
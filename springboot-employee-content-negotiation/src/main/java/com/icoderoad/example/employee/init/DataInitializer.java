package com.icoderoad.example.employee.init;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.icoderoad.example.employee.entity.Employee;
import com.icoderoad.example.employee.repository.EmployeeRepository;

@Component
public class DataInitializer implements ApplicationRunner {
    private final EmployeeRepository employeeRepository;

    @Autowired
    public DataInitializer(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
    	long total = employeeRepository.count();
    	if( total<10 ) {
	        // 初始化10个员工数据
	        for (int i = 1; i <= 10; i++) {
	            Employee employee = new Employee();
	            employee.setName("员工" + i);
	            employee.setDescription("员工" + i + "的描述");
	            employee.setSalary(BigDecimal.valueOf(50000 + i * 1000));
	            employeeRepository.save(employee);
	        }
    	}
    }
}
package com.icoderoad.example.employee.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.icoderoad.example.employee.entity.Employee;
import com.icoderoad.example.employee.repository.EmployeeRepository;

@Controller
@RequestMapping("/view")
public class EmployeeViewController {
    private final EmployeeRepository employeeRepository;

    @Autowired
    public EmployeeViewController(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @GetMapping(value = "/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getJsonView(Model model) {
        List<Employee> employees = employeeRepository.findAll();
        model.addAttribute("employees", employees);
        return "employee/employee-json";
    }

    @GetMapping(value = "/xml")
    public String getXmlView(Model model) {
        List<Employee> employees = employeeRepository.findAll();
        model.addAttribute("employees", employees);
        return "employee/employee-xml";
    }
}
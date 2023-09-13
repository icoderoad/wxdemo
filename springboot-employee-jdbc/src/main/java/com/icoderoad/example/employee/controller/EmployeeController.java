package com.icoderoad.example.employee.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.icoderoad.example.employee.entity.Employee;
import com.icoderoad.example.employee.repository.EmployeeRepository;

@Controller
@RequestMapping("/employee")
public class EmployeeController {
    private final EmployeeRepository employeeRepository;

    public EmployeeController(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/list")
    public String listEmployees(Model model,
                                 @RequestParam(name = "page", defaultValue = "1") int page,
                                 @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        int totalEmployees = employeeRepository.countEmployees();
        int totalPages = (int) Math.ceil((double) totalEmployees / pageSize);
        if (page < 1) {
            page = 1;
        } else if (page > totalPages && totalPages!=0) {
            page = totalPages;
        }

        model.addAttribute("employees", employeeRepository.findAllEmployees(page, pageSize));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        return "employee/list";
    }

    @GetMapping("/add")
    public String addEmployeeForm(Model model) {
        model.addAttribute("employee", new Employee());
        return "employee/add";
    }

    @PostMapping("/add")
    public String addEmployee(@ModelAttribute Employee employee) {
        employeeRepository.addEmployee(employee);
        return "redirect:/employee/list";
    }

    @GetMapping("/edit/{empId}")
    public String editEmployeeForm(@PathVariable String empId, Model model) {
        Employee employee = employeeRepository.findEmployeeById(empId);
        model.addAttribute("employee", employee);
        return "employee/edit";
    }

    @PostMapping("/edit/{empId}")
    public String editEmployee(@PathVariable String empId, @ModelAttribute Employee employee) {
        employeeRepository.updateEmployee(employee);
        return "redirect:/employee/list";
    }

    @GetMapping("/delete/{empId}")
    public String deleteEmployee(@PathVariable String empId) {
        employeeRepository.deleteEmployee(empId);
        return "redirect:/employee/list";
    }
}
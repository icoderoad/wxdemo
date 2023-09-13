package com.icoderoad.example.employee.repository;

import java.util.List;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.icoderoad.example.employee.entity.Employee;

@Repository
public class EmployeeRepository {
    private final JdbcTemplate jdbcTemplate;

    public EmployeeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @SuppressWarnings("deprecation")
	public List<Employee> findAllEmployees(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        String sql = "SELECT * FROM employee LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, new Object[]{pageSize, offset}, new BeanPropertyRowMapper<>(Employee.class));
    }

    public int countEmployees() {
        String sql = "SELECT COUNT(*) FROM employee";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    public void addEmployee(Employee employee) {
        String sql = "INSERT INTO employee (emp_id, emp_name) VALUES (?, ?)";
        jdbcTemplate.update(sql, employee.getEmpId(), employee.getEmpName());
    }

    public Employee findEmployeeById(String empId) {
        String sql = "SELECT * FROM employee WHERE emp_id = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{empId}, new BeanPropertyRowMapper<>(Employee.class));
    }

    public void updateEmployee(Employee employee) {
        String sql = "UPDATE employee SET emp_name = ? WHERE emp_id = ?";
        jdbcTemplate.update(sql, employee.getEmpName(), employee.getEmpId());
    }

    public void deleteEmployee(String empId) {
        String sql = "DELETE FROM employee WHERE emp_id = ?";
        jdbcTemplate.update(sql, empId);
    }
}
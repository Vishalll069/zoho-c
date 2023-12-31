package com.clayfin.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.clayfin.dto.EmployeeDto;
import com.clayfin.entity.Attendance;
import com.clayfin.entity.Employee;
import com.clayfin.entity.EmployeeProfile;
import com.clayfin.entity.LeaveRecord;
import com.clayfin.entity.Task;
import com.clayfin.enums.RoleType;
import com.clayfin.exception.AttendanceException;
import com.clayfin.exception.EmployeeException;
import com.clayfin.exception.LeaveException;
import com.clayfin.exception.TaskException;
import com.clayfin.repository.EmployeeProfileRepo;
import com.clayfin.repository.EmployeeRepo;
import com.clayfin.utility.Constants;
import com.clayfin.utility.RepoHelper;

@Service
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

	@Autowired
	private EmployeeRepo employeeRepo;
	
	@Autowired
	private EmployeeProfileRepo employeeProfileRepo;

	@Autowired
	private RepoHelper repoHelper;

	@Override
	public Employee addEmployee(Employee employee,Integer hrId) throws EmployeeException {

		if (employee == null)
			throw new EmployeeException();
		employee.setJoiningDate(LocalDate.now());
		return employeeRepo.save(employee);
	}

	@Override
	public Employee updateEmployee(Integer employeeId, Employee employee) throws EmployeeException {
		Employee employee1 = getEmployeeById(employeeId);
		if (repoHelper.isEmployeeExist(employeeId)) {
			BeanUtils.copyProperties(employee, employee1, getNullPropertyNames(employee));
			return employeeRepo.save(employee1);
		}
		else
			throw new EmployeeException(Constants.EMPLOYEE_NOT_FOUND_WITH_ID + employeeId);
	}
	
	
	@Override
	public EmployeeProfile updateEmployeeProfileByEmployeeId(Integer employeeId, EmployeeProfile employeeProfile)
			throws EmployeeException {
		EmployeeProfile employeeProfile2 = getEmployeeProfileByEmployeeId(employeeId);
		if (employeeProfile2 != null) {
			BeanUtils.copyProperties(employeeProfile, employeeProfile2, getNullPropertyNames(employeeProfile));
			return employeeProfileRepo.save(employeeProfile2);
		}
		else
			throw new EmployeeException(Constants.EMPLOYEE_NOT_FOUND_WITH_ID + employeeId);
	}
	
	private String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<>();
        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) emptyNames.add(pd.getName());
        }

        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }

	@Override
	public Employee getEmployeeById(Integer employeeId) throws EmployeeException {
		return employeeRepo.findById(employeeId)
				.orElseThrow(() -> new EmployeeException(Constants.EMPLOYEE_NOT_FOUND_WITH_ID + employeeId));
	}

	@Override
	public Employee deleteEmployee(Integer employeeId) throws EmployeeException {
		Employee employee = getEmployeeById(employeeId);
		employeeRepo.deleteById(employeeId);
		return employee;
	}

	@Override
	public Employee getEmployeeByEmail(String email) throws EmployeeException {
		return employeeRepo.findByEmail(email)
				.orElseThrow(() -> new EmployeeException(Constants.EMPLOYEE_NOT_FOUND_WITH_EMAIL + email));

	}

	@Override
	public List<Employee> getAllEmployees() throws EmployeeException {
		List<Employee> employees = employeeRepo.findAll();

		if (employees.isEmpty())
			throw new EmployeeException(Constants.EMPLOYEES_NOT_FOUND);

		return employees;

	}

	@Override
	public Employee getEmployeeManager(Integer employeeId) throws EmployeeException {

		Employee employee = employeeRepo.findById(employeeId)
				.orElseThrow(() -> new EmployeeException(Constants.TASK_NOT_FOUND_WITH_EMPLOYEE_ID + employeeId));

		if (employee.getManager() == null)
			throw new EmployeeException("Manager Not Assigned To the Employee Id " + employeeId);

		return employee.getManager();

	}

	@Override
	public List<Employee> getEmployeesOfManager(Integer managerId) throws EmployeeException {

		Employee employee = employeeRepo.findById(managerId)
				.orElseThrow(() -> new EmployeeException(Constants.EMPLOYEE_NOT_FOUND_WITH_ID));

		if (employee.getSubEmployees().isEmpty())
			throw new EmployeeException(Constants.EMPLOYEES_NOT_FOUND);

		return employee.getSubEmployees();

	}

	@Override
	public List<Task> getAllTaskMyEmployeeId(Integer employeeId) throws EmployeeException, TaskException {

		Employee employee = getEmployeeById(employeeId);

		if (employee.getTasks().isEmpty()) {
			throw new TaskException(Constants.TASK_NOT_FOUND_WITH_EMPLOYEE_ID + employeeId);
		}

		return employee.getTasks();
	}

	@Override
	public List<LeaveRecord> getAllLeavesByEmployeeId(Integer employeeId) throws EmployeeException, LeaveException {
		Employee employee = getEmployeeById(employeeId);

		if (employee.getTasks().isEmpty()) {
			throw new LeaveException(Constants.LEAVE_NOT_FOUND_WITH_EMPLOYEE_ID + employeeId);
		}

		return employee.getLeaveRecords();
	}

	@Override
	public List<Attendance> getAllAttendanceByEmployeeId(Integer employeeId)
			throws EmployeeException, AttendanceException {
		Employee employee = getEmployeeById(employeeId);

		if (employee.getTasks().isEmpty()) {
			throw new AttendanceException(Constants.ATTENDANCE_NOT_FOUND_WITH_EMPLOYEE_ID + employeeId);
		}

		return employee.getAttendances();

	}

	@Override
	public Employee setManagerToEmployeeByHr(Integer employeeId,Integer hrId,EmployeeDto employeeDto) throws EmployeeException {
		Employee hr = employeeRepo.findById(hrId)
				.orElseThrow(() -> new EmployeeException(Constants.Hr_NOT_FOUND_WITH_ID + employeeId));
		Employee employee = employeeRepo.findById(employeeId)
				.orElseThrow(() -> new EmployeeException(Constants.EMPLOYEE_NOT_FOUND_WITH_ID + employeeId));
		Employee manager = employeeRepo.findById(employeeDto.getManagerId())
				.orElseThrow(() -> new EmployeeException(Constants.MANAGER_NOT_FOUND_WITH_ID + employeeDto.getManagerId()));

		if (manager.getRole() != RoleType.ROLE_MANAGER)
			throw new EmployeeException("Manager Id Not Correct");

		if (employeeDto.getManagerId() == employeeId)
			throw new EmployeeException("Manager Himself Cannot me Manager");

		if (employee.getManager() != null)
			throw new EmployeeException("Employee Manager Already Exist");
		if(hr.getRole().equals("ROLE_HR")) {
			employee.setManager(manager);

			employee.setReportingTo(manager.getUsername());
			employee.setRole(employeeDto.getRole());
			employee.setTitle(employeeDto.getTitle());
			employeeRepo.save(employee);

			return employee;
		}
		else {
			throw new EmployeeException(Constants.NOT_VALID_HR);
		}

	}
	
	

	@Override
	public Employee updateSkillSet(Integer employeeId, List<String> skills) throws EmployeeException {
		Employee employee = employeeRepo.findById(employeeId)
				.orElseThrow(() -> new EmployeeException(Constants.EMPLOYEE_NOT_FOUND_WITH_ID + employeeId));
		
            List<String> skillSet = employee.getSkillSet();
            skills.forEach(i -> skillSet.add(i));
            employee.setSkillSet(skillSet);
		return employeeRepo.save(employee);
	}
	
	@Override
	public EmployeeProfile getEmployeeProfileByEmployeeId(Integer employeeId) throws EmployeeException {
		if (repoHelper.isEmployeeExist(employeeId))
			return employeeProfileRepo.findByEmployeeEmployeeId(employeeId);
		else
			throw new EmployeeException(Constants.EMPLOYEE_NOT_FOUND_WITH_ID + employeeId);
	}
	
	
	
	@Override
	public EmployeeProfile addEmployeeProfileData(Integer employeeId, EmployeeProfile employeeProfile) throws EmployeeException {

		Employee employee = employeeRepo.findById(employeeId)
				.orElseThrow(() -> new EmployeeException(Constants.EMPLOYEE_NOT_FOUND_WITH_ID + employeeId));
		
		employeeProfile.setEmployee(employee);
		employeeProfile.setReportingTo(employeeRepo.findById(employee.getManager().getEmployeeId()).get().getUsername());
		return employeeProfileRepo.save(employeeProfile);
	
	}
	
	
	@Override
	public List<Employee> getAllBirthdayEmployeesBy() throws EmployeeException {
		LocalDate date = LocalDate.now();

		List<Employee> employees = new ArrayList<Employee>();
		List<EmployeeProfile> employeeProfiles =  employeeProfileRepo.findByBirthDate(date);
		for(EmployeeProfile i: employeeProfiles ) {
		    employees.add(	i.getEmployee());
		}
		return employees;
	}
	
	
	@Override
	public List<Employee> getAllNewEmployees() throws EmployeeException {
		
		List<Employee> employees =  employeeRepo.findFirst10ByOrderByEmployeeIdDesc();
		
		return employees;
	}
}

package com.clayfin.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.clayfin.dto.DayAttendanceDto;
import com.clayfin.entity.Attendance;
import com.clayfin.entity.Employee;
import com.clayfin.exception.AttendanceException;
import com.clayfin.exception.EmployeeException;
import com.clayfin.exception.LeaveException;
import com.clayfin.repository.AttendenceRepo;
import com.clayfin.repository.EmployeeRepo;
import com.clayfin.utility.Constants;
import com.clayfin.utility.RepoHelper;

@Service
public class AttendanceServiceImpl implements AttendanceService {
	
	//ZoneId zoneId = ZoneId.of("Asia/kolkata");
	ZoneOffset zoneId = ZoneOffset.ofHoursMinutes(5, 30);


	@Autowired
	private AttendenceRepo attendanceRepo;

	@Autowired
	private EmployeeRepo employeeRepo;

	@Autowired
	private RepoHelper repoHelper;

	@Override
	public List<Attendance> getAttendanceByDateAndEmployeeId(LocalDate date, Integer employeeId)
			throws EmployeeException, AttendanceException {

		List<Attendance> attendances = attendanceRepo.findByEmployeeEmployeeIdAndDate(employeeId, date);

		if (attendances.isEmpty())
			throw new AttendanceException(
					Constants.ATTENDANCE_NOT_FOUND_WITH_EMPLOYEE_ID + employeeId + Constants.WITH_DATE + date);

		return attendances;

	}

	
	

	@Override
	public List<Attendance> getAttendanceByEmployeeId(Integer employeeId)
			throws AttendanceException, EmployeeException {
		LocalDate date = LocalDate.now();
		List<Attendance> attendances = getAttendanceByDateAndEmployeeId(date,employeeId);

		if (attendances.isEmpty())
			throw new AttendanceException(Constants.ATTENDANCE_NOT_FOUND_WITH_ID + employeeId);

		return attendances;
	}
	
	@Override
	public Attendance getAttendanceByAttendanceId(Integer attendanceId) throws AttendanceException {
		Attendance attendance = attendanceRepo.findById(attendanceId).orElseThrow(
				() -> new AttendanceException(Constants.ATTENDANCE_NOT_FOUND_WITH_ATTENDANCE_ID + attendanceId));
		return attendance;
	}
	
	
	@Override
	public Attendance updateAttendance(Integer attendanceId, Attendance attendance) throws AttendanceException {

		if (!isAttendanceExist(attendanceId))
			throw new AttendanceException(Constants.ATTENDANCE_NOT_FOUND_WITH_ATTENDANCE_ID + attendanceId);
		
		Attendance attendance1 = getAttendanceByAttendanceId(attendanceId);

		BeanUtils.copyProperties(attendance,attendance1, getNullPropertyNames(attendance));
		return attendanceRepo.save(attendance1);

	}
	
	private String[] getNullPropertyNames(Object source) {
		final BeanWrapper src = new BeanWrapperImpl(source);
		java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

		Set<String> emptyNames = new HashSet<>();
		for (java.beans.PropertyDescriptor pd : pds) {
			Object srcValue = src.getPropertyValue(pd.getName());
			if (srcValue == null)
				emptyNames.add(pd.getName());
		}

		String[] result = new String[emptyNames.size()];
		return emptyNames.toArray(result);
	}

	


	@Override
	public Attendance deleteAttendance(Integer attendanceId) throws AttendanceException {
		Attendance attendance = attendanceRepo.findById(attendanceId).orElseThrow(
				() -> new AttendanceException(Constants.ATTENDANCE_NOT_FOUND_WITH_ATTENDANCE_ID + attendanceId));
		attendanceRepo.delete(attendance);

		return attendance;
	}

	@Override
	public Attendance regularize(Integer employeeId, LocalDate date, LocalTime fromTime, LocalTime toTime)
			throws AttendanceException, EmployeeException {

		LocalTime spentHours = repoHelper.findTimeBetweenTimestamps(fromTime, toTime);
		 
		Employee employee = employeeRepo.findById(employeeId)
				.orElseThrow(() -> new EmployeeException(Constants.EMPLOYEE_NOT_FOUND_WITH_ID + employeeId));

		if (spentHours.getHour() > 2)
			throw new AttendanceException(Constants.NOT_REGURALIZABLE);

		List<Attendance> alreadyPresentAttendance = getAttendanceByDateAndEmployeeId(date, employeeId);
		
		System.out.println(alreadyPresentAttendance.size()+"  attendances Found ");

		for (int i = 0; i < alreadyPresentAttendance.size() - 1; i++) {
			if (alreadyPresentAttendance.get(i).getCheckOutTimestamp().isBefore(fromTime)) {
				if (i != alreadyPresentAttendance.size() - 1) {
					Attendance attendance = new Attendance();
					attendance.setCheckInTimestamp(fromTime);
					attendance.setCheckOutTimestamp(toTime);
					attendance.setDate(date);
					attendance.setEmployee(employee);
					LocalTime spentTime = repoHelper.findTimeBetweenTimestamps(attendance.getCheckInTimestamp(),
							attendance.getCheckOutTimestamp());

					attendance.setSpentHours(spentTime);
					return attendanceRepo.save(attendance);
				} else {
					Attendance nextAttendance = alreadyPresentAttendance.get(i + 1);
					if (nextAttendance.getCheckInTimestamp().isAfter(toTime)) {

						Attendance attendance = new Attendance();
						attendance.setCheckInTimestamp(fromTime);
						attendance.setCheckOutTimestamp(toTime);
						attendance.setDate(date);
						attendance.setEmployee(employee);
						LocalTime spentTime = repoHelper.findTimeBetweenTimestamps(attendance.getCheckInTimestamp(),
								attendance.getCheckOutTimestamp());

						attendance.setSpentHours(spentTime);
						return attendanceRepo.save(attendance);

					} else {
						throw new AttendanceException("Attendance Time is Overlapping");
					}
				}
			}
		}

		if(alreadyPresentAttendance.isEmpty())
		    throw new AttendanceException(Constants.ATTENDANCE_NOT_FOUND);
		else
			throw new AttendanceException("Cannot Reguralize the for Given Timings");
		
		

	}

	@Override
	public Boolean isAttendanceExist(Integer attendanceId) throws AttendanceException {
		return attendanceRepo.findById(attendanceId).isPresent();

	}
	
	@Override
	public List<DayAttendanceDto> getAttendanceByMonthAndEmployeeId(Integer month, Integer year, Integer employeeId)
			throws AttendanceException, EmployeeException, LeaveException {
		
		
		Integer days = repoHelper.getDaysInMonth(month, year);
		
		List<DayAttendanceDto> attendances = new ArrayList<>();
		for(int i=1;i<=days;i++) {
			LocalDate date = LocalDate.of(year, month, i);
		    DayAttendanceDto attendance = repoHelper.getTotalTimeInDay(date, employeeId);
		    attendances.add(attendance);
		}
		return attendances;
	}
	
	
	@PersistenceContext
	private EntityManager entityManager;

	public Attendance findByUserId(Integer id) {
        return   (entityManager.createQuery("SELECT p FROM Attendance p WHERE p.employee.id = :id ORDER BY p.id DESC",
        		Attendance.class).setParameter("id", id).setMaxResults(1).getResultList()).get(0);
    }
	

	

	@Override
	public Attendance checkInAttendance(Integer employeeId) throws EmployeeException, AttendanceException {

		Employee employee = employeeRepo.findById(employeeId)
				.orElseThrow(() -> new EmployeeException(Constants.EMPLOYEE_NOT_FOUND_WITH_ID + employeeId));

		//Attendance lastAttendance =  findByUserId(employeeId);
		Attendance lastAttendance = attendanceRepo.findTopByEmployeeEmployeeIdOrderByEmployeeEmployeeIdDesc(employeeId);
		if(lastAttendance!=null && lastAttendance.getCheckOutTimestamp()==null)
			throw new AttendanceException("CheckOut First To Check In");

		Attendance attendance = new Attendance();
		
		
		if (lastAttendance == null) {

			attendance.setCheckInTimestamp(LocalTime.now(zoneId));
			System.out.println(attendance.getCheckInTimestamp());
			attendance.setDate(LocalDate.now());
			attendance.setEmployee(employee);
			return attendanceRepo.save(attendance);

		}
		if (lastAttendance !=null && lastAttendance.getCheckOutTimestamp() == null){
			throw new AttendanceException("Need to CheckOut before CheckIn ");
		}
		else{
		attendance.setCheckInTimestamp(LocalTime.now(zoneId));
		System.out.println(attendance.getCheckInTimestamp());
		attendance.setDate(LocalDate.now());
		attendance.setEmployee(employee);
		return attendanceRepo.save(attendance);
		}

		
	}

	public Attendance findByUserIdFirstRecord(Integer id) {
        return   (entityManager.createQuery("SELECT p FROM Attendance p WHERE p.employee.id = :id ORDER BY p.id ",
        		Attendance.class).setParameter("id", id).setMaxResults(1).getResultList()).get(0);
    }

	@Override
	public Attendance checkOutAttendance(Integer employeeId) throws AttendanceException, EmployeeException {

		if (!repoHelper.isEmployeeExist(employeeId))
			throw new EmployeeException(Constants.EMPLOYEE_NOT_FOUND_WITH_ID + employeeId);

		Attendance lastAttendance =  findByUserId(employeeId);
		Attendance firstAttendance = getAttendanceByDateAndEmployeeId(lastAttendance.getDate(), employeeId).get(0);
		if (lastAttendance != null && lastAttendance.getCheckOutTimestamp() == null) {
			if(LocalDate.now().isEqual(lastAttendance.getDate())) {
				
				lastAttendance.setCheckOutTimestamp(LocalTime.now(zoneId));
				System.out.println(lastAttendance.getCheckOutTimestamp());
				
				
			}
			else {
				//Attendance firstAttendance = findByUserIdFirstRecord(employeeId);
				
				LocalTime firstTime = firstAttendance.getCheckInTimestamp().plusHours(4);
				lastAttendance.setCheckOutTimestamp(firstTime);
				System.out.println("Half day Absent");
			}
			//modifications
			LocalTime spentTime = repoHelper.findTimeBetweenTimestamps(lastAttendance.getCheckInTimestamp(),
					lastAttendance.getCheckOutTimestamp());

			lastAttendance.setSpentHours(spentTime);

		} else
			throw new AttendanceException("You Have To Check In First To CheckOut");

		

		return attendanceRepo.save(lastAttendance);
	}



}

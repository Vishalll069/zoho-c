package com.clayfin.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.clayfin.dto.DayAttendanceDto;
import com.clayfin.entity.Attendance;
import com.clayfin.exception.AttendanceException;
import com.clayfin.exception.EmployeeException;
import com.clayfin.exception.LeaveException;

public interface AttendanceService {

	Attendance checkInAttendance(Integer employeeId) throws EmployeeException,AttendanceException;
	
	Attendance checkOutAttendance(Integer employeeId) throws EmployeeException,AttendanceException;
	
	Boolean isAttendanceExist(Integer attendanceId)throws AttendanceException;

	List<Attendance> getAttendanceByDateAndEmployeeId(LocalDate date, Integer employeeId)
			throws EmployeeException, AttendanceException;

	List<Attendance> getAttendanceByEmployeeId(Integer employeeId) throws AttendanceException, EmployeeException;

	Attendance updateAttendance(Integer attendanceId,Attendance attendance) throws AttendanceException;

	Attendance deleteAttendance(Integer attendanceId) throws AttendanceException;
	
	
	Attendance regularize(Integer employeeId,LocalDate date,LocalTime fromTime,LocalTime toTime) throws AttendanceException,EmployeeException;
	
	Attendance getAttendanceByAttendanceId(Integer attendanceId) throws AttendanceException;

	List<DayAttendanceDto> getAttendanceByMonthAndEmployeeId(Integer month, Integer year, Integer employeeId)throws AttendanceException,EmployeeException, LeaveException;

	
}

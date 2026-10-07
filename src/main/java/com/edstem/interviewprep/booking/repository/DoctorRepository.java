package com.edstem.interviewprep.booking.repository;

import com.edstem.interviewprep.booking.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {}

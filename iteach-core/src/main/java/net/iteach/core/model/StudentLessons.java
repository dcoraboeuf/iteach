package net.iteach.core.model;

import java.math.BigDecimal;
import java.util.List;

import org.joda.time.LocalDate;

import lombok.Data;

@Data
public class StudentLessons {
	
	private final LocalDate date;
	private final List<StudentLesson> lessons;
	private final BigDecimal hours;

}

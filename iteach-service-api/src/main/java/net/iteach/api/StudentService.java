package net.iteach.api;

import net.iteach.core.model.Ack;
import net.iteach.core.model.ID;
import net.iteach.core.model.LessonForm;
import net.iteach.core.model.StudentForm;
import net.iteach.core.model.StudentSummaries;

public interface StudentService {

	StudentSummaries getStudentsForTeacher(int teacherId);

	ID createStudentForTeacher(int teacherId, StudentForm form);

	Ack deleteStudentForTeacher(int teacherId, int id);

	Ack editStudentForTeacher(int userId, int id, StudentForm form);

	ID createLessonForTeacher(int userId, LessonForm form);

}

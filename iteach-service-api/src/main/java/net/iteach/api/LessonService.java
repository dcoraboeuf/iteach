package net.iteach.api;

import net.iteach.core.model.ID;
import net.iteach.core.model.LessonForm;
import net.iteach.core.model.LessonRange;
import net.iteach.core.model.Lessons;

public interface LessonService {

	Lessons getLessonsForTeacher(int userId, LessonRange range);

	ID createLessonForTeacher(int userId, LessonForm form);

}
package net.iteach.service.impl;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import net.iteach.api.CommentsService;
import net.iteach.api.CoordinatesService;
import net.iteach.api.TeacherService;
import net.iteach.api.model.CommentEntity;
import net.iteach.api.model.CoordinateEntity;
import net.iteach.core.model.*;
import net.iteach.core.validation.LessonFormValidation;
import net.iteach.core.validation.SchoolFormValidation;
import net.iteach.core.validation.StudentFormValidation;
import net.iteach.service.dao.LessonDao;
import net.iteach.service.dao.SchoolDao;
import net.iteach.service.dao.StudentDao;
import net.iteach.service.dao.model.TLesson;
import net.iteach.service.dao.model.TSchool;
import net.iteach.service.dao.model.TStudent;
import net.iteach.service.db.SQLUtils;
import net.sf.jstring.LocalizableMessage;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Validator;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class TeacherServiceImpl extends AbstractServiceImpl implements
        TeacherService {

    private final CoordinatesService coordinatesService;
    private final CommentsService commentsService;
    private final LessonDao lessonDao;
    private final StudentDao studentDao;
    private final SchoolDao schoolDao;

    private final Function<TSchool, SchoolSummary> schoolSummaryFunction = new Function<TSchool, SchoolSummary>() {
        @Override
        public SchoolSummary apply(TSchool t) {
            return new SchoolSummary(
                    t.getId(),
                    t.getName(),
                    t.getColor(),
                    t.getHourlyRate()
            );
        }
    };
    private final Function<TStudent, StudentSummary> studentSummaryFunction = new Function<TStudent, StudentSummary>() {
        @Override
        public StudentSummary apply(TStudent t) {
            return new StudentSummary(
                    t.getId(),
                    t.getSubject(),
                    t.getName(),
                    getSchoolSummary(t.getSchool()),
                    t.isDisabled()
            );
        }
    };

    @Autowired
    public TeacherServiceImpl(Validator validator, CoordinatesService coordinatesService, CommentsService commentsService, LessonDao lessonDao, StudentDao studentDao, SchoolDao schoolDao) {
        super(validator);
        this.coordinatesService = coordinatesService;
        this.commentsService = commentsService;
        this.lessonDao = lessonDao;
        this.studentDao = studentDao;
        this.schoolDao = schoolDao;
    }

    protected void checkTeacherForSchool(int userId, int id) {
        if (!schoolDao.doesSchoolBelongToTeacher(id, userId)) {
            throw new AccessDeniedException(String.format("User %d cannot access school %d", userId, id));
        }
    }

    protected void checkTeacherForStudent(int userId, int id) {
        if (!studentDao.doesStudentBelongToTeacher(id, userId)) {
            throw new AccessDeniedException(String.format("User %d cannot access student %d", userId, id));
        }
    }

    protected void checkTeacherForLesson(int userId, int id) {
        if (!lessonDao.doesLessonBelongToTeacher(id, userId)) {
            throw new AccessDeniedException(String.format("User %d cannot access lesson %d", userId, id));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolSummaries getSchoolsForTeacher(int teacherId) {
        return new SchoolSummaries(
                Lists.transform(
                        schoolDao.findSchoolsByTeacher(teacherId),
                        schoolSummaryFunction)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolDetails getSchoolForTeacher(final int userId, final int id) {
        // Check for the associated teacher
        checkTeacherForSchool(userId, id);

        List<TStudent> studentsForSchool = studentDao.findStudentsBySchool(id);
        final List<SchoolDetailsStudent> students = Lists.transform(
                studentsForSchool,
                new Function<TStudent, SchoolDetailsStudent>() {
                    @Override
                    public SchoolDetailsStudent apply(TStudent t) {
                        return new SchoolDetailsStudent(
                                t.getId(),
                                t.getName(),
                                t.getSubject(),
                                t.isDisabled(),
                                getStudentHours(userId, t.getId())
                        );
                    }
                }
        );
        // Total hours
        final AtomicReference<BigDecimal> totalHours = new AtomicReference<>(BigDecimal.ZERO);
        for (SchoolDetailsStudent student : students) {
            totalHours.set(totalHours.get().add(student.getHours()));
        }
        // Details
        TSchool school = schoolDao.getSchoolById(id);
        return new SchoolDetails(
                school.getId(),
                school.getName(),
                school.getColor(),
                school.getHourlyRate(),
                coordinatesService.getCoordinates(CoordinateEntity.SCHOOL, id),
                students,
                totalHours.get()
        );
    }

    @Override
    @Transactional
    public ID createSchoolForTeacher(int teacherId, SchoolForm form) {
        validate(form, SchoolFormValidation.class);
        ID id = schoolDao.createSchool(teacherId, form.getName(), form.getColor(), form.getHourlyRate());
        // Coordinates
        if (id.isSuccess()) {
            coordinatesService.setCoordinates(CoordinateEntity.SCHOOL, id.getValue(), form.getCoordinates());
        }
        // OK
        return id;
    }

    @Override
    @Transactional
    public Ack deleteSchoolForTeacher(int teacherId, int id) {
        // Check for the associated teacher
        checkTeacherForSchool(teacherId, id);
        // Update
        return schoolDao.deleteSchool(id);
    }

    @Override
    @Transactional
    public Ack editSchoolForTeacher(int userId, int id, SchoolForm form) {
        // Check for the associated teacher
        checkTeacherForSchool(userId, id);
        // Form validation
        validate(form, SchoolFormValidation.class);
        // Query
        Ack ack = schoolDao.updateSchool(id, form.getName(), form.getColor(), form.getHourlyRate());
        // Coordinates
        if (ack.isSuccess()) {
            coordinatesService.setCoordinates(CoordinateEntity.SCHOOL, id, form.getCoordinates());
        }
        // OK
        return ack;
    }

    @Override
    @Transactional(readOnly = true)
    public Coordinates getSchoolCoordinates(int userId, int id) {
        // Check for the associated teacher
        checkTeacherForSchool(userId, id);
        // OK
        return coordinatesService.getCoordinates(CoordinateEntity.SCHOOL, id);
    }

    @Override
    @Transactional(readOnly = true)
    public Comments getSchoolComments(int userId, int schoolId, int offset, int count, int maxlength, CommentFormat format) {
        // Check for the associated teacher
        checkTeacherForSchool(userId, schoolId);
        // Gets the comments
        return commentsService.getComments(CommentEntity.SCHOOL, schoolId, offset, count, maxlength, format);
    }

    @Override
    @Transactional(readOnly = true)
    public Comment getSchoolComment(int userId, int schoolId, int commentId, CommentFormat format) {
        // Check for the associated teacher
        checkTeacherForSchool(userId, schoolId);
        // Gets the comment
        return commentsService.getComment(CommentEntity.SCHOOL, schoolId, commentId, format);
    }

    @Override
    @Transactional
    public Comment editSchoolComment(int userId, int schoolId, CommentFormat format, CommentsForm form) {
        // Check for the associated teacher
        checkTeacherForSchool(userId, schoolId);
        // Creates the comment
        return commentsService.editComment(CommentEntity.SCHOOL, schoolId, format, form);
    }

    @Override
    @Transactional
    public Ack deleteSchoolComment(int userId, int schoolId, int commentId) {
        // Check for the associated teacher
        checkTeacherForSchool(userId, schoolId);
        // Deletes the comment
        return commentsService.deleteComment(CommentEntity.SCHOOL, schoolId, commentId);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentSummaries getStudentsForTeacher(int teacherId) {
        return new StudentSummaries(
                Lists.transform(
                        studentDao.findStudentsByTeacher(teacherId),
                        studentSummaryFunction
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public StudentDetails getStudentForTeacher(int userId, final int id) {
        // Check for the associated teacher
        checkTeacherForStudent(userId, id);
        // Total hours
        final BigDecimal studentHours = getStudentHours(userId, id);
        // Details
        TStudent student = studentDao.getStudentById(id);
        return new StudentDetails(
                student.getId(),
                student.getSubject(),
                student.getName(),
                coordinatesService.getCoordinates(CoordinateEntity.STUDENT, id),
                getSchoolSummary(student.getSchool()),
                studentHours,
                student.isDisabled()
        );
    }

    private SchoolSummary getSchoolSummary(int school) {
        return schoolSummaryFunction.apply(schoolDao.getSchoolById(school));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getStudentHours(int userId, int id) {
        checkTeacherForStudent(userId, id);
        return lessonDao.getHoursForStudent(id);
    }

    @Override
    @Transactional
    public ID createStudentForTeacher(int teacherId, StudentForm form) {
        // Validation
        validate(form, StudentFormValidation.class);
        // Check for the associated teacher
        checkTeacherForSchool(teacherId, form.getSchool());
        // Creation
        return studentDao.createStudent(form.getName(), form.getSchool(), form.getSubject());
    }

    @Override
    @Transactional
    public Ack deleteStudentForTeacher(int teacherId, int id) {
        // Check for the associated teacher
        checkTeacherForStudent(teacherId, id);
        // Deletion
        return studentDao.deleteStudent(id);
    }

    @Override
    @Transactional
    public Ack disableStudentForTeacher(int teacherId, int id) {
        // Check for the associated teacher
        checkTeacherForStudent(teacherId, id);
        // Update
        return studentDao.disableStudent(id);
    }

    @Override
    @Transactional
    public Ack enableStudentForTeacher(int teacherId, int id) {
        // Check for the associated teacher
        checkTeacherForStudent(teacherId, id);
        // Update
        return studentDao.enableStudent(id);
    }

    @Override
    @Transactional
    public Ack editStudentForTeacher(int userId, int id, StudentForm form) {
        // Check for the associated teacher
        checkTeacherForStudent(userId, id);
        // Validation
        validate(form, StudentFormValidation.class);
        // Update
        Ack ack = studentDao.updateStudent(
                id,
                form.getName(),
                form.getSchool(),
                form.getSubject()
        );
        // Coordinates
        if (ack.isSuccess()) {
            coordinatesService.setCoordinates(CoordinateEntity.STUDENT, id, form.getCoordinates());
        }
        // OK
        return ack;
    }

    @Override
    @Transactional(readOnly = true)
    public Coordinates getStudentCoordinates(int userId, int id) {
        // Check for the associated teacher
        checkTeacherForStudent(userId, id);
        // Gets the coordinates
        return coordinatesService.getCoordinates(CoordinateEntity.STUDENT, id);
    }

    @Override
    @Transactional(readOnly = true)
    public Comments getStudentComments(int userId, int studentId, int offset, int count, int maxlength, CommentFormat format) {
        // Check for the associated teacher
        checkTeacherForStudent(userId, studentId);
        // Gets the comments
        return commentsService.getComments(CommentEntity.STUDENT, studentId, offset, count, maxlength, format);
    }

    @Override
    @Transactional(readOnly = true)
    public Comment getStudentComment(int userId, int studentId, int commentId, CommentFormat format) {
        // Check for the associated teacher
        checkTeacherForStudent(userId, studentId);
        // Gets the comment
        return commentsService.getComment(CommentEntity.STUDENT, studentId, commentId, format);
    }

    @Override
    @Transactional
    public Comment editStudentComment(int userId, int studentId, CommentFormat format, CommentsForm form) {
        // Check for the associated teacher
        checkTeacherForStudent(userId, studentId);
        // Creates the comment
        return commentsService.editComment(CommentEntity.STUDENT, studentId, format, form);
    }

    @Override
    @Transactional
    public Ack deleteStudentComment(int userId, int studentId, int commentId) {
        // Check for the associated teacher
        checkTeacherForStudent(userId, studentId);
        // Deletes the comment
        return commentsService.deleteComment(CommentEntity.STUDENT, studentId, commentId);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentLessons getLessonsForStudent(int userId, int id, LocalDate date, Locale locale) {
        // Check for the associated teacher
        checkTeacherForStudent(userId, id);
        // From: first day of the month
        LocalDate from = date.withDayOfMonth(1);
        // To: last day of the month
        LocalDate to = date.withDayOfMonth(date.dayOfMonth().getMaximumValue());
        // Localization
        final DateTimeFormatter dateFormat = DateTimeFormat.mediumDate().withLocale(locale);
        final DateTimeFormatter timeFormat = DateTimeFormat.shortTime().withLocale(locale);
        // All lessons
        List<StudentLesson> lessons = Lists.transform(
                lessonDao.findLessonsForStudent(id, from, to),
                new Function<TLesson, StudentLesson>() {

                    @Override
                    public StudentLesson apply(TLesson t) {
                        // Localization
                        String sDate = dateFormat.print(t.getDate());
                        String sFrom = timeFormat.print(t.getFrom());
                        String sTo = timeFormat.print(t.getTo());
                        // OK
                        return new StudentLesson(
                                t.getId(),
                                t.getDate(),
                                t.getFrom(),
                                t.getTo(),
                                t.getLocation(),
                                sDate,
                                sFrom,
                                sTo
                        );
                    }
                }
        );
        // Total hours
        BigDecimal hours = BigDecimal.ZERO;
        for (StudentLesson lesson : lessons) {
            hours = hours.add(SQLUtils.getHours(lesson.getFrom(), lesson.getTo()));
        }
        // OK
        return new StudentLessons(
                date,
                lessons,
                hours
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Lessons getLessonsForTeacher(int userId, LessonRange range) {
        return new Lessons(
                Lists.transform(lessonDao.findLessonsForTeacher(userId, range.getFrom(), range.getTo()),
                        new Function<TLesson, Lesson>() {
                            @Override
                            public Lesson apply(TLesson t) {
                                return new Lesson(
                                        t.getId(),
                                        getStudentSummary(t.getStudent()),
                                        t.getDate(),
                                        t.getFrom(),
                                        t.getTo(),
                                        t.getLocation()
                                );
                            }
                        }
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public StudentSummary getStudentSummary(int studentId) {
        return studentSummaryFunction.apply(studentDao.getStudentById(studentId));
    }

    @Override
    @Transactional(readOnly = true)
    public LessonDetails getLessonDetails(int userId, int id) {
        // Check for the associated teacher
        checkTeacherForLesson(userId, id);
        // The lesson
        TLesson t = lessonDao.getLessonById(id);
        // Student information
        int studentId = t.getStudent();
        StudentSummary studentSummary = getStudentSummary(studentId);
        StudentSummaryWithCoordinates studentSummaryWithCoordinates = new StudentSummaryWithCoordinates(
                studentSummary,
                new SchoolSummaryWithCoordinates(
                        studentSummary.getSchool(),
                        getSchoolCoordinates(userId, studentSummary.getSchool().getId())
                ),
                getStudentCoordinates(userId, studentId)
        );
        return new LessonDetails(
                id,
                studentSummaryWithCoordinates,
                t.getDate(),
                t.getFrom(),
                t.getTo(),
                t.getLocation()
        );
    }

    @Override
    @Transactional
    public ID createLessonForTeacher(int userId, LessonForm form) {
        // Validation
        validate(form, LessonFormValidation.class);
        validate(form.getTo().isAfter(form.getFrom()), new LocalizableMessage("lesson.error.timeorder"));
        checkTeacherForStudent(userId, form.getStudent());
        // Creation
        return lessonDao.createLesson(
                form.getStudent(),
                form.getLocation(),
                form.getDate(),
                form.getFrom(),
                form.getTo()
        );
    }

    @Override
    @Transactional
    public Ack editLessonForTeacher(int userId, int id, LessonForm form) {
        // Validation
        validate(form, LessonFormValidation.class);
        checkTeacherForLesson(userId, id);
        // Update
        return lessonDao.updateLesson(
                id,
                form.getStudent(),
                form.getLocation(),
                form.getDate(),
                form.getFrom(),
                form.getTo()
        );
    }

    @Override
    @Transactional
    public Ack deleteLessonForTeacher(int teacherId, int id) {
        checkTeacherForLesson(teacherId, id);
        return lessonDao.deleteLesson(id);
    }

    protected Ack changeLessonRange(int userId, int lessonId, Function<LessonRange, LessonRange> changeFn) {
        // Check for the associated teacher
        checkTeacherForLesson(userId, lessonId);
        // Loads the lesson range
        final LessonRange range = lessonDao.getLessonRange(lessonId);
        // Adjust the range
        LessonRange newRange = changeFn.apply(range);
        // Updates the period
        return lessonDao.setLessonRange(lessonId,
                newRange.getFrom().toLocalDate(),
                newRange.getFrom().toLocalTime(),
                newRange.getTo().toLocalTime());
    }

    @Override
    @Transactional
    public Ack changeLessonForTeacher(int userId, int lessonId, final LessonChange change) {
        return changeLessonRange(userId, lessonId, new Function<LessonRange, LessonRange>() {
            @Override
            public LessonRange apply(final LessonRange range) {
                // Adjust the range
                LocalDateTime from = range.getFrom();
                LocalDateTime to = range.getTo();
                // Days?
                int dayDelta = change.getDayDelta();
                if (dayDelta != 0) {
                    // Shifts both dates
                    from = from.plusDays(dayDelta);
                    to = to.plusDays(dayDelta);
                }
                // Minutes
                int minuteDelta = change.getMinuteDelta();
                if (minuteDelta != 0) {
                    // Shifts only the end
                    to = to.plusMinutes(minuteDelta);
                }
                // Redefines the lesson range
                return new LessonRange(from, to);
            }
        });
    }

    @Override
    @Transactional
    public Ack moveLessonForTeacher(int userId, int lessonId, final LessonChange change) {
        return changeLessonRange(userId, lessonId, new Function<LessonRange, LessonRange>() {
            @Override
            public LessonRange apply(final LessonRange range) {
                // Adjust the range
                LocalDateTime from = range.getFrom();
                LocalDateTime to = range.getTo();
                // Days?
                int dayDelta = change.getDayDelta();
                if (dayDelta != 0) {
                    // Shifts both dates
                    from = from.plusDays(dayDelta);
                    to = to.plusDays(dayDelta);
                }
                // Minutes
                int minuteDelta = change.getMinuteDelta();
                if (minuteDelta != 0) {
                    // Shifts both times
                    from = from.plusMinutes(minuteDelta);
                    to = to.plusMinutes(minuteDelta);
                }
                // Redefines the lesson range
                return new LessonRange(from, to);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Comments getLessonComments(int userId, int lessonId, int offset, int count, int maxlength, CommentFormat format) {
        // Check for the associated teacher
        checkTeacherForLesson(userId, lessonId);
        // Gets the comments
        return commentsService.getComments(CommentEntity.LESSON, lessonId, offset, count, maxlength, format);
    }

    @Override
    @Transactional(readOnly = true)
    public Comment getLessonComment(int userId, int lessonId, int commentId, CommentFormat format) {
        // Check for the associated teacher
        checkTeacherForLesson(userId, lessonId);
        // Gets the comment
        return commentsService.getComment(CommentEntity.LESSON, lessonId, commentId, format);
    }

    @Override
    @Transactional
    public Comment editLessonComment(int userId, int lessonId, CommentFormat format, CommentsForm form) {
        // Check for the associated teacher
        checkTeacherForLesson(userId, lessonId);
        // Creates the comment
        return commentsService.editComment(CommentEntity.LESSON, lessonId, format, form);
    }

    @Override
    @Transactional
    public Ack deleteLessonComment(int userId, int lessonId, int commentId) {
        // Check for the associated teacher
        checkTeacherForLesson(userId, lessonId);
        // Deletes the comment
        return commentsService.deleteComment(CommentEntity.LESSON, lessonId, commentId);
    }

}

package net.iteach.service.db;

public interface SQL {
	
	// Users
	
	String USER_CREATE = "INSERT INTO USERS (MODE, IDENTIFIER, PASSWORD, EMAIL, FIRSTNAME, LASTNAME) VALUES (:mode, :identifier, :password, :email, :firstName, :lastName)";

	String USER_BY_OPENID = "SELECT ID, EMAIL, FIRSTNAME, LASTNAME FROM USERS WHERE MODE = 'openid' AND IDENTIFIER = :identifier";

	String USER_BY_PASSWORD = "SELECT ID, PASSWORD, EMAIL, FIRSTNAME, LASTNAME FROM USERS WHERE MODE = 'password' AND IDENTIFIER = :identifier";
	
	// Schools
	
	String SCHOOLS_FOR_TEACHER = "SELECT * FROM SCHOOLS WHERE TEACHER = :teacher ORDER BY NAME";

	String SCHOOL_CREATE = "INSERT INTO SCHOOLS (TEACHER, NAME, COLOR) VALUES (:teacher, :name, :color)";

	String SCHOOL_DELETE = "DELETE FROM SCHOOLS WHERE TEACHER = :teacher AND ID = :id";

	String SCHOOL_UPDATE = "UPDATE SCHOOLS SET NAME = :name, COLOR = :color WHERE TEACHER = :teacher AND ID = :id";
	
	// Students
	
	String STUDENTS_FOR_TEACHER = "SELECT S.*, H.ID AS SCHOOL_ID, H.NAME AS SCHOOL_NAME, H.COLOR AS SCHOOL_COLOR FROM STUDENTS S INNER JOIN SCHOOLS H ON S.SCHOOL = H.ID WHERE H.TEACHER = :teacher ORDER BY S.NAME";

	String STUDENT_CREATE = "INSERT INTO STUDENTS (SCHOOL, SUBJECT, NAME) VALUES (:school, :subject, :name)";

	String STUDENT_DELETE = "DELETE FROM STUDENTS WHERE ID = :id";

	String STUDENT_UPDATE = "UPDATE STUDENTS SET SCHOOL = :school, SUBJECT = :subject, NAME = :name WHERE ID = :id";
	
	// Lessons

	String LESSON_CREATE = "INSERT INTO LESSONS (STUDENT, PDATE, PFROM, PTO, LOCATION) VALUES (:student, :date, :from, :to, :location)";

}

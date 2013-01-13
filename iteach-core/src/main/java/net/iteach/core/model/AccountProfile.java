package net.iteach.core.model;

import lombok.Data;

@Data
public class AccountProfile {

	private final int id;
	private final String firstName;
	private final String lastName;
	private final String email;
	private final boolean admin;
	
	private final int schoolCount;
	private final int studentCount;
	private final int lessonCount;

}

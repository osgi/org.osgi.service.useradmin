/*******************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0 
 *******************************************************************************/
/**
 * OSGi Test Suite Implementation. OSGi Confidential.
 */
package org.osgi.test.cases.useradmin.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminListener;

public class UserAdminControl extends UserAdminTestSupport {


	String				testMethodName;

	@BeforeEach
	void testMethodName(TestInfo testInfo) {
		testMethodName = testInfo.getTestMethod().get().getName();
	}
	private UserAdmin	useradmin;
	private List<Role>	rolesToKeep;

	@BeforeEach
	public void setUp() throws Exception {
		useradmin = getService(UserAdmin.class);
		rolesToKeep = asList(useradmin.getRoles(null));
	}

	@AfterEach
	public void tearDown() throws Exception {
		List<Role> roles = new ArrayList<>(asList(useradmin.getRoles(null)));
		roles.removeAll(rolesToKeep);
		for (Iterator<Role> iter = roles.iterator(); iter.hasNext();) {
			Role role = iter.next();
			useradmin.removeRole(role.getName());
		}
		ungetService(useradmin);
	}

	private List<Role> asList(Role[] roles) {
		if (roles == null) {
			return Collections.emptyList();
		}
		return Arrays.asList(roles);
	}

	@Test
	public void testCreateRole() throws Exception {
		Role role = null;
		role = useradmin.createRole("foo-user", Role.USER);
		assertThat(roleToString(role)).as("incorrect role name").isEqualTo("foo-user");
		role = useradmin.createRole("foo-user", Role.USER);
		assertThat(roleToString(role)).as("incorrect role name").isNull();
		role = useradmin.createRole("foo-group", Role.GROUP);
		assertThat(roleToString(role)).as("incorrect role name").isEqualTo("foo-group");
		try {
			role = useradmin.createRole("foobar", Role.ROLE);
			fail("expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	@Test
	public void testRemoveRole() throws Exception {
		useradmin.createRole("foo-user", Role.USER);
		/* Remove the role once */
		assertThat(useradmin.removeRole("foo-user")).as("role not removed").isTrue();
		/* And try to remove it again */
		assertThat(useradmin.removeRole("foo-user")).as("role removed").isFalse();
	}

	@Test
	public void testGetRole() throws Exception {
		useradmin.createRole("foo1", Role.USER);
		useradmin.createRole("foo2", Role.USER);
		useradmin.createRole("foo3", Role.GROUP);
		useradmin.createRole("foo4", Role.USER);
		assertThat(roleToString(useradmin
				.getRole("foo1"))).as("incorrect role name").isEqualTo("foo1");
		assertThat(roleToString(useradmin
				.getRole("foo2"))).as("incorrect role name").isEqualTo("foo2");
		assertThat(roleToString(useradmin
				.getRole("foo3"))).as("incorrect role name").isEqualTo("foo3");
		assertThat(roleToString(useradmin
				.getRole("foo4"))).as("incorrect role name").isEqualTo("foo4");
		useradmin.removeRole("foo1");
		assertThat(roleToString(useradmin
				.getRole("foo1"))).as("incorrect role name").isNull();
		assertThat(roleToString(useradmin.getRole("user.anyone"))).as("incorrect role name").isEqualTo("user.anyone");
	}

	@Test
	public void testGetRoles() throws Exception {
		Role r1 = useradmin.createRole("role1", Role.GROUP);
		r1.getProperties().put(testMethodName, "r1");
		Role r2 = useradmin.createRole("role2", Role.USER);
		r2.getProperties().put(testMethodName, "r3");
		Role r3 = useradmin.createRole("role3", Role.GROUP);
		r3.getProperties().put(testMethodName, "r3");
		String[] result = sortRoleNames(useradmin.getRoles("(" + testMethodName
				+ "=r*)"));
		assertThat(result.length).as("wrong number of roles").isEqualTo(3);
		assertThat(result[0]).as("wrong role name").isEqualTo("role1");
		assertThat(result[1]).as("wrong role name").isEqualTo("role2");
		assertThat(result[2]).as("wrong role name").isEqualTo("role3");
	}

	@Test
	public void testGetUser() throws Exception {
		Role r1 = useradmin.createRole("user1", Role.USER);
		r1.getProperties().put(testMethodName, "u1");
		r1.getProperties().put("foobar", "xxx");
		Role r2 = useradmin.createRole("user2", Role.USER);
		r2.getProperties().put(testMethodName, "u3");
		Role r3 = useradmin.createRole("user3", Role.USER);
		r3.getProperties().put(testMethodName, "u3");
		r3.getProperties().put("foobar", "xxx");
		assertThat(roleToString(useradmin.getUser(
				"unexisting-key", "foobar"))).as("role not null").isNull();
		assertThat(roleToString(useradmin
				.getUser(testMethodName, "u1"))).as("could not find role").isEqualTo("user1");
		assertThat(roleToString(useradmin.getUser("foobar",
				"xxx"))).as("role not null").isNull();
	}

	@Test
	public void testRoleGetType() throws Exception {
		assertConstant(Integer.valueOf(Role.USER), "USER", Role.class);
		assertConstant(Integer.valueOf(Role.GROUP), "GROUP", Role.class);
		assertConstant(Integer.valueOf(Role.ROLE), "ROLE", Role.class);
		Role role = useradmin.createRole("r1", Role.USER);
		assertThat(Role.USER).isEqualTo(role.getType());
		role = useradmin.createRole("r2", Role.GROUP);
		assertThat(Role.GROUP).isEqualTo(role.getType());
	}

	@Test
	public void testRoleChangeProperties() throws Exception {
		Role role = useradmin.createRole("r1", Role.USER);
		Dictionary<String,Object> p = role.getProperties();
		p.put("testKey", "testValue");
		assertThat((String) role
				.getProperties().get("testKey")).as("property not set").isEqualTo("testValue");
	}

	@Test
	public void testRolePutProperties() throws Exception {
		Role role = useradmin.createRole("r1", Role.USER);
		@SuppressWarnings({
				"rawtypes", "unchecked"
		})
		Dictionary<Object,Object> p = (Dictionary) role.getProperties();
		p.put("testKey", "testValue");
		p.put("key", new byte[] {0, 0, 0});
		try {
			p.put(Integer.valueOf(1), "123");
			fail("expected IllegalArgumentException");
		}
		catch (ClassCastException | IllegalArgumentException e) {
			// expected
		}
	}

	@Test
	public void testRoleGetName() throws Exception {
		assertThat(roleToString(useradmin
				.createRole("r1", Role.GROUP))).as("incorrect role name").isEqualTo("r1");
	}

	@Test
	public void testUserChangeCredentials() throws Exception {
		User user = (User) useradmin.createRole("u1", Role.USER);
		Dictionary<String,Object> cred = user.getCredentials();
		cred.put("testKey", "testValue");
		assertThat((String) user
				.getCredentials().get("testKey")).as("incorrect value").isEqualTo("testValue");
	}

	@Test
	public void testUserPutCredentials() throws Exception {
		User user = (User) useradmin.createRole("u1", Role.USER);
		@SuppressWarnings({
				"rawtypes", "unchecked"
		})
		Dictionary<Object,Object> p = (Dictionary) user.getCredentials();
		p.put("testKey", "testValue");
		p.put("key", new byte[] {0, 0, 0});
		try {
			p.put(Integer.valueOf(1), "123");
			fail("expected IllegalArgumentException");
		}
		catch (ClassCastException | IllegalArgumentException e) {
			// expected
		}
	}

	@Test
	public void testUserHasCredential() throws Exception {
		User user = (User) useradmin.createRole("u1", Role.USER);
		user.getCredentials().put("password", "1234");
		user.getCredentials().put("pwd2", new byte[] {0, 0, 0});
		assertThat(user.hasCredential("password", "1234")).as("missing credential").isTrue();
		assertThat(user
				.hasCredential("xxx", Integer.valueOf(1))).as("credential found that was not present").isFalse();
		assertThat(user.hasCredential("pwd2", new byte[] {
				0, 0, 0})).as("missing credential").isTrue();
	}

	@Test
	public void testGroupAddMember() throws Exception {
		Group group = (Group) useradmin.createRole("group", Role.GROUP);
		Role user = useradmin.createRole("user1", Role.USER);
		assertThat(group.addMember(user)).as("adding member to group failed").isTrue();
		assertThat(group
				.addMember(user)).as("adding existing member to group succeed").isFalse();
	}

	@Test
	public void testGroupAddRequiredMembers() throws Exception {
		Group group = (Group) useradmin.createRole("group", Role.GROUP);
		Role user = useradmin.createRole("user1", Role.USER);
		assertThat(group
				.addRequiredMember(user)).as("adding member to group failed").isTrue();
		assertThat(group
				.addRequiredMember(user)).as("adding existing member to group succeed").isFalse();
	}

	@Test
	public void testGroupGetMembers() throws Exception {
		Group group = (Group) useradmin.createRole("group", Role.GROUP);
		String[] result = sortRoleNames(group.getMembers());
		assertThat(result.length).as("wrong number of roles").isZero();
		Role u1 = useradmin.createRole("user1", Role.USER);
		Role u2 = useradmin.createRole("user2", Role.USER);
		group.addMember(u1);
		group.addMember(u2);
		result = sortRoleNames(group.getMembers());
		assertThat(result.length).as("wrong number of roles").isEqualTo(2);
		assertThat(result[0]).as("wrong role name").isEqualTo("user1");
		assertThat(result[1]).as("wrong role name").isEqualTo("user2");
	}

	@Test
	public void testGroupGetRequiredMembers() throws Exception {
		Group group = (Group) useradmin.createRole("group", Role.GROUP);
		String[] result = sortRoleNames(group.getRequiredMembers());
		assertThat(result.length).as("wrong number of roles").isZero();
		Role u1 = useradmin.createRole("user1", Role.USER);
		Role u2 = useradmin.createRole("user2", Role.USER);
		group.addRequiredMember(u1);
		group.addRequiredMember(u2);
		result = sortRoleNames(group.getRequiredMembers());
		assertThat(result.length).as("wrong number of roles").isEqualTo(2);
		assertThat(result[0]).as("wrong role name").isEqualTo("user1");
		assertThat(result[1]).as("wrong role name").isEqualTo("user2");
	}

	@Test
	public void testGroupRemoveMember() throws Exception {
		Group group = (Group) useradmin.createRole("group", Role.GROUP);
		Role u1 = useradmin.createRole("user1", Role.USER);
		Role u2 = useradmin.createRole("user2", Role.USER);
		group.addMember(u1);
		group.addMember(u2);
		group.removeMember(u2);
		String[] result = sortRoleNames(group.getMembers());
		assertThat(result.length).as("wrong number of roles").isOne();
		assertThat(result[0]).as("wrong role name").isEqualTo("user1");
	}

	@Test
	public void testUserAnyone() {
		Role user1 = useradmin.createRole("user1", Role.USER);
		Group foorole = (Group) useradmin.createRole("foorole", Role.GROUP);
		foorole.addRequiredMember(user1);
		foorole.addMember(useradmin.getRole("user.anyone"));
		Authorization auth = useradmin.getAuthorization((User) user1);
		assertThat(auth.hasRole("foorole")).as("role not present").isTrue();
		String roles[] = auth.getRoles();
		if (roles != null) {
			for (int i = 0; i < roles.length; i++) {
				if (roles[i].equals("user.anyone")) {
					fail("user.anyone found");
				}
			}
		}
	}

	@Test
	public void testBasicRequired() {
		setupRoles();
		User user1 = (User) useradmin.getRole("user1");
		User user2 = (User) useradmin.getRole("user2");
		User user3 = (User) useradmin.getRole("user3");
		Group required = (Group) useradmin.getRole("requiredgroup");
		assertThat(required).isNotNull();
		Group testrole = (Group) useradmin.getRole("testrole");
		assertThat(testrole).isNotNull();
		Authorization auth = useradmin.getAuthorization(user1);
		assertThat(auth.hasRole("testrole")).as("hasRole testrole").isTrue();
		auth = useradmin.getAuthorization(user2);
		assertThat(auth.hasRole("testrole")).as("hasRole testrole").isTrue();
		auth = useradmin.getAuthorization(user3);
		assertThat(auth.hasRole("testrole")).as("hasRole testrole").isFalse();
	}

	@Test
	public void testRequiredAuth() {
		setupRoles();
		User user1 = (User) useradmin.getRole("user1");
		User user2 = (User) useradmin.getRole("user2");
		User user3 = (User) useradmin.getRole("user3");
		Group required = (Group) useradmin.getRole("requiredgroup");
		assertThat(required).isNotNull();
		Group testrole = (Group) useradmin.getRole("testrole");
		assertThat(testrole).isNotNull();
		testrole.removeMember(user1);
		testrole.removeMember(user2);
		testrole.removeMember(user3);
		testrole.removeMember(useradmin.getRole("user.anyone"));
		Authorization auth = useradmin.getAuthorization(user1);
		assertThat(auth.hasRole("testrole")).as("hasRole testrole").isFalse();
	}

	@Test
	public void testUserAnyoneBasic() {
		setupRoles();
		User user1 = (User) useradmin.getRole("user1");
		User user2 = (User) useradmin.getRole("user2");
		User user3 = (User) useradmin.getRole("user3");
		User user4 = (User) useradmin.getRole("user4");
		Group required = (Group) useradmin.getRole("requiredgroup");
		Group testrole = (Group) useradmin.getRole("testrole");
		testrole.removeMember(user1);
		testrole.removeMember(user2);
		testrole.removeMember(user3);
		Role anyone = useradmin.getRole("user.anyone");
		required.removeMember(useradmin.getRole("user.anyone"));
		required.removeMember(user3);
		testrole.addMember(anyone);
		Authorization auth = useradmin.getAuthorization(user1);
		assertThat(auth.hasRole("testrole")).as("hasRole testrole").isTrue();
		auth = useradmin.getAuthorization(user2);
		assertThat(auth.hasRole("testrole")).as("hasRole testrole").isTrue();
		auth = useradmin.getAuthorization(user3);
		assertThat(auth.hasRole("testrole")).as("hasRole testrole").isFalse();
		auth = useradmin.getAuthorization(user4);
		assertThat(auth.hasRole("testrole")).as("hasRole testrole").isFalse();
	}

	@Test
	public void testUserAnyoneRequired() {
		setupRoles();
		User user1 = (User) useradmin.getRole("user1");
		User user2 = (User) useradmin.getRole("user2");
		User user3 = (User) useradmin.getRole("user3");
		User user4 = (User) useradmin.getRole("user4");
		Group required = (Group) useradmin.getRole("requiredgroup");
		Group testrole = (Group) useradmin.getRole("testrole");
		required.removeMember(user1);
		required.removeMember(user2);
		required.removeMember(user3);
		required.addMember(useradmin.getRole("user.anyone"));
		testrole.removeMember(user2);
		testrole.removeMember(user3);
		testrole.addMember(user4);
		Authorization auth = useradmin.getAuthorization(user1);
		assertThat(auth.hasRole("testrole")).as("hasRole testrole").isTrue();
		auth = useradmin.getAuthorization(user2);
		assertThat(auth.hasRole("testrole")).as("hasRole testrole").isFalse();
		auth = useradmin.getAuthorization(user3);
		assertThat(auth.hasRole("testrole")).as("hasRole testrole").isFalse();
		auth = useradmin.getAuthorization(user4);
		assertThat(auth.hasRole("testrole")).as("hasRole testrole").isTrue();
	}

	@Test
	public void testAnonymousUser() {
		setupRoles();
		Authorization auth = useradmin.getAuthorization(null);
		assertThat(auth.getName()).as("anonymous user").isNull();
		useradmin.removeRole("anygroup");
		assertThat(auth.hasRole("anygroup")).as("hasRole anygroup").isFalse();
		Group anygroup = (Group) useradmin.createRole("anygroup", Role.GROUP);
		anygroup.addMember(useradmin.getRole("user.anyone"));
		anygroup.addRequiredMember(useradmin.getRole("user.anyone"));
		assertThat(auth.hasRole("anygroup")).as("hasRole anygroup").isTrue();
		assertThat(auth.hasRole("requiredgroup")).as("hasRole requiredgroup").isFalse();
		assertThat(auth.hasRole("user.anyone")).as("hasRole user.anyone").isTrue();
	}

	@Test
	public void testGroupInGroup() {
		User user1 = (User) useradmin.createRole("user1", Role.USER);
		Group group1 = (Group) useradmin.createRole("group1", Role.GROUP);
		assertThat(group1.addRequiredMember(group1)).isTrue();
		assertThat(group1.addMember(user1)).isTrue();
		Authorization auth = useradmin.getAuthorization(user1);
		assertThat(auth.hasRole("group1")).as("hasRole group1").isFalse();
	}

	@Test
	public void testUserAdminEvents() throws Exception {
		UserAdminEventCollector uaec = new UserAdminEventCollector(
				UserAdminEvent.ROLE_CREATED | UserAdminEvent.ROLE_CHANGED
						| UserAdminEvent.ROLE_REMOVED);
		ServiceRegistration<UserAdminListener> sr = context
				.registerService(UserAdminListener.class, uaec, null);
		try {
			/* Create a new role */
			useradmin.createRole("user1", Role.USER);
			List<UserAdminEvent> events = uaec.getList(1,
					1000 * OSGiTestCaseProperties
					.getScaling());
			assertThat(events).as("missing event").hasSize(1);
			UserAdminEvent event = events.get(0);
			assertThat(event
					.getType()).as("wrong event type").isEqualTo(UserAdminEvent.ROLE_CREATED);
			assertThat(roleToString(event
					.getRole())).as("wrong event role").isEqualTo("user1");

			/* Change a role */
			Role role = useradmin.getRole("user1");
			role.getProperties().put("newKey", "xxxxx");
			events = uaec
					.getList(1, 1000 * OSGiTestCaseProperties.getScaling());
			assertThat(events).as("missing event").hasSize(1);
			event = events.get(0);
			assertThat(event
					.getType()).as("wrong event type").isEqualTo(UserAdminEvent.ROLE_CHANGED);
			assertThat(roleToString(event
					.getRole())).as("wrong event role").isEqualTo("user1");

			/* Remove a role */
			useradmin.removeRole("user1");
			events = uaec
					.getList(1, 1000 * OSGiTestCaseProperties.getScaling());
			assertThat(events).as("missing event").hasSize(1);
			event = events.get(0);
			assertThat(event
					.getType()).as("wrong event type").isEqualTo(UserAdminEvent.ROLE_REMOVED);
			assertThat(roleToString(event
					.getRole())).as("wrong event role").isEqualTo("user1");
		}
		finally {
			sr.unregister();
		}
	}

	/** *** Helper methods **** */
	private void setupRoles() {
		useradmin.createRole("user1", Role.USER);
		useradmin.createRole("user2", Role.USER);
		useradmin.createRole("user3", Role.USER);
		useradmin.createRole("user4", Role.USER);
		useradmin.createRole("testrole", Role.GROUP);
		useradmin.createRole("requiredgroup", Role.GROUP);
		User user1 = (User) useradmin.getRole("user1");
		User user2 = (User) useradmin.getRole("user2");
		User user3 = (User) useradmin.getRole("user3");
		Group required = (Group) useradmin.getRole("requiredgroup");
		Group testrole = (Group) useradmin.getRole("testrole");
		required.addMember(user1);
		required.addMember(user2);
		required.addMember(user3);
		testrole.addRequiredMember(required);
		testrole.addMember(user1);
		testrole.addMember(user2);
	}

	private String roleToString(Role role) {
		String roleString = null;
		if (role != null) {
			roleString = role.getName();
		}
		return roleString;
	}

	private String[] sortRoleNames(Role[] roles) {
		if (roles == null) {
			return new String[] {};
		}
		String[] roleStrings = new String[roles.length];
		/* Get name of every role */
		for (int i = 0; i < roles.length; i++) {
			roleStrings[i] = roleToString(roles[i]);
		}
		/*
		 * The order of the roles isn't specified, so we better sort them.
		 */
		Arrays.sort(roleStrings);
		return roleStrings;
	}
}

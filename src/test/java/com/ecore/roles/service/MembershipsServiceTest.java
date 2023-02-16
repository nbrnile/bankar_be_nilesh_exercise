package com.ecore.roles.service;

import com.ecore.roles.exception.InvalidArgumentException;
import com.ecore.roles.exception.ResourceExistsException;
import com.ecore.roles.model.Membership;
import com.ecore.roles.model.Role;
import com.ecore.roles.repository.MembershipRepository;
import com.ecore.roles.repository.RoleRepository;
import com.ecore.roles.service.impl.MembershipsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityNotFoundException;
import java.util.Optional;
import java.util.UUID;

import static com.ecore.roles.utils.TestData.DEFAULT_MEMBERSHIP;
import static com.ecore.roles.utils.TestData.DEVELOPER_ROLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MembershipsServiceTest {

    @InjectMocks
    private MembershipsServiceImpl membershipsService;
    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UsersService usersService;
    @Mock
    private TeamsService teamsService;

    @Test
    public void shouldCreateMembership() {
        Membership expectedMembership = DEFAULT_MEMBERSHIP();
        when(roleRepository.findById(expectedMembership.getRole().getId()))
                .thenReturn(Optional.ofNullable(DEVELOPER_ROLE()));
        when(membershipRepository.findByUserIdAndTeamId(expectedMembership.getUserId(),
                expectedMembership.getTeamId()))
                        .thenReturn(Optional.empty());
        when(membershipRepository
                .save(expectedMembership))
                        .thenReturn(expectedMembership);

        Membership actualMembership = membershipsService.assignRoleToMembership(expectedMembership);

        assertNotNull(actualMembership);
        assertEquals(actualMembership, expectedMembership);
        verify(roleRepository).findById(expectedMembership.getRole().getId());
    }

    @Test
    public void shouldFailToCreateMembershipWhenMembershipsIsNull() {
        assertThrows(NullPointerException.class,
                () -> membershipsService.assignRoleToMembership(null));
    }

    @Test
    public void shouldFailToCreateMembershipWhenItExists() {
        Membership expectedMembership = DEFAULT_MEMBERSHIP();
        when(membershipRepository.findByUserIdAndTeamId(expectedMembership.getUserId(),
                expectedMembership.getTeamId()))
                        .thenReturn(Optional.of(expectedMembership));

        ResourceExistsException exception = assertThrows(ResourceExistsException.class,
                () -> membershipsService.assignRoleToMembership(expectedMembership));

        assertEquals("Membership already exists", exception.getMessage());
        verify(roleRepository, times(0)).getById(any());
        verify(usersService, times(0)).getUser(any());
        verify(teamsService, times(0)).getTeam(any());
    }

    @Test
    public void shouldFailToCreateMembershipWhenItHasInvalidRole() {
        Membership expectedMembership = DEFAULT_MEMBERSHIP();
        expectedMembership.setRole(null);

        InvalidArgumentException exception = assertThrows(InvalidArgumentException.class,
                () -> membershipsService.assignRoleToMembership(expectedMembership));

        assertEquals("Invalid 'Role' object", exception.getMessage());
        verify(membershipRepository, times(0)).findByUserIdAndTeamId(any(), any());
        verify(roleRepository, times(0)).getById(any());
        verify(usersService, times(0)).getUser(any());
        verify(teamsService, times(0)).getTeam(any());
    }

    @Test
    public void shouldFailToGetMembershipsWhenRoleIdIsNull() {
        assertThrows(NullPointerException.class,
                () -> membershipsService.getMemberships(null));
    }

    @Test
    public void testGetRoleWithDatabaseException() {
        // Create input values for the method
        UUID userId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();

        // Mock the repository to throw a runtime exception
        when(membershipRepository.findByUserIdAndTeamId(userId, teamId))
                .thenThrow(new RuntimeException("Database error"));

        // Call the method and verify the exception message
        Exception exception = assertThrows(RuntimeException.class, () -> {
            membershipsService.getRole(userId, teamId);
        });
        assertEquals("Database error", exception.getMessage());
    }

    @Test
    public void testGetRoleWithNonExistentMembership() {
        // Create a mock membership object without a role
        UUID userId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        Membership membership = new Membership();
        membership.setUserId(userId);
        membership.setTeamId(teamId);

        // Mock the repository to return an empty optional
        when(membershipRepository.findByUserIdAndTeamId(userId, teamId))
                .thenReturn(Optional.empty());

        // Call the method and verify the exception message
        Exception exception = assertThrows(EntityNotFoundException.class, () -> {
            membershipsService.getRole(userId, teamId);
        });
        assertEquals("Team %s not found", exception.getMessage());
    }

    @Test
    public void testGetRoleWithValidMembership() {
        // Create a mock membership object with a role
        UUID userId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setName("Admin");
        Membership membership = new Membership();
        membership.setUserId(userId);
        membership.setTeamId(teamId);
        membership.setRole(role);

        // Mock the repository to return the membership
        when(membershipRepository.findByUserIdAndTeamId(userId, teamId))
                .thenReturn(Optional.of(membership));

        // Call the method and verify the returned role
        Role result = membershipsService.getRole(userId, teamId);
        assertEquals(role.getId(), result.getId());
        assertEquals(role.getName(), result.getName());
    }
}
